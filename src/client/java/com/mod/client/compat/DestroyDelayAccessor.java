package com.mod.client.compat;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public interface DestroyDelayAccessor {
    void setDestroyDelay(int value);

    static void reset(MultiPlayerGameMode gm) {
        ((DestroyDelayAccessor) gm).setDestroyDelay(0);
    }
}
