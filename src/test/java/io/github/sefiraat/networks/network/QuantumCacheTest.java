package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuantumCacheTest extends BukkitTestSupport {

    private QuantumCache cache(int amount) {
        return new QuantumCache(new ItemStack(Material.DIAMOND), amount, 1000, false);
    }

    @Test
    void reduceAmountNeverGoesNegative() {
        final QuantumCache c = cache(5);
        c.reduceAmount(100);
        assertEquals(0, c.getAmount());
    }

    @Test
    void withdrawFromEmptyCacheReturnsNull() {
        final QuantumCache c = cache(0);
        assertNull(c.withdrawItem(10));
    }

    @Test
    void withdrawExactAmountLeavesZero() {
        final QuantumCache c = cache(10);
        final ItemStack out = c.withdrawItem(10);
        assertEquals(10, out.getAmount());
        assertEquals(0, c.getAmount());
    }

    @Test
    void withdrawMoreThanAvailableReturnsCapped() {
        final QuantumCache c = cache(3);
        final ItemStack out = c.withdrawItem(10);
        assertEquals(3, out.getAmount());
        assertEquals(0, c.getAmount());
    }

    @Test
    void increaseAmountReturnsLeftoverWhenNotVoiding() {
        final QuantumCache c = new QuantumCache(new ItemStack(Material.DIAMOND), 990, 1000, false);
        final int leftover = c.increaseAmount(20);
        assertEquals(10, leftover);
        assertEquals(1000, c.getAmount());
    }

    @Test
    void voidExcessAbsorbsAllWithNoLeftover() {
        final QuantumCache c = new QuantumCache(new ItemStack(Material.DIAMOND), 990, 1000, true);
        final int leftover = c.increaseAmount(20);
        assertEquals(0, leftover);
        assertEquals(1000, c.getAmount());
    }
}
