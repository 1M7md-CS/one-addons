package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ThreadLocalRandom

object Flower : Module(
    name = "Flower",
    description = "Auto-breaks tall flowers with configurable CPS.",
    category = Categories.ONEADDONS
) {
    private val targetCps by NumberSetting("Target CPS", 14, 1, 20, 1, desc = "Target clicks per second.")
    private val hardMaxCps by NumberSetting("Hard Max CPS", 18, 1, 25, 1, desc = "Hard cap on clicks per second.")
    private val reachDistance by NumberSetting("Reach", 6.0, 3.0, 8.0, 0.5, unit = "blocks", desc = "Max distance to target.")

    private const val TICKS_PER_SECOND = 20.0
    private const val FAST_START_UNTIL_CPS = 16
    private const val FAST_START_CREDIT_TICK = 1.0
    private const val STABLE_CREDIT_PER_TICK = 14.0 / TICKS_PER_SECOND
    private const val MAX_CLICK_CREDIT = 2.0
    private const val MAX_CLICKS_PER_TICK = 1
    private const val PLANT_CHECK_RANGE_UP = 3
    private const val TARGET_SWITCH_GRACE_MS = 700L
    private const val HIT_OFFSET_RANGE = 0.10

    private var clickCredit = 0.0
    private var hadValidTargetLast = false
    private var lastTargetLostTime = 0L
    private val clickTracker = ClickTracker()

    init {
        on<TickEvent.Start> {
            val now = System.currentTimeMillis()
            clickTracker.pruneOldClicks(now)
            val player = mc.player ?: run { stopAimingSoft(now); return@on }
            val level = mc.level ?: run { stopAimingSoft(now); return@on }
            val gameMode = mc.gameMode ?: run { stopAimingSoft(now); return@on }

            if (mc.screen != null) { stopAimingSoft(now); return@on }

            val target = getValidBlockTarget() ?: run { stopAimingSoft(now); return@on }
            val pos = target.blockPos
            if (!isHarvestableBlock(pos) || !isWithinReach(player, pos)) {
                stopAimingSoft(now)
                return@on
            }

            markValidTarget(now)
            val realCps = clickTracker.currentCps
            if (realCps >= hardMaxCps) return@on

            addClickCredit(realCps)
            val clicks = calculateClicksThisTick(realCps)
            for (i in 0 until clicks) {
                performClick(gameMode, player, pos)
                clickTracker.recordClick(now)
                clickCredit -= 1.0
            }
        }
    }

    private fun stopAimingSoft(now: Long) {
        if (hadValidTargetLast) lastTargetLostTime = now
        hadValidTargetLast = false
        clickCredit = minOf(clickCredit, MAX_CLICK_CREDIT)
    }

    private fun markValidTarget(now: Long) {
        if (!hadValidTargetLast) {
            val gap = now - lastTargetLostTime
            if (gap >= 0L && gap <= TARGET_SWITCH_GRACE_MS) clickCredit = MAX_CLICK_CREDIT
        }
        hadValidTargetLast = true
    }

    private fun addClickCredit(realCps: Int) {
        val inc = if (realCps < FAST_START_UNTIL_CPS) FAST_START_CREDIT_TICK else STABLE_CREDIT_PER_TICK
        clickCredit = minOf(clickCredit + inc, MAX_CLICK_CREDIT)
    }

    private fun calculateClicksThisTick(realCps: Int): Int {
        val remaining = hardMaxCps - realCps
        val fromCredit = kotlin.math.floor(clickCredit).toInt()
        if (remaining <= 0 || fromCredit <= 0) return 0
        return minOf(fromCredit, MAX_CLICKS_PER_TICK)
    }

    private fun getValidBlockTarget(): BlockHitResult? {
        val hit = mc.hitResult ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null
        return hit as BlockHitResult
    }

    private fun isHarvestableBlock(pos: BlockPos): Boolean {
        return mc.level != null && isTallPlant(pos) && hasTallPlantAbove(pos)
    }

    private fun hasTallPlantAbove(pos: BlockPos): Boolean {
        for (i in 1..PLANT_CHECK_RANGE_UP) {
            if (isTallPlant(pos.above(i))) return true
        }
        return false
    }

    private fun isTallPlant(pos: BlockPos): Boolean {
        return mc.level?.getBlockState(pos)?.block is DoublePlantBlock
    }

    private fun isWithinReach(player: LocalPlayer, pos: BlockPos): Boolean {
        val d = reachDistance
        val dSq = d * d
        val eyeX = player.x
        val eyeY = player.eyeY
        val eyeZ = player.z
        val dx = eyeX - (pos.x + 0.5)
        val dy = eyeY - (pos.y + 0.5)
        val dz = eyeZ - (pos.z + 0.5)
        return (dx * dx + dy * dy + dz * dz) <= dSq
    }

    private fun performClick(gameMode: net.minecraft.client.multiplayer.MultiPlayerGameMode, player: LocalPlayer, pos: BlockPos) {
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, buildHitResult(pos))
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
