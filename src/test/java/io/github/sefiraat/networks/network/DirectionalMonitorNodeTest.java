package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionalMonitorNodeTest extends BukkitTestSupport {

    @Test
    void inputOnlyMonitorRegistersOnlyAsAnInputNode() {
        final Location location = new Location(null, 10, 64, 10);
        final NetworkRoot root = new NetworkRoot(location, NodeType.INPUT_ONLY_MONITOR, 32);

        assertTrue(root.getInputOnlyMonitors().contains(location));
        assertFalse(root.getOutputOnlyMonitors().contains(location));
        assertFalse(root.getMonitors().contains(location));
    }

    @Test
    void outputOnlyMonitorIsRemovedFromItsOwnNodeSet() {
        final Location location = new Location(null, 12, 64, 12);
        final NetworkRoot root = new NetworkRoot(location, NodeType.OUTPUT_ONLY_MONITOR, 32);

        root.unregisterNode(location, NodeType.OUTPUT_ONLY_MONITOR);

        assertFalse(root.getOutputOnlyMonitors().contains(location));
        assertFalse(root.getNodeLocations().contains(location));
    }
}
