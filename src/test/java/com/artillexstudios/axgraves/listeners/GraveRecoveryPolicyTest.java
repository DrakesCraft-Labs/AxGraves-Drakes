package com.artillexstudios.axgraves.listeners;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GraveRecoveryPolicyTest {
    @Test
    void onlyCreatesRecoveryForARealGraveAndCancelsDamageDuringItsWindow() throws Exception {
        String death = Files.readString(Path.of("src", "main", "java", "com", "artillexstudios", "axgraves", "listeners", "DeathListener.java"));
        String recovery = Files.readString(Path.of("src", "main", "java", "com", "artillexstudios", "axgraves", "listeners", "GraveRecoveryListener.java"));

        assertTrue(death.contains("if (spawnGrave(player, graveLocation, graveDrops, graveExperience, debug))"));
        assertTrue(death.contains("GraveRecoveryListener.queue(player, graveLocation)"));
        assertTrue(recovery.contains("event.setRespawnLocation(grave)"));
        assertTrue(recovery.contains("event.setCancelled(true)"));
        assertTrue(recovery.contains("grave-respawn.invulnerability-seconds"));
    }
}