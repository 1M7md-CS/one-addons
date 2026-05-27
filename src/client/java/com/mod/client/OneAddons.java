package com.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class OneAddons implements ClientModInitializer {

    public static boolean enchantingEnabled = false;
    public static boolean flowerEnabled = false;
    public static boolean mushroomEnabled = false;
    public static boolean autoMineEnabled = false;

    private static final Identifier HUD_ID = Identifier.of("oneaddons", "hud");

    private static final int HUD_PAD = 10;
    private static final int HUD_W = 175;

    private static final int C_OUTLINE_BORDER = 0xFF4C46A6;
    private static final int C_CONTAINER_BG = 0xF0101117;
    private static final int C_TITLE = 0xFFFFFFFF;
    private static final int C_VERSION = 0xFF555565;
    private static final int C_BODY_TEXT = 0xFFFFFFFF;
    private static final int C_CPS_COUNTER = 0xFF7163EF;

    private static final int C_PILL_ON_BG = 0xCC0D463E;
    private static final int C_PILL_ON_BORDER = 0xFF147A6A;
    private static final int C_PILL_ON_TXT = 0xFF2FE3C9;
    private static final int C_PILL_OFF_BG = 0xCC2A2B36;
    private static final int C_PILL_OFF_BORDER = 0xFF4A4B59;
    private static final int C_PILL_OFF_TXT = 0xFF9A9BAC;
    private static final int C_PILL_LOCK_BG = 0xCC54390B;
    private static final int C_PILL_LOCK_EDGE = 0xFF96620E;
    private static final int C_PILL_LOCK_TXT = 0xFFFFB834;

    private static final Text ICON_FLOWER = Text.literal("◆ ").formatted(Formatting.BLUE);
    private static final Text ICON_MUSHROOM = Text.literal("❖ ").formatted(Formatting.DARK_PURPLE);
    private static final Text ICON_BOLT = Text.literal("⚡ ").formatted(Formatting.WHITE);
    private static final Text ICON_ENCHANT = Text.literal("✦ ").formatted(Formatting.GOLD);

    private static final Text TITLE_TEXT = Text.literal("OneAddons");
    private static final Text VERSION_TEXT = Text.literal("v1.0");
    private static final Text LABEL_FLOWER = Text.literal("Flower");
    private static final Text LABEL_MUSHROOM = Text.literal("Mushroom");
    private static final Text LABEL_ENCHANTING = Text.literal("Enchanting");

    private EnchantingModule enchantingModule;
    private FlowerModule flowerModule;
    private MushroomModule mushroomModule;
    private AutoMineModule autoMineModule;

    private int lastDisplayedCps = -1;
    private String cachedCpsString = "0 CPS";
    private boolean pendingScreenOpen = false;

    @Override
    public void onInitializeClient() {
        enchantingModule = new EnchantingModule();
        flowerModule = new FlowerModule();
        mushroomModule = new MushroomModule();
        autoMineModule = new AutoMineModule();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("oneaddons").executes(ctx -> {
                pendingScreenOpen = true;
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.addLast(HUD_ID, this::onHud);
    }

    private void onTick(MinecraftClient client) {
        if (pendingScreenOpen) {
            pendingScreenOpen = false;
            client.setScreen(new OneAddonsScreen());
        }

        if (enchantingEnabled) enchantingModule.tick(client);

        if (mushroomEnabled) {
            mushroomModule.tick(client);
        } else {
            mushroomModule.resetTrackedPos();
        }

        if (flowerEnabled) flowerModule.tick(client);

        if (autoMineEnabled) autoMineModule.tick(client);
    }

    private void onHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!flowerEnabled && !mushroomEnabled && !enchantingEnabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int realCps = flowerModule.getCurrentCps();
        if (realCps != lastDisplayedCps) {
            cachedCpsString = realCps + " CPS";
            lastDisplayedCps = realCps;
        }

        int x = mc.getWindow().getScaledWidth() - HUD_W - HUD_PAD;
        int y = HUD_PAD;

        int count = 0;
        if (flowerEnabled) count++;
        if (mushroomEnabled) count++;
        if (enchantingEnabled) count++;

        int hudH = 18 + count * 16;

        ctx.fill(x, y, x + HUD_W, y + hudH, C_OUTLINE_BORDER);
        ctx.fill(x + 1, y + 1, x + HUD_W - 1, y + hudH - 1, C_CONTAINER_BG);

        int textYOffset = 4;
        ctx.drawText(mc.textRenderer, ICON_BOLT, x + 8, y + textYOffset, 0xFFFFFFFF, false);
        ctx.drawText(mc.textRenderer, TITLE_TEXT, x + 18, y + textYOffset, C_TITLE, false);

        int versionWidth = mc.textRenderer.getWidth(VERSION_TEXT);
        ctx.drawText(mc.textRenderer, VERSION_TEXT, x + HUD_W - versionWidth - 8, y + textYOffset, C_VERSION, false);

        int rowY = y + 20;

        if (flowerEnabled) {
            ctx.drawText(mc.textRenderer, ICON_FLOWER, x + 8, rowY + 2, 0xFFFFFFFF, false);
            ctx.drawText(mc.textRenderer, LABEL_FLOWER, x + 19, rowY + 2, C_BODY_TEXT, false);
            int badgeX = x + 19 + mc.textRenderer.getWidth(LABEL_FLOWER) + 8;
            drawStatusBadge(ctx, mc, badgeX, rowY, true, false);

            int cpsWidth = mc.textRenderer.getWidth(cachedCpsString);
            ctx.drawText(mc.textRenderer, cachedCpsString, x + HUD_W - cpsWidth - 8, rowY + 2, C_CPS_COUNTER, false);
            rowY += 16;
        }

        if (mushroomEnabled) {
            ctx.drawText(mc.textRenderer, ICON_MUSHROOM, x + 8, rowY + 2, 0xFFFFFFFF, false);
            ctx.drawText(mc.textRenderer, LABEL_MUSHROOM, x + 19, rowY + 2, C_BODY_TEXT, false);
            int badgeX = x + 19 + mc.textRenderer.getWidth(LABEL_MUSHROOM) + 8;
            drawStatusBadge(ctx, mc, badgeX, rowY, true, mushroomModule.isTracking());
            rowY += 16;
        }

        if (enchantingEnabled) {
            ctx.drawText(mc.textRenderer, ICON_ENCHANT, x + 8, rowY + 2, 0xFFFFFFFF, false);
            ctx.drawText(mc.textRenderer, LABEL_ENCHANTING, x + 19, rowY + 2, C_BODY_TEXT, false);
            int badgeX = x + 19 + mc.textRenderer.getWidth(LABEL_ENCHANTING) + 8;
            drawStatusBadge(ctx, mc, badgeX, rowY, true, false);
        }
    }

    private void drawStatusBadge(DrawContext ctx, MinecraftClient mc, int badgeX, int badgeY, boolean active, boolean locked) {
        int badgeW = 18;
        int badgeH = 10;

        int bg, edge, txtColor;
        String text;

        if (locked) {
            bg = C_PILL_LOCK_BG;
            edge = C_PILL_LOCK_EDGE;
            txtColor = C_PILL_LOCK_TXT;
            text = "LK";
            badgeW = 16;
        } else if (active) {
            bg = C_PILL_ON_BG;
            edge = C_PILL_ON_BORDER;
            txtColor = C_PILL_ON_TXT;
            text = "ON";
        } else {
            bg = C_PILL_OFF_BG;
            edge = C_PILL_OFF_BORDER;
            txtColor = C_PILL_OFF_TXT;
            text = "OFF";
            badgeW = 22;
        }

        ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, edge);
        ctx.fill(badgeX + 1, badgeY + 1, badgeX + badgeW - 1, badgeY + badgeH - 1, bg);

        int textInnerWidth = mc.textRenderer.getWidth(text);
        int tx = badgeX + ((badgeW - textInnerWidth) / 2);
        int ty = badgeY + 1;

        ctx.drawText(mc.textRenderer, text, tx, ty, txtColor, false);
    }
}
