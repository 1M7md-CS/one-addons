package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.concurrent.ThreadLocalRandom

object ChestAssist : Module(
    name = "Chest Assist",
    description = "Automatically interacts with chests with a configurable delay.",
    category = Categories.ONEADDONS
) {
    private const val INTERACT_DELAY_MIN = 0
    private const val INTERACT_DELAY_MAX = 1

    private var lastChestPos: BlockPos? = null
    private var interactTimer = 0
    private var pendingChestPos: BlockPos? = null
    private var pendingChestHit: BlockHitResult? = null

    init {
        on<TickEvent.Start> {
            val player = mc.player ?: return@on
            val level = mc.level ?: return@on
            val gameMode = mc.gameMode ?: return@on

            if (mc.screen != null) {
                pendingChestPos = null
                pendingChestHit = null
                return@on
            }

            if (pendingChestPos != null) {
                if (interactTimer > 0) {
                    interactTimer--
                } else {
                    gameMode.useItemOn(player, InteractionHand.MAIN_HAND, pendingChestHit!!)
                    lastChestPos = pendingChestPos
                    pendingChestPos = null
                    pendingChestHit = null
                }
                return@on
            }

            val hitResult = mc.hitResult ?: return@on
            if (hitResult.type != HitResult.Type.BLOCK) {
                lastChestPos = null
                pendingChestPos = null
                pendingChestHit = null
                return@on
            }

            val hit = hitResult as BlockHitResult
            val pos = hit.blockPos
            val state = level.getBlockState(pos)

            if (state.`is`(Blocks.CHEST) || state.`is`(Blocks.TRAPPED_CHEST)) {
                if (pos != lastChestPos && pendingChestPos == null) {
                    pendingChestPos = pos
                    pendingChestHit = hit
                    interactTimer = if (INTERACT_DELAY_MIN >= INTERACT_DELAY_MAX) INTERACT_DELAY_MIN
                        else ThreadLocalRandom.current().nextInt(INTERACT_DELAY_MIN, INTERACT_DELAY_MAX + 1)
                }
            } else {
                lastChestPos = null
                pendingChestPos = null
                pendingChestHit = null
            }
        }
    }
}
