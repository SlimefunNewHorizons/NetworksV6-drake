package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NetworkControllerConflictTest extends BukkitTestSupport {

    @Test
    void conflictIsRecordedWithoutRegisteringForeignControllerAsNode() {
        final Location controller = new Location(null, 10, 64, 10);
        final Location foreignController = new Location(null, 12, 64, 10);
        final NetworkRoot root = new NetworkRoot(controller, NodeType.CONTROLLER, 32);

        root.recordControllerConflict(foreignController);
        root.recordControllerConflict(foreignController);

        assertEquals(1, root.getConflictingControllers().size());
        assertFalse(root.getNodeLocations().contains(foreignController));
    }
}
