package com.mod.client.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

object ScreenCompat {
    @JvmStatic
    fun wrap(ctx: GuiGraphicsExtractor): ScreenGraphics = ScreenGraphicsImpl(ctx)

    @JvmStatic
    fun keyName(code: Int): String {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "None"
        val key = InputConstants.getKey(KeyEvent(code, 0, 0))
        return key.displayName.string
    }

}
