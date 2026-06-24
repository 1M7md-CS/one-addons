package com.mod.client.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ScreenGraphicsImpl implements ScreenGraphics {
    private final GuiGraphicsExtractor inner;

    public ScreenGraphicsImpl(GuiGraphicsExtractor inner) {
        this.inner = inner;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        inner.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        inner.text(font, text, x, y, color, shadow);
    }
}
