package com.mod.client;

import com.mod.client.compat.ContainerCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class KeyMakerFeature {

    private KeyMakerState state = KeyMakerState.IDLE;
    private CurrentCraftType currentType = CurrentCraftType.TUNGSTEN;
    private boolean bothSwitched = false;
    private int currentSlotIndex = 0;
    private int actionSlot = -1;
    private long lastActionTime = 0;

    public void tick(Minecraft client) {
        if (!OneAddons.keyMakerEnabled) {
            reset();
            return;
        }

        if (client.player == null || client.level == null || client.gameMode == null) {
            reset();
            return;
        }

        if (OneAddons.keyMakerMode != KeyMode.BOTH) {
            currentType = OneAddons.keyMakerMode == KeyMode.UMBER
                ? CurrentCraftType.UMBER
                : CurrentCraftType.TUNGSTEN;
        }

        switch (state) {
            case IDLE -> tickIdle(client);
            case SCANNING -> tickScanning(client);
            case COLLECTING -> tickCollecting(client);
            case CRAFT_FURNACE -> tickCraftFurnace(client);
            case WAIT_PROCESS -> tickWaitProcess(client);
            case CONFIRM -> tickConfirm(client);
            case CLOSE_ABORT -> tickCloseAbort(client);
            case WAIT_FORGE_RETURN -> tickWaitForgeReturn(client);
        }
    }

    private void reset() {
        state = KeyMakerState.IDLE;
        currentType = OneAddons.keyMakerMode == KeyMode.UMBER
            ? CurrentCraftType.UMBER
            : CurrentCraftType.TUNGSTEN;
        bothSwitched = false;
        currentSlotIndex = 0;
    }

    private void tickIdle(Minecraft client) {
        if (isForgeOpen(client)) {
            state = KeyMakerState.SCANNING;
            currentSlotIndex = 0;
        }
    }

    private void tickScanning(Minecraft client) {
        if (!isForgeOpen(client)) {
            reset();
            return;
        }

        ForgeSlot[] slots = SlotMappings.FORGE_PROCESS_SLOTS;

        for (int i = 0; i < slots.length; i++) {
            int idx = (currentSlotIndex + i) % slots.length;
            ForgeSlot slot = slots[idx];
            if (isSlotCompleted(client, slot)) {
                currentSlotIndex = (idx + 1) % slots.length;
                actionSlot = slot.topSlot();
                state = KeyMakerState.COLLECTING;
                return;
            }
        }

        if (!readyToClick()) return;

        for (int i = 0; i < slots.length; i++) {
            int idx = (currentSlotIndex + i) % slots.length;
            ForgeSlot slot = slots[idx];
            if (isSlotEmpty(client, slot)) {
                currentSlotIndex = (idx + 1) % slots.length;
                actionSlot = slot.topSlot();
                performClick(client, actionSlot);
                state = KeyMakerState.CRAFT_FURNACE;
                return;
            }
        }

        currentSlotIndex = (currentSlotIndex + 1) % slots.length;
    }

    private void tickCollecting(Minecraft client) {
        if (!isForgeOpen(client)) {
            return;
        }

        if (!readyToClick()) return;

        ForgeSlot forgeSlot = findSlotByTop(actionSlot);
        if (forgeSlot == null || !isSlotCompleted(client, forgeSlot)) {
            state = KeyMakerState.SCANNING;
            return;
        }

        performClick(client, actionSlot);
        state = KeyMakerState.SCANNING;
    }

    private void tickCraftFurnace(Minecraft client) {
        if (client.screen == null) {
            return;
        }

        if (!readyToClick()) return;

        AbstractContainerScreen<?> screen = getScreen(client);
        if (screen == null) {
            return;
        }

        String title = screen.getTitle().getString();
        if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            performClick(client, SlotMappings.SELECT_PROCESS_OTHER_SLOT);
            state = KeyMakerState.WAIT_PROCESS;
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            return;
        } else {
            reset();
        }
    }

    private void tickWaitProcess(Minecraft client) {
        if (client.screen == null) return;
        if (!readyToClick()) return;

        int keySlot = currentType == CurrentCraftType.TUNGSTEN
            ? SlotMappings.KEY_SELECTION_TUNGSTEN_SLOT
            : SlotMappings.KEY_SELECTION_UMBER_SLOT;
        performClick(client, keySlot);
        state = KeyMakerState.CONFIRM;
    }

    private void tickConfirm(Minecraft client) {
        if (client.screen == null) return;
        if (!readyToClick()) return;

        AbstractContainerScreen<?> screen = getScreen(client);
        if (screen == null) return;

        String title = screen.getTitle().getString();
        if (title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm")) {
            performClick(client, SlotMappings.CONFIRM_BUTTON_SLOT);
            state = KeyMakerState.WAIT_FORGE_RETURN;
        } else if (title.contains(SlotMappings.FORGE_TITLE)) {
            state = KeyMakerState.SCANNING;
        } else if (title.contains(SlotMappings.SELECT_PROCESS_TITLE)) {
            return;
        } else {
            reset();
        }
    }

    private void tickWaitForgeReturn(Minecraft client) {
        if (isForgeOpen(client)) {
            state = KeyMakerState.SCANNING;
            currentSlotIndex = 0;
        } else if (readyToClick() && isConfirmOpen(client)) {
            handleExhausted(client);
        }
    }

    private void tickCloseAbort(Minecraft client) {
        if (!readyToClick()) return;

        if (client.player != null) {
            client.player.closeContainer();
        }

        KeyMode mode = OneAddons.keyMakerMode;

        if (mode == KeyMode.BOTH && currentType == CurrentCraftType.TUNGSTEN) {
            currentType = CurrentCraftType.UMBER;
            bothSwitched = true;
            state = KeyMakerState.WAIT_FORGE_RETURN;
        } else {
            sendNotification(client);
            reset();
        }
    }

    private void handleExhausted(Minecraft client) {
        state = KeyMakerState.CLOSE_ABORT;
        lastActionTime = System.currentTimeMillis();
    }

    private void sendNotification(Minecraft client) {
        if (client.player == null) return;

        KeyMode mode = OneAddons.keyMakerMode;
        String msg;

        if (mode == KeyMode.BOTH) {
            msg = "[KeyMaker] Finished crafting all available Tungsten and Umber Keys.";
        } else if (mode == KeyMode.TUNGSTEN) {
            msg = "[KeyMaker] Finished crafting Tungsten Keys.";
        } else {
            msg = "[KeyMaker] Finished crafting Umber Keys.";
        }

        client.player.sendSystemMessage(Component.literal(msg));
    }

    private boolean isForgeOpen(Minecraft client) {
        AbstractContainerScreen<?> screen = getScreen(client);
        if (screen == null) return false;
        String title = screen.getTitle().getString();
        return title.contains(SlotMappings.FORGE_TITLE);
    }

    private boolean isSlotEmpty(Minecraft client, ForgeSlot slot) {
        ChestMenu menu = getMenu(client);
        if (menu == null) return false;

        List<Slot> slots = menu.slots;
        if (slot.topSlot() >= slots.size() || slot.lastGlassSlot() >= slots.size()) return false;

        ItemStack top = slots.get(slot.topSlot()).getItem();
        ItemStack lastGlass = slots.get(slot.lastGlassSlot()).getItem();

        return SlotMappings.isFurnace(top) && lastGlass.getItem() == Items.RED_STAINED_GLASS_PANE;
    }

    private boolean isSlotCompleted(Minecraft client, ForgeSlot slot) {
        ChestMenu menu = getMenu(client);
        if (menu == null) return false;

        List<Slot> slots = menu.slots;
        if (slot.topSlot() >= slots.size() || slot.lastGlassSlot() >= slots.size()) return false;

        ItemStack lastGlass = slots.get(slot.lastGlassSlot()).getItem();
        ItemStack top = slots.get(slot.topSlot()).getItem();
        return lastGlass.getItem() == Items.LIME_STAINED_GLASS_PANE && !top.isEmpty() && !SlotMappings.isFurnace(top);
    }

    private ForgeSlot findSlotByTop(int topSlot) {
        for (ForgeSlot slot : SlotMappings.FORGE_PROCESS_SLOTS) {
            if (slot.topSlot() == topSlot) return slot;
        }
        return null;
    }

    private boolean isConfirmOpen(Minecraft client) {
        AbstractContainerScreen<?> screen = getScreen(client);
        if (screen == null) return false;
        String title = screen.getTitle().getString();
        return title.contains(SlotMappings.CONFIRM_PROCESS_TITLE) || title.contains("Confirm");
    }

    private boolean readyToClick() {
        long delay = OneAddons.keyMakerClickDelay;
        return System.currentTimeMillis() - lastActionTime >= delay;
    }

    private void performClick(Minecraft client, int slotIndex) {
        ChestMenu menu = getMenu(client);
        if (menu == null) return;
        ContainerCompat.leftClick(menu.containerId, slotIndex);
        lastActionTime = System.currentTimeMillis();
    }

    private AbstractContainerScreen<?> getScreen(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen<?> screen) {
            return screen;
        }
        return null;
    }

    private ChestMenu getMenu(Minecraft client) {
        AbstractContainerScreen<?> screen = getScreen(client);
        if (screen != null && screen.getMenu() instanceof ChestMenu menu) {
            return menu;
        }
        return null;
    }
}
