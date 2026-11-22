package io.github.sefiraat.networks.holograms;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkHologramManagerTest extends BukkitTestSupport {

    @Test
    void compactNumbersFormatCleanlyAcrossMagnitudes() {
        assertEquals("0", NetworkHologramManager.formatCompactNumber(-5));
        assertEquals("0", NetworkHologramManager.formatCompactNumber(0));
        assertEquals("64", NetworkHologramManager.formatCompactNumber(64));
        assertEquals("999", NetworkHologramManager.formatCompactNumber(999));
        assertEquals("1K", NetworkHologramManager.formatCompactNumber(1_000));
        assertEquals("1.2K", NetworkHologramManager.formatCompactNumber(1_200));
        assertEquals("64K", NetworkHologramManager.formatCompactNumber(64_000));
        assertEquals("1.5M", NetworkHologramManager.formatCompactNumber(1_500_000));
        assertEquals("2.1B", NetworkHologramManager.formatCompactNumber(Integer.MAX_VALUE));
    }

    @Test
    void itemNamesAreStrippedAndTruncatedToPreventOverlap() {
        assertEquals("Iron Ingot", NetworkHologramManager.truncateItemName("§aIron Ingot", 16));
        String truncated = NetworkHologramManager.truncateItemName("§6§lSuper Long Quantum Singularity Core", 16);
        assertTrue(truncated.length() <= 16);
        assertTrue(truncated.endsWith("…"));
        assertFalse(truncated.contains("§"));
    }

    @Test
    void quantumTextIsCompactTwoLines() {
        ItemStack stack = new ItemStack(Material.IRON_INGOT);
        String text = NetworkHologramManager.formatQuantumText(stack, 1_250, 64_000);
        String[] lines = text.split("\n");
        assertEquals(2, lines.length);
        assertEquals("§bIron Ingot", lines[0]);
        assertEquals("§e1.3K§8/§764K", lines[1]);

        ItemStack named = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = named.getItemMeta();
        meta.setDisplayName("§d§lUltra Mega Hyper Quantum Core Generator");
        named.setItemMeta(meta);
        String namedText = NetworkHologramManager.formatQuantumText(named, 32_768, 262_144);
        String[] namedLines = namedText.split("\n");
        assertEquals(2, namedLines.length);
        assertTrue(namedLines[0].length() <= 2 + NetworkHologramManager.QUANTUM_MAX_NAME_LENGTH);

        String emptyText = NetworkHologramManager.formatQuantumText(null, 0, 4_096);
        assertEquals("§7Vacío\n§80/4.1K", emptyText);
    }

    @Test
    void quantumDisplayDimensionsStayCompact() {
        assertTrue(NetworkHologramManager.QUANTUM_SCALE >= 0.45f && NetworkHologramManager.QUANTUM_SCALE <= 0.55f);
        assertTrue(NetworkHologramManager.QUANTUM_LINE_WIDTH <= 140);
        assertTrue(NetworkHologramManager.QUANTUM_VIEW_RANGE >= 0.15f && NetworkHologramManager.QUANTUM_VIEW_RANGE <= 0.25f);
    }
}
