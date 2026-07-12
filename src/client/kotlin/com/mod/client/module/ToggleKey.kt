package com.mod.client.module

import com.mod.client.category.Categories
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object ToggleKey : Module(
    name = "Toggle Key",
    description = "Toggle a key on/off with a single press. Press once to hold, press again to release.",
    category = Categories.ONEADDONS
) {
    private val toggleKey by KeybindSetting("Toggle Key", InputConstants.UNKNOWN, desc = "Press to toggle on/off.")

    private var toggled = false
    private var prevKeyState = false

    init {
        on<TickEvent.Start> {
            if (!enabled) return@on
            val key = toggleKey
            if (key == InputConstants.UNKNOWN) return@on

            if (mc.screen != null) {
                if (toggled) {
                    findKeyMapping(key)?.let { setKeyMappingDown(it, false) }
                }
                prevKeyState = GLFW.glfwGetKey(mc.window.handle(), key.value) == GLFW.GLFW_PRESS
                return@on
            }

            val window = mc.window.handle()
            val nowPressed = GLFW.glfwGetKey(window, key.value) == GLFW.GLFW_PRESS

            if (nowPressed && !prevKeyState) {
                toggled = !toggled
                mc.player?.sendSystemMessage(
                    Component.literal(
                        if (toggled) "§aToggle Key: §fON"
                        else "§cToggle Key: §fOFF"
                    )
                )
            }
            prevKeyState = nowPressed

            findKeyMapping(key)?.let { setKeyMappingDown(it, toggled) }
        }

        on<TickEvent.End> {
            if (!enabled) return@on
            val key = toggleKey
            if (key == InputConstants.UNKNOWN) return@on

            if (mc.screen != null) return@on

            findKeyMapping(key)?.let { setKeyMappingDown(it, toggled) }
        }
    }

    override fun onDisable() {
        val key = toggleKey
        if (key != InputConstants.UNKNOWN && toggled) {
            findKeyMapping(key)?.let { setKeyMappingDown(it, false) }
        }
        toggled = false
        prevKeyState = false
    }

    private fun findKeyMapping(targetKey: InputConstants.Key): KeyMapping? {
        return mc.options.keyMappings.firstOrNull { km ->
            try {
                val field = KeyMapping::class.java.getDeclaredField("key")
                field.isAccessible = true
                val key = field.get(km) as InputConstants.Key
                key.value == targetKey.value
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun setKeyMappingDown(km: KeyMapping, down: Boolean) {
        try {
            val isDownField = KeyMapping::class.java.getDeclaredField("isDown")
            isDownField.isAccessible = true
            isDownField.setBoolean(km, down)
        } catch (_: Exception) {
        }
        if (!down) {
            try {
                val clickField = KeyMapping::class.java.getDeclaredField("clickCount")
                clickField.isAccessible = true
                clickField.setInt(km, 0)
            } catch (_: Exception) {
            }
            try {
                val wasDownField = KeyMapping::class.java.getDeclaredField("wasDown")
                wasDownField.isAccessible = true
                wasDownField.setBoolean(km, false)
            } catch (_: Exception) {
            }
        }
    }
}
