package com.mod.client

import com.mod.client.compat.EntityCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ThreadLocalRandom

class FlowerModule {

    companion object {
        private const val TICKS_PER_SECOND = 20.0
        private const val TARGET_CPS = 14
        private const val HARD_MAX_CPS = 18
        private const val FAST_START_UNTIL_CPS = 16
        private const val FAST_START_CREDIT_TICK = 1.0
        private const val STABLE_CREDIT_PER_TICK = TARGET_CPS / TICKS_PER_SECOND
        private const val MAX_CLICK_CREDIT = 2.0
        private const val MAX_CLICKS_PER_TICK = 1

        private const val PLANT_CHECK_RANGE_UP = 3
        private const val TARGET_SWITCH_GRACE_MS = 700L
        private const val REACH_DISTANCE = 6.0
        private const val REACH_DISTANCE_SQ = REACH_DISTANCE * REACH_DISTANCE
        private const val HIT_OFFSET_RANGE = 0.10
    }

    private var clickCredit = 0.0
    private var hadValidTargetLast = false
    private var lastTargetLostTime = 0L

    private val clickTracker = ClickTracker()

    fun tick(mc: Minecraft) {
        val now = System.currentTimeMillis()
        clickTracker.pruneOldClicks(now)

        if (mc.screen != null
            || mc.player == null
            || mc.level == null
            || mc.gameMode == null
        ) {
            stopAimingSoft(now)
            return
        }

        val target = getValidBlockTarget(mc) ?: run { stopAimingSoft(now); return }

        val pos = target.blockPos
        if (!isHarvestableBlock(mc, pos) || !isWithinReach(mc.player!!, pos)) {
            stopAimingSoft(now)
            return
        }

        markValidTarget(now)
        runClickScheduler(mc, mc.player!!, pos, now)
    }

    fun reset(starting: Boolean) {
        clickCredit = if (starting) 1.0 else 0.0
        hadValidTargetLast = false
        lastTargetLostTime = 0L
        clickTracker.reset()
    }

    fun getCurrentCps(): Int = clickTracker.currentCps

    private fun stopAimingSoft(now: Long) {
        if (hadValidTargetLast) lastTargetLostTime = now
        hadValidTargetLast = false
        clickCredit = Math.min(clickCredit, MAX_CLICK_CREDIT)
    }

    private fun markValidTarget(now: Long) {
        if (!hadValidTargetLast) {
            val gap = now - lastTargetLostTime
            if (gap >= 0L && gap <= TARGET_SWITCH_GRACE_MS) clickCredit = MAX_CLICK_CREDIT
        }
        hadValidTargetLast = true
    }

    private fun runClickScheduler(mc: Minecraft, player: LocalPlayer, pos: BlockPos, now: Long) {
        val realCps = clickTracker.currentCps
        if (realCps >= HARD_MAX_CPS) return

        addClickCredit(realCps)

        val clicks = calculateClicksThisTick(realCps)
        for (i in 0 until clicks) {
            performClick(mc, player, pos)
            clickTracker.recordClick(now)
            clickCredit -= 1.0
        }
    }

    private fun addClickCredit(realCps: Int) {
        val inc = if (realCps < FAST_START_UNTIL_CPS) FAST_START_CREDIT_TICK else STABLE_CREDIT_PER_TICK
        clickCredit = Math.min(clickCredit + inc, MAX_CLICK_CREDIT)
    }

    private fun calculateClicksThisTick(realCps: Int): Int {
        val remaining = HARD_MAX_CPS - realCps
        val fromCredit = Math.floor(clickCredit).toInt()
        if (remaining <= 0 || fromCredit <= 0) return 0
        return Math.min(fromCredit, MAX_CLICKS_PER_TICK)
    }

    private fun getValidBlockTarget(mc: Minecraft): BlockHitResult? {
        val hit = mc.hitResult ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null
        return hit as BlockHitResult
    }

    private fun isHarvestableBlock(mc: Minecraft, pos: BlockPos): Boolean {
        return mc.level != null && isTallPlant(mc, pos) && hasTallPlantAbove(mc, pos)
    }

    private fun hasTallPlantAbove(mc: Minecraft, pos: BlockPos): Boolean {
        for (i in 1..PLANT_CHECK_RANGE_UP) {
            if (isTallPlant(mc, pos.above(i))) return true
        }
        return false
    }

    private fun isTallPlant(mc: Minecraft, pos: BlockPos): Boolean {
        return mc.level?.getBlockState(pos)?.block is DoublePlantBlock
    }

    private fun isWithinReach(player: LocalPlayer, pos: BlockPos): Boolean {
        val eyeX = player.x
        val eyeY = EntityCompat.getEyeY(player)
        val eyeZ = player.z
        val dx = eyeX - (pos.x + 0.5)
        val dy = eyeY - (pos.y + 0.5)
        val dz = eyeZ - (pos.z + 0.5)
        return (dx * dx + dy * dy + dz * dz) <= REACH_DISTANCE_SQ
    }

    private fun performClick(mc: Minecraft, player: LocalPlayer, pos: BlockPos) {
        if (mc.gameMode == null) return
        mc.gameMode!!.useItemOn(player, InteractionHand.MAIN_HAND, buildHitResult(pos))
    }

    private fun buildHitResult(pos: BlockPos): BlockHitResult {
        val ox = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE)
        val oy = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE)
        val oz = ThreadLocalRandom.current().nextDouble(-HIT_OFFSET_RANGE, HIT_OFFSET_RANGE)
        return BlockHitResult(
            Vec3(pos.x + 0.5 + ox, pos.y + 0.5 + oy, pos.z + 0.5 + oz),
            Direction.UP, pos, false
        )
    }

    private class ClickTracker {
        companion object {
            private const val CPS_WINDOW_MILLIS = 1_000L
            private const val RING_CAPACITY = 128
            private const val RING_MASK = RING_CAPACITY - 1
        }

        private val timestamps = LongArray(RING_CAPACITY)
        private var head = 0
        private var size = 0

        val currentCps: Int get() = size

        fun recordClick(now: Long) {
            val tail = (head + size) and RING_MASK
            timestamps[tail] = now
            if (size < RING_CAPACITY) size++ else head = (head + 1) and RING_MASK
        }

        fun pruneOldClicks(now: Long) {
            val cutoff = now - CPS_WINDOW_MILLIS
            while (size > 0 && timestamps[head] < cutoff) {
                head = (head + 1) and RING_MASK
                size--
            }
        }

        fun reset() {
            head = 0
            size = 0
        }
    }
}
