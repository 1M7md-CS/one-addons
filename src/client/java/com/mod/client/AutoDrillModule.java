package com.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

import java.util.concurrent.ThreadLocalRandom;

public class AutoDrillModule {

    private int prevSlot = 0;
    private int step = 0;
    private int delay = 0;

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (step > 0) {
            if (delay > 0) {
                delay--;
                return;
            }

            if (step == 1) {
                mc.player.getInventory().setSelectedSlot(6);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                delay = randDelay();
                step = 2;
                return;
            }

            if (step == 2) {
                mc.player.getInventory().setSelectedSlot(0);
                step = 0;
                return;
            }

            if (step == 3) {
                mc.player.getInventory().setSelectedSlot(0);
                step = 0;
                return;
            }
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (currentSlot == 7 && prevSlot != 7) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            delay = randDelay();
            step = 1;
        } else if (currentSlot == 2 && prevSlot != 2) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            delay = randDelay();
            step = 3;
        }

        prevSlot = currentSlot;
    }

    private static int randDelay() {
        return ThreadLocalRandom.current().nextInt(0, 3);
    }
}
