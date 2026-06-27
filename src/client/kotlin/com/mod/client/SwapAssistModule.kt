package com.mod.client

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import java.util.concurrent.ThreadLocalRandom

class SwapAssistModule {

    val entries = mutableListOf<SwapEntry>()

    private val prevInSlot = BooleanArray(9)
    private var step = 0
    private var stepEntry = 0
    private var delay = 0

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
            return
        }

        if (step > 0) {
            if (delay > 0) {
                delay--
                return
            }

            if (stepEntry >= entries.size) {
                step = 0
                return
            }

            val e = entries[stepEntry]
            if (!e.enabled) {
                step = 0
                return
            }

            if (step == 1) {
                player.inventory.selectedSlot = e.targetSlot
                step = 0
                prevInSlot[e.triggerSlot] = false
                return
            }

            if (step == 2) {
                player.inventory.selectedSlot = e.targetSlot
                delay = POST_SWAP_DELAY
                step = 3
                return
            }

            if (step == 3) {
                gameMode.useItem(player, InteractionHand.MAIN_HAND)
                delay = randDelay()
                step = 0
                prevInSlot[e.triggerSlot] = false
                return
            }
        }

        for (i in entries.indices) {
            val e = entries[i]
            if (!e.enabled) continue
            if (e.triggerSlot < 0 || e.triggerSlot > 8 || e.targetSlot < 0 || e.targetSlot > 8) continue

            val currentSlot = player.inventory.selectedSlot
            val nowIn = currentSlot == e.triggerSlot

            if (nowIn && !prevInSlot[e.triggerSlot]) {
                stepEntry = i

                if (e.triggerInteract) {
                    gameMode.useItem(player, InteractionHand.MAIN_HAND)
                }

                delay = randDelay()

                step = if (e.targetSlot != e.triggerSlot) {
                    if (e.targetInteract) 2 else 1
                } else {
                    if (e.targetInteract) 3 else 0
                }
            }

            prevInSlot[e.triggerSlot] = nowIn
        }
    }

    fun addEntry(triggerSlot: Int, triggerInteract: Boolean, targetSlot: Int, targetInteract: Boolean, enabled: Boolean) {
        var tSlot = triggerSlot.coerceIn(0, 8)
        var tarSlot = targetSlot.coerceIn(0, 8)
        entries.add(SwapEntry(tSlot, triggerInteract, tarSlot, targetInteract, enabled))
        prevInSlot[tSlot] = false
    }

    fun removeEntry(index: Int) {
        if (index >= 0 && index < entries.size) {
            prevInSlot[entries[index].triggerSlot] = false
            entries.removeAt(index)
            step = 0
        }
    }

    data class SwapEntry(
        val triggerSlot: Int,
        val triggerInteract: Boolean,
        val targetSlot: Int,
        val targetInteract: Boolean,
        val enabled: Boolean
    )

    companion object {
        private const val POST_SWAP_DELAY = 10

        private fun randDelay(): Int {
            return ThreadLocalRandom.current().nextInt(1, 3)
        }
    }
}
