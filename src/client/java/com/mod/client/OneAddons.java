package com.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OneAddons implements ClientModInitializer {

    public static boolean enchantingEnabled = false;
    public static boolean flowerEnabled = false;
    public static boolean mushroomEnabled = false;
    public static boolean chestAssistEnabled = false;
    public static boolean waypointEnabled = false;
    public static boolean swapAssistEnabled = false;
    public static boolean cooldownFixEnabled = false;

    public static int waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    private static boolean waypointKeyPrev = false;

    private static KeyBinding cooldownFixKey;

    private EnchantingModule enchantingModule;
    private FlowerModule flowerModule;
    private MushroomModule mushroomModule;
    private ChestAssistModule chestAssistModule;
    private WaypointModule waypointModule;
    public static SwapAssistModule swapAssistModule;
    private boolean pendingScreenOpen = false;

    @Override
    public void onInitializeClient() {
        enchantingModule = new EnchantingModule();
        flowerModule = new FlowerModule();
        mushroomModule = new MushroomModule();
        chestAssistModule = new ChestAssistModule();
        waypointModule = new WaypointModule();
        swapAssistModule = new SwapAssistModule();

        cooldownFixKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oneaddons.cooldownfix",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("oneaddons").executes(ctx -> {
                pendingScreenOpen = true;
                return 1;
            }));
        });

        OneAddonsConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
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
        if (chestAssistEnabled) chestAssistModule.tick(client);
        if (swapAssistEnabled) swapAssistModule.tick(client);

        if (waypointEnabled && waypointKeyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long window = client.getWindow().getHandle();
            boolean now = GLFW.glfwGetKey(window, waypointKeyCode) == GLFW.GLFW_PRESS;
            if (now && !waypointKeyPrev) {
                waypointModule.saveCurrentPosition();
            }
            waypointKeyPrev = now;
        }

        if (cooldownFixKey.wasPressed()) {
            cooldownFixEnabled = !cooldownFixEnabled;
        }
    }
}
