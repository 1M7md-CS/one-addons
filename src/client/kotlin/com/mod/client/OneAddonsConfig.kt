package com.mod.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object OneAddonsConfig {

    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val FILE = File(Minecraft.getInstance().gameDirectory, "config/oneaddons/config.json")

    private var loaded = false

    @JvmStatic
    fun load() {
        if (loaded) return
        if (!FILE.exists()) {
            save()
            loaded = true
            return
        }
        try {
            FileReader(FILE).use { reader ->
                val data = GSON.fromJson(reader, Data::class.java) ?: run {
                    save()
                    loaded = true
                    return
                }
                OneAddons.flowerEnabled = data.flowerEnabled
                OneAddons.mushroomEnabled = data.mushroomEnabled
                OneAddons.enchantingEnabled = data.enchantingEnabled
                OneAddons.autoClose = data.autoClose
                OneAddons.closeChronoEnabled = data.closeChronoEnabled
                OneAddons.closeUltraEnabled = data.closeUltraEnabled
                OneAddons.closeCountChronomatron = data.closeCountChronomatron
                OneAddons.closeCountUltrasequencer = data.closeCountUltrasequencer
                OneAddons.chestAssistEnabled = data.chestAssistEnabled
                OneAddons.waypointEnabled = data.waypointEnabled
                OneAddons.swapAssistEnabled = data.swapAssistEnabled
                OneAddons.placeOnPositionEnabled = data.placeOnPositionEnabled
                OneAddons.keyMakerEnabled = data.keyMakerEnabled
                OneAddons.keyMakerMode = KeyMode.valueOf(data.keyMakerMode)
                OneAddons.keyMakerClickDelay = data.keyMakerClickDelay
                OneAddons.waypointKeyCode = data.waypointKeyCode

                OneAddons.swapAssistModule.entries.clear()
                for (sed in data.swapEntries) {
                    OneAddons.swapAssistModule.addEntry(sed.triggerSlot, sed.triggerInteract, sed.targetSlot, sed.targetInteract, sed.enabled)
                }

                OneAddons.placeOnPositionModule.entries.clear()
                for (ped in data.placeEntries) {
                    OneAddons.placeOnPositionModule.addEntry(ped.placeSlot, ped.placeInteract, ped.restoreSlot, ped.restoreInteract, ped.enabled)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loaded = true
    }

    @JvmStatic
    fun save() {
        FILE.parentFile.mkdirs()
        val data = Data()
        data.flowerEnabled = OneAddons.flowerEnabled
        data.mushroomEnabled = OneAddons.mushroomEnabled
        data.enchantingEnabled = OneAddons.enchantingEnabled
        data.autoClose = OneAddons.autoClose
        data.closeChronoEnabled = OneAddons.closeChronoEnabled
        data.closeUltraEnabled = OneAddons.closeUltraEnabled
        data.closeCountChronomatron = OneAddons.closeCountChronomatron
        data.closeCountUltrasequencer = OneAddons.closeCountUltrasequencer
        data.chestAssistEnabled = OneAddons.chestAssistEnabled
        data.waypointEnabled = OneAddons.waypointEnabled
        data.swapAssistEnabled = OneAddons.swapAssistEnabled
        data.placeOnPositionEnabled = OneAddons.placeOnPositionEnabled
        data.keyMakerEnabled = OneAddons.keyMakerEnabled
        data.keyMakerMode = OneAddons.keyMakerMode.name
        data.keyMakerClickDelay = OneAddons.keyMakerClickDelay
        data.waypointKeyCode = OneAddons.waypointKeyCode

        data.swapEntries = ArrayList()
        for (e in OneAddons.swapAssistModule.entries) {
            data.swapEntries.add(SwapEntryData(e.triggerSlot, e.triggerInteract, e.targetSlot, e.targetInteract, e.enabled))
        }

        data.placeEntries = ArrayList()
        for (e in OneAddons.placeOnPositionModule.entries) {
            data.placeEntries.add(PlaceEntryData(e.placeSlot, e.placeInteract, e.restoreSlot, e.restoreInteract, e.enabled))
        }

        try {
            FileWriter(FILE).use { writer ->
                GSON.toJson(data, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class Data {
        @JvmField var flowerEnabled = false
        @JvmField var mushroomEnabled = false
        @JvmField var enchantingEnabled = false
        @JvmField var autoClose = false
        @JvmField var closeChronoEnabled = true
        @JvmField var closeUltraEnabled = true
        @JvmField var closeCountChronomatron = 14
        @JvmField var closeCountUltrasequencer = 14
        @JvmField var chestAssistEnabled = false
        @JvmField var waypointEnabled = false
        @JvmField var swapAssistEnabled = false
        @JvmField var placeOnPositionEnabled = false
        @JvmField var keyMakerEnabled = false
        @JvmField var keyMakerMode = "TUNGSTEN"
        @JvmField var keyMakerClickDelay = 500
        @JvmField var waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN
        @JvmField var swapEntries: MutableList<SwapEntryData> = ArrayList()
        @JvmField var placeEntries: MutableList<PlaceEntryData> = ArrayList()
    }

    private class SwapEntryData {
        @JvmField var triggerSlot = 0
        @JvmField var triggerInteract = false
        @JvmField var targetSlot = 0
        @JvmField var targetInteract = false
        @JvmField var enabled = true

        constructor()

        constructor(triggerSlot: Int, triggerInteract: Boolean, targetSlot: Int, targetInteract: Boolean, enabled: Boolean) {
            this.triggerSlot = triggerSlot
            this.triggerInteract = triggerInteract
            this.targetSlot = targetSlot
            this.targetInteract = targetInteract
            this.enabled = enabled
        }
    }

    private class PlaceEntryData {
        @JvmField var placeSlot = 0
        @JvmField var placeInteract = false
        @JvmField var restoreSlot = 0
        @JvmField var restoreInteract = false
        @JvmField var enabled = true

        constructor()

        constructor(placeSlot: Int, placeInteract: Boolean, restoreSlot: Int, restoreInteract: Boolean, enabled: Boolean) {
            this.placeSlot = placeSlot
            this.placeInteract = placeInteract
            this.restoreSlot = restoreSlot
            this.restoreInteract = restoreInteract
            this.enabled = enabled
        }
    }
}
