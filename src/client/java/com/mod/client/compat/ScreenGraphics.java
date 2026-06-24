package com.mod.client.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public interface ScreenGraphics {
    void fill(int x1, int y1, int x2, int y2, int color);
    void text(Font font, Component text, int x, int y, int color, boolean shadow);
}
