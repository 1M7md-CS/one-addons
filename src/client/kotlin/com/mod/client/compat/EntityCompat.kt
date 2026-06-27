package com.mod.client.compat

import net.minecraft.client.player.LocalPlayer

object EntityCompat {
    @JvmStatic
    fun getEyeY(player: LocalPlayer): Double = player.y + player.getEyeHeight(player.pose)
}
