package com.mod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OneAddonsScreen extends Screen {

    private static final int PANEL_W = 190;
    private static final int PANEL_H = 135;
    private static final int BORDER_COLOR = 0xFF4C46A6;
    private static final int PANEL_COLOR = 0xF0101117;

    private ButtonWidget flowerBtn;
    private ButtonWidget mushroomBtn;
    private ButtonWidget enchantingBtn;

    public OneAddonsScreen() {
        super(Text.literal("OneAddons"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int cy = height / 2 - PANEL_H / 2;

        flowerBtn = ButtonWidget.builder(
            Text.literal("Flower: " + (OneAddons.flowerEnabled ? "ON" : "OFF")),
            btn -> {
                OneAddons.flowerEnabled = !OneAddons.flowerEnabled;
                btn.setMessage(Text.literal("Flower: " + (OneAddons.flowerEnabled ? "ON" : "OFF")));
            }
        ).dimensions(cx - 75, cy + 30, 150, 20).build();
        addDrawableChild(flowerBtn);

        mushroomBtn = ButtonWidget.builder(
            Text.literal("Mushroom: " + (OneAddons.mushroomEnabled ? "ON" : "OFF")),
            btn -> {
                OneAddons.mushroomEnabled = !OneAddons.mushroomEnabled;
                btn.setMessage(Text.literal("Mushroom: " + (OneAddons.mushroomEnabled ? "ON" : "OFF")));
            }
        ).dimensions(cx - 75, cy + 54, 150, 20).build();
        addDrawableChild(mushroomBtn);

        enchantingBtn = ButtonWidget.builder(
            Text.literal("Enchanting: " + (OneAddons.enchantingEnabled ? "ON" : "OFF")),
            btn -> {
                OneAddons.enchantingEnabled = !OneAddons.enchantingEnabled;
                btn.setMessage(Text.literal("Enchanting: " + (OneAddons.enchantingEnabled ? "ON" : "OFF")));
            }
        ).dimensions(cx - 75, cy + 78, 150, 20).build();
        addDrawableChild(enchantingBtn);

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            btn -> close()
        ).dimensions(cx - 35, cy + 106, 70, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int cx = width / 2 - PANEL_W / 2;
        int cy = height / 2 - PANEL_H / 2;
        ctx.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, BORDER_COLOR);
        ctx.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, PANEL_COLOR);
        int cxCenter = width / 2;
        Text title = Text.literal("\u00A7e\u26A1 OneAddons v1.0");
        ctx.drawText(textRenderer, title, cxCenter - textRenderer.getWidth(title) / 2, cy + 10, 0xFFFFFFFF, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
