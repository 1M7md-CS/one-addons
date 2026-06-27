package com.mod.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class WaypointModule {

    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    private val dataFile: File
    private val waypoints = mutableListOf<Waypoint>()

    init {
        val mc = Minecraft.getInstance()
        dataFile = File(mc.gameDirectory, "config/oneaddons/positions.json")
    }

    fun saveCurrentPosition() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        loadWaypoints()

        val pos = player.blockPosition()
        val nextId = waypoints.size + 1

        waypoints.add(Waypoint(pos.x, pos.y, pos.z, 0, 1, 0, nextId))

        saveWaypoints()

        client.player!!.sendSystemMessage(
            Component.literal("§aSaved waypoint §f#$nextId §aat §f${pos.x} ${pos.y} ${pos.z}")
        )
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
