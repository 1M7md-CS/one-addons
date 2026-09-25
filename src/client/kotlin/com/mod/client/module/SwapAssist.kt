package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.world.InteractionHand
import java.util.concurrent.ThreadLocalRandom

object SwapAssist : Module(
    name = "Auto Slot Swap",
    description = "Switches from a trigger slot to a target slot and optionally uses items.",
    category = Categories.ONEADDONS
) {
    private val prevInSlot = BooleanArray(9)
    private var step = 0
    private var stepEntry = 0
    private var delay = 0

    private val swapDelay: Int by NumberSetting(
        "Swap Delay",
        10,
        1,
        40,
        1,
        unit = "ticks",
        desc = "Ticks to wait before using the target-slot item."
    )

    private val profile1Enabled by DropdownSetting("Profile 1", true, "Enable and configure the first swap rule.")
    private val s1Trig: Int by NumberSetting("P1 From Slot", 1, 1, 9, 1, desc = "Select the trigger slot.").withDependency { profile1Enabled }
    private val s1Tar: Int by NumberSetting("P1 To Slot", 2, 1, 9, 1, desc = "Select the target slot.").withDependency { profile1Enabled }
    private val s1TrigRC by BooleanSetting("P1 Right Click Trigger", false, "Right-click the trigger item before swapping.").withDependency { profile1Enabled }
    private val s1TargRC by BooleanSetting("P1 Right Click Target", true, "Right-click the target item after swapping.").withDependency { profile1Enabled }

    private val profile2Enabled by DropdownSetting("Profile 2", false, "Enable and configure the second swap rule.")
    private val s2Trig: Int by NumberSetting("P2 From Slot", 3, 1, 9, 1, desc = "Select the trigger slot.").withDependency { profile2Enabled }
    private val s2Tar: Int by NumberSetting("P2 To Slot", 4, 1, 9, 1, desc = "Select the target slot.").withDependency { profile2Enabled }
    private val s2TrigRC by BooleanSetting("P2 Right Click Trigger", false, "Right-click the trigger item before swapping.").withDependency { profile2Enabled }
    private val s2TargRC by BooleanSetting("P2 Right Click Target", true, "Right-click the target item after swapping.").withDependency { profile2Enabled }

    private val profile3Enabled by DropdownSetting("Profile 3", false, "Enable and configure the third swap rule.")
    private val s3Trig: Int by NumberSetting("P3 From Slot", 1, 1, 9, 1, desc = "Select the trigger slot.").withDependency { profile3Enabled }
    private val s3Tar: Int by NumberSetting("P3 To Slot", 1, 1, 9, 1, desc = "Select the target slot.").withDependency { profile3Enabled }
    private val s3TrigRC by BooleanSetting("P3 Right Click Trigger", false, "Right-click the trigger item before swapping.").withDependency { profile3Enabled }
    private val s3TargRC by BooleanSetting("P3 Right Click Target", true, "Right-click the target item after swapping.").withDependency { profile3Enabled }

    private val profile4Enabled by DropdownSetting("Profile 4", false, "Enable and configure the fourth swap rule.")
    private val s4Trig: Int by NumberSetting("P4 From Slot", 1, 1, 9, 1, desc = "Select the trigger slot.").withDependency { profile4Enabled }
    private val s4Tar: Int by NumberSetting("P4 To Slot", 1, 1, 9, 1, desc = "Select the target slot.").withDependency { profile4Enabled }
    private val s4TrigRC by BooleanSetting("P4 Right Click Trigger", false, "Right-click the trigger item before swapping.").withDependency { profile4Enabled }
    private val s4TargRC by BooleanSetting("P4 Right Click Target", true, "Right-click the target item after swapping.").withDependency { profile4Enabled }

    private fun activeProfiles(): List<SwapProfile> {
        val list = mutableListOf<SwapProfile>()
        fun add(from: Int, to: Int, useBefore: Boolean, useAfter: Boolean) {
            val triggerSlot = from - 1
            val targetSlot = to - 1
            if (triggerSlot in 0..8 && targetSlot in 0..8 && triggerSlot != targetSlot) {
                list.add(SwapProfile(triggerSlot, useBefore, targetSlot, useAfter))
            }
        }
        if (profile1Enabled) add(s1Trig, s1Tar, s1TrigRC, s1TargRC)
        if (profile2Enabled) add(s2Trig, s2Tar, s2TrigRC, s2TargRC)
        if (profile3Enabled) add(s3Trig, s3Tar, s3TrigRC, s3TargRC)
        if (profile4Enabled) add(s4Trig, s4Tar, s4TrigRC, s4TargRC)
        return list
    }

    override fun onDisable() {
        prevInSlot.fill(false)
        step = 0
        stepEntry = 0
        delay = 0
    }

    init {
        on<TickEvent.End> {
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
                    delay = swapDelay
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
