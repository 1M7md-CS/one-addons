package com.mod.client.compat

import net.minecraft.client.Minecraft
import net.minecraft.world.inventory.ContainerInput

object ContainerCompat {
    @JvmStatic
    fun leftClick(containerId: Int, slot: Int) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return
        gameMode.handleContainerInput(
            containerId,
            slot,
            0,
            ContainerInput.PICKUP,
            player
        )
    }
}
