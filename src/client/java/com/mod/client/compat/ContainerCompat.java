package com.mod.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;

public class ContainerCompat {

    public static void leftClick(int containerId, int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleContainerInput(
                containerId,
                slot,
                0,
                ContainerInput.PICKUP,
                mc.player
        );
    }

    private ContainerCompat() {}
}
