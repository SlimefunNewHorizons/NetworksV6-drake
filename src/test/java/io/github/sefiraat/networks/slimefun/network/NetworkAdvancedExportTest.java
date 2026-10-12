package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkAdvancedExportTest extends BukkitTestSupport {

    @Test
    void fullOutputBufferPreventsNetworkWithdrawal() {
        final ItemStack[] output = new ItemStack[18];
        for (int index = 0; index < output.length; index++) {
            output[index] = new ItemStack(Material.DIAMOND, 64);
        }

        assertFalse(NetworkAdvancedExport.hasOutputSpace(output));
        output[7].setAmount(63);
        assertTrue(NetworkAdvancedExport.hasOutputSpace(output));
    }
}
