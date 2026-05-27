package com.mod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FlowerModule {

    private static final double TICKS_PER_SECOND        = 20.0;
    private static final int    TARGET_CPS              = 14;
    private static final int    HARD_MAX_CPS            = 18;
    private static final int    FAST_START_UNTIL_CPS    = 16;
    private static final double FAST_START_CREDIT_TICK  = 1.0;
    private static final double STABLE_CREDIT_PER_TICK  = TARGET_CPS / TICKS_PER_SECOND;
    private static final double MAX_CLICK_CREDIT        = 2.0;
    private static final int    MAX_CLICKS_PER_TICK     = 1;

    private static final int    PLANT_CHECK_RANGE_UP    = 3;
    private static final long   TARGET_SWITCH_GRACE_MS  = 700L;
    private static final double REACH_DISTANCE          = 6.0;
    private static final double REACH_DISTANCE_SQ       = REACH_DISTANCE * REACH_DISTANCE;
    private static final double HIT_OFFSET_RANGE        = 0.10;

    private double  clickCredit        = 0.0;
    private boolean hadValidTargetLast = false;
    private long    lastTargetLostTime = 0L;

    private final ClickTracker clickTracker = new ClickTracker();

    public void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        clickTracker.pruneOldClicks(now);

        if (mc.currentScreen != null
                || mc.player == null
                || mc.world == null
                || mc.interactionManager == null) {
            stopAimingSoft(now);
            return;
        }

        BlockHitResult target = getValidBlockTarget(mc);
        if (target == null) { stopAimingSoft(now); return; }

        BlockPos pos = target.getBlockPos();
        if (!isHarvestableBlock(mc, pos) || !isWithinReach(mc.player, pos)) {
            stopAimingSoft(now);
            return;
        }

        markValidTarget(now);
        runClickScheduler(mc, mc.player, pos, now);
    }

    public void reset(boolean starting) {
        clickCredit        = starting ? 1.0 : 0.0;
        hadValidTargetLast = false;
        lastTargetLostTime = 0L;
        clickTracker.reset();
    }

    public int getCurrentCps() {
        return clickTracker.getCurrentCps();
    }

    private void stopAimingSoft(long now) {
        if (hadValidTargetLast) lastTargetLostTime = now;
        hadValidTargetLast = false;
        clickCredit = Math.min(clickCredit, MAX_CLICK_CREDIT);
    }

    private void markValidTarget(long now) {
        if (!hadValidTargetLast) {
            long gap = now - lastTargetLostTime;
            if (gap >= 0L && gap <= TARGET_SWITCH_GRACE_MS) clickCredit = MAX_CLICK_CREDIT;
        }
        hadValidTargetLast = true;
    }

    private void runClickScheduler(MinecraftClient mc, ClientPlayerEntity player, BlockPos pos, long now) {
        int realCps = clickTracker.getCurrentCps();
        if (realCps >= HARD_MAX_CPS) return;

        addClickCredit(realCps);

        int clicks = calculateClicksThisTick(realCps);
        for (int i = 0; i < clicks; i++) {
            performClick(mc, player, pos);
            clickTracker.recordClick(now);
            clickCredit -= 1.0;
        }
    }

    private void addClickCredit(int realCps) {
        double inc = realCps < FAST_START_UNTIL_CPS ? FAST_START_CREDIT_TICK : STABLE_CREDIT_PER_TICK;
        clickCredit = Math.min(clickCredit + inc, MAX_CLICK_CREDIT);
    }

    private int calculateClicksThisTick(int realCps) {
        int remaining = HARD_MAX_CPS - realCps;
        int fromCredit = (int) Math.floor(clickCredit);
        if (remaining <= 0 || fromCredit <= 0) return 0;
        return Math.min(fromCredit, MAX_CLICKS_PER_TICK);
    }

    private BlockHitResult getValidBlockTarget(MinecraftClient mc) {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return null;
        return (BlockHitResult) mc.crosshairTarget;
    }

    private boolean isHarvestableBlock(MinecraftClient mc, BlockPos pos) {
        return mc.world != null && isTallPlant(mc, pos) && !hasTallPlantAbove(mc, pos);
    }

    private boolean hasTallPlantAbove(MinecraftClient mc, BlockPos pos) {
        for (int i = 1; i <= PLANT_CHECK_RANGE_UP; i++) {
            if (isTallPlant(mc, pos.up(i))) return true;
        }
        return false;
    }

    private boolean isTallPlant(MinecraftClient mc, BlockPos pos) {
        return mc.world != null && mc.world.getBlockState(pos).getBlock() instanceof TallPlantBlock;
    }

    private boolean isWithinReach(ClientPlayerEntity player, BlockPos pos) {
        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight(player.getPose());
        double eyeZ = player.getZ();
        double dx = eyeX - (pos.getX() + 0.5);
        double dy = eyeY - (pos.getY() + 0.5);
        double dz = eyeZ - (pos.getZ() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= REACH_DISTANCE_SQ;
    }

    private void performClick(MinecraftClient mc, ClientPlayerEntity player, BlockPos pos) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, buildHitResult(pos));
    }

    private BlockHitResult buildHitResult(BlockPos pos) {
        double ox = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        double oy = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        double oz = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        return new BlockHitResult(
                new Vec3d(pos.getX() + 0.5 + ox, pos.getY() + 0.5 + oy, pos.getZ() + 0.5 + oz),
                Direction.UP, pos, false
        );
    }

    private static final class ClickTracker {
        private static final long CPS_WINDOW_MILLIS = 1_000L;
        private static final int  RING_CAPACITY     = 128;
        private static final int  RING_MASK         = RING_CAPACITY - 1;

        private final long[] timestamps = new long[RING_CAPACITY];
        private int head = 0, size = 0;

        void recordClick(long now) {
            int tail = (head + size) & RING_MASK;
            timestamps[tail] = now;
            if (size < RING_CAPACITY) size++; else head = (head + 1) & RING_MASK;
        }

        void pruneOldClicks(long now) {
            long cutoff = now - CPS_WINDOW_MILLIS;
            while (size > 0 && timestamps[head] < cutoff) { head = (head + 1) & RING_MASK; size--; }
        }

        int getCurrentCps() { return size; }
        void reset()         { head = 0; size = 0; }
    }
}
