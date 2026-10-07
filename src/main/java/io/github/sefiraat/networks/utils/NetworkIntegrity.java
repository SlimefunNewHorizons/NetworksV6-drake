package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Valida que los nodos registrados sigan siendo máquinas NTW reales en el mundo.
 * Mitiga #229 (miembros huérfanos) y #230 (celdas fantasma en grid).
 */
public final class NetworkIntegrity {

    private static final String NETWORK_ID_PREFIX = "NTW_";

    private NetworkIntegrity() {}

    public static boolean isNetworksMachine(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        // Un chunk descargado no significa que la maquina ya no exista. Consultar el bloque
        // devolvia AIR y pruneStaleLocations expulsaba del grafo a nodos perfectamente validos:
        // el jugador se alejaba, el chunk se descargaba y al volver su granja ya no estaba en la
        // red. Sin el chunk cargado no hay nada que comprobar, asi que se le da por bueno.
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return true;
        }
        if (location.getBlock().getType() == Material.AIR) {
            return false;
        }
        final SlimefunItem item = BlockStorage.check(location);
        return item != null && item.getId().startsWith(NETWORK_ID_PREFIX)
            && hasExpectedPhysicalMaterial(location.getBlock(), item);
    }

    /**
     * Confirms that persisted Slimefun metadata still describes the physical block in the world.
     * A non-air vanilla block is not enough: stale NTW metadata on dirt or snow previously made
     * Networks treat an invisible machine as real and Slimefun rejected every replacement.
     *
     * This deliberately mirrors Slimefun's physical-material predicate instead of linking to
     * it: the currently published 1.21.11 Drake API predates BlockStorageIntegrity. Networks
     * items are all physical NTW_* blocks, so virtual-item exemptions do not apply here.
     */
    public static boolean hasExpectedPhysicalMaterial(@Nonnull Block block, @Nonnull SlimefunItem item) {
        return matchesPhysicalMaterial(block.getType(), item.getItem().getType());
    }

    private static boolean matchesPhysicalMaterial(@Nonnull Material physical, @Nonnull Material expected) {
        if (physical == expected || (isCauldron(physical) && isCauldron(expected))) {
            return true;
        }

        final String physicalName = physical.name();
        if (physicalName.startsWith("WALL_") && physicalName.substring(5).equals(expected.name())) {
            return true;
        }
        return physicalName.contains("_WALL_")
            && physicalName.replace("_WALL_", "_").equals(expected.name());
    }

    private static boolean isCauldron(@Nonnull Material material) {
        return material == Material.CAULDRON
            || material == Material.WATER_CAULDRON
            || material == Material.LAVA_CAULDRON
            || material == Material.POWDER_SNOW_CAULDRON;
    }

    public static boolean isExpectedMachine(@Nullable Location location, @Nonnull Class<? extends SlimefunItem> type) {
        if (!isNetworksMachine(location)) {
            return false;
        }
        // Mismo motivo que arriba: sin el chunk cargado, BlockStorage devuelve null y la maquina
        // se daria por perdida. Se conserva hasta poder comprobarla de verdad.
        if (location != null && location.getWorld() != null
                && !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return true;
        }
        final SlimefunItem item = BlockStorage.check(location);
        return type.isInstance(item);
    }

    /**
     * Si el bloque ya no es NTW pero el grafo sigue registrado, limpia la membresía.
     */
    public static void purgeGhostMembership(@Nullable Location location) {
        if (location == null) {
            return;
        }
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(location);
        if (definition == null) {
            return;
        }
        if (!isNetworksMachine(location)) {
            NetworkUtils.clearNetwork(location);
        }
    }

    /**
     * Elimina ubicaciones del set interno que ya no son del tipo esperado.
     */
    public static void pruneStaleLocations(@Nonnull Set<Location> locations, @Nonnull Class<? extends SlimefunItem> expectedType) {
        final Iterator<Location> iterator = locations.iterator();
        while (iterator.hasNext()) {
            final Location location = iterator.next();
            if (!isExpectedMachine(location, expectedType)) {
                iterator.remove();
                purgeGhostMembership(location);
            }
        }
    }

    /**
     * Fluffy Barrel u otro bloque sobre coordenada de celda/monitor: expulsa del grafo (#230).
     */
    public static void onForeignBlockOccupied(@Nonnull Location location) {
        if (isNetworksMachine(location)) {
            return;
        }

        final Set<NetworkRoot> roots = new HashSet<>();
        for (NodeDefinition definition : NetworkStorage.getAllNetworkObjects().values()) {
            if (definition.getNode() != null && definition.getNode().getRoot() != null) {
                roots.add(definition.getNode().getRoot());
            }
        }

        for (NetworkRoot root : roots) {
            if (root.getNodeLocations().contains(location)) {
                root.evictStaleLocation(location);
            }
        }

        purgeGhostMembership(location);
    }
}
