package com.artillexstudios.axgraves.schedulers;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.Objects;

public class TickGraves {
    public static void start() {
        // Grave state contains Bukkit inventories and packet entities. Send each
        // update to the Folia region that owns its location.
        Scheduler.get().runTimer(() -> {
            for (Grave grave : SpawnedGraves.getGraves()) {
                Scheduler.get().runAt(grave.getLocation(), task -> grave.update());
                for (HumanEntity viewer : new ArrayList<>(grave.getGui().getViewers())) {
                    if (!Objects.equals(viewer.getWorld(), grave.getLocation().getWorld())) {
                        grave.closeInventory(viewer);
                        continue;
                    }
                    if (viewer.getLocation().distanceSquared(grave.getLocation()) <= 49) continue;
                    grave.closeInventory(viewer);
                }
            }
        }, 2, 2);
    }

    public static void stop() {
        // Scheduler tasks are owned by the plugin and are cancelled on disable.
    }
}
