package com.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public class OneAddonsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(MinecraftClient.getInstance().runDirectory, "config/oneaddons/config.json");

    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        if (!FILE.exists()) {
            save();
            loaded = true;
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null) {
                save();
                loaded = true;
                return;
            }
            OneAddons.flowerEnabled = data.flowerEnabled;
            OneAddons.mushroomEnabled = data.mushroomEnabled;
            OneAddons.enchantingEnabled = data.enchantingEnabled;
            OneAddons.chestAssistEnabled = data.chestAssistEnabled;
            OneAddons.waypointEnabled = data.waypointEnabled;
            OneAddons.swapAssistEnabled = data.swapAssistEnabled;
            OneAddons.cooldownFixEnabled = data.cooldownFixEnabled;
            OneAddons.waypointKeyCode = data.waypointKeyCode;

            OneAddons.swapAssistModule.entries.clear();
            if (data.swapEntries != null) {
                for (SwapEntryData sed : data.swapEntries) {
                    OneAddons.swapAssistModule.addEntry(sed.triggerSlot, sed.triggerInteract, sed.targetSlot, sed.targetInteract);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        loaded = true;
    }

    public static void save() {
        FILE.getParentFile().mkdirs();
        Data data = new Data();
        data.flowerEnabled = OneAddons.flowerEnabled;
        data.mushroomEnabled = OneAddons.mushroomEnabled;
        data.enchantingEnabled = OneAddons.enchantingEnabled;
        data.chestAssistEnabled = OneAddons.chestAssistEnabled;
        data.waypointEnabled = OneAddons.waypointEnabled;
        data.swapAssistEnabled = OneAddons.swapAssistEnabled;
        data.cooldownFixEnabled = OneAddons.cooldownFixEnabled;
        data.waypointKeyCode = OneAddons.waypointKeyCode;

        data.swapEntries = new ArrayList<>();
        for (SwapAssistModule.SwapEntry e : OneAddons.swapAssistModule.entries) {
            data.swapEntries.add(new SwapEntryData(e.triggerSlot(), e.triggerInteract(), e.targetSlot(), e.targetInteract()));
        }

        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Data {
        boolean flowerEnabled = false;
        boolean mushroomEnabled = false;
        boolean enchantingEnabled = false;
        boolean chestAssistEnabled = false;
        boolean waypointEnabled = false;
        boolean swapAssistEnabled = false;
        boolean cooldownFixEnabled = false;
        int waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        List<SwapEntryData> swapEntries = new ArrayList<>();
    }

    private static class SwapEntryData {
        int triggerSlot;
        boolean triggerInteract;
        int targetSlot;
        boolean targetInteract;

        SwapEntryData() {}
        SwapEntryData(int triggerSlot, boolean triggerInteract, int targetSlot, boolean targetInteract) {
            this.triggerSlot = triggerSlot;
            this.triggerInteract = triggerInteract;
            this.targetSlot = targetSlot;
            this.targetInteract = targetInteract;
        }
    }
}
