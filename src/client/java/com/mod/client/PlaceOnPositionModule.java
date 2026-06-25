package com.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlaceOnPositionModule {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int RANGE = 5;

    private final File dataFile;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private boolean waypointsLoaded = false;

    public final List<PlaceEntry> entries = new ArrayList<>();

    private int step = 0;
    private int stepEntry = 0;
    private int delay = 0;
    private int currentIndex = 0;
    private Level lastWorld = null;
    private Waypoint currentTarget = null;

    public PlaceOnPositionModule() {
        Minecraft mc = Minecraft.getInstance();
        dataFile = new File(mc.gameDirectory, "config/oneaddons/placeonposition.json");
    }

    public void tick(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return;

        boolean hasEnabled = false;
        for (PlaceEntry e : entries) {
            if (e.enabled) { hasEnabled = true; break; }
        }
        if (!hasEnabled) {
            step = 0;
            stepEntry = entries.size();
            currentTarget = null;
            return;
        }

        if (currentTarget != null && !isNear(mc.player.blockPosition(), currentTarget)) {
            if (step > 0) {
                currentTarget = null;
                return;
            }
            step = 0;
            stepEntry = entries.size();
            currentTarget = null;
            return;
        }

        if (step > 0) {
            if (delay > 0) {
                delay--;
                return;
            }

            if (stepEntry >= entries.size()) {
                step = 0;
                stepEntry = entries.size();
                return;
            }

            PlaceEntry e = entries.get(stepEntry);
            if (!e.enabled) {
                stepEntry++;
                step = stepEntry < entries.size() ? 1 : 0;
                return;
            }

            if (step == 1) {
                mc.player.getInventory().setSelectedSlot(e.placeSlot);
                delay = 1;
                step = 2;
                return;
            }
            if (step == 2) {
                if (e.placeInteract) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }
                delay = randDelay(1, 2);
                step = 3;
                return;
            }
            if (step == 3) {
                mc.player.getInventory().setSelectedSlot(e.restoreSlot);
                if (e.restoreInteract) {
                    delay = 1;
                    step = 4;
                } else {
                    stepEntry++;
                    step = stepEntry < entries.size() ? 1 : 0;
                }
                return;
            }
            if (step == 4) {
                if (e.restoreInteract) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }
                stepEntry++;
                step = stepEntry < entries.size() ? 1 : 0;
                return;
            }
        }

        if (step == 0 && currentTarget == null) {
            stepEntry = 0;

            if (mc.level != lastWorld) {
                lastWorld = mc.level;
                currentIndex = 0;
            }

            loadWaypoints();
            if (waypoints.isEmpty()) return;

            if (currentIndex >= waypoints.size()) currentIndex = 0;

            Waypoint target = waypoints.get(currentIndex);
            if (isNear(mc.player.blockPosition(), target)) {
                currentTarget = target;
                step = 1;
                delay = randDelay(1, 2);
                currentIndex++;
                if (currentIndex >= waypoints.size()) currentIndex = 0;
            } else {
                stepEntry = entries.size();
            }
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
        stepEntry = entries.size();
        delay = 0;
        currentTarget = null;
    }

    public void addEntry(int placeSlot, boolean placeInteract, int restoreSlot, boolean restoreInteract, boolean enabled) {
        entries.add(new PlaceEntry(placeSlot, placeInteract, restoreSlot, restoreInteract, enabled));
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            step = 0;
            stepEntry = entries.size();
            currentTarget = null;
        }
    }

    private static int randDelay(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public record PlaceEntry(
            int placeSlot,
            boolean placeInteract,
            int restoreSlot,
            boolean restoreInteract,
            boolean enabled
    ) {}

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
