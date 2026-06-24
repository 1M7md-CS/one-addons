package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MushroomModule {

    private static final int    MUSHROOM_BREAK_COOLDOWN_MIN = 2;
    private static final int    MUSHROOM_BREAK_COOLDOWN_MAX = 5;

    private BlockPos trackedPos    = null;
    private int      breakAttempts = 0;
    private int      breakCooldown = 0;

    public void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            trackedPos = null;
            return;
        }

        if (mc.screen != null) {
            trackedPos = null;
            return;
        }

        if (trackedPos != null) {
            var state = mc.level.getBlockState(trackedPos);

            if (state.is(Blocks.RED_MUSHROOM)) {
                if (mc.player.blockPosition().distSqr(trackedPos) > 36) {
                    trackedPos = null;
                    return;
                }
                if (breakCooldown > 0) {
                    breakCooldown--;
                    return;
                }
                breakAttempts++;
                mc.gameMode.startDestroyBlock(trackedPos, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                breakCooldown = ThreadLocalRandom.current().nextInt(
                        MUSHROOM_BREAK_COOLDOWN_MIN, MUSHROOM_BREAK_COOLDOWN_MAX + 1);
                var stateAfter = mc.level.getBlockState(trackedPos);
                if (breakAttempts > 5 || stateAfter.isAir()) {
                    trackedPos = null;
                    breakAttempts = 0;
                    breakCooldown = 0;
                }
                return;
            }

            if (state.is(Blocks.BROWN_MUSHROOM)) {
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) mc.hitResult;
                    BlockPos newPos = hit.getBlockPos();
                    if (!newPos.equals(trackedPos) && mc.level.getBlockState(newPos).is(Blocks.BROWN_MUSHROOM)) {
                        trackedPos = newPos;
                        breakAttempts = 0;
                    }
                }
            } else {
                trackedPos = null;
                breakAttempts = 0;
            }
            return;
        }

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.hitResult;
            BlockPos pos = hit.getBlockPos();
            if (mc.level.getBlockState(pos).is(Blocks.BROWN_MUSHROOM)) {
                trackedPos = pos;
                breakAttempts = 0;
            }
        }
    }

    public void resetTrackedPos() {
        trackedPos = null;
        breakAttempts = 0;
        breakCooldown = 0;
    }

    public boolean isTracking() {
        return trackedPos != null;
    }
}
