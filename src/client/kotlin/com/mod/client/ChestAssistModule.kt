package com.mod.client

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.concurrent.ThreadLocalRandom

class ChestAssistModule {

    companion object {
        private const val INTERACT_DELAY_MIN = 0
        private const val INTERACT_DELAY_MAX = 1
    }

    private var lastChestPos: BlockPos? = null
    private var interactTimer = 0
    private var pendingChestPos: BlockPos? = null
    private var pendingChestHit: BlockHitResult? = null

    fun tick(mc: Minecraft) {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val gameMode = mc.gameMode ?: return

        if (mc.screen != null) {
            pendingChestPos = null
            pendingChestHit = null
            return
        }

        tickChestInteract(gameMode, player)

        val hitResult = mc.hitResult ?: return
        if (hitResult.type != HitResult.Type.BLOCK) {
            lastChestPos = null
            pendingChestPos = null
            pendingChestHit = null
            return
        }

        val hit = hitResult as BlockHitResult
        val pos = hit.blockPos
        val state = level.getBlockState(pos)

        if (state.`is`(Blocks.CHEST)
            || state.`is`(Blocks.TRAPPED_CHEST)
        ) {
            handleChest(pos, hit)
        } else {
            lastChestPos = null
            pendingChestPos = null
            pendingChestHit = null
        }
    }

    private fun tickChestInteract(gameMode: net.minecraft.client.multiplayer.MultiPlayerGameMode, player: net.minecraft.client.player.LocalPlayer) {
        if (pendingChestPos == null) return

        if (interactTimer > 0) {
            interactTimer--
        } else {
            gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                pendingChestHit!!
            )
            lastChestPos = pendingChestPos
            pendingChestPos = null
            pendingChestHit = null
        }
    }

    private fun handleChest(pos: BlockPos, hit: BlockHitResult) {
        if (pos != lastChestPos
            && pendingChestPos == null
        ) {
            pendingChestPos = pos
            pendingChestHit = hit
            interactTimer = randInt(INTERACT_DELAY_MIN, INTERACT_DELAY_MAX)
        }
    }

    private fun randInt(min: Int, max: Int): Int {
        return if (min >= max) min
        else ThreadLocalRandom.current().nextInt(min, max + 1)
    }
}
