package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkCrafterMatrixTest extends BukkitTestSupport {

    @Test
    void matrixSlotsConfigurationIsSymmetric() {
        assertEquals(9, NetworkCrafterMatrix.BLUEPRINT_SLOTS.length);
        assertEquals(9, NetworkCrafterMatrix.OUTPUT_SLOTS.length);
        assertEquals(22, NetworkCrafterMatrix.STATUS_SLOT);
    }

    @Test
    void matrixStatusRendersCorrectly() {
        ItemStack empty = NetworkCrafterMatrix.getMatrixStatusIcon(0, 0, 0, 500, null);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, empty.getType());
        assertTrue(empty.getItemMeta().getDisplayName().contains("Matrix en Espera"));

        ItemStack op = NetworkCrafterMatrix.getMatrixStatusIcon(3, 2, 10000, 500, null);
        assertEquals(Material.LIME_STAINED_GLASS_PANE, op.getType());
        assertTrue(op.getItemMeta().getDisplayName().contains("Matrix Operativa"));

        ItemStack missing = NetworkCrafterMatrix.getMatrixStatusIcon(3, 0, 10000, 500, List.of("• Falta: 4x Iron Ingot"));
        assertEquals(Material.RED_STAINED_GLASS_PANE, missing.getType());
        assertTrue(missing.getItemMeta().getDisplayName().contains("Faltan Materiales"));

        ItemStack lowPower = NetworkCrafterMatrix.getMatrixStatusIcon(3, 0, 100, 500, null);
        assertEquals(Material.YELLOW_STAINED_GLASS_PANE, lowPower.getType());
        assertTrue(lowPower.getItemMeta().getDisplayName().contains("Energía Insuficiente"));
    }
}
