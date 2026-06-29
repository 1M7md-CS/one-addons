package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.world.InteractionHand
import java.util.concurrent.ThreadLocalRandom

object SwapAssist : Module(
    name = "Swap Assist",
    description = "Automatically swap to a target slot when a trigger slot is selected.",
    category = Categories.ONEADDONS
) {
    private val prevInSlot = BooleanArray(9)
    private var step = 0
    private var stepEntry = 0
    private var delay = 0

    private val postSwapDelay: Int by NumberSetting("Delay", 10, 1, 40, 1, unit = "t", desc = "")

    private val s1Trig: Int by NumberSetting("S1 Trigger", 0, 0, 8, 1, desc = "")
    private val s1Tar: Int by NumberSetting("S1 Target", 1, 0, 8, 1, desc = "")
    private val s1TrigRC: Int by SelectorSetting("S1 Trig RC", "No", listOf("No", "Yes"), desc = "")
    private val s1TargRC: Int by SelectorSetting("S1 Targ RC", "Yes", listOf("No", "Yes"), desc = "")

    private val s2Trig: Int by NumberSetting("S2 Trigger", 2, 0, 8, 1, desc = "")
    private val s2Tar: Int by NumberSetting("S2 Target", 3, 0, 8, 1, desc = "")
    private val s2TrigRC: Int by SelectorSetting("S2 Trig RC", "No", listOf("No", "Yes"), desc = "")
    private val s2TargRC: Int by SelectorSetting("S2 Targ RC", "Yes", listOf("No", "Yes"), desc = "")

    private val s3Trig: Int by NumberSetting("S3 Trigger", 0, 0, 8, 1, desc = "")
    private val s3Tar: Int by NumberSetting("S3 Target", 0, 0, 8, 1, desc = "")
    private val s3TrigRC: Int by SelectorSetting("S3 Trig RC", "No", listOf("No", "Yes"), desc = "")
    private val s3TargRC: Int by SelectorSetting("S3 Targ RC", "Yes", listOf("No", "Yes"), desc = "")

    private val s4Trig: Int by NumberSetting("S4 Trigger", 0, 0, 8, 1, desc = "")
    private val s4Tar: Int by NumberSetting("S4 Target", 0, 0, 8, 1, desc = "")
    private val s4TrigRC: Int by SelectorSetting("S4 Trig RC", "No", listOf("No", "Yes"), desc = "")
    private val s4TargRC: Int by SelectorSetting("S4 Targ RC", "Yes", listOf("No", "Yes"), desc = "")

    private fun activeProfiles(): List<SwapProfile> {
        val list = mutableListOf<SwapProfile>()
        fun add(t: Int, tar: Int, ti: Int, tai: Int) {
            if (t in 0..8 && tar in 0..8 && t != tar) list.add(SwapProfile(t, ti == 1, tar, tai == 1))
        }
        add(s1Trig, s1Tar, s1TrigRC, s1TargRC)
        add(s2Trig, s2Tar, s2TrigRC, s2TargRC)
        add(s3Trig, s3Tar, s3TrigRC, s3TargRC)
        add(s4Trig, s4Tar, s4TrigRC, s4TargRC)
        return list
    }

    init {
        on<TickEvent.Start> {
            val player = mc.player ?: return@on
            val gameMode = mc.gameMode ?: return@on

            val profiles = activeProfiles()
            if (profiles.isEmpty()) { step = 0; return@on }

            if (step > 0) {
                if (delay > 0) { delay--; return@on }
                if (stepEntry >= profiles.size) { step = 0; return@on }
                val p = profiles[stepEntry]

                if (step == 1) {
                    player.inventory.selectedSlot = p.targetSlot
                    step = 0
                    prevInSlot[p.triggerSlot] = false
                    return@on
                }
                if (step == 2) {
                    player.inventory.selectedSlot = p.targetSlot
                    delay = postSwapDelay
                    step = 3
                    return@on
                }
                if (step == 3) {
                    gameMode.useItem(player, InteractionHand.MAIN_HAND)
                    delay = randDelay()
                    step = 0
                    prevInSlot[p.triggerSlot] = false
                    return@on
                }
            }

            for (p in profiles) {
                val currentSlot = player.inventory.selectedSlot
                val nowIn = currentSlot == p.triggerSlot

                if (nowIn && !prevInSlot[p.triggerSlot]) {
                    stepEntry = profiles.indexOf(p)
                    if (p.triggerInteract) gameMode.useItem(player, InteractionHand.MAIN_HAND)
                    delay = randDelay()
                    step = if (p.targetSlot != p.triggerSlot) {
                        if (p.targetInteract) 2 else 1
                    } else {
                        if (p.targetInteract) 3 else 0
                    }
                }
                prevInSlot[p.triggerSlot] = nowIn
            }
        }
    }

    private data class SwapProfile(
        val triggerSlot: Int,
        val triggerInteract: Boolean,
        val targetSlot: Int,
        val targetInteract: Boolean
    )

    private fun randDelay(): Int = ThreadLocalRandom.current().nextInt(1, 3)
}
