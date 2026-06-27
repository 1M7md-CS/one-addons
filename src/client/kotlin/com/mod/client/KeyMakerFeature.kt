package com.mod.client

import com.mod.client.compat.ContainerCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Items

class KeyMakerFeature {

    private var state = KeyMakerState.IDLE
    private var currentType = CurrentCraftType.TUNGSTEN
    private var currentSlotIndex = 0
    private var actionSlot = -1
    private var lastActionTime = 0L

    fun tick(client: Minecraft) {
        if (!OneAddons.keyMakerEnabled) {
            reset()
            return
        }

        if (client.player == null || client.level == null || client.gameMode == null) {
            reset()
            return
        }

        currentType = if (OneAddons.keyMakerMode == KeyMode.UMBER)
            CurrentCraftType.UMBER
        else
            CurrentCraftType.TUNGSTEN

        when (state) {
            KeyMakerState.IDLE -> tickIdle(client)
            KeyMakerState.SCANNING -> tickScanning(client)
            KeyMakerState.COLLECTING -> tickCollecting(client)
            KeyMakerState.CRAFT_FURNACE -> tickCraftFurnace(client)
            KeyMakerState.WAIT_PROCESS -> tickWaitProcess(client)
            KeyMakerState.CONFIRM -> tickConfirm(client)
            KeyMakerState.CLOSE_ABORT -> tickCloseAbort(client)
            KeyMakerState.WAIT_FORGE_RETURN -> tickWaitForgeReturn(client)
        }
    }

    private fun reset() {
        state = KeyMakerState.IDLE
        currentType = if (OneAddons.keyMakerMode == KeyMode.UMBER)
            CurrentCraftType.UMBER
        else
            CurrentCraftType.TUNGSTEN
        currentSlotIndex = 0
    }

    private fun tickIdle(client: Minecraft) {
        if (isForgeOpen(client)) {
            state = KeyMakerState.SCANNING
            currentSlotIndex = 0
        }
    }

    private fun tickScanning(client: Minecraft) {
        if (!isForgeOpen(client)) {
            reset()
            return
        }

        val slots = SlotMappings.FORGE_PROCESS_SLOTS

        for (i in slots.indices) {
            val idx = (currentSlotIndex + i) % slots.size
            val slot = slots[idx]
            if (isSlotCompleted(client, slot)) {
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
            if (isSlotEmpty(client, slot)) {
                currentSlotIndex = (idx + 1) % slots.size
                actionSlot = slot.topSlot
                performClick(client, actionSlot)
                state = KeyMakerState.CRAFT_FURNACE
                return
            }
        }

        currentSlotIndex = (currentSlotIndex + 1) % slots.size
    }

    private fun tickCollecting(client: Minecraft) {
        if (!isForgeOpen(client)) {
            return
        }

        if (!readyToClick()) return

        val forgeSlot = findSlotByTop(actionSlot)
        if (forgeSlot == null || !isSlotCompleted(client, forgeSlot)) {
            state = KeyMakerState.SCANNING
            return
        }

        performClick(client, actionSlot)
        state = KeyMakerState.SCANNING
    }

    private fun tickCraftFurnace(client: Minecraft) {
        if (client.screen == null) {
            return
        }

        if (!readyToClick()) return

        val screen = getScreen(client) ?: return

        val title = screen.title.string
        if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            performClick(client, SlotMappings.SELECT_PROCESS_OTHER_SLOT)
            state = KeyMakerState.WAIT_PROCESS
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            return
        } else {
            reset()
        }
    }

    private fun tickWaitProcess(client: Minecraft) {
        if (client.screen == null) return
        if (!readyToClick()) return

        val keySlot = if (currentType == CurrentCraftType.TUNGSTEN)
            SlotMappings.KEY_SELECTION_TUNGSTEN_SLOT
        else
            SlotMappings.KEY_SELECTION_UMBER_SLOT
        performClick(client, keySlot)
        state = KeyMakerState.CONFIRM
    }

    private fun tickConfirm(client: Minecraft) {
        if (client.screen == null) return
        if (!readyToClick()) return

        val screen = getScreen(client) ?: return

        val title = screen.title.string
        if (title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm")) {
            performClick(client, SlotMappings.CONFIRM_BUTTON_SLOT)
            state = KeyMakerState.WAIT_FORGE_RETURN
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            state = KeyMakerState.SCANNING
        } else if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            return
        } else {
            reset()
        }
    }

    private fun tickWaitForgeReturn(client: Minecraft) {
        if (isForgeOpen(client)) {
            state = KeyMakerState.SCANNING
            currentSlotIndex = 0
        } else if (readyToClick() && isConfirmOpen(client)) {
            handleExhausted(client)
        }
    }

    private fun tickCloseAbort(client: Minecraft) {
        if (!readyToClick()) return

        if (client.player != null) {
            client.player!!.closeContainer()
        }

        sendNotification(client)
        reset()
    }

    private fun handleExhausted(client: Minecraft) {
        state = KeyMakerState.CLOSE_ABORT
        lastActionTime = System.currentTimeMillis()
    }

    private fun sendNotification(client: Minecraft) {
        val player = client.player ?: return

        val msg = if (OneAddons.keyMakerMode == KeyMode.TUNGSTEN)
            "[KeyMaker] Finished crafting Tungsten Keys."
        else
            "[KeyMaker] Finished crafting Umber Keys."

        player.sendSystemMessage(Component.literal(msg))
    }

    private fun isForgeOpen(client: Minecraft): Boolean {
        val screen = getScreen(client) ?: return false
        val title = screen.title.string
        return title.contains(SlotMappings.FORGE_TITLE)
    }

    private fun isSlotEmpty(client: Minecraft, slot: ForgeSlot): Boolean {
        val menu = getMenu(client) ?: return false

        val slots = menu.slots
        if (slot.topSlot >= slots.size || slot.lastGlassSlot >= slots.size) return false

        val top = slots[slot.topSlot].item
        val lastGlass = slots[slot.lastGlassSlot].item

        return SlotMappings.isFurnace(top) && lastGlass.item === Items.RED_STAINED_GLASS_PANE
    }

    private fun isSlotCompleted(client: Minecraft, slot: ForgeSlot): Boolean {
        val menu = getMenu(client) ?: return false

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

    private fun isConfirmOpen(client: Minecraft): Boolean {
        val screen = getScreen(client) ?: return false
        val title = screen.title.string
        return title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm")
    }

    private fun readyToClick(): Boolean {
        val delay = Math.max(500, OneAddons.keyMakerClickDelay)
        return System.currentTimeMillis() - lastActionTime >= delay
    }

    private fun performClick(client: Minecraft, slotIndex: Int) {
        val menu = getMenu(client) ?: return
        ContainerCompat.leftClick(menu.containerId, slotIndex)
        lastActionTime = System.currentTimeMillis()
    }

    private fun getScreen(client: Minecraft): AbstractContainerScreen<*>? {
        return client.screen as? AbstractContainerScreen<*>
    }

    private fun getMenu(client: Minecraft): ChestMenu? {
        val screen = getScreen(client) ?: return null
        return screen.menu as? ChestMenu
    }
}
