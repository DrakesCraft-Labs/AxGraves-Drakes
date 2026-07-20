package com.artillexstudios.axgraves.hooks;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlimefunHookTest {

    @Test
    void resolvesCompatibleDrakeApi() throws Exception {
        Method method = SlimefunHook.findSoulboundMethod(CompatibleUtils.class);

        assertEquals("isSoulbound", method.getName());
        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    void rejectsIncompatibleReturnType() {
        assertThrows(NoSuchMethodException.class,
                () -> SlimefunHook.findSoulboundMethod(IncompatibleUtils.class));
    }

    public static final class CompatibleUtils {
        public static boolean isSoulbound(ItemStack ignored) {
            return true;
        }
    }

    public static final class IncompatibleUtils {
        public static Boolean isSoulbound(ItemStack ignored) {
            return Boolean.TRUE;
        }
    }
}
