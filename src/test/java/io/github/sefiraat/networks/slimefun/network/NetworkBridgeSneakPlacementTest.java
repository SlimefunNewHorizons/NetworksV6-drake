package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkBridgeSneakPlacementTest extends BukkitTestSupport {

    @Test
    void sneakingWithPlaceableBlockInMainHandAllowsPlacement() {
        ItemStack bridgeBlock = new ItemStack(Material.WHITE_STAINED_GLASS);
        assertTrue(NetworkBridge.shouldAllowSneakPlacement(true, bridgeBlock, null, bridgeBlock));
    }

    @Test
    void sneakingWithPlaceableBlockInOffHandAllowsPlacementWhenMainHandEmpty() {
        ItemStack air = new ItemStack(Material.AIR);
        ItemStack offHandBlock = new ItemStack(Material.STONE);
        assertTrue(NetworkBridge.shouldAllowSneakPlacement(true, air, offHandBlock, air));
    }

    @Test
    void sneakingWithEmptyHandsDoesNotTriggerPlacementBypass() {
        ItemStack air = new ItemStack(Material.AIR);
        assertFalse(NetworkBridge.shouldAllowSneakPlacement(true, air, air, air));
        assertFalse(NetworkBridge.shouldAllowSneakPlacement(true, null, null, null));
    }

    @Test
    void sneakingWithNonBlockItemDoesNotTriggerPlacementBypass() {
        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemStack stick = new ItemStack(Material.STICK);
        assertFalse(NetworkBridge.shouldAllowSneakPlacement(true, tool, stick, tool));
    }

    @Test
    void notSneakingNeverTriggersSneakPlacementBypass() {
        ItemStack block = new ItemStack(Material.STONE);
        assertFalse(NetworkBridge.shouldAllowSneakPlacement(false, block, block, block));
    }
}
