package com.artillexstudios.axgraves.hooks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SoulboundOwnershipPolicyTest {
    @Test
    void recognizesOdysseiaSoulboundBeforeDelegatingToSlimefun() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "artillexstudios", "axgraves", "hooks", "SlimefunHook.java"));

        assertTrue(source.contains("new NamespacedKey(\"odysseia\", \"soulbound\")"));
        assertTrue(source.contains("if (isOdysseiaSoulbound(item)) return true;"));
        assertTrue(source.contains("PersistentDataType.BYTE"));
    }
}
