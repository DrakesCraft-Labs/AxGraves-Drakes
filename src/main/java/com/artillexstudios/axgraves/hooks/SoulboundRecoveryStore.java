package com.artillexstudios.axgraves.hooks;

import com.artillexstudios.axgraves.AxGraves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** Persists Soulbound items removed from legacy or malformed graves. */
public final class SoulboundRecoveryStore {
    private static final String FILE_NAME = "soulbound-recovery.yml";

    private SoulboundRecoveryStore() {
    }

    public static void queue(UUID owner, ItemStack item) {
        try {
            YamlConfiguration data = load();
            List<ItemStack> items = readItems(data, path(owner));
            items.add(item.clone());
            data.set(path(owner), items);
            save(data);
        } catch (Exception ex) {
            AxGraves.getInstance().getLogger().log(Level.SEVERE, "Could not queue a Soulbound recovery item", ex);
        }
    }

    public static void restore(Player player) {
        try {
            YamlConfiguration data = load();
            String path = path(player.getUniqueId());
            List<ItemStack> pending = readItems(data, path);
            if (pending.isEmpty()) {
                return;
            }

            List<ItemStack> leftovers = new ArrayList<>();
            for (ItemStack item : pending) {
                leftovers.addAll(player.getInventory().addItem(item).values());
            }
            data.set(path, leftovers.isEmpty() ? null : leftovers);
            save(data);
            AxGraves.getInstance().getLogger().info("Recovered " + (pending.size() - leftovers.size()) + " Soulbound item(s) for " + player.getName());
        } catch (Exception ex) {
            AxGraves.getInstance().getLogger().log(Level.SEVERE, "Could not restore Soulbound recovery items for " + player.getName(), ex);
        }
    }

    private static String path(UUID owner) {
        return "players." + owner;
    }

    private static List<ItemStack> readItems(YamlConfiguration data, String path) {
        List<ItemStack> items = new ArrayList<>();
        for (Object value : data.getList(path, List.of())) {
            if (value instanceof ItemStack item && !item.getType().isAir()) {
                items.add(item);
            }
        }
        return items;
    }

    private static YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(new File(AxGraves.getInstance().getDataFolder(), FILE_NAME));
    }

    private static void save(YamlConfiguration data) throws IOException {
        data.save(new File(AxGraves.getInstance().getDataFolder(), FILE_NAME));
    }
}
