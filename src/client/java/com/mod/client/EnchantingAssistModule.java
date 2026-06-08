package com.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantingAssistModule {

    private static final long CLICK_DELAY_MS = 200L;
    private static final long DELAY_VARIETY_MS = 50L;
    private static final boolean AUTO_CLOSE = true;
    private static final int SERUM_COUNT = 0;
    private static final boolean GET_MAX_XP = false;

    private ExperimentHandler handler = null;
    private long lastClick = 0;

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            handler = null;
            return;
        }

        if (!(client.currentScreen instanceof HandledScreen<?> screen)
                || !(screen.getScreenHandler() instanceof GenericContainerScreenHandler containerHandler)) {
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

        if (handler.shouldClose(AUTO_CLOSE)) {
            client.player.closeHandledScreen();
            handler = null;
        }
    }

    private long delay() {
        return CLICK_DELAY_MS + ThreadLocalRandom.current().nextInt(0, (int) DELAY_VARIETY_MS + 1);
    }

    private abstract class ExperimentHandler {
        protected int clicks = 0;
        protected boolean hasData = false;

        abstract void observe(GenericContainerScreenHandler handler, List<Slot> slots);
        abstract Integer nextClick();
        abstract boolean shouldClose(boolean autoClose);
    }

    private class ChronomatronHandler extends ExperimentHandler {
        private final List<Integer> order = new ArrayList<>();
        private int lastAddedSlot = -1;
        private boolean close = false;

        @Override
        void observe(GenericContainerScreenHandler handler, List<Slot> slots) {
            var center = slots.get(49).getStack();

            if (lastAddedSlot != -1
                    && center.getItem() == net.minecraft.item.Items.GLOWSTONE
                    && !slots.get(lastAddedSlot).getStack().hasGlint()
            ) {
                close = order.size() > (GET_MAX_XP ? 15 : 11 - SERUM_COUNT);
                hasData = false;
                return;
            }

            if (hasData || center.getItem() != net.minecraft.item.Items.CLOCK) return;

            for (int i = 10; i <= 43; i++) {
                var stack = slots.get(i).getStack();
                if (!stack.isEmpty() && stack.hasGlint()) {
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

        @Override
        void observe(GenericContainerScreenHandler handler, List<Slot> slots) {
            var center = slots.get(49).getStack();
            if (center.isEmpty()) return;

            if (center.getItem() == net.minecraft.item.Items.CLOCK) {
                hasData = false;
                return;
            }

            if (hasData || center.getItem() != net.minecraft.item.Items.GLOWSTONE) return;

            order.clear();

            for (int i = 9; i <= 44; i++) {
                var stack = slots.get(i).getStack();
                if (!stack.isEmpty() && isNumberedItem(stack)) {
                    order.put(stack.getCount() - 1, i);
                }
            }

            hasData = true;
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
            return autoClose && order.size() > (GET_MAX_XP ? 20 : 9 - SERUM_COUNT);
        }
    }

    private static boolean isNumberedItem(ItemStack stack) {
        String name = stack.getName().getString();
        if (name.contains("§")) {
            name = name.replaceAll("§[0-9a-fk-or]", "");
        }
        return name.matches("\\d+");
    }

    private static void leftClick(MinecraftClient client, GenericContainerScreenHandler handler, int slot) {
        if (client.player == null) return;
        client.interactionManager.clickSlot(
                handler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }
}
