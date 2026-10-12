package io.github.sefiraat.networks;

import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import lombok.experimental.UtilityClass;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class NetworkStorage {

    private static final Map<Location, NodeDefinition> ALL_NETWORK_OBJECTS = new ConcurrentHashMap<>();

    public static void removeNode(Location location) {
        removeNodeInternal(location, true);
    }

    private static void removeNodeInternal(Location location, boolean isOrigin) {
        final NodeDefinition nodeDefinition = ALL_NETWORK_OBJECTS.get(location);

        if (nodeDefinition == null) {
            return;
        }

        final NetworkNode node = nodeDefinition.getNode();

        if (node != null) {
            final NetworkRoot root = node.getRoot();
            if (root != null) {
                root.unregisterNode(location, nodeDefinition.getType());
            }
            for (NetworkNode childNode : node.getChildrenNodes()) {
                removeNodeInternal(childNode.getNodePosition(), false);
            }
        }

        if (isOrigin) {
            ALL_NETWORK_OBJECTS.remove(location);
        } else {
            nodeDefinition.setNode(null);
        }
    }

    public static Map<Location, NodeDefinition> getAllNetworkObjects() {
        return ALL_NETWORK_OBJECTS;
    }

    /**
     * Rehydrates Networks nodes already persisted by Slimefun in one loaded chunk.
     * Persistent block data is never altered; this only restores transient graph state.
     */
    public static RepairSummary repairChunk(Chunk chunk) {
        int discovered = 0;
        int recovered = 0;
        int controllers = 0;
        int invalid = 0;

        // BlockStorage maintains a per-chunk index of every persisted Slimefun block.
        // The ticker contains only blocks already scheduled, which misses freshly loaded
        // Networks nodes and forces players to replace machines to register them.
        for (Location location : BlockStorage.getLocations(chunk)) {
            SlimefunItem item = BlockStorage.check(location);
            if (item == null) {
                invalid++;
                continue;
            }
            if (!(item instanceof NetworkObject networkObject)) {
                continue;
            }

            discovered++;
            if (ALL_NETWORK_OBJECTS.putIfAbsent(location, new NodeDefinition(networkObject.getNodeType())) == null) {
                recovered++;
            }
            if (item instanceof NetworkController controller) {
                controller.rebuildNetwork(location.getBlock());
                controllers++;
            }
        }

        return new RepairSummary(discovered, recovered, controllers, invalid);
    }

    public record RepairSummary(int discoveredNodes, int recoveredNodes, int rebuiltControllers, int invalidEntries) {
    }

    /** Clears runtime node references after the final persistence pass. */
    public static void clearRuntimeState() {
        ALL_NETWORK_OBJECTS.clear();
    }
}
