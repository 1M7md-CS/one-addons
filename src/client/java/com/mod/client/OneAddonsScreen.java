package com.mod.client;

import com.mod.client.compat.ScreenCompat;
import com.mod.client.compat.ScreenGraphics;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
    private static final int SCROLLBAR_W = 3;
    private static final int SCROLLBAR_MIN_H = 16;

    private int currentTab = 0;
    private boolean capturingKey = false;
    private int[] editingSlot = null;
    private int[] editingPlaceField = null;
    private int editingCloseField = -1;
    private String editingCloseBuf = "";
    private boolean editingKeyMakerDelay = false;
    private String editingKeyMakerDelayBuf = "";
    private int scrollOffset = 0;

    public OneAddonsScreen() {
        super(Component.literal("OneAddons"));
    }

    private int cx() { return width / 2 - PANEL_W / 2; }
    private int cy() { return height / 2 - PANEL_H / 2; }

    // =====================================================================
    // VERSION-SPECIFIC: Replace these 3 overrides for each MC version
    // =====================================================================

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        onRender(ScreenCompat.wrap(ctx), mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (onMouseClicked((int) event.x(), (int) event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (onKeyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (onMouseScrolled((int) mouseX, (int) mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // =====================================================================
    // STABLE: Everything below is version-independent
    // =====================================================================

    private void onRender(ScreenGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, C_OVERLAY);
        int cx = cx(), cy = cy();

        g.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, C_BORDER_LIGHT);
        g.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_BORDER_DARK);
        g.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_PANEL);

        Component title = Component.literal("\u00A7e\u26A1 OneAddons");
        g.text(font, title, width / 2 - font.width(title) / 2, cy + 10, C_TITLE, true);

        int tabY = cy + 28;
        int tabW = (PANEL_W - 2) / 3;
        String[] tabs = {"\u2740 Rift", "\u2726 Enchant", "\u25C9 Utility"};
        for (int i = 0; i < 3; i++) {
            int tx = cx + 1 + i * tabW;
            boolean hover = mouseX >= tx && mouseX < tx + tabW - 1 && mouseY >= tabY && mouseY < tabY + TAB_H;
            int bg = i == currentTab ? C_TAB_ACTIVE : (hover ? 0x60303030 : C_TAB_BG);
            g.fill(tx, tabY, tx + tabW - 1, tabY + TAB_H, bg);
            g.text(font, Component.literal(tabs[i]), tx + tabW / 2 - font.width(tabs[i]) / 2, tabY + 5, C_TITLE, true);
        }

        int lineY = tabY + TAB_H;
        g.fill(cx + 1, lineY, cx + PANEL_W - 1, lineY + 1, C_SEPARATOR);

        int contentY = lineY + 6;
        int contentBottom = cy + PANEL_H - 6;

        g.enableScissor(cx + 1, contentY, cx + PANEL_W - 1, contentBottom);
        drawCurrentTab(g, cx, contentY - scrollOffset, mouseX, mouseY);
        g.disableScissor();

        int contentH = contentBottom - contentY;
        int totalH = computeContentHeight();
        if (totalH > contentH) {
            int maxScroll = totalH - contentH;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            int thumbH = Math.max(SCROLLBAR_MIN_H, contentH * contentH / totalH);
            int thumbY = contentY + (contentH - thumbH) * scrollOffset / maxScroll;
            int sbX = cx + PANEL_W - SCROLLBAR_W - 4;
            g.fill(sbX, contentY, sbX + SCROLLBAR_W, contentBottom, 0x30FFFFFF);
            g.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbH, 0x80FFFFFF);
        }
    }

    private void drawCurrentTab(ScreenGraphics g, int cx, int y, int mx, int my) {
        switch (currentTab) {
            case 0 -> drawCategory(g, cx, y, mx, my, "Plants",
                new Toggle("\u25C6 Flower", () -> OneAddons.flowerEnabled, v -> OneAddons.flowerEnabled = v),
                new Toggle("\u2746 Mushroom", () -> OneAddons.mushroomEnabled, v -> OneAddons.mushroomEnabled = v)
            );
            case 1 -> drawEnchantTab(g, cx, y, mx, my);
            default -> drawUtilityTab(g, cx, y, mx, my);
        }
    }

    private void drawEnchantTab(ScreenGraphics g, int cx, int y, int mx, int my) {
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        g.text(font, Component.literal("Enchanting"), left, y, C_CATEGORY, true);
        y += 12;
        g.fill(left, y, right, y + 1, C_SEPARATOR);
        y += 8;

        drawToggleRow(g, left, right, y, mx, my, "\u2726 Enchanting",
            () -> OneAddons.enchantingEnabled, v -> OneAddons.enchantingEnabled = v);
        y += ROW_H;

        if (!OneAddons.enchantingEnabled) return;

        // Auto close
        String acTog = OneAddons.autoClose ? "[✓]" : "[ ]";
        int acTogX = left + 14;
        boolean acHover = mx >= acTogX && mx < acTogX + font.width(acTog) && my >= y && my < y + ROW_H;
        g.text(font, Component.literal(acTog), acTogX, y + 5, acHover ? C_ACCENT : (OneAddons.autoClose ? C_SWITCH_ON : C_DIM), true);
        int acLbX = acTogX + font.width(acTog) + 4;
        g.text(font, Component.literal("Auto close"), acLbX, y + 5, C_ROW_TEXT, true);
        y += ROW_H;

        // Close entries — exact SwapAssist style
        if (OneAddons.autoClose) {
        for (int i = 0; i < 2; i++) {
            boolean isChrono = i == 0;
            int count = isChrono ? OneAddons.closeCountChronomatron : OneAddons.closeCountUltrasequencer;
            boolean en = isChrono ? OneAddons.closeChronoEnabled : OneAddons.closeUltraEnabled;
            int rowY = y;

            String tog = en ? "[✓]" : "[ ]";
            int togX = left + 14;
            boolean togHover = mx >= togX && mx < togX + font.width(tog) && my >= rowY && my < rowY + ROW_H;
            g.text(font, Component.literal(tog), togX, rowY + 5, togHover ? C_ACCENT : (en ? C_SWITCH_ON : C_DIM), true);

            String name = isChrono ? "Chronomatron" : "Ultrasequencer";
            int nmX = togX + font.width(tog) + 4;
            g.text(font, Component.literal(name), nmX, rowY + 5, C_ROW_TEXT, true);

            boolean editing = editingCloseField == i;
            String numText = editing
                ? (editingCloseBuf.isEmpty() ? "__" : editingCloseBuf)
                : String.valueOf(count);
            int numX = nmX + font.width(name) + 6;
            boolean numHover = editing || (mx >= numX && mx < numX + font.width(numText) + 4 && my >= rowY && my < rowY + ROW_H);
            if (editing) g.fill(numX - 1, rowY + 1, numX + font.width(numText) + 3, rowY + ROW_H - 1, 0x307163EF);
            g.text(font, Component.literal(numText), numX + 1, rowY + 5, editing ? C_ACCENT : (numHover ? C_ACCENT : C_ROW_TEXT), true);

            y += ROW_H;
        }
        }
    }

    private void drawUtilityTab(ScreenGraphics g, int cx, int y, int mx, int my) {
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        drawToggleRow(g, left, right, y, mx, my, "\u25C9 Waypoint",
            () -> OneAddons.waypointEnabled, v -> OneAddons.waypointEnabled = v);
        y += ROW_H;

        String keyName = ScreenCompat.keyName(OneAddons.waypointKeyCode);
        String bindText = capturingKey ? "Press a key..." : "Key: " + keyName;
        int bindColor = capturingKey ? C_ACCENT : C_DIM;
        int bindX = left + 20;
        int bindY = y + 2;
        boolean bindHover = !capturingKey && mx >= bindX && mx < bindX + font.width("Key: " + keyName) + 6 && my >= bindY - 1 && my < bindY + 11;
        if (bindHover) g.fill(bindX - 2, bindY - 1, bindX + font.width("Key: " + keyName) + 4, bindY + 11, C_ROW_HOVER);
        g.text(font, Component.literal(bindText), bindX, bindY, bindColor, true);
        y += 14;

        g.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        drawToggleRow(g, left, right, y, mx, my, "\u25C9 ChestAssist",
            () -> OneAddons.chestAssistEnabled, v -> OneAddons.chestAssistEnabled = v);
        y += ROW_H;

        drawToggleRow(g, left, right, y, mx, my, "\u23F1 CooldownFix",
            () -> OneAddons.cooldownFixEnabled, v -> OneAddons.cooldownFixEnabled = v);
        y += ROW_H;

        g.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        drawToggleRow(g, left, right, y, mx, my, "\u26CF SwapAssist",
            () -> OneAddons.swapAssistEnabled, v -> OneAddons.swapAssistEnabled = v);
        y += ROW_H;

        if (OneAddons.swapAssistEnabled) {
            var entries = OneAddons.swapAssistModule.entries;
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                int rowY = y;

                String del = "\u2716";
                int delX = left + 2;
                boolean delHover = mx >= delX && mx < delX + 10 && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(del), delX, rowY + 5, delHover ? C_RED : C_DIM, true);

                String tog = e.enabled() ? "[✓]" : "[ ]";
                int togX = left + 14;
                boolean togHover = mx >= togX && mx < togX + font.width(tog) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(tog), togX, rowY + 5, togHover ? C_ACCENT : (e.enabled() ? C_SWITCH_ON : C_DIM), true);

                String tSlot = "Slot " + e.triggerSlot();
                int tsX = togX + font.width(tog) + 4;
                boolean editing = editingSlot != null && editingSlot[0] == i && editingSlot[1] == 0;
                boolean tsHover = editing || (mx >= tsX && mx < tsX + font.width(tSlot) && my >= rowY && my < rowY + ROW_H);
                if (editing) g.fill(tsX - 1, rowY + 1, tsX + font.width(tSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                g.text(font, Component.literal(tSlot), tsX, rowY + 5, tsHover ? C_ACCENT : C_ROW_TEXT, true);

                String tInt = e.triggerInteract() ? "[R]" : "[_]";
                int tiX = tsX + font.width(tSlot) + 4;
                boolean tiHover = mx >= tiX && mx < tiX + font.width(tInt) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(tInt), tiX, rowY + 5, tiHover ? C_ACCENT : C_DIM, true);

                String arrow = " \u2192 ";
                int arX = tiX + font.width(tInt);
                g.text(font, Component.literal(arrow), arX, rowY + 5, C_DIM, true);

                String sSlot = "Slot " + e.targetSlot();
                int ssX = arX + font.width(arrow);
                boolean sediting = editingSlot != null && editingSlot[0] == i && editingSlot[1] == 1;
                boolean ssHover = sediting || (mx >= ssX && mx < ssX + font.width(sSlot) && my >= rowY && my < rowY + ROW_H);
                if (sediting) g.fill(ssX - 1, rowY + 1, ssX + font.width(sSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                g.text(font, Component.literal(sSlot), ssX, rowY + 5, ssHover ? C_ACCENT : C_ROW_TEXT, true);

                String sInt = e.targetInteract() ? "[R]" : "[_]";
                int siX = ssX + font.width(sSlot) + 4;
                boolean siHover = mx >= siX && mx < siX + font.width(sInt) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(sInt), siX, rowY + 5, siHover ? C_ACCENT : C_DIM, true);

                y += ROW_H;
            }

            y += 3;

            String addText = editingSlot != null ? "Press 0-8 to set slot" : "+ Add Swap";
            int addX = left + 20;
            int addH = 16;
            boolean addHover = editingSlot == null && mx >= addX && mx < addX + font.width(addText) + 6 && my >= y && my < y + addH;
            if (addHover) g.fill(addX - 2, y, addX + font.width(addText) + 4, y + addH, C_ROW_HOVER);
            g.text(font, Component.literal(addText), addX, y + 4, editingSlot != null ? C_DIM : C_ACCENT, true);
            y += addH;
        }

        g.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        drawToggleRow(g, left, right, y, mx, my, "\u2302 PlaceOnPosition",
            () -> OneAddons.placeOnPositionEnabled, v -> OneAddons.placeOnPositionEnabled = v);
        y += ROW_H;

        if (OneAddons.placeOnPositionEnabled) {
            var entries = OneAddons.placeOnPositionModule.entries;
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                int rowY = y;

                String del = "\u2716";
                int delX = left + 2;
                boolean delHover = mx >= delX && mx < delX + 10 && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(del), delX, rowY + 5, delHover ? C_RED : C_DIM, true);

                String tog = e.enabled() ? "[✓]" : "[ ]";
                int togX = left + 14;
                boolean togHover = mx >= togX && mx < togX + font.width(tog) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(tog), togX, rowY + 5, togHover ? C_ACCENT : (e.enabled() ? C_SWITCH_ON : C_DIM), true);

                String pSlot = "Slot " + e.placeSlot();
                int psX = togX + font.width(tog) + 4;
                boolean psEditing = editingPlaceField != null && editingPlaceField[0] == i && editingPlaceField[1] == 0;
                boolean psHover = psEditing || (mx >= psX && mx < psX + font.width(pSlot) && my >= rowY && my < rowY + ROW_H);
                if (psEditing) g.fill(psX - 1, rowY + 1, psX + font.width(pSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                g.text(font, Component.literal(pSlot), psX, rowY + 5, psHover ? C_ACCENT : C_ROW_TEXT, true);

                String pInt = e.placeInteract() ? "[R]" : "[_]";
                int piX = psX + font.width(pSlot) + 4;
                boolean piHover = mx >= piX && mx < piX + font.width(pInt) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(pInt), piX, rowY + 5, piHover ? C_ACCENT : C_DIM, true);

                String arrow = " \u2192 ";
                int arX = piX + font.width(pInt);
                g.text(font, Component.literal(arrow), arX, rowY + 5, C_DIM, true);

                String rSlot = "Slot " + e.restoreSlot();
                int rsX = arX + font.width(arrow);
                boolean rsEditing = editingPlaceField != null && editingPlaceField[0] == i && editingPlaceField[1] == 1;
                boolean rsHover = rsEditing || (mx >= rsX && mx < rsX + font.width(rSlot) && my >= rowY && my < rowY + ROW_H);
                if (rsEditing) g.fill(rsX - 1, rowY + 1, rsX + font.width(rSlot) + 1, rowY + ROW_H - 1, 0x307163EF);
                g.text(font, Component.literal(rSlot), rsX, rowY + 5, rsHover ? C_ACCENT : C_ROW_TEXT, true);

                String rInt = e.restoreInteract() ? "[R]" : "[_]";
                int riX = rsX + font.width(rSlot) + 4;
                boolean riHover = mx >= riX && mx < riX + font.width(rInt) && my >= rowY && my < rowY + ROW_H;
                g.text(font, Component.literal(rInt), riX, rowY + 5, riHover ? C_ACCENT : C_DIM, true);

                y += ROW_H;
            }

            y += 3;

            String addText = editingPlaceField != null ? "Press 0-8 to set slot" : "+ Add Place";
            int addX = left + 20;
            int addH = 16;
            boolean addHover = editingPlaceField == null && mx >= addX && mx < addX + font.width(addText) + 6 && my >= y && my < y + addH;
            if (addHover) g.fill(addX - 2, y, addX + font.width(addText) + 4, y + addH, C_ROW_HOVER);
            g.text(font, Component.literal(addText), addX, y + 4, editingPlaceField != null ? C_DIM : C_ACCENT, true);
            y += addH;
        }

        g.fill(left + 20, y, right, y + 1, C_SEPARATOR);
        y += 6;

        drawToggleRow(g, left, right, y, mx, my, "\u2692 KeyMaker",
            () -> OneAddons.keyMakerEnabled, v -> OneAddons.keyMakerEnabled = v);
        y += ROW_H;

        if (OneAddons.keyMakerEnabled) {
            String modeStr = "Mode: " + OneAddons.keyMakerMode.name();
            int modeX = left + 14;
            boolean modeHover = mx >= modeX && mx < modeX + font.width(modeStr) && my >= y && my < y + ROW_H;
            g.text(font, Component.literal(modeStr), modeX, y + 5, modeHover ? C_ACCENT : C_ROW_TEXT, true);
            y += ROW_H;

            boolean editing = editingKeyMakerDelay;
            String delayStr = editing
                ? (editingKeyMakerDelayBuf.isEmpty() ? "___" : editingKeyMakerDelayBuf)
                : "Delay: " + OneAddons.keyMakerClickDelay + "ms";
            int delayX = left + 14;
            boolean delayHover = editing || (mx >= delayX && mx < delayX + font.width(delayStr) && my >= y && my < y + ROW_H);
            int delayColor = editing ? C_ACCENT : (delayHover ? C_ACCENT : C_DIM);
            if (editing) g.fill(delayX - 1, y + 1, delayX + font.width(delayStr) + 3, y + ROW_H - 1, 0x307163EF);
            g.text(font, Component.literal(delayStr), delayX, y + 5, delayColor, true);
            y += ROW_H;
        }
    }

    private void drawToggleRow(ScreenGraphics g, int left, int right, int y, int mx, int my, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        boolean hover = mx >= left && mx < right && my >= y && my < y + ROW_H;
        if (hover) g.fill(left, y, right, y + ROW_H, C_ROW_HOVER);
        g.text(font, Component.literal(label), left + 2, y + 5, C_ROW_TEXT, true);

        int swX = right - SWITCH_W;
        int swY = y + (ROW_H - SWITCH_H) / 2;
        boolean on = getter.getAsBoolean();
        int bg = on ? C_SWITCH_ON : C_SWITCH_OFF;
        int knobX = on ? swX + SWITCH_W - 9 : swX;

        g.fill(swX, swY, swX + SWITCH_W, swY + SWITCH_H, 0xFF101016);
        g.fill(swX + 1, swY + 1, swX + SWITCH_W - 1, swY + SWITCH_H - 1, bg);
        g.fill(knobX, swY, knobX + 9, swY + SWITCH_H, C_SWITCH_KNOB);
    }

    private void drawCategory(ScreenGraphics g, int cx, int y, int mx, int my, String name, Toggle... toggles) {
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        g.text(font, Component.literal(name), left, y, C_CATEGORY, true);
        y += 12;
        g.fill(left, y, right, y + 1, C_SEPARATOR);
        y += 8;

        for (Toggle t : toggles) {
            drawToggleRow(g, left, right, y, mx, my, t.displayText(), t.getter(), t.setter());
            y += ROW_H;
        }
    }

    private boolean onMouseClicked(int mx, int my, int button) {
        if (button != 0) return false;
        int cx = cx(), cy = cy();

        int tabY = cy + 28;
        int tabW = (PANEL_W - 2) / 3;
        for (int i = 0; i < 3; i++) {
            int tx = cx + 1 + i * tabW;
            if (mx >= tx && mx < tx + tabW - 1 && my >= tabY && my < tabY + TAB_H) {
                currentTab = i;
                capturingKey = false;
                editingSlot = null;
                editingPlaceField = null;
                editingKeyMakerDelay = false;
                scrollOffset = 0;
                return true;
            }
        }

        int lineY = tabY + TAB_H;
        int contentY = lineY + 6;

        if (currentTab == 1) {
            clickEnchantTab(mx, my, cx, contentY - scrollOffset);
            return true;
        }

        if (currentTab == 2) {
            clickUtilityTab(mx, my, cx, contentY - scrollOffset);
            return true;
        }

        clickCategoryTab(mx, my, cx, contentY - scrollOffset);
        return false;
    }

    private boolean onMouseScrolled(int mx, int my, double amount) {
        int tabY = cy() + 28;
        int lineY = tabY + TAB_H;
        int contentY = lineY + 6;
        int contentBottom = cy() + PANEL_H - 6;
        if (mx >= cx() + 1 && mx < cx() + PANEL_W - 1 && my >= contentY && my < contentBottom) {
            int totalH = computeContentHeight();
            int contentH = contentBottom - contentY;
            if (totalH > contentH) {
                int maxScroll = totalH - contentH;
                scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - amount * 20));
                return true;
            }
        }
        return false;
    }

    private int computeContentHeight() {
        return switch (currentTab) {
            case 0 -> 12 + 8 + 2 * ROW_H + 10;
            case 1 -> {
                int h = 12 + 8 + ROW_H + 10;
                if (OneAddons.enchantingEnabled) {
                    h += ROW_H;
                    if (OneAddons.autoClose) h += 2 * ROW_H;
                }
                yield h;
            }
            default -> {
                int h = ROW_H + 14;
                h += 6 + ROW_H + ROW_H;
                h += 6 + ROW_H;
                if (OneAddons.swapAssistEnabled) {
                    h += OneAddons.swapAssistModule.entries.size() * ROW_H + 3 + 16;
                }
                h += 6 + ROW_H;
                if (OneAddons.placeOnPositionEnabled) {
                    h += OneAddons.placeOnPositionModule.entries.size() * ROW_H + 3 + 16;
                }
                h += 6 + ROW_H;
                if (OneAddons.keyMakerEnabled) {
                    h += ROW_H + ROW_H;
                }
                yield h + 10;
            }
        };
    }

    private void clickUtilityTab(int mx, int my, int cx, int y) {
        editingSlot = null;
        editingPlaceField = null;
        editingKeyMakerDelay = false;
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;

        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.waypointEnabled = !OneAddons.waypointEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        String keyName = ScreenCompat.keyName(OneAddons.waypointKeyCode);
        int bindX = left + 20;
        int bindW = font.width("Key: " + keyName) + 6;
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
        y += 6;

        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.chestAssistEnabled = !OneAddons.chestAssistEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.cooldownFixEnabled = !OneAddons.cooldownFixEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        y += 6;

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

                String tog = e.enabled() ? "[✓]" : "[ ]";
                int togX = left + 14;
                if (inRect(mx, my, togX, rowY, font.width(tog), ROW_H)) {
                    OneAddons.swapAssistModule.entries.set(i, new SwapAssistModule.SwapEntry(
                        e.triggerSlot(), e.triggerInteract(), e.targetSlot(), e.targetInteract(), !e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                String tSlot = "Slot " + e.triggerSlot();
                int tsX = togX + font.width(tog) + 4;
                if (inRect(mx, my, tsX, rowY, font.width(tSlot), ROW_H)) {
                    editingSlot = new int[]{i, 0};
                    return;
                }

                String tInt = e.triggerInteract() ? "[R]" : "[_]";
                int tiX = tsX + font.width(tSlot) + 4;
                if (inRect(mx, my, tiX, rowY, font.width(tInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries.set(i, new SwapAssistModule.SwapEntry(
                        e.triggerSlot(), !e.triggerInteract(), e.targetSlot(), e.targetInteract(), e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                String sSlot = "Slot " + e.targetSlot();
                int ssX = tiX + font.width(tInt) + font.width(" \u2192 ");
                if (inRect(mx, my, ssX, rowY, font.width(sSlot), ROW_H)) {
                    editingSlot = new int[]{i, 1};
                    return;
                }

                String sInt = e.targetInteract() ? "[R]" : "[_]";
                int siX = ssX + font.width(sSlot) + 4;
                if (inRect(mx, my, siX, rowY, font.width(sInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries.set(i, new SwapAssistModule.SwapEntry(
                        e.triggerSlot(), e.triggerInteract(), e.targetSlot(), !e.targetInteract(), e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                y += ROW_H;
            }

            y += 3;

            String addText = "+ Add Swap";
            int addX = left + 20;
            int addW = font.width(addText) + 6;
            if (inRect(mx, my, addX - 2, y, addW, 16)) {
                OneAddons.swapAssistModule.addEntry(0, false, 0, false, true);
                OneAddonsConfig.save();
                return;
            }
            y += 16;
        }

        y += 6;

        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            boolean was = OneAddons.placeOnPositionEnabled;
            OneAddons.placeOnPositionEnabled = !was;
            if (!was) OneAddons.placeOnPositionModule.reload();
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (OneAddons.placeOnPositionEnabled) {
            var entries = OneAddons.placeOnPositionModule.entries;
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                int rowY = y;

                int delX = left + 2;
                if (inRect(mx, my, delX, rowY, 10, ROW_H)) {
                    OneAddons.placeOnPositionModule.removeEntry(i);
                    OneAddonsConfig.save();
                    return;
                }

                String tog = e.enabled() ? "[✓]" : "[ ]";
                int togX = left + 14;
                if (inRect(mx, my, togX, rowY, font.width(tog), ROW_H)) {
                    var mod = OneAddons.placeOnPositionModule;
                    mod.entries.set(i, new PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot(), e.placeInteract(), e.restoreSlot(), e.restoreInteract(), !e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                String pSlot = "Slot " + e.placeSlot();
                int psX = togX + font.width(tog) + 4;
                if (inRect(mx, my, psX, rowY, font.width(pSlot), ROW_H)) {
                    editingPlaceField = new int[]{i, 0};
                    return;
                }

                String pInt = e.placeInteract() ? "[R]" : "[_]";
                int piX = psX + font.width(pSlot) + 4;
                if (inRect(mx, my, piX, rowY, font.width(pInt), ROW_H)) {
                    var mod = OneAddons.placeOnPositionModule;
                    mod.entries.set(i, new PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot(), !e.placeInteract(), e.restoreSlot(), e.restoreInteract(), e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                String rSlot = "Slot " + e.restoreSlot();
                int rsX = piX + font.width(pInt) + font.width(" \u2192 ");
                if (inRect(mx, my, rsX, rowY, font.width(rSlot), ROW_H)) {
                    editingPlaceField = new int[]{i, 1};
                    return;
                }

                String rInt = e.restoreInteract() ? "[R]" : "[_]";
                int riX = rsX + font.width(rSlot) + 4;
                if (inRect(mx, my, riX, rowY, font.width(rInt), ROW_H)) {
                    var mod = OneAddons.placeOnPositionModule;
                    mod.entries.set(i, new PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot(), e.placeInteract(), e.restoreSlot(), !e.restoreInteract(), e.enabled()
                    ));
                    OneAddonsConfig.save();
                    return;
                }

                y += ROW_H;
            }

            y += 3;

            String addText = "+ Add Place";
            int addX = left + 20;
            int addW = font.width(addText) + 6;
            if (inRect(mx, my, addX - 2, y, addW, 16)) {
                OneAddons.placeOnPositionModule.addEntry(2, true, 0, false, true);
                OneAddonsConfig.save();
                return;
            }
            y += 16;
        }

        y += 6;

        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.keyMakerEnabled = !OneAddons.keyMakerEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (OneAddons.keyMakerEnabled) {
            String modeStr = "Mode: " + OneAddons.keyMakerMode.name();
            int modeX = left + 14;
            if (inRect(mx, my, modeX, y, font.width(modeStr), ROW_H)) {
                OneAddons.keyMakerMode = OneAddons.keyMakerMode == KeyMode.TUNGSTEN ? KeyMode.UMBER : KeyMode.TUNGSTEN;
                OneAddonsConfig.save();
                return;
            }
            y += ROW_H;

            String delayStr = "Delay: " + OneAddons.keyMakerClickDelay + "ms";
            int delayX = left + 14;
            if (!editingKeyMakerDelay && inRect(mx, my, delayX, y, font.width(delayStr), ROW_H)) {
                editingKeyMakerDelay = true;
                editingKeyMakerDelayBuf = "";
                return;
            }
            y += ROW_H;
        }
    }

    private void clickEnchantTab(int mx, int my, int cx, int y) {
        editingCloseField = -1;
        editingCloseBuf = "";
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;
        y += 12;
        y += 8;

        // Enchanting toggle — full row click like Utility tab
        if (inRect(mx, my, left, y, right - left, ROW_H)) {
            OneAddons.enchantingEnabled = !OneAddons.enchantingEnabled;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (!OneAddons.enchantingEnabled) return;

        // Auto close toggle
        String acTog = OneAddons.autoClose ? "[✓]" : "[ ]";
        int acTogX = left + 14;
        if (inRect(mx, my, acTogX, y, font.width(acTog), ROW_H)) {
            OneAddons.autoClose = !OneAddons.autoClose;
            OneAddonsConfig.save();
            return;
        }
        y += ROW_H;

        if (OneAddons.autoClose) {
            for (int i = 0; i < 2; i++) {
                boolean isChrono = i == 0;
                boolean en = isChrono ? OneAddons.closeChronoEnabled : OneAddons.closeUltraEnabled;
                int count = isChrono ? OneAddons.closeCountChronomatron : OneAddons.closeCountUltrasequencer;

                String tog = en ? "[✓]" : "[ ]";
                int togX = left + 14;
                if (inRect(mx, my, togX, y, font.width(tog), ROW_H)) {
                    if (isChrono) OneAddons.closeChronoEnabled = !OneAddons.closeChronoEnabled;
                    else OneAddons.closeUltraEnabled = !OneAddons.closeUltraEnabled;
                    OneAddonsConfig.save();
                    return;
                }

                String name = isChrono ? "Chronomatron" : "Ultrasequencer";
                int nmX = togX + font.width(tog) + 4;

                String numStr = String.valueOf(count);
                int numX = nmX + font.width(name) + 6;
                if (inRect(mx, my, numX, y, font.width(numStr) + 4, ROW_H)) {
                    editingCloseField = i;
                    editingCloseBuf = "";
                    return;
                }

                y += ROW_H;
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
            default -> toggles = new Toggle[]{};
        }
        int left = cx + PAD;
        int right = cx + PANEL_W - PAD;
        y += 12;
        y += 8;
        for (Toggle t : toggles) {
            if (inRect(mx, my, left, y, right - left, ROW_H)) {
                t.setter().accept(!t.getter().getAsBoolean());
                OneAddonsConfig.save();
                return;
            }
            y += ROW_H;
        }
    }

    private boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                OneAddons.waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
            } else {
                OneAddons.waypointKeyCode = keyCode;
            }
            capturingKey = false;
            OneAddonsConfig.save();
            return true;
        }

        if (editingCloseField != -1) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                editingCloseField = -1;
                editingCloseBuf = "";
                return true;
            }
            int num = keyCode - GLFW.GLFW_KEY_0;
            if (num >= 0 && num <= 9 && editingCloseBuf.length() < 2) {
                editingCloseBuf += (char)('0' + num);
                int val = Integer.parseInt(editingCloseBuf);
                if (editingCloseField == 0) OneAddons.closeCountChronomatron = val;
                else OneAddons.closeCountUltrasequencer = val;
                OneAddonsConfig.save();
                return true;
            }
        }

        if (editingKeyMakerDelay) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingKeyMakerDelay = false;
                editingKeyMakerDelayBuf = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!editingKeyMakerDelayBuf.isEmpty()) {
                    OneAddons.keyMakerClickDelay = Math.max(500, Integer.parseInt(editingKeyMakerDelayBuf));
                    OneAddonsConfig.save();
                }
                editingKeyMakerDelay = false;
                editingKeyMakerDelayBuf = "";
                return true;
            }
            int num = keyCode - GLFW.GLFW_KEY_0;
            if (num >= 0 && num <= 9 && editingKeyMakerDelayBuf.length() < 4) {
                editingKeyMakerDelayBuf += (char)('0' + num);
                return true;
            }
        }

        if (editingSlot != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingSlot = null;
                return true;
            }
            int num = keyCode - GLFW.GLFW_KEY_0;
            if (num >= 0 && num <= 8) {
                int idx = editingSlot[0];
                boolean isTrigger = editingSlot[1] == 0;
                var e = OneAddons.swapAssistModule.entries.get(idx);
                OneAddons.swapAssistModule.entries.set(idx, new SwapAssistModule.SwapEntry(
                    isTrigger ? num : e.triggerSlot(),
                    e.triggerInteract(),
                    isTrigger ? e.targetSlot() : num,
                    e.targetInteract(),
                    e.enabled()
                ));
                editingSlot = null;
                OneAddonsConfig.save();
                return true;
            }
        }

        if (editingPlaceField != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingPlaceField = null;
                return true;
            }
            int num = keyCode - GLFW.GLFW_KEY_0;
            if (num >= 0 && num <= 8) {
                int idx = editingPlaceField[0];
                boolean isPlace = editingPlaceField[1] == 0;
                var e = OneAddons.placeOnPositionModule.entries.get(idx);
                OneAddons.placeOnPositionModule.entries.set(idx, new PlaceOnPositionModule.PlaceEntry(
                    isPlace ? num : e.placeSlot(),
                    e.placeInteract(),
                    isPlace ? e.restoreSlot() : num,
                    e.restoreInteract(),
                    e.enabled()
                ));
                editingPlaceField = null;
                OneAddonsConfig.save();
                return true;
            }
        }

        return false;
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Toggle(String displayText, BooleanSupplier getter, Consumer<Boolean> setter) {}
}
