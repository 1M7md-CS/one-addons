package com.mod.client.compat

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

interface ScreenGraphics {
    fun fill(x1: Int, y1: Int, x2: Int, y2: Int, color: Int)
    fun text(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean)
    fun enableScissor(x1: Int, y1: Int, x2: Int, y2: Int)
    fun disableScissor()
}
