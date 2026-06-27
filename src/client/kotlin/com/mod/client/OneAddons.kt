package com.mod.client

import com.mod.client.compat.KeyBindingCompat
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

class OneAddons : ClientModInitializer {

    companion object {
        @JvmField var enchantingEnabled = false
        @JvmField var autoClose = false
        @JvmField var closeChronoEnabled = true
        @JvmField var closeUltraEnabled = true
        @JvmField var closeCountChronomatron = 14
        @JvmField var closeCountUltrasequencer = 14
        @JvmField var flowerEnabled = false
        @JvmField var mushroomEnabled = false
        @JvmField var chestAssistEnabled = false
        @JvmField var waypointEnabled = false
        @JvmField var swapAssistEnabled = false
        @JvmField var cooldownFixEnabled = false
        @JvmField var placeOnPositionEnabled = false
        @JvmField var keyMakerEnabled = false
        @JvmField var keyMakerMode = KeyMode.TUNGSTEN
        @JvmField var keyMakerClickDelay = 500
        @JvmField var waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN
        @JvmField var swapAssistModule = SwapAssistModule()
        @JvmField var placeOnPositionModule = PlaceOnPositionModule()
    }

    private var waypointKeyPrev = false
    private var cooldownFixKey: KeyMapping? = null
    private var enchantingAssistModule: EnchantingAssistModule? = null
    private var flowerModule: FlowerModule? = null
    private var mushroomModule: MushroomModule? = null
    private var chestAssistModule: ChestAssistModule? = null
    private var waypointModule: WaypointModule? = null
    private var keyMakerFeature: KeyMakerFeature? = null
    private var pendingScreenOpen = false

    override fun onInitializeClient() {
        enchantingAssistModule = EnchantingAssistModule()
        flowerModule = FlowerModule()
        mushroomModule = MushroomModule()
        chestAssistModule = ChestAssistModule()
        waypointModule = WaypointModule()
        swapAssistModule = SwapAssistModule()
        placeOnPositionModule = PlaceOnPositionModule()
        keyMakerFeature = KeyMakerFeature()

        cooldownFixKey = KeyBindingCompat.register(
            "key.oneaddons.cooldownfix",
            GLFW.GLFW_KEY_UNKNOWN,
            "misc"
        )

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("oneaddons").executes {
                pendingScreenOpen = true
                1
            })
        }

        OneAddonsConfig.load()

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick)
    }

    private fun onTick(client: Minecraft) {
        if (pendingScreenOpen) {
            pendingScreenOpen = false
            client.setScreen(OneAddonsScreen())
        }

        if (enchantingEnabled) enchantingAssistModule?.tick(client)
        if (mushroomEnabled) {
            mushroomModule?.tick(client)
        } else {
            mushroomModule?.resetTrackedPos()
        }
        if (flowerEnabled) flowerModule?.tick(client)
        if (chestAssistEnabled) chestAssistModule?.tick(client)
        if (swapAssistEnabled) swapAssistModule.tick(client)
        if (placeOnPositionEnabled) placeOnPositionModule.tick(client)
        if (keyMakerEnabled) keyMakerFeature?.tick(client)

        if (waypointEnabled && waypointKeyCode != GLFW.GLFW_KEY_UNKNOWN) {
            val window = client.window.handle()
            val now = GLFW.glfwGetKey(window, waypointKeyCode) == GLFW.GLFW_PRESS
            if (now && !waypointKeyPrev) {
                waypointModule?.saveCurrentPosition()
            }
            waypointKeyPrev = now
        }

        if (cooldownFixKey?.consumeClick() == true) {
            cooldownFixEnabled = !cooldownFixEnabled
        }
    }
}
