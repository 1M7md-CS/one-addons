package com.mod.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class OneAddonsScreen extends Screen {

    private static final int C_OVERLAY = 0x88000000;
    private static final int C_PANEL = 0xFF202026;
    private static final int C_BORDER_LIGHT = 0xFF303036;
    private static final int C_BORDER_DARK = 0xFF101016;
    private static final int C_TAB_BG = 0x50303030;
    private static final int C_TAB_ACTIVE = 0x50000000;
    private static final int C_TITLE = 0xFFFFFFFF;
    private static final int C_CATEGORY = 0xFF7163EF;
    private static final int C_SEPARATOR = 0xFF303036;
    private static final int C_ROW_TEXT = 0xFFCCCCCC;
    private static final int C_ROW_HOVER = 0x15FFFFFF;
    private static final int C_SWITCH_ON = 0xFF2FE3C9;
    private static final int C_SWITCH_OFF = 0xFF4A4B59;
    private static final int C_SWITCH_KNOB = 0xFFFFFFFF;
    private static final int C_ACCENT = 0xFF7163EF;
    private static final int C_DIM = 0xFF6A6A7A;
    private static final int C_RED = 0xFFFF4444;

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 300;
    private static final int TAB_H = 20;
    private static final int PAD = 12;
    private static final int ROW_H = 22;
    private static final int SWITCH_W = 28;
    private static final int SWITCH_H = 11;
    private static final int BTN_H = 16;

    private int currentTab = 0;
    private boolean capturingKey = false;
    private int[] editingSlot = null;

    public OneAddonsScreen() {
        super(Text.literal("OneAddons"));
    }

    private int cx() { return width / 2 - PANEL_W / 2; }
    private int cy() { return height / 2 - PANEL_H / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, C_OVERLAY);
        int cx = cx(), cy = cy();

        ctx.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, C_BORDER_LIGHT);
        ctx.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_BORDER_DARK);
        ctx.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_PANEL);

        Text title = Text.literal("\u00A7e\u26A1 OneAddons v1.0");
        ctx.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, cy + 10, C_TITLE, true);

        int tabY = cy + 28;
        int tabW = (PANEL_W - 2) / 3;
        String[] tabs = {"\u2740 Rift", "\u2726 Enchant", "\u25C9 Utility"};
        for (int i = 0; i < 3; i++) {
            int tx = cx + 1 + i * tabW;
            boolean hover = mouseX >= tx && mouseX < tx + tabW - 1 && mouseY >= tabY && mouseY < tabY + TAB_H;
            int bg = i == currentTab ? C_TAB_ACTIVE : (hover ? 0x60303030 : C_TAB_BG);
            ctx.fill(tx, tabY, tx + tabW - 1, tabY + TAB_H, bg);
            ctx.drawText(textRenderer, Text.literal(tabs[i]), tx + tabW / 2 - textRenderer.getWidth(tabs[i]) / 2, tabY + 5, C_TITLE, true);
        }

        int lineY = tabY + TAB_H;
        ctx.fill(cx + 1, lineY, cx + PANEL_W - 1, lineY + 1, C_SEPARATOR);

        int contentY = lineY + 6;
        drawCurrentTab(ctx, cx, contentY, mouseX, mouseY);
    }

    private void drawCurrentTab(DrawContext ctx, int cx, int y, int mx, int my) {
        switch (currentTab) {
            case 0 -> drawCategory(ctx, cx, y, mx, my, "Plants",
                new Toggle("\u25C6 Flower", () -> OneAddons.flowerEnabled, v -> OneAddons.flowerEnabled = v),
                new Toggle("\u2746 Mushroom", () -> OneAddons.mushroomEnabled, v -> OneAddons.mushroomEnabled = v)
            );
            case 1 -> drawCategory(ctx, cx, y, mx, my, "Enchanting",
                new Toggle("\u2726 Enchanting", () -> OneAddons.enchantingEnabled, v -> OneAddons.enchantingEnabled = v)
            );
            default -> drawUtilityTab(ctx, cx, y, mx, my);
        }
    }

    private void drawUtilityTab(DrawContext ctx, int cx, int y, int mx, int my) {
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        // ---- Waypoint ----
        drawToggleRow(ctx, left, right, y, mx, my, "\u25C9 Waypoint",
            () -> OneAddons.waypointEnabled, v -> OneAddons.waypointEnabled = v);
        y += ROW_H;

        // keybind editor
        String keyName = keyName(OneAddons.waypointKeyCode);
        String bindText = capturingKey ? "Press a key..." : "Key: " + keyName;
        int bindColor = capturingKey ? C_ACCENT : C_DIM;
        int bindX = left + 20;
        int bindY = y + 2;
        boolean bindHover = !capturingKey && mx >= bindX && mx < bindX + textRenderer.getWidth("Key: " + keyName) + 6 && my >= bindY - 1 && my < bindY + 11;
        if (bindHover) ctx.fill(bindX - 2, bindY - 1, bindX + textRenderer.getWidth("Key: " + keyName) + 4, bindY + 11, C_ROW_HOVER);
        ctx.drawText(textRenderer, Text.literal(bindText), bindX, bindY, bindColor, true);
        y += 14;

        ctx.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        // ---- ChestAssist ----
        drawToggleRow(ctx, left, right, y, mx, my, "\u25C9 ChestAssist",
            () -> OneAddons.chestAssistEnabled, v -> OneAddons.chestAssistEnabled = v);
        y += ROW_H;

        // ---- CooldownFix ----
        drawToggleRow(ctx, left, right, y, mx, my, "\u23F1 CooldownFix",
            () -> OneAddons.cooldownFixEnabled, v -> OneAddons.cooldownFixEnabled = v);
        y += ROW_H;

        ctx.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        // ---- SwapAssist ----
        drawToggleRow(ctx, left, right, y, mx, my, "\u26CF SwapAssist",
            () -> OneAddons.swapAssistEnabled, v -> OneAddons.swapAssistEnabled = v);
        y += ROW_H;

        if (OneAddons.swapAssistEnabled) {
            var entries = OneAddons.swapAssistModule.entries;
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                int rowY = y;

                // delete button
                String del = "\u2716";
                int delX = left + 2;
                boolean delHover = mx >= delX && mx < delX + 10 && my >= rowY && my < rowY + ROW_H;
                ctx.drawText(textRenderer, Text.literal(del), delX, rowY + 5, delHover ? C_RED : C_DIM, true);

                // trigger slot
                String tSlot = "Slot " + e.triggerSlot();
                int tsX = left + 16;
                boolean editing = editingSlot != null && editingSlot[0] == i && editingSlot[1] == 0;
                boolean tsHover = editing || (mx >= tsX && mx < tsX + textRenderer.getWidth(tSlot) && my >= rowY && my < rowY + ROW_H);
                if (editing) ctx.fill(tsX - 1, rowY + 1, tsX + textRenderer.getWidth(tSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                ctx.drawText(textRenderer, Text.literal(tSlot), tsX, rowY + 5, tsHover ? C_ACCENT : C_ROW_TEXT, true);

                // trigger interact
                String tInt = e.triggerInteract() ? "[R]" : "[_]";
                int tiX = tsX + textRenderer.getWidth(tSlot) + 4;
                boolean tiHover = mx >= tiX && mx < tiX + textRenderer.getWidth(tInt) && my >= rowY && my < rowY + ROW_H;
                ctx.drawText(textRenderer, Text.literal(tInt), tiX, rowY + 5, tiHover ? C_ACCENT : C_DIM, true);

                // arrow
                String arrow = " \u2192 ";
                int arX = tiX + textRenderer.getWidth(tInt);
                ctx.drawText(textRenderer, Text.literal(arrow), arX, rowY + 5, C_DIM, true);

                // target slot
                String sSlot = "Slot " + e.targetSlot();
                int ssX = arX + textRenderer.getWidth(arrow);
                boolean sediting = editingSlot != null && editingSlot[0] == i && editingSlot[1] == 1;
                boolean ssHover = sediting || (mx >= ssX && mx < ssX + textRenderer.getWidth(sSlot) && my >= rowY && my < rowY + ROW_H);
                if (sediting) ctx.fill(ssX - 1, rowY + 1, ssX + textRenderer.getWidth(sSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                ctx.drawText(textRenderer, Text.literal(sSlot), ssX, rowY + 5, ssHover ? C_ACCENT : C_ROW_TEXT, true);

                // target interact
                String sInt = e.targetInteract() ? "[R]" : "[_]";
                int siX = ssX + textRenderer.getWidth(sSlot) + 4;
                boolean siHover = mx >= siX && mx < siX + textRenderer.getWidth(sInt) && my >= rowY && my < rowY + ROW_H;
                ctx.drawText(textRenderer, Text.literal(sInt), siX, rowY + 5, siHover ? C_ACCENT : C_DIM, true);

                y += ROW_H;
            }

            // Add button
            String addText = editingSlot != null ? "Press 0-8 to set slot" : "+ Add Swap";
            int addX = left + 20;
            int addY2 = y + 2;
            int addColor = editingSlot != null ? C_DIM : C_ACCENT;
            boolean addHover = editingSlot == null && mx >= addX && mx < addX + textRenderer.getWidth(addText) + 6 && my >= addY2 - 1 && my < addY2 + 11;
            if (addHover) ctx.fill(addX - 2, addY2 - 1, addX + textRenderer.getWidth(addText) + 4, addY2 + 11, C_ROW_HOVER);
            ctx.drawText(textRenderer, Text.literal(addText), addX, addY2, addColor, true);
        }
    }

    private void drawToggleRow(DrawContext ctx, int left, int right, int y, int mx, int my, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        boolean hover = mx >= left && mx < right && my >= y && my < y + ROW_H;
        if (hover) ctx.fill(left, y, right, y + ROW_H, C_ROW_HOVER);
        ctx.drawText(textRenderer, Text.literal(label), left + 2, y + 5, C_ROW_TEXT, true);

        int swX = right - SWITCH_W;
        int swY = y + (ROW_H - SWITCH_H) / 2;
        boolean on = getter.getAsBoolean();
        int bg = on ? C_SWITCH_ON : C_SWITCH_OFF;
        int knobX = on ? swX + SWITCH_W - 9 : swX;

        ctx.fill(swX, swY, swX + SWITCH_W, swY + SWITCH_H, 0xFF101016);
        ctx.fill(swX + 1, swY + 1, swX + SWITCH_W - 1, swY + SWITCH_H - 1, bg);
        ctx.fill(knobX, swY, knobX + 9, swY + SWITCH_H, C_SWITCH_KNOB);
    }

    private void drawCategory(DrawContext ctx, int cx, int y, int mx, int my, String name, Toggle... toggles) {
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        ctx.drawText(textRenderer, Text.literal(name), left, y, C_CATEGORY, true);
        y += 12;
        ctx.fill(left, y, right, y + 1, C_SEPARATOR);
        y += 8;

        for (Toggle t : toggles) {
            drawToggleRow(ctx, left, right, y, mx, my, t.displayText(), t.getter(), t.setter());
            y += ROW_H;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int cx = cx(), cy = cy();
        int mX = (int) click.x(), mY = (int) click.y();

        int tabY = cy + 28;
        int tabW = (PANEL_W - 2) / 3;
        for (int i = 0; i < 3; i++) {
            int tx = cx + 1 + i * tabW;
            if (mX >= tx && mX < tx + tabW - 1 && mY >= tabY && mY < tabY + TAB_H) {
                currentTab = i;
                capturingKey = false;
                editingSlot = null;
                return true;
            }
        }

        int lineY = tabY + TAB_H;
        int contentY = lineY + 6;

        if (currentTab == 2) {
            clickUtilityTab(mX, mY, cx, contentY);
            return true;
        }

        clickCategoryTab(mX, mY, cx, contentY);
        return super.mouseClicked(click, doubled);
    }

    private void clickUtilityTab(int mx, int my, int cx, int y) {
        editingSlot = null;
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        // Waypoint toggle
        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.waypointEnabled = !OneAddons.waypointEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        // Keybind
        String keyName = keyName(OneAddons.waypointKeyCode);
        int bindX = left + 20;
        int bindW = textRenderer.getWidth("Key: " + keyName) + 6;
        if (inRect(mx, my, bindX - 2, y + 1, bindW, 11)) {
            if (OneAddons.waypointKeyCode == GLFW.GLFW_KEY_UNKNOWN) {
                capturingKey = true;
            } else {
                OneAddons.waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
                capturingKey = true;
            }
            return;
        }
        y += 14;
        y += 6; // separator

        // ChestAssist toggle
        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.chestAssistEnabled = !OneAddons.chestAssistEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        // CooldownFix toggle
        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.cooldownFixEnabled = !OneAddons.cooldownFixEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        y += 6; // separator

        // SwapAssist toggle
        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.swapAssistEnabled = !OneAddons.swapAssistEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (OneAddons.swapAssistEnabled) {
            var entries = OneAddons.swapAssistModule.entries;
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                int rowY = y;

                int delX = left + 2;
                if (inRect(mx, my, delX, rowY, 10, ROW_H)) {
                    OneAddons.swapAssistModule.removeEntry(i);
                    OneAddonsConfig.save();
                    return;
                }

                String tSlot = "Slot " + e.triggerSlot();
                int tsX = left + 16;
                if (inRect(mx, my, tsX, rowY, textRenderer.getWidth(tSlot), ROW_H)) {
                    editingSlot = new int[]{i, 0};
                    return;
                }

                String tInt = e.triggerInteract() ? "[R]" : "[_]";
                int tiX = tsX + textRenderer.getWidth(tSlot) + 4;
                if (inRect(mx, my, tiX, rowY, textRenderer.getWidth(tInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries.set(i, new SwapAssistModule.SwapEntry(
                        e.triggerSlot(), !e.triggerInteract(), e.targetSlot(), e.targetInteract()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                String sSlot = "Slot " + e.targetSlot();
                int ssX = tiX + textRenderer.getWidth(tInt) + textRenderer.getWidth(" \u2192 ");
                if (inRect(mx, my, ssX, rowY, textRenderer.getWidth(sSlot), ROW_H)) {
                    editingSlot = new int[]{i, 1};
                    return;
                }

                String sInt = e.targetInteract() ? "[R]" : "[_]";
                int siX = ssX + textRenderer.getWidth(sSlot) + 4;
                if (inRect(mx, my, siX, rowY, textRenderer.getWidth(sInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries.set(i, new SwapAssistModule.SwapEntry(
                        e.triggerSlot(), e.triggerInteract(), e.targetSlot(), !e.targetInteract()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                y += ROW_H;
            }

            String addText = "+ Add Swap";
            int addX = left + 20;
            int addW = textRenderer.getWidth(addText) + 6;
            if (inRect(mx, my, addX - 2, y + 1, addW, 11)) {
                OneAddons.swapAssistModule.addEntry(0, false, 0, false);
                OneAddonsConfig.save();
                return;
            }
        }
    }

    private void clickCategoryTab(int mx, int my, int cx, int y) {
        Toggle[] toggles;
        switch (currentTab) {
            case 0 -> toggles = new Toggle[] {
                new Toggle("", () -> OneAddons.flowerEnabled, v -> OneAddons.flowerEnabled = v),
                new Toggle("", () -> OneAddons.mushroomEnabled, v -> OneAddons.mushroomEnabled = v)
            };
            case 1 -> toggles = new Toggle[] {
                new Toggle("", () -> OneAddons.enchantingEnabled, v -> OneAddons.enchantingEnabled = v)
            };
            default -> toggles = new Toggle[]{};
        }
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;
        y += 12; // category name
        y += 8;  // separator
        for (Toggle t : toggles) {
            int swX = right - SWITCH_W;
            if (mx >= swX && mx < swX + SWITCH_W && my >= y && my < y + ROW_H) {
                t.setter().accept(!t.getter().getAsBoolean());
                OneAddonsConfig.save();
                return;
            }
            y += ROW_H;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (capturingKey) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                OneAddons.waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
            } else {
                OneAddons.waypointKeyCode = input.key();
            }
            capturingKey = false;
            OneAddonsConfig.save();
            return true;
        }

        if (editingSlot != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                editingSlot = null;
                return true;
            }
            int num = input.key() - GLFW.GLFW_KEY_0;
            if (num >= 0 && num <= 8) {
                int idx = editingSlot[0];
                boolean isTrigger = editingSlot[1] == 0;
                var e = OneAddons.swapAssistModule.entries.get(idx);
                OneAddons.swapAssistModule.entries.set(idx, new SwapAssistModule.SwapEntry(
                    isTrigger ? num : e.triggerSlot(),
                    e.triggerInteract(),
                    isTrigger ? e.targetSlot() : num,
                    e.targetInteract()
                ));
                editingSlot = null;
                OneAddonsConfig.save();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "None";
        InputUtil.Key key = InputUtil.fromKeyCode(new KeyInput(code, 0, 0));
        return key.getLocalizedText().getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Toggle(String displayText, BooleanSupplier getter, Consumer<Boolean> setter) {}
}
