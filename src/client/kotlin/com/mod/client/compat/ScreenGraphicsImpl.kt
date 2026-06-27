package com.mod.client.compat

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

class ScreenGraphicsImpl(private val inner: GuiGraphicsExtractor) : ScreenGraphics {
    override fun fill(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        inner.fill(x1, y1, x2, y2, color)
    }

    override fun text(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean) {
        inner.text(font, text, x, y, color, shadow)
    }

    override fun enableScissor(x1: Int, y1: Int, x2: Int, y2: Int) {
        inner.enableScissor(x1, y1, x2, y2)
    }

    override fun disableScissor() {
        inner.disableScissor()
    }
}
