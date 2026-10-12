package io.github.sefiraat.networks.utils.datatypes;

import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.utils.ItemStackNormalizer;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class PersistentCraftingBlueprintTypeTest extends BukkitTestSupport {

    @Test
    void normalizesSlimefunItemStacksBeforePersistence() {
        final SlimefunItemStack slimefunStack = new SlimefunItemStack("TEST_INGOT", new ItemStack(Material.IRON_INGOT));
        slimefunStack.setAmount(12);
        final ItemMeta meta = slimefunStack.getItemMeta();
        meta.setDisplayName("Test Ingot");
        slimefunStack.setItemMeta(meta);

        final ItemStack normalized = ItemStackNormalizer.normalize(slimefunStack);

        assertEquals(ItemStack.class, normalized.getClass());
        assertNotSame(slimefunStack, normalized);
        assertEquals(slimefunStack.getType(), normalized.getType());
        assertEquals(slimefunStack.getAmount(), normalized.getAmount());
        assertEquals(slimefunStack.getItemMeta(), normalized.getItemMeta());
    }

    @Test
    void keepsRegularStacksIndependentBeforePersistence() {
        final ItemStack original = new ItemStack(Material.REDSTONE, 4);

        final ItemStack normalized = ItemStackNormalizer.normalize(original);

        assertNotSame(original, normalized);
        assertEquals(ItemStack.class, normalized.getClass());
        assertSame(original.getType(), normalized.getType());
    }
}
