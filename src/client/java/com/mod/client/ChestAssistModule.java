package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ChestAssistModule {

    private static final int INTERACT_DELAY_MIN = 0;
    private static final int INTERACT_DELAY_MAX = 1;

    private BlockPos lastChestPos = null;

    private int interactTimer = 0;
    private BlockPos pendingChestPos = null;
    private BlockHitResult pendingChestHit = null;

    public void tick(Minecraft mc) {

        if (mc.player == null
                || mc.level == null
                || mc.gameMode == null) {

            reset();
            return;
        }

        if (mc.screen != null) {
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        tickChestInteract(mc);

        if (mc.hitResult == null
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {

            lastChestPos = null;
            pendingChestPos = null;
            pendingChestHit = null;
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.hitResult;

        BlockPos pos = hit.getBlockPos();

        var state = mc.level.getBlockState(pos);

        if (state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)) {

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

    private void tickChestInteract(Minecraft mc) {

        if (pendingChestPos == null) {
            return;
        }

        if (interactTimer > 0) {

            interactTimer--;

        } else {

            mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
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
