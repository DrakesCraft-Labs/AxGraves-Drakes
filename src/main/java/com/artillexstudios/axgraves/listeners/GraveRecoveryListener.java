package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.utils.LocationUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Returns a player to a newly-created grave and protects the recovery window. */
public final class GraveRecoveryListener implements Listener {
    private static final Map<UUID, Location> pendingRespawns = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> protectedUntil = new ConcurrentHashMap<>();
    private static volatile boolean enabled;
    private static volatile long protectionMillis;

    public GraveRecoveryListener() {
        reload();
    }

    public static void reload() {
        enabled = AxGraves.CONFIG.getBoolean("grave-respawn.enabled", true);
        long seconds = Math.clamp(AxGraves.CONFIG.getLong("grave-respawn.invulnerability-seconds", 60L), 0L, 300L);
        protectionMillis = seconds * 1_000L;
    }

    /** Queues recovery only after AxGraves has successfully created the grave. */
    static void queue(Player player, Location graveLocation) {
        if (!enabled || !player.isOnline()) {
            return;
        }
        Location target = graveLocation.clone().add(0.5D, 1.5D, 0.5D);
        LocationUtils.clampLocation(target);
        pendingRespawns.put(player.getUniqueId(), target);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location grave = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (grave == null) {
            return;
        }
        event.setRespawnLocation(grave);
        if (protectionMillis <= 0L) {
            return;
        }

        Player player = event.getPlayer();
        long until = System.currentTimeMillis() + protectionMillis;
        protectedUntil.put(player.getUniqueId(), until);
        Bukkit.getScheduler().runTask(AxGraves.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            player.setNoDamageTicks((int) Math.min(Integer.MAX_VALUE, Math.max(20L, protectionMillis / 50L)));
            player.sendMessage("§a[AxGraves] Reapareciste junto a tu tumba. Tienes " + (protectionMillis / 1_000L) + " segundos de invulnerabilidad.");
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Long until = protectedUntil.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        if (until <= System.currentTimeMillis()) {
            protectedUntil.remove(player.getUniqueId(), until);
            return;
        }
        event.setCancelled(true);
    }
}