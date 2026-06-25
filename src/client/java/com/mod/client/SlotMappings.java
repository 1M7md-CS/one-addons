package com.mod.client;

import net.minecraft.world.item.Items;

public final class SlotMappings {

    public static final String FORGE_TITLE = "The Forge";
    public static final String SELECT_PROCESS_TITLE = "Select Process";
    public static final String CONFIRM_PROCESS_TITLE = "Confirm Process";

    public static final int FORGE_SLOT_COUNT = 54;

    // (index, topSlot, bottomCompletionSlot)
    public static final ForgeSlot[] FORGE_PROCESS_SLOTS = {
        new ForgeSlot(0, 10, 37),
        new ForgeSlot(1, 11, 38),
        new ForgeSlot(2, 12, 39),
        new ForgeSlot(3, 13, 40),
        new ForgeSlot(4, 14, 41),
        new ForgeSlot(5, 15, 42),
        new ForgeSlot(6, 16, 43),
        new ForgeSlot(7, 17, 44),
    };

    public static final int SELECT_PROCESS_OTHER_SLOT = 33;

    public static final int KEY_SELECTION_TUNGSTEN_SLOT = 24;
    public static final int KEY_SELECTION_UMBER_SLOT = 25;

    public static final int CONFIRM_BUTTON_SLOT = 31;

    public static boolean isFurnace(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() == Items.FURNACE;
    }

    public static boolean isNetherStar(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() == Items.NETHER_STAR;
    }

    private SlotMappings() {}
}
