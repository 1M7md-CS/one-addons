package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ChestAssistModule {

    private static final int INTERACT_DELAY_MIN = 0;
    private static final int INTERACT_DELAY_MAX = 1;

    private BlockPos lastChestPos = null;

    private int interactTimer = 0;
    private BlockPos pendingChestPos = null;
    private BlockHitResult pendingChestHit = null;

    public void tick(MinecraftClient mc) {

        if (mc.player == null
                || mc.world == null
                || mc.interactionManager == null) {

            reset();
            return;
        }

        if (mc.currentScreen != null) {
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        tickChestInteract(mc);

        if (mc.crosshairTarget == null
                || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {

            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;

        BlockPos pos = hit.getBlockPos();

        var state = mc.world.getBlockState(pos);

        if (state.isOf(Blocks.CHEST)
                || state.isOf(Blocks.TRAPPED_CHEST)) {

            handleChest(pos, hit);

        } else {

            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
        }
    }

    private void handleChest(BlockPos pos, BlockHitResult hit) {

        if (!pos.equals(lastChestPos)
                && pendingChestPos == null) {

            pendingChestPos = pos;
            pendingChestHit = hit;

            interactTimer = randInt(
                    INTERACT_DELAY_MIN,
                    INTERACT_DELAY_MAX
            );
        }
    }

    private void tickChestInteract(MinecraftClient mc) {

        if (pendingChestPos == null) {
            return;
        }

        if (interactTimer > 0) {

            interactTimer--;

        } else {

            mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    pendingChestHit
            );

            lastChestPos = pendingChestPos;

            pendingChestPos = null;
            pendingChestHit = null;
        }
    }

    private void reset() {

        lastChestPos = null;

        pendingChestPos = null;
        pendingChestHit = null;

        interactTimer = 0;
    }

    private static int randInt(int min, int max) {

        if (min >= max) {
            return min;
        }

        return ThreadLocalRandom.current()
                .nextInt(min, max + 1);
    }
}