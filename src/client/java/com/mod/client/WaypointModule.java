package com.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WaypointModule {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File dataFile;
    private final List<Waypoint> waypoints = new ArrayList<>();

    public WaypointModule() {
        MinecraftClient mc = MinecraftClient.getInstance();
        dataFile = new File(mc.runDirectory, "config/oneaddons/positions.json");
    }

    public void saveCurrentPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        loadWaypoints();

        BlockPos pos = client.player.getBlockPos();
        int nextId = waypoints.size() + 1;

        waypoints.add(new Waypoint(pos.getX(), pos.getY(), pos.getZ(), 0, 1, 0, nextId));

        saveWaypoints();

        client.player.sendMessage(
                Text.literal("§aSaved waypoint §f#" + nextId
                        + " §aat §f" + pos.getX() + " " + pos.getY() + " " + pos.getZ()),
                false
        );
    }

    private void loadWaypoints() {
        waypoints.clear();
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<Waypoint>>() {}.getType();
            List<Waypoint> loaded = GSON.fromJson(reader, type);
            if (loaded != null) waypoints.addAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveWaypoints() {
        dataFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(waypoints, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Waypoint {
        int x, y, z, r, g, b;
        Options options;

        Waypoint(int x, int y, int z, int r, int g, int b, int name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.options = new Options(name);
        }
    }

    private static class Options {
        int name;

        Options(int name) {
            this.name = name;
        }
    }
}
