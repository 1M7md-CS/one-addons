package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoMineModule {

    private static final int STONE_SLOT = 0;
    private static final int CHEST_SLOT = 7;

    private static final int MINE_INTERVAL_MIN = 0;
    private static final int MINE_INTERVAL_MAX = 1;

    private static final int SWITCH_DELAY_MIN = 1;
    private static final int SWITCH_DELAY_MAX = 2;

    private static final int INTERACT_DELAY_MIN = 0;
    private static final int INTERACT_DELAY_MAX = 1;

    private boolean mining = false;
    private int mineTimer = 0;
    private BlockPos lastChestPos = null;

    private int switchTimer = 0;
    private int pendingSlot = -1;

    private int interactTimer = 0;
    private BlockPos pendingChestPos = null;
    private BlockHitResult pendingChestHit = null;

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        if (mc.currentScreen != null) {
            stopMining(mc);
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        tickSlotSwitch(mc);
        tickChestInteract(mc);

        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            stopMining(mc);
            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        var state = mc.world.getBlockState(pos);

        if (state.isOf(Blocks.STONE)) {
            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
            handleStone(mc, pos, hit);
        } else if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)) {
            stopMining(mc);
            handleChest(mc, pos, hit);
        } else {
            stopMining(mc);
            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
        }
    }

    private void handleStone(MinecraftClient mc, BlockPos pos, BlockHitResult hit) {
        scheduleSlotSwitch(mc, STONE_SLOT);

        if (mineTimer > 0) {
            mineTimer--;
            return;
        }

        if (!mining) {
            mc.interactionManager.attackBlock(pos, hit.getSide());
            mining = true;
        } else {
            mc.interactionManager.updateBlockBreakingProgress(pos, hit.getSide());
        }

        if (ThreadLocalRandom.current().nextInt(100) < 85) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        mineTimer = randInt(MINE_INTERVAL_MIN, MINE_INTERVAL_MAX);
    }

    private void handleChest(MinecraftClient mc, BlockPos pos, BlockHitResult hit) {
        if (!pos.equals(lastChestPos) && pendingChestPos == null) {
            scheduleSlotSwitch(mc, CHEST_SLOT);

            if (pendingSlot < 0) {
                pendingChestPos = pos;
                pendingChestHit = hit;
                interactTimer = randInt(INTERACT_DELAY_MIN, INTERACT_DELAY_MAX);
            }
        }
    }

    private void scheduleSlotSwitch(MinecraftClient mc, int slot) {
        if (pendingSlot >= 0) return;
        if (mc.player.getInventory().getSelectedSlot() == slot) return;

        pendingSlot = slot;
        switchTimer = randInt(SWITCH_DELAY_MIN, SWITCH_DELAY_MAX);
    }

    private void tickSlotSwitch(MinecraftClient mc) {
        if (pendingSlot < 0) return;

        if (switchTimer > 0) {
            switchTimer--;
        } else {
            mc.player.getInventory().setSelectedSlot(pendingSlot);
            pendingSlot = -1;
        }
    }

    private void tickChestInteract(MinecraftClient mc) {
        if (pendingChestPos == null) return;

        if (interactTimer > 0) {
            interactTimer--;
        } else {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, pendingChestHit);
            if (ThreadLocalRandom.current().nextInt(100) < 90) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            lastChestPos = pendingChestPos;
            pendingChestPos = null;
            pendingChestHit = null;
        }
    }

    private void stopMining(MinecraftClient mc) {
        if (mining) {
            mc.interactionManager.cancelBlockBreaking();
            mining = false;
            mineTimer = 0;
        }
    }

    private void reset() {
        mining = false;
        mineTimer = 0;
        lastChestPos = null;
        pendingSlot = -1;
        switchTimer = 0;
        pendingChestPos = null;
        pendingChestHit = null;
        interactTimer = 0;
    }

    private static int randInt(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
