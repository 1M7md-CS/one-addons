package com.mod.client;

import com.mod.client.compat.KeyBindingCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class OneAddons implements ClientModInitializer {

    public static boolean enchantingEnabled = false;
    public static boolean autoClose = true;
    public static boolean closeChronoEnabled = true;
    public static boolean closeUltraEnabled = true;
    public static int closeCountChronomatron = 9;
    public static int closeCountUltrasequencer = 9;
    public static boolean flowerEnabled = false;
    public static boolean mushroomEnabled = false;
    public static boolean chestAssistEnabled = false;
    public static boolean waypointEnabled = false;
    public static boolean swapAssistEnabled = false;
    public static boolean cooldownFixEnabled = false;
    public static boolean placeOnPositionEnabled = false;
    public static boolean keyMakerEnabled = false;
    public static KeyMode keyMakerMode = KeyMode.BOTH;
    public static int keyMakerClickDelay = 250;

    public static int waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    private static boolean waypointKeyPrev = false;

    private static KeyMapping cooldownFixKey;

    private EnchantingAssistModule enchantingAssistModule;
    private FlowerModule flowerModule;
    private MushroomModule mushroomModule;
    private ChestAssistModule chestAssistModule;
    private WaypointModule waypointModule;
    public static SwapAssistModule swapAssistModule;
    public static PlaceOnPositionModule placeOnPositionModule;
    private KeyMakerFeature keyMakerFeature;
    private boolean pendingScreenOpen = false;

    @Override
    public void onInitializeClient() {
        enchantingAssistModule = new EnchantingAssistModule();
        flowerModule = new FlowerModule();
        mushroomModule = new MushroomModule();
        chestAssistModule = new ChestAssistModule();
        waypointModule = new WaypointModule();
        swapAssistModule = new SwapAssistModule();
        placeOnPositionModule = new PlaceOnPositionModule();
        keyMakerFeature = new KeyMakerFeature();

        cooldownFixKey = KeyBindingCompat.register(
                "key.oneaddons.cooldownfix",
                GLFW.GLFW_KEY_UNKNOWN,
                "misc"
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("oneaddons").executes(ctx -> {
                pendingScreenOpen = true;
                return 1;
            }));
        });

        OneAddonsConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (pendingScreenOpen) {
            pendingScreenOpen = false;
            client.setScreen(new OneAddonsScreen());
        }

        if (enchantingEnabled) enchantingAssistModule.tick(client);
        if (mushroomEnabled) {
            mushroomModule.tick(client);
        } else {
            mushroomModule.resetTrackedPos();
        }
        if (flowerEnabled) flowerModule.tick(client);
        if (chestAssistEnabled) chestAssistModule.tick(client);
        if (swapAssistEnabled) swapAssistModule.tick(client);
        if (placeOnPositionEnabled) placeOnPositionModule.tick(client);
        if (keyMakerEnabled) keyMakerFeature.tick(client);

        if (waypointEnabled && waypointKeyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long window = client.getWindow().handle();
            boolean now = GLFW.glfwGetKey(window, waypointKeyCode) == GLFW.GLFW_PRESS;
            if (now && !waypointKeyPrev) {
                waypointModule.saveCurrentPosition();
            }
            waypointKeyPrev = now;
        }

        if (cooldownFixKey.consumeClick()) {
            cooldownFixEnabled = !cooldownFixEnabled;
        }
    }
}
