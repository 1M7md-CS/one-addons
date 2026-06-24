package com.mod.client;

import com.mod.client.compat.ContainerCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantingAssistModule {

    private static final long CLICK_DELAY_MS = 200L;
    private static final long DELAY_VARIETY_MS = 50L;

    private ExperimentHandler handler = null;
    private long lastClick = 0;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            handler = null;
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof ChestMenu containerHandler)) {
            handler = null;
            return;
        }

        List<Slot> slots = containerHandler.slots;
        if (slots.size() < 54) {
            handler = null;
            return;
        }

        String title = screen.getTitle().getString();
        ExperimentHandler newHandler = null;

        if (title.startsWith("Chronomatron (")) {
            if (!(handler instanceof ChronomatronHandler)) {
                newHandler = new ChronomatronHandler();
            }
        } else if (title.startsWith("Ultrasequencer (")) {
            if (!(handler instanceof UltrasequencerHandler)) {
                newHandler = new UltrasequencerHandler();
            }
        } else {
            handler = null;
            return;
        }

        if (newHandler != null) {
            handler = newHandler;
        }

        if (handler == null) return;

        handler.observe(containerHandler, slots);

        long now = System.currentTimeMillis();
        if (now - lastClick < delay()) return;

        Integer slotId = handler.nextClick();
        if (slotId != null) {
            leftClick(client, containerHandler, slotId);
            lastClick = now;
        }

        boolean autoClose = handler instanceof ChronomatronHandler
            ? (OneAddons.autoClose && OneAddons.closeChronoEnabled)
            : (OneAddons.autoClose && OneAddons.closeUltraEnabled);
        if (handler.shouldClose(autoClose)) {
            client.player.closeContainer();
            handler = null;
        }
    }

    private long delay() {
        return CLICK_DELAY_MS + ThreadLocalRandom.current().nextInt(0, (int) DELAY_VARIETY_MS + 1);
    }

    private abstract class ExperimentHandler {
        protected int clicks = 0;
        protected boolean hasData = false;

        abstract void observe(ChestMenu handler, List<Slot> slots);
        abstract Integer nextClick();
        abstract boolean shouldClose(boolean autoClose);
    }

    private class ChronomatronHandler extends ExperimentHandler {
        private final List<Integer> order = new ArrayList<>();
        private int lastAddedSlot = -1;
        private boolean close = false;

        @Override
        void observe(ChestMenu handler, List<Slot> slots) {
            var center = slots.get(49).getItem();

            if (lastAddedSlot != -1
                    && center.getItem() == Items.GLOWSTONE
                    && !slots.get(lastAddedSlot).getItem().hasFoil()
            ) {
                close = order.size() >= OneAddons.closeCountChronomatron;
                hasData = false;
                return;
            }

            if (hasData || center.getItem() != Items.CLOCK) return;

            for (int i = 10; i <= 43; i++) {
                var stack = slots.get(i).getItem();
                if (!stack.isEmpty() && stack.hasFoil()) {
                    if (order.isEmpty() || order.get(order.size() - 1) != i) {
                        order.add(i);
                        lastAddedSlot = i;
                        hasData = true;
                        clicks = 0;
                    }
                    break;
                }
            }
        }

        @Override
        Integer nextClick() {
            if (hasData && clicks < order.size()) return order.get(clicks++);
            return null;
        }

        @Override
        boolean shouldClose(boolean autoClose) {
            if (!autoClose || !close) return false;
            if (clicks < order.size()) return false;
            close = false;
            return true;
        }
    }

    private class UltrasequencerHandler extends ExperimentHandler {
        private final ConcurrentHashMap<Integer, Integer> order = new ConcurrentHashMap<>();
        private boolean glowstoneSeen = false;

        @Override
        void observe(ChestMenu handler, List<Slot> slots) {
            var center = slots.get(49).getItem();
            if (center.isEmpty()) return;

            if (center.getItem() == Items.CLOCK) {
                // Clock phase: start clicking the saved order
                if (!order.isEmpty() && !hasData) {
                    hasData = true;
                    clicks = 0;
                }
                glowstoneSeen = false;
                return;
            }

            if (center.getItem() != Items.GLOWSTONE) return;

            // Glowstone phase: wait 1 tick for grid to settle, then scan
            if (!glowstoneSeen) {
                glowstoneSeen = true;
                hasData = false;
                return;
            }

            order.clear();

            for (int i = 9; i <= 44; i++) {
                var stack = slots.get(i).getItem();
                if (!stack.isEmpty() && isNumberedItem(stack)) {
                    order.put(stack.getCount() - 1, i);
                }
            }

            clicks = 0;
        }

        @Override
        Integer nextClick() {
            if (!hasData) return null;
            Integer slot = order.get(clicks);
            if (slot != null) {
                clicks++;
                return slot;
            }
            return null;
        }

        @Override
        boolean shouldClose(boolean autoClose) {
            return autoClose && order.size() >= OneAddons.closeCountUltrasequencer;
        }
    }

    private static boolean isNumberedItem(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (name.contains("§")) {
            name = name.replaceAll("§[0-9a-fk-or]", "");
        }
        return name.matches("\\d+");
    }

    private static void leftClick(Minecraft client, ChestMenu handler, int slot) {
        ContainerCompat.leftClick(handler.containerId, slot);
    }
}
