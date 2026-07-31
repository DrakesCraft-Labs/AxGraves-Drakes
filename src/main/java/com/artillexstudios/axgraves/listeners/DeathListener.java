package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axapi.utils.logging.LogUtils;
import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.api.events.GravePreSpawnEvent;
import com.artillexstudios.axgraves.api.events.GraveSpawnEvent;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.hooks.SlimefunHook;
import com.artillexstudios.axgraves.utils.ExperienceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class DeathListener implements Listener {
    private static List<String> disabledWorlds;
    private static List<String> blacklistedDeathCauses;
    private static boolean overrideKeepInventory;
    private static boolean preserveSoulbound;
    private static boolean overrideKeepLevel;
    private static boolean storeItems;
    private static boolean storeXP;
    private static float xpKeepPercentage;

    public static void reload() {
        disabledWorlds = CONFIG.getStringList("disabled-worlds");
        blacklistedDeathCauses = CONFIG.getStringList("blacklisted-death-causes");
        overrideKeepInventory = CONFIG.getBoolean("override-keep-inventory", false);
        preserveSoulbound = CONFIG.getBoolean("preserve-soulbound-in-inventory", true);
        overrideKeepLevel = CONFIG.getBoolean("override-keep-level", true);
        storeItems = CONFIG.getBoolean("store-items", true);
        storeXP = CONFIG.getBoolean("store-xp", true);
        xpKeepPercentage = CONFIG.getFloat("xp-keep-percentage", 1f);
    }

    public DeathListener() {
        reload();

        String priority = CONFIG.getString("death-listener-priority", "MONITOR");
        EventPriority eventPriority;
        try {
            eventPriority = EventPriority.valueOf(priority);
        } catch (IllegalArgumentException ex) {
            LogUtils.error("invalid event priority: {} (defaulting to MONITOR)", priority);
            eventPriority = EventPriority.MONITOR;
        }

        EventExecutor executor = (listener, event) -> {
            if (listener instanceof DeathListener && event instanceof PlayerDeathEvent deathEvent) {
                onDeath(deathEvent);
            }
        };

        AxGraves.getInstance().getServer().getPluginManager().registerEvent(
                PlayerDeathEvent.class,
                this,
                eventPriority,
                executor,
                AxGraves.getInstance(),
                true
        );
    }

    public void onDeath(PlayerDeathEvent event) {
        boolean debug = AxGraves.isDebugMode();
        Player player = event.getEntity();

        if (debug) LogUtils.debug("[{}] spawning grave", player.getName());
        if (disabledWorlds.contains(player.getWorld().getName())) {
            if (debug) LogUtils.debug("[{}] return: disabled world {}", player.getName(), player.getWorld().getName());
            return;
        }

        if (!player.hasPermission("axgraves.allowgraves")) {
            if (debug) LogUtils.debug("[{}] return: missing permission axgraves.allowgraves", player.getName());
            return;
        }

        if (player.getLastDamageCause() != null && blacklistedDeathCauses.contains(player.getLastDamageCause().getCause().name())) {
            if (debug) LogUtils.debug("[{}] return: blacklisted death cause {}", player.getName(), player.getLastDamageCause().getCause().name());
            return;
        }

        Location location = player.getLocation();
        location.add(0, -0.5, 0);
        if (debug) LogUtils.debug("[{}] location moved to {}", player.getName(), location.toString());

        if (debug) {
            LogUtils.debug("[{}] storeItems: {} - getKeepInventory: {} - overrideKeepInventory: {}", player.getName(), storeItems, event.getKeepInventory(), overrideKeepInventory);
            LogUtils.debug("[{}] storeXP: {} - getKeepLevel: {} - overrideKeepLevel: {}", player.getName(), storeXP, event.getKeepLevel(), overrideKeepLevel);
        }

        List<ItemStack> drops = new ArrayList<>();
        boolean managedInventory = false;
        if (storeItems) {
            boolean store = false;

            if (!overrideKeepInventory && event.getKeepInventory()) {
                // A protection/recovery plugin owns this death. Do not alter its inventory.
                store = false;
            } else if (overrideKeepInventory || preserveSoulbound) {
                // Toma un snapshot antes de que otro listener pueda reescribir los drops.
                ItemStack[] contents = player.getInventory().getContents();
                store = true;
                managedInventory = true;
                drops = copyNonSoulbound(Arrays.asList(contents));

                // Keep only Soulbound items in the inventory; the grave owns every other item.
                // Clearing event drops prevents a second copy when keepInventory is enabled.
                event.getDrops().clear();
                event.setKeepInventory(true);

                // Soulbound permanece en sus slots como keepInventory por objeto.
                for (int slot = 0; slot < contents.length; slot++) {
                    ItemStack item = contents[slot];
                    if (item != null && !SlimefunHook.isSoulbound(item)) {
                        player.getInventory().setItem(slot, null);
                    }
                }
            } else if (!event.getKeepInventory()) {
                store = true;
                drops = copyNonSoulbound(event.getDrops());
                event.getDrops().removeIf(item -> !SlimefunHook.isSoulbound(item));
            }

            if (debug) LogUtils.debug("[{}] store: {} - drops size: {}", player.getName(), store, drops.size());
        }

        int xp = 0;
        if (storeXP) {
            boolean store = false;
            if (!event.getKeepLevel()) {
                store = true;
            } else if (overrideKeepLevel) {
                store = true;
                player.setLevel(0);
                player.setTotalExperience(0);
            }

            if (store) {
                xp = Math.round(ExperienceUtils.getExp(player) * xpKeepPercentage);
                event.setDroppedExp(0);
            }
            if (debug) LogUtils.debug("[{}] store: {} - xp: {}", player.getName(), store, xp);
        }

        if (drops.isEmpty() && xp == 0) {
            if (debug) LogUtils.debug("[{}] return: drops empty and xp is 0", player.getName());
            return;
        }
        List<ItemStack> graveDrops = List.copyOf(drops);
        Location graveLocation = location.clone();
        int graveExperience = xp;
        final boolean axGravesManagedInventory = managedInventory;

        // Another MONITOR listener can still enable keepInventory after this listener runs.
        // Deferring creation gives the final event state authority over every item type.
        Bukkit.getScheduler().runTask(AxGraves.getInstance(), () -> {
            if (!overrideKeepInventory && event.getKeepInventory() && !axGravesManagedInventory) {
                if (debug) LogUtils.debug("[{}] final keepInventory=true; no grave created", player.getName());
                return;
            }
            if (spawnGrave(player, graveLocation, graveDrops, graveExperience, debug)) {
                GraveRecoveryListener.queue(player, graveLocation);
            }
        });
    }

    private static boolean spawnGrave(Player player, Location location, List<ItemStack> drops, int xp, boolean debug) {
        GravePreSpawnEvent gravePreSpawnEvent = new GravePreSpawnEvent(player, location);
        Bukkit.getPluginManager().callEvent(gravePreSpawnEvent);
        if (gravePreSpawnEvent.isCancelled()) {
            if (debug) LogUtils.debug("[{}] return: GravePreSpawnEvent cancelled", player.getName());
            return false;
        }

        // Soulbound items are intentionally excluded from this normal-death grave.
        // They remain under Slimefun ownership and must never enter recovery storage.
        Grave grave = new Grave(location, player, drops, xp, System.currentTimeMillis(), false);
        SpawnedGraves.addGrave(grave);
        if (debug) LogUtils.debug("[{}] created and added grave", player.getName());

        GraveSpawnEvent graveSpawnEvent = new GraveSpawnEvent(player, grave);
        Bukkit.getPluginManager().callEvent(graveSpawnEvent);
        return true;
    }

    /** Copies only grave-safe items; Soulbound ownership remains with Slimefun. */
    private static List<ItemStack> copyNonSoulbound(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir() && !SlimefunHook.isSoulbound(item)) {
                result.add(item.clone());
            }
        }
        return result;
    }
}
