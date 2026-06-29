package com.mod.client.keymaker

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object SlotMappings {
    @JvmStatic val FORGE_TITLE = "The Forge"
    @JvmStatic val SELECT_PROCESS_TITLE = "Select Process"
    @JvmStatic val CONFIRM_PROCESS_TITLE = "Confirm Process"

    @JvmField
    val FORGE_PROCESS_SLOTS = arrayOf(
        ForgeSlot(0, 10, 37),
        ForgeSlot(1, 11, 38),
        ForgeSlot(2, 12, 39),
        ForgeSlot(3, 13, 40),
        ForgeSlot(4, 14, 41),
        ForgeSlot(5, 15, 42),
        ForgeSlot(6, 16, 43),
        ForgeSlot(7, 17, 44),
    )

    @JvmStatic val SELECT_PROCESS_OTHER_SLOT = 33

    @JvmStatic val KEY_SELECTION_TUNGSTEN_SLOT = 24
    @JvmStatic val KEY_SELECTION_UMBER_SLOT = 25

    @JvmStatic val CONFIRM_BUTTON_SLOT = 31

    @JvmStatic
    fun isFurnace(stack: ItemStack): Boolean = stack.item === Items.FURNACE
}
