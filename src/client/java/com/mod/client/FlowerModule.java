package com.mod.client;

import com.mod.client.compat.EntityCompat;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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

    public void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        clickTracker.pruneOldClicks(now);

        if (mc.screen != null
                || mc.player == null
                || mc.level == null
                || mc.gameMode == null) {
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

    private void runClickScheduler(Minecraft mc, LocalPlayer player, BlockPos pos, long now) {
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

    private BlockHitResult getValidBlockTarget(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        return (BlockHitResult) mc.hitResult;
    }

    private boolean isHarvestableBlock(Minecraft mc, BlockPos pos) {
        return mc.level != null && isTallPlant(mc, pos) && hasTallPlantAbove(mc, pos);
    }

    private boolean hasTallPlantAbove(Minecraft mc, BlockPos pos) {
        for (int i = 1; i <= PLANT_CHECK_RANGE_UP; i++) {
            if (isTallPlant(mc, pos.above(i))) return true;
        }
        return false;
    }

    private boolean isTallPlant(Minecraft mc, BlockPos pos) {
        return mc.level != null && mc.level.getBlockState(pos).getBlock() instanceof DoublePlantBlock;
    }

    private boolean isWithinReach(LocalPlayer player, BlockPos pos) {
        double eyeX = player.getX();
        double eyeY = EntityCompat.getEyeY(player);
        double eyeZ = player.getZ();
        double dx = eyeX - (pos.getX() + 0.5);
        double dy = eyeY - (pos.getY() + 0.5);
        double dz = eyeZ - (pos.getZ() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= REACH_DISTANCE_SQ;
    }

    private void performClick(Minecraft mc, LocalPlayer player, BlockPos pos) {
        if (mc.gameMode == null) return;
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, buildHitResult(pos));
    }

    private BlockHitResult buildHitResult(BlockPos pos) {
        double ox = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        double oy = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        double oz = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE);
        return new BlockHitResult(
                new Vec3(pos.getX() + 0.5 + ox, pos.getY() + 0.5 + oy, pos.getZ() + 0.5 + oz),
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
