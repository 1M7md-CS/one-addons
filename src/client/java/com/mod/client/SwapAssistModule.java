package com.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SwapAssistModule {

    public final List<SwapEntry> entries = new ArrayList<>();

    private final boolean[] prevInSlot = new boolean[9];
    private int step = 0;
    private int stepEntry = 0;
    private int delay = 0;

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (step > 0) {
            if (delay > 0) {
                delay--;
                return;
            }

            SwapEntry e = entries.get(stepEntry);
            if (step == 1) {
                mc.player.getInventory().setSelectedSlot(e.targetSlot);
                step = 0;
                prevInSlot[e.triggerSlot] = false;
                return;
            }
            if (step == 2) {
                mc.player.getInventory().setSelectedSlot(e.targetSlot);
                delay = randDelay();
                step = 3;
                return;
            }
            if (step == 3) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                delay = randDelay();
                step = 0;
                prevInSlot[e.triggerSlot] = false;
                return;
            }
        }

        for (int i = 0; i < entries.size(); i++) {
            SwapEntry e = entries.get(i);
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            boolean nowIn = currentSlot == e.triggerSlot;

            if (nowIn && !prevInSlot[e.triggerSlot]) {
                stepEntry = i;
                if (e.triggerInteract) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                delay = randDelay();
                if (e.targetSlot != e.triggerSlot) {
                    step = e.targetInteract ? 2 : 1;
                } else {
                    step = e.targetInteract ? 3 : 0;
                    if (!e.targetInteract) step = 0;
                }
            }

            prevInSlot[e.triggerSlot] = nowIn;
        }
    }

    public void addEntry(int triggerSlot, boolean triggerInteract, int targetSlot, boolean targetInteract) {
        entries.add(new SwapEntry(triggerSlot, triggerInteract, targetSlot, targetInteract));
        prevInSlot[triggerSlot] = false;
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            prevInSlot[entries.get(index).triggerSlot] = false;
            entries.remove(index);
            step = 0;
        }
    }

    public record SwapEntry(int triggerSlot, boolean triggerInteract, int targetSlot, boolean targetInteract) {}

    private static int randDelay() {
        return ThreadLocalRandom.current().nextInt(0, 3);
    }
}
