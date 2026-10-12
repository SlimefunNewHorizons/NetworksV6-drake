package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NetworkBarrelAccessSafetyTest extends BukkitTestSupport {

    @Test
    void accessBarrelsSafelyHandlesNullLocationsAndUncachedState() {
        final Location controller = new Location(null, 10, 64, 10);
        final NetworkRoot root = new NetworkRoot(controller, NodeType.CONTROLLER, 32);

        // Null location queries
        assertNull(root.accessInputAbleBarrel(null));
        assertNull(root.accessOutputAbleBarrel(null));

        // Non-null location queries on unpopulated/empty network
        final Location testLoc = new Location(null, 20, 64, 20);
        assertNull(root.accessInputAbleBarrel(testLoc));
        assertNull(root.accessOutputAbleBarrel(testLoc));

        // Maps are never null
        assertNotNull(root.getMapInputAbleBarrels());
        assertNotNull(root.getMapOutputAbleBarrels());

        // Refresh caches and re-test
        root.refreshRootItems();
        assertNull(root.accessInputAbleBarrel(testLoc));
        assertNull(root.accessOutputAbleBarrel(testLoc));
        assertNotNull(root.getMapInputAbleBarrels());
        assertNotNull(root.getMapOutputAbleBarrels());
    }
}
