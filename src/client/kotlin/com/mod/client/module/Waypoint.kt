package com.mod.client.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mod.client.category.Categories
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.network.chat.Component
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object Waypoint : Module(
    name = "Waypoint",
    description = "Save your current position as a waypoint with a keybind.",
    category = Categories.ONEADDONS
) {
    private val saveKey by KeybindSetting("Save Key", InputConstants.UNKNOWN, desc = "Press to save current position.")

    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dataFile = File(com.odtheking.odin.OdinMod.mc.gameDirectory, "config/oneaddons/positions.json")
    private val waypoints = mutableListOf<Waypoint>()
    private var prevKeyState = false

    init {
        on<TickEvent.Start> {
            val key = saveKey
            if (key == InputConstants.UNKNOWN) return@on

            val window = mc.window.handle()
            val nowPressed = org.lwjgl.glfw.GLFW.glfwGetKey(window, key.value) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            if (nowPressed && !prevKeyState) {
                val player = mc.player ?: return@on
                loadWaypoints()
                val pos = player.blockPosition()
                val nextId = waypoints.size + 1
                waypoints.add(Waypoint(pos.x, pos.y, pos.z, 0, 1, 0, nextId))
                saveWaypoints()
                player.sendSystemMessage(
                    Component.literal("§aSaved waypoint §f#$nextId §aat §f${pos.x} ${pos.y} ${pos.z}")
                )
            }
            prevKeyState = nowPressed
        }
    }

    private fun loadWaypoints() {
        waypoints.clear()
        if (!dataFile.exists()) return
        try {
            FileReader(dataFile).use { reader ->
                val type = object : TypeToken<List<Waypoint>>() {}.type
                val loaded: List<Waypoint>? = GSON.fromJson(reader, type)
                if (loaded != null) waypoints.addAll(loaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveWaypoints() {
        dataFile.parentFile.mkdirs()
        try {
            FileWriter(dataFile).use { writer ->
                GSON.toJson(waypoints, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class Waypoint {
        @JvmField var x = 0
        @JvmField var y = 0
        @JvmField var z = 0
        @JvmField var r = 0
        @JvmField var g = 0
        @JvmField var b = 0
        @JvmField var options: Options? = null

        constructor()

        constructor(x: Int, y: Int, z: Int, r: Int, g: Int, b: Int, name: Int) {
            this.x = x
            this.y = y
            this.z = z
            this.r = r
            this.g = g
            this.b = b
            this.options = Options(name)
        }
    }

    private class Options {
        @JvmField var name = 0

        constructor()

        constructor(name: Int) {
            this.name = name
        }
    }
}
