package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.NetworkIntegrity;
import io.github.sefiraat.networks.utils.NetworkUtils;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

import javax.annotation.Nonnull;

public class SyncListener implements Listener {

    /**
     * Removes a persisted Networks ghost before Slimefun's normal-priority placement guard sees
     * the coordinate. The replaced state is the real block that existed before this event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlacePreflight(@Nonnull BlockPlaceEvent event) {
        final Location location = event.getBlock().getLocation();
        final SlimefunItem stored = BlockStorage.check(location);
        if (stored instanceof NetworkObject
                && event.getBlockReplacedState().getType() != stored.getItem().getType()) {
            NetworkUtils.clearNetwork(location);
            BlockStorage.clearBlockInfo(location);
        }
    }

    /** Clears invisible Networks machines when the player first interacts with their old block. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGhostInteract(@Nonnull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        final Location location = event.getClickedBlock().getLocation();
        final SlimefunItem stored = BlockStorage.check(location);
        if (stored instanceof NetworkObject
                && !NetworkIntegrity.hasExpectedPhysicalMaterial(event.getClickedBlock(), stored)) {
            NetworkUtils.clearNetwork(location);
            BlockStorage.clearBlockInfo(location);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(@Nonnull BlockBreakEvent event) {
        NetworkUtils.clearNetwork(event.getBlock().getLocation());
    }

    /**
     * Expulsa del grafo la coordenada que acaba de ocupar un bloque ajeno (#230).
     *
     * El BlockListener de Slimefun tambien escucha en HIGHEST y corre ANTES que este, porque
     * Networks se habilita despues de su dependencia: cuando lo que se coloca es una maquina NTW,
     * Slimefun ya persistio su id y ya invoco NetworkObject.onPlace, que registro el nodo. Borrarlo
     * aqui sin mirar dejaba el registro recien creado en nada, y el nodo solo volvia al grafo
     * cuando su propio ticker lo redescubria y disparaba otra reconstruccion diferida: eso es lo
     * que los jugadores describen como tener que colocar la maquina tres veces (#268).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@Nonnull BlockPlaceEvent event) {
        final Location location = event.getBlock().getLocation();
        NetworkIntegrity.onForeignBlockOccupied(location);

        // Una maquina NTW real en la coordenada es el nodo que se acaba de colocar, no un fantasma.
        if (NetworkIntegrity.isNetworksMachine(location)) {
            return;
        }

        NetworkUtils.clearNetwork(location);
    }

    /** Wither, dripleaf, pistons: evita nodo NTW huérfano (#229). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(@Nonnull EntityChangeBlockEvent event) {
        final Location location = event.getBlock().getLocation();
        NetworkIntegrity.purgeGhostMembership(location);
        if (NetworkStorage.getAllNetworkObjects().containsKey(location)) {
            NetworkUtils.clearNetwork(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(@Nonnull BlockFromToEvent event) {
        NetworkIntegrity.purgeGhostMembership(event.getToBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(@Nonnull BlockBreakEvent event) {
        NetworkIntegrity.purgeGhostMembership(event.getBlock().getLocation());
    }

    // Fixed a dupe — árboles/estructuras sobre nodos NTW (#229 / #230)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(@Nonnull StructureGrowEvent event) {
        for (BlockState state : event.getBlocks()) {
            final Location location = state.getBlock().getLocation();
            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(location);

            if (definition == null || definition.getNode() == null) {
                continue;
            }

            NetworkUtils.clearNetwork(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(@Nonnull org.bukkit.event.world.ChunkLoadEvent event) {
        // A newly generated chunk cannot contain persisted Networks nodes.
        if (event.isNewChunk()) {
            return;
        }

        final org.bukkit.Chunk chunk = event.getChunk();
        org.bukkit.Bukkit.getScheduler().runTask(io.github.sefiraat.networks.Networks.getInstance(), () -> {
            if (chunk.isLoaded()) {
                indexChunk(chunk);
                if (chunkHasDetachedNodes(chunk)) {
                    io.github.sefiraat.networks.slimefun.network.NetworkController
                        .markNetworksDirtyInWorld(chunk.getWorld());
                }
            }
        });
    }

    /**
     * Si el chunk contiene algun nodo de Networks que no esta ligado a su red activa.
     *
     * Cuenta como desligado el nodo sin NetworkNode --recien devuelto al registro y aun sin
     * adoptar-- y el que apunta a una raiz que ya no es la vigente de su controlador, porque este
     * reconstruyo y creo una nueva sin alcanzarlo. En ambos casos el nodo existe para Networks
     * pero no para su propia red, que es la desincronizacion que hay que reconciliar. Un nodo bien
     * conectado apunta a la raiz que NETWORKS tiene registrada para su controlador, y no dispara
     * nada, de modo que una recarga normal no provoca reconstrucciones.
     */
    public static boolean chunkHasDetachedNodes(@Nonnull org.bukkit.Chunk chunk) {
        for (Location location : com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage.getLocations(chunk)) {
            final com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem item =
                com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage.check(location);

            if (!(item instanceof io.github.sefiraat.networks.slimefun.network.NetworkObject)) {
                continue;
            }

            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(location);
            if (definition == null) {
                continue;
            }

            final io.github.sefiraat.networks.network.NetworkNode node = definition.getNode();
            if (node == null) {
                return true;
            }

            final io.github.sefiraat.networks.network.NetworkRoot root = node.getRoot();
            final Location controller = root.getController();
            if (controller == null
                || io.github.sefiraat.networks.slimefun.network.NetworkController
                        .getNetworks().get(controller) != root) {
                return true;
            }
        }
        return false;
    }

    /** Reindexes network nodes in a chunk that Slimefun loaded before Networks was ready. */
    public static int indexChunk(@Nonnull org.bukkit.Chunk chunk) {
        int indexed = 0;

        // Use Slimefun's persistent per-chunk index. The ticker index only contains
        // blocks already scheduled and therefore cannot repair a cold-loaded network.
        // Copy the locations because clearing a ghost updates Slimefun's backing index.
        for (Location location : new java.util.ArrayList<>(BlockStorage.getLocations(chunk))) {
            final com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem item =
                BlockStorage.check(location);

            if (item instanceof NetworkObject
                    && !NetworkIntegrity.hasExpectedPhysicalMaterial(location.getBlock(), item)) {
                NetworkUtils.clearNetwork(location);
                BlockStorage.clearBlockInfo(location);
                continue;
            }

            if (item instanceof NetworkObject networkObject
                && !NetworkStorage.getAllNetworkObjects().containsKey(location)) {
                final NodeDefinition nodeDefinition = new NodeDefinition(networkObject.getNodeType());
                NetworkStorage.getAllNetworkObjects().put(location, nodeDefinition);
                indexed++;
            }
        }

        return indexed;
    }

    /** Reindexes chunks that were already loaded before Bukkit could emit ChunkLoadEvent. */
    public static int indexLoadedChunks() {
        int indexed = 0;
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                indexed += indexChunk(chunk);
            }
        }
        return indexed;
    }
}
