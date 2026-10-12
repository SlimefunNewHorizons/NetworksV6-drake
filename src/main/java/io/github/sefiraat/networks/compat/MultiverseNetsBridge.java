package io.github.sefiraat.networks.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [EN] Reflection bridge so Slimefun's Networks (NetworksV6) can read/write the VIRTUAL storage of
 * a MultiverseNets network (Chagui68) directly, without a compile-time dependency. Mirrors Chagui's
 * SlimefunBridge in the opposite direction: it targets the public {@code MultiverseNetsAPI}
 * (extract/insert/count/isNetworkBlock). If MultiverseNets is absent, the bridge stays dormant and
 * every method is a no-op, so Networks keeps working standalone.
 *
 * [ES] Puente por reflexion para que la red de Slimefun (NetworksV6) lea/escriba directamente el
 * almacenamiento VIRTUAL de una red de MultiverseNets (Chagui68), sin dependencia en compilacion.
 * Simetrico al SlimefunBridge de Chagui. Si MultiverseNets no esta, queda inerte.
 */
public final class MultiverseNetsBridge {

    private static boolean available;
    private static Method mIsNetworkBlock;
    private static Method mExtract;
    private static Method mInsert;
    private static Method mCount;

    private MultiverseNetsBridge() {
    }

    public static void init(Logger logger) {
        try {
            if (Bukkit.getPluginManager().getPlugin("MultiverseNets") == null) {
                available = false;
                return;
            }
            final Class<?> api = Class.forName("com.chagui68.multiversenets.api.MultiverseNetsAPI");
            mIsNetworkBlock = api.getMethod("isNetworkBlock", Block.class);
            mExtract = api.getMethod("extract", Block.class, Predicate.class, int.class);
            mInsert = api.getMethod("insert", Block.class, ItemStack.class);
            mCount = api.getMethod("count", Block.class, Predicate.class);
            available = true;
            if (logger != null) {
                logger.info("[Networks] Puente con MultiverseNets activo: las redes de Slimefun y Chagui pueden intercambiar items.");
            }
        } catch (Throwable t) {
            available = false;
            if (logger != null) {
                logger.log(Level.FINE, "[Networks] MultiverseNets no disponible; el puente queda inerte.", t);
            }
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /** [ES] true si el bloque es un nodo de una red de MultiverseNets. */
    public static boolean isNetworkBlock(Block block) {
        if (!available || block == null) {
            return false;
        }
        try {
            return (boolean) mIsNetworkBlock.invoke(null, block);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** [ES] Extrae hasta {@code amount} items que cumplan {@code matcher} de la red de MultiverseNets. */
    public static ItemStack extract(Block block, Predicate<ItemStack> matcher, int amount) {
        if (!available) {
            return null;
        }
        try {
            return (ItemStack) mExtract.invoke(null, block, matcher, amount);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** [ES] Inserta {@code stack} en la red de MultiverseNets. Devuelve el sobrante (0 = todo entro). */
    public static int insert(Block block, ItemStack stack) {
        if (!available || stack == null) {
            return stack == null ? 0 : stack.getAmount();
        }
        try {
            return (int) mInsert.invoke(null, block, stack);
        } catch (Throwable ignored) {
            return stack.getAmount();
        }
    }

    /** [ES] Cuenta cuantos items cumplen {@code matcher} en la red de MultiverseNets. */
    public static long count(Block block, Predicate<ItemStack> matcher) {
        if (!available) {
            return 0L;
        }
        try {
            return (long) mCount.invoke(null, block, matcher);
        } catch (Throwable ignored) {
            return 0L;
        }
    }
}
