package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.concurrent.ThreadLocalRandom

object Mushroom : Module(
    name = "Mushroom",
    description = "Auto-breaks red and brown mushrooms.",
    category = Categories.ONEADDONS
) {
    private const val MUSHROOM_BREAK_COOLDOWN_MIN = 2
    private const val MUSHROOM_BREAK_COOLDOWN_MAX = 5

    private var trackedPos: BlockPos? = null
    private var breakAttempts = 0
    private var breakCooldown = 0

    init {
        on<TickEvent.Start> {
            val player = mc.player ?: run { trackedPos = null; return@on }
            val level = mc.level ?: run { trackedPos = null; return@on }
            val gameMode = mc.gameMode ?: run { trackedPos = null; return@on }

            if (mc.screen != null) {
                trackedPos = null
                return@on
            }

            if (trackedPos != null) {
                val pos = trackedPos!!
                val state = level.getBlockState(pos)

                if (state.`is`(Blocks.RED_MUSHROOM)) {
                    if (player.blockPosition().distSqr(pos) > 36) {
                        trackedPos = null
                        return@on
                    }
                    if (breakCooldown > 0) {
                        breakCooldown--
                        return@on
                    }
                    breakAttempts++
                    gameMode.startDestroyBlock(pos, Direction.UP)
                    player.swing(InteractionHand.MAIN_HAND)
                    breakCooldown = ThreadLocalRandom.current().nextInt(
                        MUSHROOM_BREAK_COOLDOWN_MIN, MUSHROOM_BREAK_COOLDOWN_MAX + 1
                    )
                    val stateAfter = level.getBlockState(pos)
                    if (breakAttempts > 5 || stateAfter.isAir) {
                        trackedPos = null
                        breakAttempts = 0
                        breakCooldown = 0
                    }
                    return@on
                }

                if (state.`is`(Blocks.BROWN_MUSHROOM)) {
                    val hitResult = mc.hitResult
                    if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
                        val hit = hitResult as BlockHitResult
                        val newPos = hit.blockPos
                        if (newPos != pos && level.getBlockState(newPos).`is`(Blocks.BROWN_MUSHROOM)) {
                            trackedPos = newPos
                            breakAttempts = 0
                        }
                    }
                } else {
                    trackedPos = null
                    breakAttempts = 0
                }
                return@on
            }

            val hitResult = mc.hitResult
            if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
                val hit = hitResult as BlockHitResult
                val pos = hit.blockPos
                if (level.getBlockState(pos).`is`(Blocks.BROWN_MUSHROOM)) {
                    trackedPos = pos
                    breakAttempts = 0
                }
            }
        }
    }
}
