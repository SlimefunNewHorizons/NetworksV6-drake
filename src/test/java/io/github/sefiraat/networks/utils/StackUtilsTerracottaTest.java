package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackUtilsTerracottaTest extends BukkitTestSupport {

    @Test
    void sameColorMatches() {
        final ItemStack a = new ItemStack(Material.ORANGE_TERRACOTTA, 1);
        final ItemStack b = new ItemStack(Material.ORANGE_TERRACOTTA, 64);
        assertTrue(StackUtils.itemsMatch(new ItemStackCache(a), b, true));
    }

    @Test
    void differentColorsDoNotMatch() {
        final ItemStack orange = new ItemStack(Material.ORANGE_TERRACOTTA, 1);
        final ItemStack red = new ItemStack(Material.RED_TERRACOTTA, 1);
        assertFalse(StackUtils.itemsMatch(new ItemStackCache(orange), red, true));
    }

    @Test
    void terracottaNotPlainHardenedClay() {
        final ItemStack terracotta = new ItemStack(Material.TERRACOTTA, 1);
        final ItemStack orange = new ItemStack(Material.ORANGE_TERRACOTTA, 1);
        assertFalse(StackUtils.itemsMatch(new ItemStackCache(terracotta), orange, true));
    }
}
