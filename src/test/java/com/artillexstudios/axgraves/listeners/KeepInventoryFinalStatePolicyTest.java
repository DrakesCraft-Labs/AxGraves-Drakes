package com.artillexstudios.axgraves.listeners;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KeepInventoryFinalStatePolicyTest {
    @Test
    void waitsForTheFinalKeepInventoryStateBeforeCreatingAnyGrave() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "artillexstudios", "axgraves", "listeners", "DeathListener.java"));

        assertTrue(source.contains("getBoolean(\"override-keep-inventory\", false)"));
        assertTrue(source.contains("Bukkit.getScheduler().runTask(AxGraves.getInstance(), () -> {"));
        assertTrue(source.contains("if (!overrideKeepInventory && event.getKeepInventory())"));
        assertTrue(source.contains("final keepInventory=true; no grave created"));
        assertTrue(source.indexOf("runTask(AxGraves.getInstance()") < source.indexOf("spawnGrave(player, graveLocation"));
    }
}