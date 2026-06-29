package com.mod.client.module

import com.mod.client.category.Categories
import com.mod.client.keymaker.ForgeSlot
import com.mod.client.keymaker.KeyMakerState
import com.mod.client.keymaker.SlotMappings
import com.mod.client.utils.guiClick
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Items

object KeyMaker : Module(
    name = "Key Maker",
    description = "Automates the Forge UI for crafting Tungsten and Umber Keys.",
    category = Categories.ONEADDONS
) {
    private val mode by SelectorSetting("Mode", "Tungsten", listOf("Tungsten", "Umber"), desc = "Key type to craft.")
    private val clickDelay by NumberSetting("Click Delay", 500, 100, 2000, 50, unit = "ms", desc = "Delay between clicks.")

    private var state = KeyMakerState.IDLE
    private var currentSlotIndex = 0
    private var actionSlot = -1
    private var lastActionTime = 0L

    init {
        on<TickEvent.Start> {
            if (mc.player == null || mc.level == null || mc.gameMode == null) {
                reset(); return@on
            }

            when (state) {
                KeyMakerState.IDLE -> tickIdle()
                KeyMakerState.SCANNING -> tickScanning()
                KeyMakerState.COLLECTING -> tickCollecting()
                KeyMakerState.CRAFT_FURNACE -> tickCraftFurnace()
                KeyMakerState.WAIT_PROCESS -> tickWaitProcess()
                KeyMakerState.CONFIRM -> tickConfirm()
                KeyMakerState.CLOSE_ABORT -> tickCloseAbort()
                KeyMakerState.WAIT_FORGE_RETURN -> tickWaitForgeReturn()
            }
        }
    }

    private fun reset() {
        state = KeyMakerState.IDLE
        currentSlotIndex = 0
    }

    private fun tickIdle() {
        if (isForgeOpen()) {
            state = KeyMakerState.SCANNING
            currentSlotIndex = 0
        }
    }

    private fun tickScanning() {
        if (!isForgeOpen()) { reset(); return }

        val slots = SlotMappings.FORGE_PROCESS_SLOTS

        for (i in slots.indices) {
            val idx = (currentSlotIndex + i) % slots.size
            val slot = slots[idx]
            if (isSlotCompleted(slot)) {
                currentSlotIndex = (idx + 1) % slots.size
                actionSlot = slot.topSlot
                state = KeyMakerState.COLLECTING
                return
            }
        }

        if (!readyToClick()) return

        for (i in slots.indices) {
            val idx = (currentSlotIndex + i) % slots.size
            val slot = slots[idx]
            if (isSlotEmpty(slot)) {
                currentSlotIndex = (idx + 1) % slots.size
                actionSlot = slot.topSlot
                performClick(actionSlot)
                state = KeyMakerState.CRAFT_FURNACE
                return
            }
        }

        currentSlotIndex = (currentSlotIndex + 1) % slots.size
    }

    private fun tickCollecting() {
        if (!isForgeOpen()) { return }
        if (!readyToClick()) return
        val forgeSlot = findSlotByTop(actionSlot)
        if (forgeSlot == null || !isSlotCompleted(forgeSlot)) {
            state = KeyMakerState.SCANNING
            return
        }
        performClick(actionSlot)
        state = KeyMakerState.SCANNING
    }

    private fun tickCraftFurnace() {
        if (mc.screen == null) { return }
        if (!readyToClick()) return
        val screen = getScreen() ?: return
        val title = screen.title.string
        if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            performClick(SlotMappings.SELECT_PROCESS_OTHER_SLOT)
            state = KeyMakerState.WAIT_PROCESS
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            return
        } else {
            reset()
        }
    }

    private fun tickWaitProcess() {
        if (mc.screen == null) return
        if (!readyToClick()) return
            val keySlot = if (mode == 0) SlotMappings.KEY_SELECTION_TUNGSTEN_SLOT
                else SlotMappings.KEY_SELECTION_UMBER_SLOT
        performClick(keySlot)
        state = KeyMakerState.CONFIRM
    }

    private fun tickConfirm() {
        if (mc.screen == null) return
        if (!readyToClick()) return
        val screen = getScreen() ?: return
        val title = screen.title.string
        if (title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm")) {
            performClick(SlotMappings.CONFIRM_BUTTON_SLOT)
            state = KeyMakerState.WAIT_FORGE_RETURN
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            state = KeyMakerState.SCANNING
        } else if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            return
        } else {
            reset()
        }
    }

    private fun tickWaitForgeReturn() {
        if (isForgeOpen()) {
            state = KeyMakerState.SCANNING
            currentSlotIndex = 0
        } else if (readyToClick() && isConfirmOpen()) {
            handleExhausted()
        }
    }

    private fun tickCloseAbort() {
        if (!readyToClick()) return
        mc.player?.closeContainer()
        sendNotification()
        reset()
    }

    private fun handleExhausted() {
        state = KeyMakerState.CLOSE_ABORT
        lastActionTime = System.currentTimeMillis()
    }

    private fun sendNotification() {
        val player = mc.player ?: return
        val msg = if (mode == 0) "[KeyMaker] Finished crafting Tungsten Keys."
            else "[KeyMaker] Finished crafting Umber Keys."
        player.sendSystemMessage(Component.literal(msg))
    }

    private fun isForgeOpen(): Boolean {
        val screen = getScreen() ?: return false
        return screen.title.string.contains(SlotMappings.FORGE_TITLE)
    }

    private fun isSlotEmpty(slot: ForgeSlot): Boolean {
        val menu = getMenu() ?: return false
        val slots = menu.slots
        if (slot.topSlot >= slots.size || slot.lastGlassSlot >= slots.size) return false
        val top = slots[slot.topSlot].item
        val lastGlass = slots[slot.lastGlassSlot].item
        return SlotMappings.isFurnace(top) && lastGlass.item === Items.RED_STAINED_GLASS_PANE
    }

    private fun isSlotCompleted(slot: ForgeSlot): Boolean {
        val menu = getMenu() ?: return false
        val slots = menu.slots
        if (slot.topSlot >= slots.size || slot.lastGlassSlot >= slots.size) return false
        val lastGlass = slots[slot.lastGlassSlot].item
        val top = slots[slot.topSlot].item
        return lastGlass.item === Items.LIME_STAINED_GLASS_PANE && !top.isEmpty && !SlotMappings.isFurnace(top)
    }

    private fun findSlotByTop(topSlot: Int): ForgeSlot? {
        for (slot in SlotMappings.FORGE_PROCESS_SLOTS) {
            if (slot.topSlot == topSlot) return slot
        }
        return null
    }

    private fun isConfirmOpen(): Boolean {
        val screen = getScreen() ?: return false
        val title = screen.title.string
        return title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm")
    }

    private fun readyToClick(): Boolean {
        return System.currentTimeMillis() - lastActionTime >= clickDelay
    }

    private fun performClick(slotIndex: Int) {
        val menu = getMenu() ?: return
        guiClick(menu.containerId, slotIndex)
        lastActionTime = System.currentTimeMillis()
    }

    private fun getScreen(): AbstractContainerScreen<*>? {
        return mc.screen as? AbstractContainerScreen<*>
    }

    private fun getMenu(): ChestMenu? {
        return getScreen()?.menu as? ChestMenu
    }
}
