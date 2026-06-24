package com.mod.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ScreenCompat {

    public static ScreenGraphics wrap(GuiGraphicsExtractor ctx) {
        return new ScreenGraphicsImpl(ctx);
    }

    public static KeyEventData extract(KeyEvent event) {
        return new KeyEventData(event.key(), event.scancode(), event.modifiers());
    }

    public static MouseEventData extract(MouseButtonEvent event, boolean doubleClick) {
        return new MouseEventData(event.x(), event.y(), event.button(), doubleClick);
    }

    public static String keyName(int code) {
        if (code == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return "None";
        var key = InputConstants.getKey(new KeyEvent(code, 0, 0));
        return key.getDisplayName().getString();
    }

    public record KeyEventData(int keyCode, int scanCode, int modifiers) {}
    public record MouseEventData(double x, double y, int button, boolean doubleClick) {}

    private ScreenCompat() {}
}
