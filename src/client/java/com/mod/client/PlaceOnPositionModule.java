package com.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlaceOnPositionModule {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int RANGE = 3;

    private final File dataFile;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private boolean waypointsLoaded = false;

    public int placeSlot = 2;
    public int restoreSlot = 0;
    public boolean placeInteract = true;
    public boolean restoreInteract = false;

    private int step = 0;
    private int delay = 0;
    private int currentIndex = 0;
    private World lastWorld = null;

    public PlaceOnPositionModule() {
        MinecraftClient mc = MinecraftClient.getInstance();
        dataFile = new File(mc.runDirectory, "config/oneaddons/placeonposition.json");
    }

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (step > 0) {
            if (delay > 0) {
                delay--;
                return;
            }
            if (step == 1) {
                mc.player.getInventory().setSelectedSlot(placeSlot);
                delay = 1;
                step = 2;
                return;
            }
            if (step == 2) {
                if (placeInteract) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                delay = randDelay(1, 2);
                step = 3;
                return;
            }
            if (step == 3) {
                mc.player.getInventory().setSelectedSlot(restoreSlot);
                if (restoreInteract) {
                    delay = 1;
                    step = 4;
                } else {
                    step = 0;
                }
                return;
            }
            if (step == 4) {
                if (restoreInteract) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                step = 0;
                return;
            }
        }

        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            currentIndex = 0;
        }

        loadWaypoints();
        if (waypoints.isEmpty()) return;

        if (currentIndex >= waypoints.size()) currentIndex = 0;

        Waypoint target = waypoints.get(currentIndex);
        if (isNear(mc.player.getBlockPos(), target)) {
            step = 1;
            delay = randDelay(1, 2);
            currentIndex++;
            if (currentIndex >= waypoints.size()) currentIndex = 0;
        }
    }

    private boolean isNear(BlockPos pos, Waypoint wp) {
        return Math.abs(pos.getX() - wp.x) <= RANGE
            && Math.abs(pos.getY() - wp.y) <= RANGE
            && Math.abs(pos.getZ() - wp.z) <= RANGE;
    }

    private void loadWaypoints() {
        if (waypointsLoaded) return;
        waypoints.clear();
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<Waypoint>>() {}.getType();
            List<Waypoint> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                waypoints.addAll(loaded);
                waypoints.sort(Comparator.comparingInt(w -> w.options.name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        waypointsLoaded = true;
    }

    public void reload() {
        waypointsLoaded = false;
        currentIndex = 0;
        step = 0;
        delay = 0;
    }

    private static int randDelay(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static class Waypoint {
        int x, y, z, r, g, b;
        Options options;

        Waypoint() {}
    }

    private static class Options {
        int name;

        Options() {}
    }
}
