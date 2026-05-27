package com.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnchantingModule implements ClientModInitializer {

    private static final long CLICK_DELAY_MS = 250L;

    private static final boolean AUTO_CLOSE = true;
    private static final int SERUM_COUNT = 0;
    private static final boolean GET_MAX_XP = false;

    public static boolean enabled = true;

    private enum ChronoPhase {
        IDLE,
        SHOWING,
        REPLAYING
    }

    private final HashMap<Integer, Integer> ultrasequencerOrder = new HashMap<>();
    private final ArrayList<Integer> chronomatronOrder = new ArrayList<>();

    private ChronoPhase chronoPhase = ChronoPhase.IDLE;

    private long lastClickTime = 0L;

    private boolean hasAdded = false;
    private boolean waitingForNext = false;
    private boolean ultraCaptured = false;

    private int lastAdded = 0;
    private int clicks = 0;

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("autoenchant")
                                .executes(ctx -> {

                                    enabled = !enabled;

                                    reset();

                                    ctx.getSource().sendFeedback(
                                            Text.literal(
                                                    "AutoEnchant "
                                                            + (enabled ? "§aEnabled" : "§cDisabled")
                                            )
                                    );

                                    return 1;
                                })
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void reset() {

        ultrasequencerOrder.clear();
        chronomatronOrder.clear();

        chronoPhase = ChronoPhase.IDLE;

        hasAdded = false;
        waitingForNext = false;
        ultraCaptured = false;

        lastAdded = 0;

        clicks = 0;

        lastClickTime = 0L;
    }

    public void tick(MinecraftClient client) {
        onTick(client);
    }

    private void onTick(MinecraftClient client) {

        if (!enabled
                || client.player == null
                || client.world == null
                || client.interactionManager == null) {

            return;
        }

        if (!(client.currentScreen instanceof HandledScreen<?> screen)
                || !(screen.getScreenHandler() instanceof GenericContainerScreenHandler handler)) {

            reset();
            return;
        }

        List<Slot> slots = handler.slots;

        if (slots.size() < 54) {
            return;
        }

        String title = screen.getTitle().getString();

        if (title.startsWith("Chronomatron (")) {

            solveChronomatron(client, handler, slots);

        } else if (title.startsWith("Ultrasequencer (")) {

            solveUltraSequencer(client, handler, slots);

        } else {

            reset();
        }
    }

    private void solveChronomatron(
            MinecraftClient client,
            GenericContainerScreenHandler handler,
            List<Slot> invSlots
    ) {

        int maxChronomatron = GET_MAX_XP
                ? 15
                : 11 - SERUM_COUNT;

        ItemStack progress = getStack(invSlots, 49);

        boolean glowstone = isGlowstone(progress);
        boolean clock = isClock(progress);

        if (glowstone) {

            chronoPhase = ChronoPhase.SHOWING;

            if (waitingForNext
                    && lastAdded != 0
                    && !getStack(invSlots, lastAdded).isEmpty()
                    && !isEnchanted(getStack(invSlots, lastAdded))) {

                waitingForNext = false;
            }
        }

        if (chronoPhase == ChronoPhase.SHOWING
                && !waitingForNext
                && clock) {

            for (int i = 10; i <= 43; i++) {

                ItemStack stack = getStack(invSlots, i);

                if (!isEnchanted(stack)) {
                    continue;
                }

                if (chronomatronOrder.isEmpty()
                        || chronomatronOrder.get(chronomatronOrder.size() - 1) != i) {

                    chronomatronOrder.add(i);

                    lastAdded = i;

                    waitingForNext = true;

                    clicks = 0;

                    break;
                }
            }
        }

        if (clock && !chronomatronOrder.isEmpty()) {

            chronoPhase = ChronoPhase.REPLAYING;

            if (AUTO_CLOSE
                    && chronomatronOrder.size() > maxChronomatron) {

                client.player.closeHandledScreen();
                return;
            }

            long now = System.currentTimeMillis();

            if (clicks < chronomatronOrder.size()
                    && now - lastClickTime > CLICK_DELAY_MS) {

                int slot = chronomatronOrder.get(clicks);

                leftClick(client, handler, slot);

                lastClickTime = now;

                clicks++;
            }
        }

        if (clicks >= chronomatronOrder.size()
                && chronoPhase == ChronoPhase.REPLAYING
                && glowstone) {

            chronoPhase = ChronoPhase.SHOWING;

            clicks = 0;
        }
    }

    private void solveUltraSequencer(
            MinecraftClient client,
            GenericContainerScreenHandler handler,
            List<Slot> invSlots
    ) {

        int maxUltraSequencer = GET_MAX_XP
                ? 20
                : 9 - SERUM_COUNT;

        ItemStack progress = getStack(invSlots, 49);

        if (isClock(progress)) {
            ultraCaptured = false;
        }

        if (!ultraCaptured && isGlowstone(progress)) {

            if (getStack(invSlots, 44).isEmpty()) {
                return;
            }

            ultrasequencerOrder.clear();

            for (int i = 9; i <= 44; i++) {

                ItemStack stack = getStack(invSlots, i);

                if (isDye(stack)) {

                    ultrasequencerOrder.put(
                            stack.getCount() - 1,
                            i
                    );
                }
            }

            ultraCaptured = true;

            clicks = 0;

            if (AUTO_CLOSE
                    && ultrasequencerOrder.size() > maxUltraSequencer) {

                client.player.closeHandledScreen();
            }
        }

        if (isClock(progress)
                && ultrasequencerOrder.containsKey(clicks)
                && System.currentTimeMillis() - lastClickTime > CLICK_DELAY_MS) {

            Integer slot = ultrasequencerOrder.get(clicks);

            if (slot != null) {

                leftClick(client, handler, slot);
            }

            lastClickTime = System.currentTimeMillis();

            clicks++;
        }
    }

    private static ItemStack getStack(List<Slot> slots, int index) {

        return (index >= 0 && index < slots.size())
                ? slots.get(index).getStack()
                : ItemStack.EMPTY;
    }

    private static boolean isClock(ItemStack stack) {

        return !stack.isEmpty()
                && stack.getItem() == Items.CLOCK;
    }

    private static boolean isGlowstone(ItemStack stack) {

        return !stack.isEmpty()
                && stack.getItem() == Items.GLOWSTONE;
    }

    private static boolean isEnchanted(ItemStack stack) {

        return !stack.isEmpty()
                && stack.hasGlint();
    }

    private static boolean isDye(ItemStack stack) {

        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        if (item == Items.LAPIS_LAZULI
                || item == Items.BONE_MEAL
                || item == Items.INK_SAC) {

            return true;
        }

        String path = item.toString().toLowerCase();

        return path.contains("dye")
                || path.contains("lapis")
                || path.contains("meal");
    }

    private static void leftClick(
            MinecraftClient client,
            GenericContainerScreenHandler handler,
            int slot
    ) {

        if (client.player == null) {
            return;
        }

        client.interactionManager.clickSlot(
                handler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }
}