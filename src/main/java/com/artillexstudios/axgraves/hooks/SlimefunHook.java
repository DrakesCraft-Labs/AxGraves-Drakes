package com.artillexstudios.axgraves.hooks;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class SlimefunHook {

    private static final NamespacedKey ODYSSEIA_SOULBOUND = new NamespacedKey("odysseia", "soulbound");
    private static boolean enabled;
    private static Method soulboundMethod;
    private static final AtomicBoolean detectionFailureLogged = new AtomicBoolean();

    private SlimefunHook() {
    }

    public static void init() {
        Plugin slimefun = Bukkit.getPluginManager().getPlugin("Slimefun");
        enabled = slimefun != null;
        soulboundMethod = null;
        detectionFailureLogged.set(false);
        if (!enabled) {
            return;
        }

        try {
            Class<?> utilsClass = Class.forName(
                    "com.github.drakescraft_labs.slimefun4.utils.SlimefunUtils",
                    true,
                    slimefun.getClass().getClassLoader()
            );
            soulboundMethod = findSoulboundMethod(utilsClass);
        } catch (ReflectiveOperationException | LinkageError ex) {
            logDetectionFailure(ex);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isSoulbound(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        // Odysseia kit gear is vanilla, so Slimefun cannot identify its marker.
        if (isOdysseiaSoulbound(item)) return true;
        if (!enabled) return false;
        if (soulboundMethod == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(soulboundMethod.invoke(null, item));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            logDetectionFailure(ex);
            return true;
        }
    }

    /** Recognizes the canonical marker used by Odysseia VIP kits. */
    static boolean isOdysseiaSoulbound(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer()
                .get(ODYSSEIA_SOULBOUND, PersistentDataType.BYTE);
        return marker != null && marker != 0;
    }

    static Method findSoulboundMethod(Class<?> utilsClass) throws NoSuchMethodException {
        Method method = utilsClass.getMethod("isSoulbound", ItemStack.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
            throw new NoSuchMethodException("SlimefunUtils#isSoulbound debe ser static boolean");
        }
        return method;
    }

    private static void logDetectionFailure(Throwable throwable) {
        if (detectionFailureLogged.compareAndSet(false, true)) {
            Bukkit.getLogger().log(
                    Level.SEVERE,
                    "[AxGraves] Fallo al detectar Soulbound; los items se trataran como Soulbound para impedir duplicaciones.",
                    throwable
            );
        }
    }
}
