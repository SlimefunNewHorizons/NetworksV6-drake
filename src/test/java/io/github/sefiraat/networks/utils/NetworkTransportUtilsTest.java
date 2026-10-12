package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.slimefun.network.NetworkAdvancedPusher;
import io.github.sefiraat.networks.slimefun.network.NetworkAutoCrafter;
import io.github.sefiraat.networks.slimefun.network.NetworkCell;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumWorkbench;
import io.github.sefiraat.networks.slimefun.network.NetworkVanillaGrabber;
import io.github.sefiraat.networks.slimefun.network.NetworkVanillaPusher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTransportUtilsTest {

    @Test
    void vanillaBridgeNodesAreValidExternalTransportTargets() {
        assertTrue(NetworkTransportUtils.isExternalInventoryType("NTW_VANILLA_GRABBER", NetworkVanillaGrabber.class));
        assertTrue(NetworkTransportUtils.isExternalInventoryType("NTW_VANILLA_PUSHER", NetworkVanillaPusher.class));
    }

    @Test
    void withholdingAutoCrafterIsAValidGrabberTarget() {
        assertTrue(NetworkTransportUtils.isExternalInventoryType("NTW_AUTO_CRAFTER_WITHHOLDING", NetworkAutoCrafter.class));
        assertTrue(NetworkTransportUtils.isExternalInventoryType("NTW_ADVANCED_AUTO_CRAFTER_WITHHOLDING", NetworkAutoCrafter.class));
        assertTrue(NetworkTransportUtils.isExternalInventoryType("NTW_QUANTUM_WORKBENCH", NetworkQuantumWorkbench.class));
    }

    @Test
    void internalNetworkStorageNodesStayBlockedAsExternalTargets() {
        assertFalse(NetworkTransportUtils.isExternalInventoryType("NTW_CELL", NetworkCell.class));
        assertFalse(NetworkTransportUtils.isExternalInventoryType("NTW_QUANTUM_STORAGE_1", Object.class));
        assertFalse(NetworkTransportUtils.isExternalInventoryType("NTW_ADVANCED_PUSHER", NetworkAdvancedPusher.class));
        assertFalse(NetworkTransportUtils.isExternalInventoryType("NTW_GRID", null));
    }

    @Test
    void nonNetworkInventoriesRemainExternalTargets() {
        assertTrue(NetworkTransportUtils.isExternalInventoryType(null, null));
        assertTrue(NetworkTransportUtils.isExternalInventoryType("SOME_OTHER_MACHINE", Object.class));
    }
}
