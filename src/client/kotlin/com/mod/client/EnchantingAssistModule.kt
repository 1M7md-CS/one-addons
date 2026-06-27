package com.mod.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class EnchantingAssistModule {

    private var handler: ExperimentHandler? = null
    private var lastClick = 0L

    fun tick(client: Minecraft) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            handler = null
            return
        }

        val screen = client.screen
        val containerHandler = (screen as? AbstractContainerScreen<*>)?.menu as? ChestMenu
        if (containerHandler == null) {
            handler = null
            return
        }

        val slots = containerHandler.slots
        if (slots.size < 54) {
            handler = null
            return
        }

        val title = screen.title.string
        var newHandler: ExperimentHandler? = null

        if (title.startsWith("Chronomatron (")) {
            if (handler !is ChronomatronHandler) {
                newHandler = ChronomatronHandler()
            }
        } else if (title.startsWith("Ultrasequencer (")) {
            if (handler !is UltrasequencerHandler) {
                newHandler = UltrasequencerHandler()
            }
        } else {
            handler = null
            return
        }

        if (newHandler != null) {
            handler = newHandler
        }

        val h = handler ?: return

        h.observe(containerHandler, slots)

        val now = System.currentTimeMillis()
        if (now - lastClick < delay()) return

        val slotId = h.nextClick()
        if (slotId != null) {
            leftClick(client, containerHandler, slotId)
            lastClick = now
        }

        val autoClose = if (h is ChronomatronHandler)
            (OneAddons.autoClose && OneAddons.closeChronoEnabled)
        else
            (OneAddons.autoClose && OneAddons.closeUltraEnabled)
        if (h.shouldClose(autoClose)) {
            client.player?.closeContainer()
            handler = null
        }
    }

    companion object {
        private const val CLICK_DELAY_MS = 200L
        private const val DELAY_VARIETY_MS = 50L

        private fun delay(): Long {
            return CLICK_DELAY_MS + ThreadLocalRandom.current().nextInt(0, DELAY_VARIETY_MS.toInt() + 1)
        }

        private fun isNumberedItem(stack: ItemStack): Boolean {
            var name = stack.hoverName.string
            if (name.contains("§")) {
                name = name.replace("§[0-9a-fk-or]".toRegex(), "")
            }
            return name.matches("\\d+".toRegex())
        }

        private fun leftClick(client: Minecraft, handler: ChestMenu, slot: Int) {
            val player = client.player ?: return
            val gameMode = client.gameMode ?: return
            gameMode.handleContainerInput(
                handler.containerId,
                slot,
                0,
                ContainerInput.PICKUP,
                player
            )
        }
    }

    private abstract inner class ExperimentHandler {
        protected var clicks = 0
        protected var hasData = false

        abstract fun observe(handler: ChestMenu, slots: List<Slot>)
        abstract fun nextClick(): Int?
        abstract fun shouldClose(autoClose: Boolean): Boolean
    }

    private inner class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var lastAddedSlot = -1
        private var close = false

        override fun observe(handler: ChestMenu, slots: List<Slot>) {
            val center = slots[49].item

            if (lastAddedSlot != -1
                && center.item === Items.GLOWSTONE
                && !slots[lastAddedSlot].item.hasFoil()
            ) {
                close = order.size >= OneAddons.closeCountChronomatron
                hasData = false
                return
            }

            if (hasData || center.item !== Items.CLOCK) return

            for (i in 10..43) {
                val stack = slots[i].item
                if (!stack.isEmpty && stack.hasFoil()) {
                    if (order.isEmpty() || order[order.size - 1] != i) {
                        order.add(i)
                        lastAddedSlot = i
                        hasData = true
                        clicks = 0
                    }
                    break
                }
            }
        }

        override fun nextClick(): Int? {
            if (hasData && clicks < order.size) return order[clicks++]
            return null
        }

        override fun shouldClose(autoClose: Boolean): Boolean {
            if (!autoClose || !close) return false
            if (clicks < order.size) return false
            close = false
            return true
        }
    }

    private inner class UltrasequencerHandler : ExperimentHandler() {
        private val order = ConcurrentHashMap<Int, Int>()
        private var glowstoneSeen = false

        override fun observe(handler: ChestMenu, slots: List<Slot>) {
            val center = slots[49].item
            if (center.isEmpty) return

            if (center.item === Items.CLOCK) {
                if (order.isNotEmpty() && !hasData) {
                    hasData = true
                    clicks = 0
                }
                glowstoneSeen = false
                return
            }

            if (center.item !== Items.GLOWSTONE) return

            if (!glowstoneSeen) {
                glowstoneSeen = true
                hasData = false
                return
            }

            order.clear()

            for (i in 9..44) {
                val stack = slots[i].item
                if (!stack.isEmpty && isNumberedItem(stack)) {
                    order[stack.count - 1] = i
                }
            }

            clicks = 0
        }

        override fun nextClick(): Int? {
            if (!hasData) return null
            val slot = order[clicks]
            if (slot != null) {
                clicks++
                return slot
            }
            return null
        }

        override fun shouldClose(autoClose: Boolean): Boolean {
            return autoClose && order.size >= OneAddons.closeCountUltrasequencer
        }
    }
}
