package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ItemCreatorTest extends BukkitTestSupport {

    @Test
    void createFromItemStackDoesNotMutateOriginal() {
        final ItemStack original = new ItemStack(Material.DIAMOND);
        final ItemStack displayItem = ItemCreator.create(original, "Display Name", "Display Lore");

        assertNotSame(original, displayItem);
        assertFalse(original.getItemMeta().hasDisplayName());
        assertEquals("Display Name", displayItem.getItemMeta().getDisplayName());
    }

    @Test
    void createFromItemStackKeepsOriginalMetaIntact() {
        final ItemStack original = new ItemStack(Material.EMERALD);
        final ItemMeta meta = original.getItemMeta();
        meta.setDisplayName("Original Name");
        original.setItemMeta(meta);

        final ItemStack displayItem = ItemCreator.create(original, "Display Name");

        assertNotSame(original, displayItem);
        assertEquals("Original Name", original.getItemMeta().getDisplayName());
        assertEquals("Display Name", displayItem.getItemMeta().getDisplayName());
    }
}
