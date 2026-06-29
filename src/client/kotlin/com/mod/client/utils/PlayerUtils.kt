package com.mod.client.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.world.inventory.ContainerInput

fun guiClick(id: Int, index: Int, button: Int = 0, clickType: ContainerInput = ContainerInput.PICKUP) {
    val player = mc.player ?: return
    mc.gameMode?.handleContainerInput(id, index, button, clickType, player)
}
