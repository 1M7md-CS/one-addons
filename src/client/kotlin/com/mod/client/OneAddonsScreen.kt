package com.mod.client

import com.mod.client.compat.ScreenCompat
import com.mod.client.compat.ScreenGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class OneAddonsScreen : Screen(Component.literal("OneAddons")) {

    private var currentTab = 0
    private var capturingKey = false
    private var editingSlot: IntArray? = null
    private var editingPlaceField: IntArray? = null
    private var editingKeyMakerDelay = false
    private var editingKeyMakerDelayBuf = ""
    private var scrollOffset = 0

    private fun cx(): Int = width / 2 - PANEL_W / 2
    private fun cy(): Int = height / 2 - PANEL_H / 2

    // =====================================================================
    // VERSION-SPECIFIC: Replace these 3 overrides for each MC version
    // =====================================================================

    override fun extractRenderState(ctx: net.minecraft.client.gui.GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        onRender(ScreenCompat.wrap(ctx), mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: net.minecraft.client.input.MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (onMouseClicked(event.x().toInt(), event.y().toInt(), event.button())) return true
        return super.mouseClicked(event, doubleClick)
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (onKeyPressed(event.key(), event.scancode(), event.modifiers())) return true
        return super.keyPressed(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (onMouseScrolled(mouseX.toInt(), mouseY.toInt(), scrollY)) return true
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    // =====================================================================
    // STABLE: Everything below is version-independent
    // =====================================================================

    private fun onRender(g: ScreenGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        g.fill(0, 0, width, height, C_OVERLAY)
        val cx = cx()
        val cy = cy()

        g.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, C_BORDER_LIGHT)
        g.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_BORDER_DARK)
        g.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, C_PANEL)

        val title = Component.literal("\u00A7e\u26A1 OneAddons")
        g.text(font, title, width / 2 - font.width(title) / 2, cy + 10, C_TITLE, true)

        val tabY = cy + 28
        val tabW = (PANEL_W - 2) / 3
        val tabs = arrayOf("\u2740 Rift", "\u2726 Enchant", "\u25C9 Utility")
        for (i in 0..2) {
            val tx = cx + 1 + i * tabW
            val hover = mouseX >= tx && mouseX < tx + tabW - 1 && mouseY >= tabY && mouseY < tabY + TAB_H
            val bg = if (i == currentTab) C_TAB_ACTIVE else if (hover) 0x60303030 else C_TAB_BG
            g.fill(tx, tabY, tx + tabW - 1, tabY + TAB_H, bg)
            g.text(font, Component.literal(tabs[i]), tx + tabW / 2 - font.width(tabs[i]) / 2, tabY + 5, C_TITLE, true)
        }

        val lineY = tabY + TAB_H
        g.fill(cx + 1, lineY, cx + PANEL_W - 1, lineY + 1, C_SEPARATOR)

        val contentY = lineY + 6
        val contentBottom = cy + PANEL_H - 6

        g.enableScissor(cx + 1, contentY, cx + PANEL_W - 1, contentBottom)
        drawCurrentTab(g, cx, contentY - scrollOffset, mouseX, mouseY)
        g.disableScissor()

        val contentH = contentBottom - contentY
        val totalH = computeContentHeight()
        if (totalH > contentH) {
            val maxScroll = totalH - contentH
            if (scrollOffset > maxScroll) scrollOffset = maxScroll
            val thumbH = Math.max(SCROLLBAR_MIN_H, contentH * contentH / totalH)
            val thumbY = contentY + (contentH - thumbH) * scrollOffset / maxScroll
            val sbX = cx + PANEL_W - SCROLLBAR_W - 4
            g.fill(sbX, contentY, sbX + SCROLLBAR_W, contentBottom, 0x30FFFFFF)
            g.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbH, 0x80FFFFFF.toInt())
        }
    }

    private fun drawCurrentTab(g: ScreenGraphics, cx: Int, y: Int, mx: Int, my: Int) {
        when (currentTab) {
            0 -> drawCategory(g, cx, y, mx, my, "Plants",
                Toggle("\u25C6 Flower", { OneAddons.flowerEnabled }, { v -> OneAddons.flowerEnabled = v }),
                Toggle("\u2746 Mushroom", { OneAddons.mushroomEnabled }, { v -> OneAddons.mushroomEnabled = v })
            )
            1 -> drawEnchantTab(g, cx, y, mx, my)
            else -> drawUtilityTab(g, cx, y, mx, my)
        }
    }

    private fun drawEnchantTab(g: ScreenGraphics, cx: Int, y: Int, mx: Int, my: Int) {
        val left = cx + PAD
        val right = cx + PANEL_W - PAD

        g.text(font, Component.literal("Enchanting"), left, y, C_CATEGORY, true)
        var yy = y + 12
        g.fill(left, yy, right, yy + 1, C_SEPARATOR)
        yy += 8

        drawToggleRow(g, left, right, yy, mx, my, "\u2726 Enchanting",
            { OneAddons.enchantingEnabled }, { v -> OneAddons.enchantingEnabled = v })
        yy += ROW_H

        if (!OneAddons.enchantingEnabled) return
    }

    private fun drawUtilityTab(g: ScreenGraphics, cx: Int, y: Int, mx: Int, my: Int) {
        val left = cx + PAD
        val right = cx + PANEL_W - PAD
        var yy = y

        drawToggleRow(g, left, right, yy, mx, my, "\u25C9 Waypoint",
            { OneAddons.waypointEnabled }, { v -> OneAddons.waypointEnabled = v })
        yy += ROW_H

        val keyName = ScreenCompat.keyName(OneAddons.waypointKeyCode)
        val bindText = if (capturingKey) "Press a key..." else "Key: $keyName"
        val bindColor = if (capturingKey) C_ACCENT else C_DIM
        val bindX = left + 20
        val bindY = yy + 2
        val bindHover = !capturingKey && mx >= bindX && mx < bindX + font.width("Key: $keyName") + 6 && my >= bindY - 1 && my < bindY + 11
        if (bindHover) g.fill(bindX - 2, bindY - 1, bindX + font.width("Key: $keyName") + 4, bindY + 11, C_ROW_HOVER)
        g.text(font, Component.literal(bindText), bindX, bindY, bindColor, true)
        yy += 14

        g.fill(left + 20, yy, right, yy + 1, C_SEPARATOR)
        yy += 6

        drawToggleRow(g, left, right, yy, mx, my, "\u25C9 ChestAssist",
            { OneAddons.chestAssistEnabled }, { v -> OneAddons.chestAssistEnabled = v })
        yy += ROW_H

        drawToggleRow(g, left, right, yy, mx, my, "\u23F1 CooldownFix",
            { OneAddons.cooldownFixEnabled }, { v -> OneAddons.cooldownFixEnabled = v })
        yy += ROW_H

        g.fill(left + 20, yy, right, yy + 1, C_SEPARATOR)
        yy += 6

        drawToggleRow(g, left, right, yy, mx, my, "\u26CF SwapAssist",
            { OneAddons.swapAssistEnabled }, { v -> OneAddons.swapAssistEnabled = v })
        yy += ROW_H

        if (OneAddons.swapAssistEnabled) {
            val entries = OneAddons.swapAssistModule.entries
            for (i in entries.indices) {
                val e = entries[i]
                val rowY = yy

                val del = "\u2716"
                val delX = left + 2
                val delHover = mx >= delX && mx < delX + 10 && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(del), delX, rowY + 5, if (delHover) C_RED else C_DIM, true)

                val tog = if (e.enabled) "[✓]" else "[ ]"
                val togX = left + 14
                val togHover = mx >= togX && mx < togX + font.width(tog) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(tog), togX, rowY + 5, if (togHover) C_ACCENT else if (e.enabled) C_SWITCH_ON else C_DIM, true)

                val tSlot = "Slot " + e.triggerSlot
                val tsX = togX + font.width(tog) + 4
                val editing = editingSlot != null && editingSlot!![0] == i && editingSlot!![1] == 0
                val tsHover = editing || (mx >= tsX && mx < tsX + font.width(tSlot) && my >= rowY && my < rowY + ROW_H)
                if (editing) g.fill(tsX - 1, rowY + 1, tsX + font.width(tSlot) + 1, rowY + ROW_H - 1, 0x307163EF)
                g.text(font, Component.literal(tSlot), tsX, rowY + 5, if (tsHover) C_ACCENT else C_ROW_TEXT, true)

                val tInt = if (e.triggerInteract) "[R]" else "[_]"
                val tiX = tsX + font.width(tSlot) + 4
                val tiHover = mx >= tiX && mx < tiX + font.width(tInt) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(tInt), tiX, rowY + 5, if (tiHover) C_ACCENT else C_DIM, true)

                val arrow = " \u2192 "
                val arX = tiX + font.width(tInt)
                g.text(font, Component.literal(arrow), arX, rowY + 5, C_DIM, true)

                val sSlot = "Slot " + e.targetSlot
                val ssX = arX + font.width(arrow)
                val sediting = editingSlot != null && editingSlot!![0] == i && editingSlot!![1] == 1
                val ssHover = sediting || (mx >= ssX && mx < ssX + font.width(sSlot) && my >= rowY && my < rowY + ROW_H)
                if (sediting) g.fill(ssX - 1, rowY + 1, ssX + font.width(sSlot) + 1, rowY + ROW_H - 1, 0x307163EF)
                g.text(font, Component.literal(sSlot), ssX, rowY + 5, if (ssHover) C_ACCENT else C_ROW_TEXT, true)

                val sInt = if (e.targetInteract) "[R]" else "[_]"
                val siX = ssX + font.width(sSlot) + 4
                val siHover = mx >= siX && mx < siX + font.width(sInt) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(sInt), siX, rowY + 5, if (siHover) C_ACCENT else C_DIM, true)

                yy += ROW_H
            }

            yy += 3

            val addText = if (editingSlot != null) "Press 0-8 to set slot" else "+ Add Swap"
            val addX = left + 20
            val addH = 16
            val addHover = editingSlot == null && mx >= addX && mx < addX + font.width(addText) + 6 && my >= yy && my < yy + addH
            if (addHover) g.fill(addX - 2, yy, addX + font.width(addText) + 4, yy + addH, C_ROW_HOVER)
            g.text(font, Component.literal(addText), addX, yy + 4, if (editingSlot != null) C_DIM else C_ACCENT, true)
            yy += addH
        }

        g.fill(left + 20, yy, right, yy + 1, C_SEPARATOR)
        yy += 6

        drawToggleRow(g, left, right, yy, mx, my, "\u2302 PlaceOnPosition",
            { OneAddons.placeOnPositionEnabled }, { v -> OneAddons.placeOnPositionEnabled = v })
        yy += ROW_H

        if (OneAddons.placeOnPositionEnabled) {
            val entries = OneAddons.placeOnPositionModule.entries
            for (i in entries.indices) {
                val e = entries[i]
                val rowY = yy

                val del = "\u2716"
                val delX = left + 2
                val delHover = mx >= delX && mx < delX + 10 && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(del), delX, rowY + 5, if (delHover) C_RED else C_DIM, true)

                val tog = if (e.enabled) "[✓]" else "[ ]"
                val togX = left + 14
                val togHover = mx >= togX && mx < togX + font.width(tog) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(tog), togX, rowY + 5, if (togHover) C_ACCENT else if (e.enabled) C_SWITCH_ON else C_DIM, true)

                val pSlot = "Slot " + e.placeSlot
                val psX = togX + font.width(tog) + 4
                val psEditing = editingPlaceField != null && editingPlaceField!![0] == i && editingPlaceField!![1] == 0
                val psHover = psEditing || (mx >= psX && mx < psX + font.width(pSlot) && my >= rowY && my < rowY + ROW_H)
                if (psEditing) g.fill(psX - 1, rowY + 1, psX + font.width(pSlot) + 1, rowY + ROW_H - 1, 0x307163EF)
                g.text(font, Component.literal(pSlot), psX, rowY + 5, if (psHover) C_ACCENT else C_ROW_TEXT, true)

                val pInt = if (e.placeInteract) "[R]" else "[_]"
                val piX = psX + font.width(pSlot) + 4
                val piHover = mx >= piX && mx < piX + font.width(pInt) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(pInt), piX, rowY + 5, if (piHover) C_ACCENT else C_DIM, true)

                val arrow = " \u2192 "
                val arX = piX + font.width(pInt)
                g.text(font, Component.literal(arrow), arX, rowY + 5, C_DIM, true)

                val rSlot = "Slot " + e.restoreSlot
                val rsX = arX + font.width(arrow)
                val rsEditing = editingPlaceField != null && editingPlaceField!![0] == i && editingPlaceField!![1] == 1
                val rsHover = rsEditing || (mx >= rsX && mx < rsX + font.width(rSlot) && my >= rowY && my < rowY + ROW_H)
                if (rsEditing) g.fill(rsX - 1, rowY + 1, rsX + font.width(rSlot) + 1, rowY + ROW_H - 1, 0x307163EF)
                g.text(font, Component.literal(rSlot), rsX, rowY + 5, if (rsHover) C_ACCENT else C_ROW_TEXT, true)

                val rInt = if (e.restoreInteract) "[R]" else "[_]"
                val riX = rsX + font.width(rSlot) + 4
                val riHover = mx >= riX && mx < riX + font.width(rInt) && my >= rowY && my < rowY + ROW_H
                g.text(font, Component.literal(rInt), riX, rowY + 5, if (riHover) C_ACCENT else C_DIM, true)

                yy += ROW_H
            }

            yy += 3

            val addText = if (editingPlaceField != null) "Press 0-8 to set slot" else "+ Add Place"
            val addX = left + 20
            val addH = 16
            val addHover = editingPlaceField == null && mx >= addX && mx < addX + font.width(addText) + 6 && my >= yy && my < yy + addH
            if (addHover) g.fill(addX - 2, yy, addX + font.width(addText) + 4, yy + addH, C_ROW_HOVER)
            g.text(font, Component.literal(addText), addX, yy + 4, if (editingPlaceField != null) C_DIM else C_ACCENT, true)
            yy += addH
        }

        g.fill(left + 20, yy, right, yy + 1, C_SEPARATOR)
        yy += 6

        drawToggleRow(g, left, right, yy, mx, my, "\u2692 KeyMaker",
            { OneAddons.keyMakerEnabled }, { v -> OneAddons.keyMakerEnabled = v })
        yy += ROW_H

        if (OneAddons.keyMakerEnabled) {
            val modeStr = "Mode: " + OneAddons.keyMakerMode.name
            val modeX = left + 14
            val modeHover = mx >= modeX && mx < modeX + font.width(modeStr) && my >= yy && my < yy + ROW_H
            g.text(font, Component.literal(modeStr), modeX, yy + 5, if (modeHover) C_ACCENT else C_ROW_TEXT, true)
            yy += ROW_H

            val editing = editingKeyMakerDelay
            val delayStr = if (editing)
                (if (editingKeyMakerDelayBuf.isEmpty()) "___" else editingKeyMakerDelayBuf)
            else
                "Delay: " + OneAddons.keyMakerClickDelay + "ms"
            val delayX = left + 14
            val delayHover = editing || (mx >= delayX && mx < delayX + font.width(delayStr) && my >= yy && my < yy + ROW_H)
            val delayColor = if (editing) C_ACCENT else if (delayHover) C_ACCENT else C_DIM
            if (editing) g.fill(delayX - 1, yy + 1, delayX + font.width(delayStr) + 3, yy + ROW_H - 1, 0x307163EF)
            g.text(font, Component.literal(delayStr), delayX, yy + 5, delayColor, true)
            yy += ROW_H
        }
    }

    private fun drawToggleRow(g: ScreenGraphics, left: Int, right: Int, y: Int, mx: Int, my: Int, label: String, getter: BooleanSupplier, setter: Consumer<Boolean>) {
        val hover = mx >= left && mx < right && my >= y && my < y + ROW_H
        if (hover) g.fill(left, y, right, y + ROW_H, C_ROW_HOVER)
        g.text(font, Component.literal(label), left + 2, y + 5, C_ROW_TEXT, true)

        val swX = right - SWITCH_W
        val swY = y + (ROW_H - SWITCH_H) / 2
        val on = getter.asBoolean
        val bg = if (on) C_SWITCH_ON else C_SWITCH_OFF
        val knobX = if (on) swX + SWITCH_W - 9 else swX

        g.fill(swX, swY, swX + SWITCH_W, swY + SWITCH_H, 0xFF101016.toInt())
        g.fill(swX + 1, swY + 1, swX + SWITCH_W - 1, swY + SWITCH_H - 1, bg)
        g.fill(knobX, swY, knobX + 9, swY + SWITCH_H, C_SWITCH_KNOB)
    }

    private fun drawCategory(g: ScreenGraphics, cx: Int, y: Int, mx: Int, my: Int, name: String, vararg toggles: Toggle) {
        val left = cx + PAD
        val right = cx + PANEL_W - PAD
        var yy = y

        g.text(font, Component.literal(name), left, yy, C_CATEGORY, true)
        yy += 12
        g.fill(left, yy, right, yy + 1, C_SEPARATOR)
        yy += 8

        for (t in toggles) {
            drawToggleRow(g, left, right, yy, mx, my, t.displayText, t.getter, t.setter)
            yy += ROW_H
        }
    }

    private fun onMouseClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button != 0) return false
        val cx = cx()
        val cy = cy()

        val tabY = cy + 28
        val tabW = (PANEL_W - 2) / 3
        for (i in 0..2) {
            val tx = cx + 1 + i * tabW
            if (mx >= tx && mx < tx + tabW - 1 && my >= tabY && my < tabY + TAB_H) {
                currentTab = i
                capturingKey = false
                editingSlot = null
                editingPlaceField = null
                editingKeyMakerDelay = false
                scrollOffset = 0
                return true
            }
        }

        val lineY = tabY + TAB_H
        val contentY = lineY + 6

        when (currentTab) {
            1 -> {
                clickEnchantTab(mx, my, cx, contentY - scrollOffset)
                return true
            }
            2 -> {
                clickUtilityTab(mx, my, cx, contentY - scrollOffset)
                return true
            }
        }

        clickCategoryTab(mx, my, cx, contentY - scrollOffset)
        return false
    }

    private fun onMouseScrolled(mx: Int, my: Int, amount: Double): Boolean {
        val tabY = cy() + 28
        val lineY = tabY + TAB_H
        val contentY = lineY + 6
        val contentBottom = cy() + PANEL_H - 6
        if (mx >= cx() + 1 && mx < cx() + PANEL_W - 1 && my >= contentY && my < contentBottom) {
            val totalH = computeContentHeight()
            val contentH = contentBottom - contentY
            if (totalH > contentH) {
                val maxScroll = totalH - contentH
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (amount * 20).toInt()))
                return true
            }
        }
        return false
    }

    private fun computeContentHeight(): Int {
        return when (currentTab) {
            0 -> 12 + 8 + 2 * ROW_H + 10
            1 -> 12 + 8 + ROW_H + 10
            else -> {
                var h = ROW_H + 14
                h += 6 + ROW_H + ROW_H
                h += 6 + ROW_H
                if (OneAddons.swapAssistEnabled) {
                    h += OneAddons.swapAssistModule.entries.size * ROW_H + 3 + 16
                }
                h += 6 + ROW_H
                if (OneAddons.placeOnPositionEnabled) {
                    h += OneAddons.placeOnPositionModule.entries.size * ROW_H + 3 + 16
                }
                h += 6 + ROW_H
                if (OneAddons.keyMakerEnabled) {
                    h += ROW_H + ROW_H
                }
                h + 10
            }
        }
    }

    private fun clickUtilityTab(mx: Int, my: Int, cx: Int, y: Int) {
        editingSlot = null
        editingPlaceField = null
        editingKeyMakerDelay = false
        val left = cx + PAD
        val right = cx + PANEL_W - PAD
        var yy = y

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.waypointEnabled = !OneAddons.waypointEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        val keyName = ScreenCompat.keyName(OneAddons.waypointKeyCode)
        val bindX = left + 20
        val bindW = font.width("Key: $keyName") + 6
        if (inRect(mx, my, bindX - 2, yy + 1, bindW, 11)) {
            if (OneAddons.waypointKeyCode == GLFW.GLFW_KEY_UNKNOWN) {
                capturingKey = true
            } else {
                OneAddons.waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN
                capturingKey = true
            }
            return
        }
        yy += 14
        yy += 6

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.chestAssistEnabled = !OneAddons.chestAssistEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.cooldownFixEnabled = !OneAddons.cooldownFixEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        yy += 6

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.swapAssistEnabled = !OneAddons.swapAssistEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        if (OneAddons.swapAssistEnabled) {
            val entries = OneAddons.swapAssistModule.entries
            for (i in entries.indices) {
                val e = entries[i]
                val rowY = yy

                val delX = left + 2
                if (inRect(mx, my, delX, rowY, 10, ROW_H)) {
                    OneAddons.swapAssistModule.removeEntry(i)
                    OneAddonsConfig.save()
                    return
                }

                val tog = if (e.enabled) "[✓]" else "[ ]"
                val togX = left + 14
                if (inRect(mx, my, togX, rowY, font.width(tog), ROW_H)) {
                    OneAddons.swapAssistModule.entries[i] = SwapAssistModule.SwapEntry(
                        e.triggerSlot, e.triggerInteract, e.targetSlot, e.targetInteract, !e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                val tSlot = "Slot " + e.triggerSlot
                val tsX = togX + font.width(tog) + 4
                if (inRect(mx, my, tsX, rowY, font.width(tSlot), ROW_H)) {
                    editingSlot = intArrayOf(i, 0)
                    return
                }

                val tInt = if (e.triggerInteract) "[R]" else "[_]"
                val tiX = tsX + font.width(tSlot) + 4
                if (inRect(mx, my, tiX, rowY, font.width(tInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries[i] = SwapAssistModule.SwapEntry(
                        e.triggerSlot, !e.triggerInteract, e.targetSlot, e.targetInteract, e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                val sSlot = "Slot " + e.targetSlot
                val ssX = tiX + font.width(tInt) + font.width(" \u2192 ")
                if (inRect(mx, my, ssX, rowY, font.width(sSlot), ROW_H)) {
                    editingSlot = intArrayOf(i, 1)
                    return
                }

                val sInt = if (e.targetInteract) "[R]" else "[_]"
                val siX = ssX + font.width(sSlot) + 4
                if (inRect(mx, my, siX, rowY, font.width(sInt), ROW_H)) {
                    OneAddons.swapAssistModule.entries[i] = SwapAssistModule.SwapEntry(
                        e.triggerSlot, e.triggerInteract, e.targetSlot, !e.targetInteract, e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                yy += ROW_H
            }

            yy += 3

            val addText = "+ Add Swap"
            val addX = left + 20
            val addW = font.width(addText) + 6
            if (inRect(mx, my, addX - 2, yy, addW, 16)) {
                OneAddons.swapAssistModule.addEntry(0, false, 0, false, true)
                OneAddonsConfig.save()
                return
            }
            yy += 16
        }

        yy += 6

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            val was = OneAddons.placeOnPositionEnabled
            OneAddons.placeOnPositionEnabled = !was
            if (!was) OneAddons.placeOnPositionModule.reload()
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        if (OneAddons.placeOnPositionEnabled) {
            val entries = OneAddons.placeOnPositionModule.entries
            for (i in entries.indices) {
                val e = entries[i]
                val rowY = yy

                val delX = left + 2
                if (inRect(mx, my, delX, rowY, 10, ROW_H)) {
                    OneAddons.placeOnPositionModule.removeEntry(i)
                    OneAddonsConfig.save()
                    return
                }

                val tog = if (e.enabled) "[✓]" else "[ ]"
                val togX = left + 14
                if (inRect(mx, my, togX, rowY, font.width(tog), ROW_H)) {
                    val mod = OneAddons.placeOnPositionModule
                    mod.entries[i] = PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot, e.placeInteract, e.restoreSlot, e.restoreInteract, !e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                val pSlot = "Slot " + e.placeSlot
                val psX = togX + font.width(tog) + 4
                if (inRect(mx, my, psX, rowY, font.width(pSlot), ROW_H)) {
                    editingPlaceField = intArrayOf(i, 0)
                    return
                }

                val pInt = if (e.placeInteract) "[R]" else "[_]"
                val piX = psX + font.width(pSlot) + 4
                if (inRect(mx, my, piX, rowY, font.width(pInt), ROW_H)) {
                    val mod = OneAddons.placeOnPositionModule
                    mod.entries[i] = PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot, !e.placeInteract, e.restoreSlot, e.restoreInteract, e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                val rSlot = "Slot " + e.restoreSlot
                val rsX = piX + font.width(pInt) + font.width(" \u2192 ")
                if (inRect(mx, my, rsX, rowY, font.width(rSlot), ROW_H)) {
                    editingPlaceField = intArrayOf(i, 1)
                    return
                }

                val rInt = if (e.restoreInteract) "[R]" else "[_]"
                val riX = rsX + font.width(rSlot) + 4
                if (inRect(mx, my, riX, rowY, font.width(rInt), ROW_H)) {
                    val mod = OneAddons.placeOnPositionModule
                    mod.entries[i] = PlaceOnPositionModule.PlaceEntry(
                        e.placeSlot, e.placeInteract, e.restoreSlot, !e.restoreInteract, e.enabled
                    )
                    OneAddonsConfig.save()
                    return
                }

                yy += ROW_H
            }

            yy += 3

            val addText = "+ Add Place"
            val addX = left + 20
            val addW = font.width(addText) + 6
            if (inRect(mx, my, addX - 2, yy, addW, 16)) {
                OneAddons.placeOnPositionModule.addEntry(2, true, 0, false, true)
                OneAddonsConfig.save()
                return
            }
            yy += 16
        }

        yy += 6

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.keyMakerEnabled = !OneAddons.keyMakerEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H

        if (OneAddons.keyMakerEnabled) {
            val modeStr = "Mode: " + OneAddons.keyMakerMode.name
            val modeX = left + 14
            if (inRect(mx, my, modeX, yy, font.width(modeStr), ROW_H)) {
                OneAddons.keyMakerMode = if (OneAddons.keyMakerMode == KeyMode.TUNGSTEN) KeyMode.UMBER else KeyMode.TUNGSTEN
                OneAddonsConfig.save()
                return
            }
            yy += ROW_H

            val delayStr = "Delay: " + OneAddons.keyMakerClickDelay + "ms"
            val delayX = left + 14
            if (!editingKeyMakerDelay && inRect(mx, my, delayX, yy, font.width(delayStr), ROW_H)) {
                editingKeyMakerDelay = true
                editingKeyMakerDelayBuf = ""
                return
            }
            yy += ROW_H
        }
    }

    private fun clickEnchantTab(mx: Int, my: Int, cx: Int, y: Int) {
        val left = cx + PAD
        val right = cx + PANEL_W - PAD
        var yy = y + 12
        yy += 8

        if (inRect(mx, my, left, yy, right - left, ROW_H)) {
            OneAddons.enchantingEnabled = !OneAddons.enchantingEnabled
            OneAddonsConfig.save()
            return
        }
        yy += ROW_H
    }

    private fun clickCategoryTab(mx: Int, my: Int, cx: Int, y: Int) {
        val toggles = when (currentTab) {
            0 -> arrayOf(
                Toggle("", { OneAddons.flowerEnabled }, { v -> OneAddons.flowerEnabled = v }),
                Toggle("", { OneAddons.mushroomEnabled }, { v -> OneAddons.mushroomEnabled = v })
            )
            else -> emptyArray()
        }
        val left = cx + PAD
        val right = cx + PANEL_W - PAD
        var yy = y + 12
        yy += 8
        for (t in toggles) {
            if (inRect(mx, my, left, yy, right - left, ROW_H)) {
                t.setter.accept(!t.getter.asBoolean)
                OneAddonsConfig.save()
                return
            }
            yy += ROW_H
        }
    }

    private fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (capturingKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                OneAddons.waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN
            } else {
                OneAddons.waypointKeyCode = keyCode
            }
            capturingKey = false
            OneAddonsConfig.save()
            return true
        }

        if (editingKeyMakerDelay) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingKeyMakerDelay = false
                editingKeyMakerDelayBuf = ""
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (editingKeyMakerDelayBuf.isNotEmpty()) {
                    OneAddons.keyMakerClickDelay = Math.max(500, editingKeyMakerDelayBuf.toInt())
                    OneAddonsConfig.save()
                }
                editingKeyMakerDelay = false
                editingKeyMakerDelayBuf = ""
                return true
            }
            val num = keyCode - GLFW.GLFW_KEY_0
            if (num >= 0 && num <= 9 && editingKeyMakerDelayBuf.length < 4) {
                editingKeyMakerDelayBuf += ('0'.code + num).toChar()
                return true
            }
        }

        if (editingSlot != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingSlot = null
                return true
            }
            val num = keyCode - GLFW.GLFW_KEY_0
            if (num >= 0 && num <= 8) {
                val idx = editingSlot!![0]
                val isTrigger = editingSlot!![1] == 0
                val e = OneAddons.swapAssistModule.entries[idx]
                OneAddons.swapAssistModule.entries[idx] = SwapAssistModule.SwapEntry(
                    if (isTrigger) num else e.triggerSlot,
                    e.triggerInteract,
                    if (isTrigger) e.targetSlot else num,
                    e.targetInteract,
                    e.enabled
                )
                editingSlot = null
                OneAddonsConfig.save()
                return true
            }
        }

        if (editingPlaceField != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingPlaceField = null
                return true
            }
            val num = keyCode - GLFW.GLFW_KEY_0
            if (num >= 0 && num <= 8) {
                val idx = editingPlaceField!![0]
                val isPlace = editingPlaceField!![1] == 0
                val e = OneAddons.placeOnPositionModule.entries[idx]
                OneAddons.placeOnPositionModule.entries[idx] = PlaceOnPositionModule.PlaceEntry(
                    if (isPlace) num else e.placeSlot,
                    e.placeInteract,
                    if (isPlace) e.restoreSlot else num,
                    e.restoreInteract,
                    e.enabled
                )
                editingPlaceField = null
                OneAddonsConfig.save()
                return true
            }
        }

        return false
    }

    override fun isPauseScreen(): Boolean = false

    private data class Toggle(val displayText: String, val getter: BooleanSupplier, val setter: Consumer<Boolean>)

    companion object {
        private val C_OVERLAY: Int = 0x88000000.toInt()
        private val C_PANEL: Int = 0xFF202026.toInt()
        private val C_BORDER_LIGHT: Int = 0xFF303036.toInt()
        private val C_BORDER_DARK: Int = 0xFF101016.toInt()
        private val C_TAB_BG: Int = 0x50303030
        private val C_TAB_ACTIVE: Int = 0x50000000
        private val C_TITLE: Int = 0xFFFFFFFF.toInt()
        private val C_CATEGORY: Int = 0xFF7163EF.toInt()
        private val C_SEPARATOR: Int = 0xFF303036.toInt()
        private val C_ROW_TEXT: Int = 0xFFCCCCCC.toInt()
        private val C_ROW_HOVER: Int = 0x15FFFFFF
        private val C_SWITCH_ON: Int = 0xFF2FE3C9.toInt()
        private val C_SWITCH_OFF: Int = 0xFF4A4B59.toInt()
        private val C_SWITCH_KNOB: Int = 0xFFFFFFFF.toInt()
        private val C_ACCENT: Int = 0xFF7163EF.toInt()
        private val C_DIM: Int = 0xFF6A6A7A.toInt()
        private val C_RED: Int = 0xFFFF4444.toInt()

        private const val PANEL_W = 260
        private const val PANEL_H = 300
        private const val TAB_H = 20
        private const val PAD = 12
        private const val ROW_H = 22
        private const val SWITCH_W = 28
        private const val SWITCH_H = 11
        private const val BTN_H = 16
        private const val SCROLLBAR_W = 3
        private const val SCROLLBAR_MIN_H = 16

        private fun inRect(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int): Boolean {
            return mx >= x && mx < x + w && my >= y && my < y + h
        }
    }
}
