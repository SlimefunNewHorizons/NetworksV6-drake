package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.Theme;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NetworkQuantumStorageTest extends BukkitTestSupport {

    @Test
    void rawItemWithoutDisplayLoreCanBeRestored() {
        final ItemMeta itemMeta = new ItemStack(Material.STONE).getItemMeta();

        assertDoesNotThrow(() -> QuantumStorageState.stripDisplayLore(itemMeta));
        assertFalse(itemMeta.hasLore());
    }

    @Test
    void generatedDisplayLoreIsRemovedWithoutDiscardingOriginalLore() {
        final ItemMeta itemMeta = new ItemStack(Material.STONE).getItemMeta();
        itemMeta.setLore(List.of(
            "Original",
            "",
            Theme.CLICK_INFO + "Voiding: " + Theme.PASSIVE + "True",
            Theme.CLICK_INFO + "Amount: " + Theme.PASSIVE + "42"
        ));

        QuantumStorageState.stripDisplayLore(itemMeta);

        assertEquals(List.of("Original"), itemMeta.getLore());
    }

    @Test
    void corruptStoredAmountsFallBackToEmptyAndValidAmountsAreBounded() {
        assertEquals(0, QuantumStorageState.parseStoredAmount("not-a-number", 64));
        assertEquals(0, QuantumStorageState.parseStoredAmount("-2", 64));
        assertEquals(64, QuantumStorageState.parseStoredAmount("1024", 64));
    }

    @Test
    void quantumItemLoreCanBeRebuiltWhenMetadataIsMissing() {
        final ItemMeta itemMeta = new ItemStack(Material.CHEST).getItemMeta();
        final QuantumCache cache = new QuantumCache(new ItemStack(Material.DIAMOND), 15, 64, true);

        assertDoesNotThrow(() -> cache.updateMetaLore(itemMeta));
        assertEquals(3, itemMeta.getLore().size());
        assertEquals(Theme.CLICK_INFO + "Amount: 15", itemMeta.getLore().get(2));
    }
}
