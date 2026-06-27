package com.mod.client.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object ScreenCompat {
    @JvmStatic
    fun wrap(ctx: GuiGraphicsExtractor): ScreenGraphics = ScreenGraphicsImpl(ctx)

    @JvmStatic
    fun extract(event: KeyEvent): KeyEventData = KeyEventData(event.key(), event.scancode(), event.modifiers())

    @JvmStatic
    fun extract(event: MouseButtonEvent, doubleClick: Boolean): MouseEventData =
        MouseEventData(event.x(), event.y(), event.button(), doubleClick)

    @JvmStatic
    fun keyName(code: Int): String {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "None"
        val key = InputConstants.getKey(KeyEvent(code, 0, 0))
        return key.displayName.string
    }

    data class KeyEventData(val keyCode: Int, val scanCode: Int, val modifiers: Int)
    data class MouseEventData(val x: Double, val y: Double, val button: Int, val doubleClick: Boolean)
}
