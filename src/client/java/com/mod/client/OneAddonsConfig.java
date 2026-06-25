package com.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public class OneAddonsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "config/oneaddons/config.json");

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
            OneAddons.autoClose = data.autoClose;
            OneAddons.closeChronoEnabled = data.closeChronoEnabled;
            OneAddons.closeUltraEnabled = data.closeUltraEnabled;
            OneAddons.closeCountChronomatron = data.closeCountChronomatron;
            OneAddons.closeCountUltrasequencer = data.closeCountUltrasequencer;
            OneAddons.chestAssistEnabled = data.chestAssistEnabled;
            OneAddons.waypointEnabled = data.waypointEnabled;
            OneAddons.swapAssistEnabled = data.swapAssistEnabled;
            OneAddons.cooldownFixEnabled = data.cooldownFixEnabled;
            OneAddons.placeOnPositionEnabled = data.placeOnPositionEnabled;
            OneAddons.keyMakerEnabled = data.keyMakerEnabled;
            OneAddons.keyMakerMode = KeyMode.valueOf(data.keyMakerMode);
            OneAddons.keyMakerClickDelay = data.keyMakerClickDelay;
            OneAddons.waypointKeyCode = data.waypointKeyCode;

            OneAddons.swapAssistModule.entries.clear();
            if (data.swapEntries != null) {
                for (SwapEntryData sed : data.swapEntries) {
                    OneAddons.swapAssistModule.addEntry(sed.triggerSlot, sed.triggerInteract, sed.targetSlot, sed.targetInteract, sed.enabled);
                }
            }

            OneAddons.placeOnPositionModule.entries.clear();
            if (data.placeEntries != null) {
                for (PlaceEntryData ped : data.placeEntries) {
                    OneAddons.placeOnPositionModule.addEntry(ped.placeSlot, ped.placeInteract, ped.restoreSlot, ped.restoreInteract, ped.enabled);
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
        data.autoClose = OneAddons.autoClose;
        data.closeChronoEnabled = OneAddons.closeChronoEnabled;
        data.closeUltraEnabled = OneAddons.closeUltraEnabled;
        data.closeCountChronomatron = OneAddons.closeCountChronomatron;
        data.closeCountUltrasequencer = OneAddons.closeCountUltrasequencer;
        data.chestAssistEnabled = OneAddons.chestAssistEnabled;
        data.waypointEnabled = OneAddons.waypointEnabled;
        data.swapAssistEnabled = OneAddons.swapAssistEnabled;
        data.cooldownFixEnabled = OneAddons.cooldownFixEnabled;
        data.placeOnPositionEnabled = OneAddons.placeOnPositionEnabled;
        data.keyMakerEnabled = OneAddons.keyMakerEnabled;
        data.keyMakerMode = OneAddons.keyMakerMode.name();
        data.keyMakerClickDelay = OneAddons.keyMakerClickDelay;
        data.waypointKeyCode = OneAddons.waypointKeyCode;

        data.swapEntries = new ArrayList<>();
        for (SwapAssistModule.SwapEntry e : OneAddons.swapAssistModule.entries) {
            data.swapEntries.add(new SwapEntryData(e.triggerSlot(), e.triggerInteract(), e.targetSlot(), e.targetInteract(), e.enabled()));
        }

        data.placeEntries = new ArrayList<>();
        for (PlaceOnPositionModule.PlaceEntry e : OneAddons.placeOnPositionModule.entries) {
            data.placeEntries.add(new PlaceEntryData(e.placeSlot(), e.placeInteract(), e.restoreSlot(), e.restoreInteract(), e.enabled()));
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
        boolean autoClose = true;
        boolean closeChronoEnabled = true;
        boolean closeUltraEnabled = true;
        int closeCountChronomatron = 9;
        int closeCountUltrasequencer = 9;
        boolean chestAssistEnabled = false;
        boolean waypointEnabled = false;
        boolean swapAssistEnabled = false;
        boolean cooldownFixEnabled = false;
        boolean placeOnPositionEnabled = false;
        boolean keyMakerEnabled = false;
        String keyMakerMode = "TUNGSTEN";
        int keyMakerClickDelay = 500;
        int waypointKeyCode = GLFW.GLFW_KEY_UNKNOWN;
        List<SwapEntryData> swapEntries = new ArrayList<>();
        List<PlaceEntryData> placeEntries = new ArrayList<>();
    }

    private static class SwapEntryData {
        int triggerSlot;
        boolean triggerInteract;
        int targetSlot;
        boolean targetInteract;
        boolean enabled = true;

        SwapEntryData() {}
        SwapEntryData(int triggerSlot, boolean triggerInteract, int targetSlot, boolean targetInteract, boolean enabled) {
            this.triggerSlot = triggerSlot;
            this.triggerInteract = triggerInteract;
            this.targetSlot = targetSlot;
            this.targetInteract = targetInteract;
            this.enabled = enabled;
        }
    }

    private static class PlaceEntryData {
        int placeSlot;
        boolean placeInteract;
        int restoreSlot;
        boolean restoreInteract;
        boolean enabled = true;

        PlaceEntryData() {}
        PlaceEntryData(int placeSlot, boolean placeInteract, int restoreSlot, boolean restoreInteract, boolean enabled) {
            this.placeSlot = placeSlot;
            this.placeInteract = placeInteract;
            this.restoreSlot = restoreSlot;
            this.restoreInteract = restoreInteract;
            this.enabled = enabled;
        }
    }
}
