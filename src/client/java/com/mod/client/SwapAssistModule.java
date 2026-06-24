package com.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SwapAssistModule {

    private static final int POST_SWAP_DELAY = 10;

    public final List<SwapEntry> entries = new ArrayList<>();

    private final boolean[] prevInSlot = new boolean[9];
    private int step = 0;
    private int stepEntry = 0;
    private int delay = 0;

    public void tick(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return;

        boolean hasEnabled = false;
        for (SwapEntry e : entries) {
            if (e.enabled) { hasEnabled = true; break; }
        }
        if (!hasEnabled) {
            step = 0;
            return;
        }

        if (step > 0) {
            if (delay > 0) {
                delay--;
                return;
            }

            if (stepEntry >= entries.size()) {
                step = 0;
                return;
            }

            SwapEntry e = entries.get(stepEntry);
            if (!e.enabled) {
                step = 0;
                return;
            }

            if (step == 1) {
                mc.player.getInventory().setSelectedSlot(e.targetSlot);
                step = 0;
                prevInSlot[e.triggerSlot] = false;
                return;
            }

            if (step == 2) {
                mc.player.getInventory().setSelectedSlot(e.targetSlot);
                delay = POST_SWAP_DELAY;
                step = 3;
                return;
            }

            if (step == 3) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                delay = randDelay();
                step = 0;
                prevInSlot[e.triggerSlot] = false;
                return;
            }
        }

        for (int i = 0; i < entries.size(); i++) {
            SwapEntry e = entries.get(i);
            if (!e.enabled) continue;

            int currentSlot = mc.player.getInventory().getSelectedSlot();
            boolean nowIn = currentSlot == e.triggerSlot;

            if (nowIn && !prevInSlot[e.triggerSlot]) {
                stepEntry = i;

                if (e.triggerInteract) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }

                delay = randDelay();

                if (e.targetSlot != e.triggerSlot) {
                    step = e.targetInteract ? 2 : 1;
                } else {
                    step = e.targetInteract ? 3 : 0;
                }
            }

            prevInSlot[e.triggerSlot] = nowIn;
        }
    }

    public void addEntry(int triggerSlot,
                         boolean triggerInteract,
                         int targetSlot,
                         boolean targetInteract,
                         boolean enabled) {
        entries.add(new SwapEntry(triggerSlot, triggerInteract, targetSlot, targetInteract, enabled));
        prevInSlot[triggerSlot] = false;
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            prevInSlot[entries.get(index).triggerSlot] = false;
            entries.remove(index);
            step = 0;
        }
    }

    public record SwapEntry(
            int triggerSlot,
            boolean triggerInteract,
            int targetSlot,
            boolean targetInteract,
            boolean enabled
    ) {}

    private static int randDelay() {
        return ThreadLocalRandom.current().nextInt(1, 3);
    }
}
