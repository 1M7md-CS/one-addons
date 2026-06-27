package com.mod.client

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.concurrent.ThreadLocalRandom

class MushroomModule {

    companion object {
        private const val MUSHROOM_BREAK_COOLDOWN_MIN = 2
        private const val MUSHROOM_BREAK_COOLDOWN_MAX = 5
    }

    private var trackedPos: BlockPos? = null
    private var breakAttempts = 0
    private var breakCooldown = 0

    fun tick(mc: Minecraft) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            trackedPos = null
            return
        }

        if (mc.screen != null) {
            trackedPos = null
            return
        }

        if (trackedPos != null) {
            val pos = trackedPos!!
            val state = mc.level!!.getBlockState(pos)

            if (state.`is`(Blocks.RED_MUSHROOM)) {
                if (mc.player!!.blockPosition().distSqr(pos) > 36) {
                    trackedPos = null
                    return
                }
                if (breakCooldown > 0) {
                    breakCooldown--
                    return
                }
                breakAttempts++
                mc.gameMode!!.startDestroyBlock(pos, Direction.UP)
                mc.player!!.swing(InteractionHand.MAIN_HAND)
                breakCooldown = ThreadLocalRandom.current().nextInt(
                    MUSHROOM_BREAK_COOLDOWN_MIN, MUSHROOM_BREAK_COOLDOWN_MAX + 1
                )
                val stateAfter = mc.level!!.getBlockState(pos)
                if (breakAttempts > 5 || stateAfter.isAir) {
                    trackedPos = null
                    breakAttempts = 0
                    breakCooldown = 0
                }
                return
            }

            if (state.`is`(Blocks.BROWN_MUSHROOM)) {
                val hitResult = mc.hitResult
                if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
                    val hit = hitResult as BlockHitResult
                    val newPos = hit.blockPos
                    if (newPos != pos && mc.level!!.getBlockState(newPos).`is`(Blocks.BROWN_MUSHROOM)) {
                        trackedPos = newPos
                        breakAttempts = 0
                    }
                }
            } else {
                trackedPos = null
                breakAttempts = 0
            }
            return
        }

        val hitResult = mc.hitResult
        if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
            val hit = hitResult as BlockHitResult
            val pos = hit.blockPos
            if (mc.level!!.getBlockState(pos).`is`(Blocks.BROWN_MUSHROOM)) {
                trackedPos = pos
                breakAttempts = 0
            }
        }
    }

    fun resetTrackedPos() {
        trackedPos = null
        breakAttempts = 0
        breakCooldown = 0
    }

    fun isTracking(): Boolean = trackedPos != null
}
