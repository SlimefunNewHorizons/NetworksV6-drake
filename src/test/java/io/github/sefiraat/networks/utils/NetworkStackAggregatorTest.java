package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresión #226: colores de terracotta no deben fusionarse en el agregador del grid.
 */
class NetworkStackAggregatorTest extends BukkitTestSupport {

    @Test
    void terracottaColorsStaySeparate() {
        final NetworkStackAggregator aggregator = new NetworkStackAggregator();
        aggregator.add(new ItemStack(Material.ORANGE_TERRACOTTA, 16), 16);
        aggregator.add(new ItemStack(Material.RED_TERRACOTTA, 8), 8);
        aggregator.add(new ItemStack(Material.WHITE_TERRACOTTA, 4), 4);

        final Map<ItemStack, Integer> map = aggregator.asMap();
        assertEquals(3, map.size());

        int orange = 0;
        int red = 0;
        int white = 0;
        for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
            switch (entry.getKey().getType()) {
                case ORANGE_TERRACOTTA -> orange = entry.getValue();
                case RED_TERRACOTTA -> red = entry.getValue();
                case WHITE_TERRACOTTA -> white = entry.getValue();
                default -> {
                }
            }
        }
        assertEquals(16, orange);
        assertEquals(8, red);
        assertEquals(4, white);
    }

    @Test
    void sameTerracottaMergesAmount() {
        final NetworkStackAggregator aggregator = new NetworkStackAggregator();
        aggregator.add(new ItemStack(Material.ORANGE_TERRACOTTA, 10), 10);
        aggregator.add(new ItemStack(Material.ORANGE_TERRACOTTA, 5), 5);

        final Map<ItemStack, Integer> map = aggregator.asMap();
        assertEquals(1, map.size());
        assertTrue(map.values().stream().anyMatch(v -> v == 15));
    }

    @Test
    void ignoresAir() {
        final NetworkStackAggregator aggregator = new NetworkStackAggregator();
        aggregator.add(new ItemStack(Material.AIR, 1), 1);
        assertTrue(aggregator.asMap().isEmpty());
    }
}
