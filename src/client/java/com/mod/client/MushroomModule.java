package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class MushroomModule {

    private static final int    MUSHROOM_BREAK_COOLDOWN_MIN = 2;
    private static final int    MUSHROOM_BREAK_COOLDOWN_MAX = 5;

    private BlockPos trackedPos    = null;
    private int      breakAttempts = 0;
    private int      breakCooldown = 0;

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            trackedPos = null;
            return;
        }

        if (mc.currentScreen != null) {
            trackedPos = null;
            return;
        }

        if (trackedPos != null) {
            var state = mc.world.getBlockState(trackedPos);

            if (state.isOf(Blocks.RED_MUSHROOM)) {
                if (mc.player.getBlockPos().getSquaredDistance(trackedPos) > 36) {
                    trackedPos = null;
                    return;
                }
                if (breakCooldown > 0) {
                    breakCooldown--;
                    return;
                }
                mc.interactionManager.attackBlock(trackedPos, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                breakAttempts++;
                breakCooldown = ThreadLocalRandom.current().nextInt(
                        MUSHROOM_BREAK_COOLDOWN_MIN, MUSHROOM_BREAK_COOLDOWN_MAX + 1);
                if (breakAttempts > 5 || state.isAir()) {
                    trackedPos = null;
                    breakAttempts = 0;
                    breakCooldown = 0;
                }
                return;
            }

            if (state.isOf(Blocks.BROWN_MUSHROOM)) {
                if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
                    BlockPos newPos = hit.getBlockPos();
                    if (!newPos.equals(trackedPos) && mc.world.getBlockState(newPos).isOf(Blocks.BROWN_MUSHROOM)) {
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

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            if (mc.world.getBlockState(pos).isOf(Blocks.BROWN_MUSHROOM)) {
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
