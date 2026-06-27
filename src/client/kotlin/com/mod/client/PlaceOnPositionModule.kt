package com.mod.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import java.io.File
import java.io.FileReader
import java.util.concurrent.ThreadLocalRandom

class PlaceOnPositionModule {

    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private const val RANGE = 5

        private fun randDelay(min: Int, max: Int): Int {
            return ThreadLocalRandom.current().nextInt(min, max + 1)
        }
    }

    private val dataFile: File
    private val waypoints = mutableListOf<Waypoint>()
    private var waypointsLoaded = false

    val entries = mutableListOf<PlaceEntry>()

    private var step = 0
    private var stepEntry = 0
    private var delay = 0
    private var currentIndex = 0
    private var lastWorld: Level? = null
    private var currentTarget: Waypoint? = null

    init {
        val mc = Minecraft.getInstance()
        dataFile = File(mc.gameDirectory, "config/oneaddons/placeonposition.json")
    }

    fun tick(mc: Minecraft) {
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return

        var hasEnabled = false
        for (e in entries) {
            if (e.enabled) {
                hasEnabled = true
                break
            }
        }
        if (!hasEnabled) {
            step = 0
            stepEntry = entries.size
            currentTarget = null
            return
        }

        if (currentTarget != null && !isNear(player.blockPosition(), currentTarget!!)) {
            if (step > 0) {
                currentTarget = null
                return
            }
            step = 0
            stepEntry = entries.size
            currentTarget = null
            return
        }

        if (step > 0) {
            if (delay > 0) {
                delay--
                return
            }

            if (stepEntry >= entries.size) {
                step = 0
                stepEntry = entries.size
                return
            }

            val e = entries[stepEntry]
            if (!e.enabled) {
                stepEntry++
                step = if (stepEntry < entries.size) 1 else 0
                return
            }

            if (step == 1) {
                player.inventory.selectedSlot = e.placeSlot
                delay = 1
                step = 2
                return
            }
            if (step == 2) {
                if (e.placeInteract) {
                    gameMode.useItem(player, InteractionHand.MAIN_HAND)
                }
                delay = randDelay(1, 2)
                step = 3
                return
            }
            if (step == 3) {
                player.inventory.selectedSlot = e.restoreSlot
                if (e.restoreInteract) {
                    delay = 1
                    step = 4
                } else {
                    stepEntry++
                    step = if (stepEntry < entries.size) 1 else 0
                }
                return
            }
            if (step == 4) {
                if (e.restoreInteract) {
                    gameMode.useItem(player, InteractionHand.MAIN_HAND)
                }
                stepEntry++
                step = if (stepEntry < entries.size) 1 else 0
                return
            }
        }

        if (step == 0 && currentTarget == null) {
            stepEntry = 0

            if (mc.level !== lastWorld) {
                lastWorld = mc.level
                currentIndex = 0
            }

            loadWaypoints()
            if (waypoints.isEmpty()) return

            if (currentIndex >= waypoints.size) currentIndex = 0

            val target = waypoints[currentIndex]
            if (isNear(player.blockPosition(), target)) {
                currentTarget = target
                step = 1
                delay = randDelay(1, 2)
                currentIndex++
                if (currentIndex >= waypoints.size) currentIndex = 0
            } else {
                stepEntry = entries.size
            }
        }
    }

    private fun isNear(pos: BlockPos, wp: Waypoint): Boolean {
        return Math.abs(pos.x - wp.x) <= RANGE
            && Math.abs(pos.y - wp.y) <= RANGE
            && Math.abs(pos.z - wp.z) <= RANGE
    }

    private fun loadWaypoints() {
        if (waypointsLoaded) return
        waypoints.clear()
        if (!dataFile.exists()) return
        try {
            FileReader(dataFile).use { reader ->
                val type = object : TypeToken<List<Waypoint>>() {}.type
                val loaded: List<Waypoint>? = GSON.fromJson(reader, type)
                if (loaded != null) {
                    waypoints.addAll(loaded)
                    waypoints.sortBy { w -> w.options?.name ?: 0 }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        waypointsLoaded = true
    }

    fun reload() {
        waypointsLoaded = false
        currentIndex = 0
        step = 0
        stepEntry = entries.size
        delay = 0
        currentTarget = null
    }

    fun addEntry(placeSlot: Int, placeInteract: Boolean, restoreSlot: Int, restoreInteract: Boolean, enabled: Boolean) {
        entries.add(PlaceEntry(placeSlot, placeInteract, restoreSlot, restoreInteract, enabled))
    }

    fun removeEntry(index: Int) {
        if (index >= 0 && index < entries.size) {
            entries.removeAt(index)
            step = 0
            stepEntry = entries.size
            currentTarget = null
        }
    }

    data class PlaceEntry(
        val placeSlot: Int,
        val placeInteract: Boolean,
        val restoreSlot: Int,
        val restoreInteract: Boolean,
        val enabled: Boolean
    )

    private class Waypoint {
        @JvmField var x = 0
        @JvmField var y = 0
        @JvmField var z = 0
        @JvmField var r = 0
        @JvmField var g = 0
        @JvmField var b = 0
        @JvmField var options: Options? = null
    }

    private class Options {
        @JvmField var name = 0
    }

}
