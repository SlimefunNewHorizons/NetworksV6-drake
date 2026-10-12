package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkAutoCrafterTest extends BukkitTestSupport {

    @Test
    void exactPowerAmountAllowsCrafting() {
        assertTrue(NetworkAutoCrafter.hasSufficientPower(500, 500));
        assertFalse(NetworkAutoCrafter.hasSufficientPower(499, 500));
    }

    @Test
    void outputCanFillStackExactly() {
        final ItemStack current = new ItemStack(Material.DIAMOND, 63);
        final ItemStack crafted = new ItemStack(Material.DIAMOND, 1);

        assertTrue(NetworkAutoCrafter.canFitOutput(current, crafted));
    }

    @Test
    void outputRejectsOverflowAndMismatchedItems() {
        assertFalse(NetworkAutoCrafter.canFitOutput(
            new ItemStack(Material.DIAMOND, 64),
            new ItemStack(Material.DIAMOND, 1)
        ));
        assertFalse(NetworkAutoCrafter.canFitOutput(
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.EMERALD, 1)
        ));
    }

    @Test
    void advancedCrafterScalesEnergyByBlueprintCount() {
        assertTrue(NetworkAutoCrafter.hasSufficientPower(
            384,
            NetworkAutoCrafter.getRequiredCharge(96, 4)
        ));
        assertFalse(NetworkAutoCrafter.hasSufficientPower(
            383,
            NetworkAutoCrafter.getRequiredCharge(96, 4)
        ));
    }

    @Test
    void advancedCrafterRejectsResultBeyondOneStack() {
        assertFalse(NetworkAutoCrafter.canFitOutput(
            null,
            new ItemStack(Material.DIAMOND, 32),
            3
        ));
        assertTrue(NetworkAutoCrafter.canFitOutput(
            new ItemStack(Material.DIAMOND, 32),
            new ItemStack(Material.DIAMOND, 8),
            4
        ));
    }

    @Test
    void statusIconRendersCorrectPanesAndLore() {
        ItemStack standby = NetworkAutoCrafter.getStatusIcon(NetworkAutoCrafter.CrafterStatus.STANDBY, "En Espera", null);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, standby.getType());
        assertTrue(standby.getItemMeta().getDisplayName().contains("En Espera"));

        ItemStack op = NetworkAutoCrafter.getStatusIcon(NetworkAutoCrafter.CrafterStatus.OPERATIONAL, "Crafteando...", List.of("Salida: Diamante"));
        assertEquals(Material.LIME_STAINED_GLASS_PANE, op.getType());
        assertTrue(op.getItemMeta().getDisplayName().contains("Operativo"));

        ItemStack missing = NetworkAutoCrafter.getStatusIcon(NetworkAutoCrafter.CrafterStatus.MISSING_MATERIALS, "Faltan:", List.of("• Falta: 4x Gold"));
        assertEquals(Material.RED_STAINED_GLASS_PANE, missing.getType());
        assertTrue(missing.getItemMeta().getDisplayName().contains("Faltan Materiales"));

        ItemStack power = NetworkAutoCrafter.getStatusIcon(NetworkAutoCrafter.CrafterStatus.INSUFFICIENT_POWER, "Baja bateria", null);
        assertEquals(Material.YELLOW_STAINED_GLASS_PANE, power.getType());
        assertTrue(power.getItemMeta().getDisplayName().contains("Energía Insuficiente"));

        ItemStack full = NetworkAutoCrafter.getStatusIcon(NetworkAutoCrafter.CrafterStatus.OUTPUT_FULL, "Lleno", null);
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, full.getType());
        assertTrue(full.getItemMeta().getDisplayName().contains("Salida Llena"));
    }
}
