package com.mod.client.compat;

import net.minecraft.client.player.LocalPlayer;

public class EntityCompat {

    public static double getEyeY(LocalPlayer player) {
        return player.getY() + player.getEyeHeight(player.getPose());
    }

    private EntityCompat() {}
}
