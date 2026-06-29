package com.mod.client.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import java.io.File
import java.io.FileReader
import java.util.concurrent.ThreadLocalRandom

object PlaceOnPosition : Module(
    name = "Place On Position",
    description = "Automatically place/use items when near saved waypoints. Configure profiles below.",
    category = Categories.ONEADDONS
) {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val range: Int by NumberSetting("Range", 5, 1, 20, 1, unit = "b", desc = "")

    private val p1Slot: Int by NumberSetting("P1 Slot", 2, 0, 8, 1, desc = "")
    private val p1RC: Int by SelectorSetting("P1 RClick", "Yes", listOf("No", "Yes"), desc = "")
    private val p1Rest: Int by NumberSetting("P1 Restore", 0, 0, 8, 1, desc = "")
    private val p1RestRC: Int by SelectorSetting("P1 Rest RC", "No", listOf("No", "Yes"), desc = "")

    private val p2Slot: Int by NumberSetting("P2 Slot", 0, 0, 8, 1, desc = "")
    private val p2RC: Int by SelectorSetting("P2 RClick", "Yes", listOf("No", "Yes"), desc = "")
    private val p2Rest: Int by NumberSetting("P2 Restore", 0, 0, 8, 1, desc = "")
    private val p2RestRC: Int by SelectorSetting("P2 Rest RC", "No", listOf("No", "Yes"), desc = "")

    private val p3Slot: Int by NumberSetting("P3 Slot", 0, 0, 8, 1, desc = "")
    private val p3RC: Int by SelectorSetting("P3 RClick", "Yes", listOf("No", "Yes"), desc = "")
    private val p3Rest: Int by NumberSetting("P3 Restore", 0, 0, 8, 1, desc = "")
    private val p3RestRC: Int by SelectorSetting("P3 Rest RC", "No", listOf("No", "Yes"), desc = "")

    private val p4Slot: Int by NumberSetting("P4 Slot", 0, 0, 8, 1, desc = "")
    private val p4RC: Int by SelectorSetting("P4 RClick", "Yes", listOf("No", "Yes"), desc = "")
    private val p4Rest: Int by NumberSetting("P4 Restore", 0, 0, 8, 1, desc = "")
    private val p4RestRC: Int by SelectorSetting("P4 Rest RC", "No", listOf("No", "Yes"), desc = "")

    private val dataFile = File(mc.gameDirectory, "config/oneaddons/placeonposition.json")
    private val waypoints = mutableListOf<Waypoint>()
    private var waypointsLoaded = false

    private val profiles: List<PlaceProfile>
        get() {
            val list = mutableListOf<PlaceProfile>()
            fun add(slot: Int, interact: Int, restore: Int, restoreInteract: Int) {
                if (slot in 0..8 && restore in 0..8) list.add(PlaceProfile(slot, interact == 1, restore, restoreInteract == 1))
            }
            add(p1Slot, p1RC, p1Rest, p1RestRC)
            add(p2Slot, p2RC, p2Rest, p2RestRC)
            add(p3Slot, p3RC, p3Rest, p3RestRC)
            add(p4Slot, p4RC, p4Rest, p4RestRC)
            return list
        }

    private var step = 0
    private var stepEntry = 0
    private var delay = 0
    private var currentIndex = 0
    private var lastWorld: Level? = null
    private var currentTarget: Waypoint? = null

    init {
        on<TickEvent.Start> {
            val player = mc.player ?: return@on
            val gameMode = mc.gameMode ?: return@on

            val active = profiles
            if (active.isEmpty()) { step = 0; stepEntry = 0; currentTarget = null; return@on }

            if (currentTarget != null && !isNear(player.blockPosition(), currentTarget!!)) {
                currentTarget = null; step = 0; stepEntry = 0; return@on
            }

            if (step > 0) {
                if (delay > 0) { delay--; return@on }
                if (stepEntry >= active.size) { step = 0; stepEntry = 0; return@on }
                val p = active[stepEntry]

                if (step == 1) {
                    player.inventory.selectedSlot = p.slot
                    delay = 1; step = 2; return@on
                }
                if (step == 2) {
                    if (p.interact) gameMode.useItem(player, InteractionHand.MAIN_HAND)
                    delay = randDelay(1, 2); step = 3; return@on
                }
                if (step == 3) {
                    player.inventory.selectedSlot = p.restoreSlot
                    if (p.restoreInteract) { delay = 1; step = 4 }
                    else { stepEntry++; step = if (stepEntry < active.size) 1 else 0 }
                    return@on
                }
                if (step == 4) {
                    if (p.restoreInteract) gameMode.useItem(player, InteractionHand.MAIN_HAND)
                    stepEntry++; step = if (stepEntry < active.size) 1 else 0
                    return@on
                }
            }

            if (step == 0 && currentTarget == null) {
                stepEntry = 0
                if (mc.level !== lastWorld) { lastWorld = mc.level; currentIndex = 0 }
                loadWaypoints()
                if (waypoints.isEmpty()) return@on
                if (currentIndex >= waypoints.size) currentIndex = 0

                val target = waypoints[currentIndex]
                if (isNear(player.blockPosition(), target)) {
                    currentTarget = target
                    step = 1
                    delay = randDelay(1, 2)
                    currentIndex++
                    if (currentIndex >= waypoints.size) currentIndex = 0
                } else {
                    stepEntry = active.size
                }
            }
        }
    }

    private fun isNear(pos: BlockPos, wp: Waypoint): Boolean {
        return kotlin.math.abs(pos.x - wp.x) <= range
            && kotlin.math.abs(pos.y - wp.y) <= range
            && kotlin.math.abs(pos.z - wp.z) <= range
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
        stepEntry = 0
        delay = 0
        currentTarget = null
    }

    private data class PlaceProfile(
        val slot: Int,
        val interact: Boolean,
        val restoreSlot: Int,
        val restoreInteract: Boolean
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

    private fun randDelay(min: Int, max: Int): Int =
        ThreadLocalRandom.current().nextInt(min, max + 1)
}
