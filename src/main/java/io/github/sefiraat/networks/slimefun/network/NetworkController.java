package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.holograms.NetworkHologramManager;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.events.PlayerRightClickEvent;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.ItemState;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import com.github.drakescraft_labs.slimefun4.utils.ChestMenuUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkController extends NetworkObject {

    private static final String CRAYON = "crayon";
    private static final String HOLOGRAM = "hologram";
    private static final Map<Location, NetworkRoot> NETWORKS = new ConcurrentHashMap<>();
    private static final Set<Location> CRAYONS = ConcurrentHashMap.newKeySet();
    private static final Set<Location> HOLOGRAMS = ConcurrentHashMap.newKeySet();
    private static final Set<Location> DIRTY_NETWORKS = ConcurrentHashMap.newKeySet();
    /** Tracks the server tick when a network was first marked dirty. Rebuild is deferred by DIRTY_DELAY_TICKS. */
    private static final Map<Location, Long> DIRTY_TICK = new ConcurrentHashMap<>();
    /** Number of server ticks to wait after markDirty() before rebuilding, so newly-placed
     *  machines have time to register themselves in NetworkStorage on their first tick. */
    private static final int DIRTY_DELAY_TICKS = 3;

    private final ItemSetting<Integer> maxNodes;
    private volatile int cachedMaxNodes = -1;
    // Shared controllers must be cleared on plugin shutdown to avoid stale locations after restart.
    protected static final Set<Location> initializedControllers = ConcurrentHashMap.newKeySet();

    public NetworkController(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CONTROLLER);

        maxNodes = new IntRangeSetting(this, "max_nodes", 10, 10000, 20000);
        addItemSetting(maxNodes);

        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block block, SlimefunItem item, Config data) {

                    final Location location = block.getLocation();
                    if (initializedControllers.add(location)) {
                        onFirstTick(block, data);
                    }

                    addToRegistry(block);
                    NetworkRoot networkRoot = NETWORKS.get(location);
                    if (networkRoot == null) {
                        networkRoot = rebuildNetwork(location, null);
                    } else if (DIRTY_NETWORKS.contains(location)) {
                        // Only rebuild after DIRTY_DELAY_TICKS have elapsed since dirty was set.
                        // This gives newly-placed machines enough time to register themselves
                        // in NetworkStorage on their first Slimefun tick.
                        long dirtyAt = DIRTY_TICK.getOrDefault(location, 0L);
                        long currentTick = block.getWorld().getGameTime();
                        if (currentTick - dirtyAt >= DIRTY_DELAY_TICKS) {
                            DIRTY_NETWORKS.remove(location);
                            DIRTY_TICK.remove(location);
                            networkRoot = rebuildNetwork(location, networkRoot);
                        }
                    }

                    networkRoot.setDisplayParticles(CRAYONS.contains(location));
                    if (HOLOGRAMS.contains(location)) {
                        NetworkHologramManager.updateHologram(location, networkRoot);
                    }
                }
            }
        );
    }

    @Override
    protected void prePlace(@Nonnull PlayerRightClickEvent event) {
        Optional<Block> blockOptional = event.getClickedBlock();

        if (blockOptional.isPresent()) {
            Block block = blockOptional.get();
            Block target = block.getRelative(event.getClickedFace());

            for (BlockFace checkFace : CHECK_FACES) {
                Block checkBlock = target.getRelative(checkFace);
                SlimefunItem slimefunItem = BlockStorage.check(checkBlock);

                // For directly adjacent controllers
                if (slimefunItem instanceof NetworkController) {
                    cancelPlace(event);
                    return;
                }

                // Check for node definitions. If there isn't one, we don't care
                NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(checkBlock.getLocation());
                if (definition == null) {
                    continue;
                }

                // There is a definition, if it has a node, then it's part of an active network.
                if (definition.getNode() != null) {
                    cancelPlace(event);
                    return;
                }
            }
        }
    }

    @Override
    protected void cancelPlace(PlayerRightClickEvent event) {
        event.getPlayer().sendMessage(Theme.ERROR.getColor() + "This network already has a controller!");
        event.cancel();
    }

    private void onFirstTick(@Nonnull Block block, @Nonnull Config data) {
        final String crayon = data.getString(CRAYON);
        if (Boolean.parseBoolean(crayon)) {
            CRAYONS.add(block.getLocation());
        }
        final String holo = data.getString(HOLOGRAM);
        // Holograms create TextDisplay entities and are refreshed by every controller tick.
        // Keep an explicitly saved preference, but make new/unconfigured controllers opt in
        // so passive Networks installations do not add entity work to the main thread.
        if (Boolean.parseBoolean(holo)) {
            HOLOGRAMS.add(block.getLocation());
        }
    }

    /**
     * Rebuilds the transient topology from persistent Slimefun block data.
     * This intentionally never writes or recreates blocks, inventories or items.
     */
    public @Nonnull NetworkRoot rebuildNetwork(@Nonnull Block block) {
        addToRegistry(block);
        final Location location = block.getLocation();
        final NetworkRoot networkRoot = rebuildNetwork(location, NETWORKS.get(location));
        networkRoot.setDisplayParticles(CRAYONS.contains(location));
        DIRTY_NETWORKS.remove(location);
        return networkRoot;
    }

    @Override
    protected void clearCachedState(@Nonnull Location location) {
        initializedControllers.remove(location);
    }

    @Override
    public void postRegister() {
        super.postRegister();
        try {
            this.cachedMaxNodes = maxNodes.getValue();
        } catch (Throwable t) {
            this.cachedMaxNodes = maxNodes.getDefaultValue();
        }
        createPreset();
    }

    public int getMaxNodes() {
        if (cachedMaxNodes > 0) {
            return cachedMaxNodes;
        }
        if (getState() == ItemState.UNREGISTERED) {
            return maxNodes.getDefaultValue();
        }
        try {
            int val = maxNodes.getValue();
            if (val > 0) {
                cachedMaxNodes = val;
                return val;
            }
        } catch (Throwable ignored) {
        }
        return maxNodes.getDefaultValue();
    }

    /**
     * Marca para reconstruccion las redes que alcanzan un mundo recien reindexado.
     *
     * Al cargarse un chunk, SyncListener devuelve sus nodos al registro global, pero el
     * NetworkRoot del controlador conserva las listas que tenia: la maquina vuelve a existir para
     * Networks y sigue sin existir para su propia red. Como rebuildNetwork solo corre si la red es
     * nula o esta sucia, el nodo recuperado no se reincorporaba nunca y la granja seguia parada
     * hasta que alguien rompia o colocaba algo cerca.
     *
     * Se marcan todas las redes del mundo afectado y no solo las cercanas porque una red puede
     * extenderse mucho mas alla del chunk que se acaba de cargar; el coste es una reconstruccion
     * diferida por controlador, que ya esta amortiguada por DIRTY_DELAY_TICKS.
     */
    public static void markNetworksDirtyInWorld(@Nonnull org.bukkit.World world) {
        long ahora = world.getGameTime();
        for (Location controlador : NETWORKS.keySet()) {
            if (world.equals(controlador.getWorld())) {
                DIRTY_NETWORKS.add(controlador);
                // Debounce from latest restored node to ensure burst finishes before rebuild
                DIRTY_TICK.put(controlador, ahora);
            }
        }
    }

    public static Map<Location, NetworkRoot> getNetworks() {
        return NETWORKS;
    }

    public static Set<Location> getCrayons() {
        return CRAYONS;
    }

    public static void addCrayon(@Nonnull Location location) {
        BlockStorage.addBlockInfo(location, CRAYON, String.valueOf(true));
        CRAYONS.add(location);
    }

    public static void removeCrayon(@Nonnull Location location) {
        BlockStorage.addBlockInfo(location, CRAYON, null);
        CRAYONS.remove(location);
    }

    public static boolean hasCrayon(@Nonnull Location location) {
        return CRAYONS.contains(location);
    }

    public static void markDirty(@Nonnull Location changedLocation) {
        final NodeDefinition changedDefinition = NetworkStorage.getAllNetworkObjects().get(changedLocation);
        markDefinitionRootDirty(changedDefinition);

        if (NETWORKS.containsKey(changedLocation)) {
            // Record the game-time tick when dirty was first set (don't overwrite if already pending).
            DIRTY_TICK.putIfAbsent(changedLocation, changedLocation.getWorld() != null
                    ? changedLocation.getWorld().getGameTime() : 0L);
            DIRTY_NETWORKS.add(changedLocation);
        }

        for (BlockFace face : CHECK_FACES) {
            final Location adjacent = changedLocation.clone().add(face.getDirection());
            final NodeDefinition adjacentDef = NetworkStorage.getAllNetworkObjects().get(adjacent);
            markDefinitionRootDirty(adjacentDef);
            // If an adjacent controller is dirty, record its dirty tick too.
            if (adjacentDef != null && adjacentDef.getNode() != null) {
                final Location ctrl = adjacentDef.getNode().getRoot().getController();
                if (ctrl != null && NETWORKS.containsKey(ctrl)) {
                    DIRTY_TICK.putIfAbsent(ctrl, ctrl.getWorld() != null
                            ? ctrl.getWorld().getGameTime() : 0L);
                }
            }
        }
    }

    private static void markDefinitionRootDirty(NodeDefinition definition) {
        if (definition == null || definition.getNode() == null) {
            return;
        }

        final Location controller = definition.getNode().getRoot().getController();
        if (controller != null && NETWORKS.containsKey(controller)) {
            // Give Slimefun a few ticks to finish creating the adjacent menu and
            // block data before the graph is traversed again. Without this tick,
            // a just-placed node can be skipped until the controller is replaced.
            DIRTY_TICK.putIfAbsent(controller, controller.getWorld() != null
                    ? controller.getWorld().getGameTime() : 0L);
            DIRTY_NETWORKS.add(controller);
        }
    }

    @Nonnull
    private NetworkRoot rebuildNetwork(@Nonnull Location location, NetworkRoot previousRoot) {
        // La red nueva se construye ANTES de tocar la anterior.
        //
        // Al reves quedaba una ventana en la que todos los nodos tenian node == null: si el BFS
        // de addAllChildren no alcanzaba alguno --porque su chunk estaba cargando o la maquina
        // aun no habia corrido su primer tick de Slimefun-- ese nodo quedaba huerfano y la red
        // dejaba de producir hasta romper y recolocar el controlador. Es lo que los jugadores
        // describen como "poner algo y que se paralice la net".
        //
        // addAllChildren no depende del estado previo: recorre por adyacencia y sobrescribe la
        // asignacion de cada nodo que alcanza, asi que reconstruir primero es seguro.
        final NetworkRoot networkRoot = new NetworkRoot(location, NodeType.CONTROLLER, getMaxNodes());
        networkRoot.addAllChildren();

        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(location);
        if (definition != null) {
            definition.setNode(networkRoot);
        }
        NETWORKS.put(location, networkRoot);

        releaseOrphans(previousRoot, networkRoot);
        return networkRoot;
    }

    /**
     * Suelta solo los nodos de la red anterior que la nueva ya no contiene.
     *
     * Lo que sigue perteneciendo a la red conserva su asignacion sin pasar por un estado nulo
     * intermedio, y lo que quedo fuera se libera para que no arrastre una red fantasma: esa era
     * la puerta del dupe del visor con el controlador roto.
     */
    private static void releaseOrphans(NetworkRoot previousRoot, NetworkRoot currentRoot) {
        if (previousRoot == null || previousRoot == currentRoot) {
            return;
        }

        final Set<Location> retained = new HashSet<>(currentRoot.getNodeLocations());
        for (Location nodeLocation : previousRoot.getNodeLocations()) {
            if (retained.contains(nodeLocation)) {
                continue;
            }
            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(nodeLocation);
            if (definition != null && definition.getNode() != null
                    && definition.getNode().getRoot() == previousRoot) {
                definition.setNode(null);
            }
        }
    }

    private static void clearAssignments(NetworkRoot networkRoot) {
        if (networkRoot == null) {
            return;
        }

        for (Location nodeLocation : networkRoot.getNodeLocations()) {
            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(nodeLocation);
            if (definition != null && definition.getNode() != null
                    && definition.getNode().getRoot() == networkRoot) {
                definition.setNode(null);
            }
        }
    }

    public static void wipeNetwork(@Nonnull Location location) {
        clearAssignments(NETWORKS.remove(location));
        DIRTY_NETWORKS.remove(location);
        DIRTY_TICK.remove(location);
        CRAYONS.remove(location);
        HOLOGRAMS.remove(location);
        NetworkHologramManager.removeHologram(location);
    }

    /** Drops runtime-only controller state so reloads cannot reuse stale graphs. */
    public static void clearRuntimeState() {
        NETWORKS.clear();
        CRAYONS.clear();
        HOLOGRAMS.clear();
        DIRTY_NETWORKS.clear();
        DIRTY_TICK.clear();
        initializedControllers.clear();
        NetworkHologramManager.clearAll();
    }

    private void createPreset() {
        new BlockMenuPreset(this.getId(), "§6✦ §eControlador de Red §6✦") {
            @Override
            public void init() {
                for (int i = 0; i < 27; i++) {
                    addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
                }
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                updateMenu(menu, b);
                menu.addMenuOpeningHandler(p -> updateMenu(menu, b));
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    private void updateMenu(@Nonnull BlockMenu menu, @Nonnull Block b) {
        final Location loc = b.getLocation();
        final NetworkRoot root = NETWORKS.get(loc);

        // Slot 10: Beacon -> Estado General
        final ItemStack beaconItem;
        if (root != null) {
            beaconItem = ItemCreator.create(
                    Material.BEACON,
                    "§b✦ Estado de la Red ✦",
                    "§7Nodos Conectados: §f" + root.getNodeCount() + " §7/ §8" + root.getMaxNodes(),
                    "§7Almacenamientos (Barrels): §f" + root.getOutputAbleBarrels().size(),
                    "§7Monitores de Stock: §f" + root.getMonitors().size(),
                    "§7Autocrafters Activos: §f" + root.getCrafters().size(),
                    "§7Celdas de Memoria: §f" + root.getCells().size(),
                    "§7Importadores / Exportadores: §f" + (root.getImporters().size() + root.getExporters().size()),
                    "§7Energía en Red: §e" + root.getRootPower() + " J",
                    "",
                    root.isOverburdened() ? "§c⚠ Red sobrecargada (límite alcanzado)" : "§a✔ Topología óptima y balanceada"
            );
        } else {
            beaconItem = ItemCreator.create(
                    Material.BEACON,
                    "§b✦ Estado de la Red ✦",
                    "§eConstruyendo topología...",
                    "§7El controlador se sincronizará en unos instantes."
            );
        }
        menu.replaceExistingItem(10, beaconItem);
        menu.addMenuClickHandler(10, (p, slot, item, action) -> false);

        // Slot 12: Hopper -> Telemetría y Flujo en tiempo real
        final double tps = root != null ? root.getThroughput().getItemsPerSecond() : 0.0;
        final long total = root != null ? root.getThroughput().getTotalTransferredItems() : 0L;

        final ItemStack hopperItem = ItemCreator.create(
                Material.HOPPER,
                "§a✦ Telemetría y Flujo en Vivo ✦",
                "§7Velocidad actual: §a+" + String.format(Locale.ROOT, "%.1f", tps) + " items/s",
                "§7Items enrutados totales: §e" + NumberFormat.getInstance().format(total),
                "",
                "§8Motor Fast-Path O(1) de DrakesCraft activo",
                "§8Rendimiento optimizado para redes masivas"
        );
        menu.replaceExistingItem(12, hopperItem);
        menu.addMenuClickHandler(12, (p, slot, item, action) -> false);

        // Slot 14: Holograma Flotante (TextDisplay)
        final boolean holoActive = HOLOGRAMS.contains(loc);
        final ItemStack holoItem = ItemCreator.create(
                holoActive ? Material.AMETHYST_CLUSTER : Material.AMETHYST_SHARD,
                "§d✦ Holograma Flotante ✦",
                "§7Muestra telemetría en tiempo real",
                "§7flotando directamente sobre el controlador.",
                "",
                "§7Estado: " + (holoActive ? "§aActivado" : "§cDesactivado"),
                "",
                "§eClick para " + (holoActive ? "desactivar" : "activar")
        );
        menu.replaceExistingItem(14, holoItem);
        menu.addMenuClickHandler(14, (p, slot, item, action) -> {
            if (HOLOGRAMS.contains(loc)) {
                HOLOGRAMS.remove(loc);
                BlockStorage.addBlockInfo(loc, HOLOGRAM, "false");
                NetworkHologramManager.removeHologram(loc);
                p.sendMessage("§c✖ Holograma de red desactivado.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            } else {
                HOLOGRAMS.add(loc);
                BlockStorage.addBlockInfo(loc, HOLOGRAM, "true");
                if (root != null) {
                    NetworkHologramManager.updateHologram(loc, root);
                }
                p.sendMessage("§a✔ Holograma de red activado.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
            }
            updateMenu(menu, b);
            return false;
        });

        // Slot 15: Crayon / Partículas de depuración
        final boolean crayonActive = CRAYONS.contains(loc);
        final ItemStack crayonItem = ItemCreator.create(
                Material.BLAZE_POWDER,
                "§6✦ Rayos de Conexión (Crayon) ✦",
                "§7Dibuja rayos de partículas entre",
                "§7cada máquina y sus nodos adyacentes.",
                "",
                "§7Estado: " + (crayonActive ? "§aActivado" : "§cDesactivado"),
                "",
                "§eClick para " + (crayonActive ? "desactivar" : "activar")
        );
        menu.replaceExistingItem(15, crayonItem);
        menu.addMenuClickHandler(15, (p, slot, item, action) -> {
            if (CRAYONS.contains(loc)) {
                removeCrayon(loc);
                p.sendMessage("§c✖ Rayos de conexión desactivados.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            } else {
                addCrayon(loc);
                p.sendMessage("§a✔ Rayos de conexión activados.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
            }
            updateMenu(menu, b);
            return false;
        });

        // Slot 16: Resumen en Chat
        final ItemStack bookItem = ItemCreator.create(
                Material.BOOK,
                "§e✦ Diagnóstico por Chat ✦",
                "§7Imprime el desglose de componentes,",
                "§7rendimiento y flujo en tu chat.",
                "",
                "§eClick para generar diagnóstico"
        );
        menu.replaceExistingItem(16, bookItem);
        menu.addMenuClickHandler(16, (p, slot, item, action) -> {
            if (root != null) {
                p.sendMessage("§8§m----------------------------------------");
                p.sendMessage("§6§l✦ DRAKESCRAFT NETWORKS · DIAGNÓSTICO ✦");
                p.sendMessage("§8§m----------------------------------------");
                p.sendMessage("§7Nodos Totales: §f" + root.getNodeCount() + "§8/§7" + root.getMaxNodes());
                p.sendMessage("§7Velocidad: §a+" + String.format(Locale.ROOT, "%.1f", root.getThroughput().getItemsPerSecond()) + " items/s");
                p.sendMessage("§7Items Enrutados: §e" + NumberFormat.getInstance().format(root.getThroughput().getTotalTransferredItems()) + " items");
                p.sendMessage("§7Almacenamientos: §f" + root.getOutputAbleBarrels().size() + " barrels");
                p.sendMessage("§7Monitores: §f" + root.getMonitors().size() + " §8| §7Autocrafters: §f" + root.getCrafters().size());
                p.sendMessage("§7Energía en Red: §e" + root.getRootPower() + " J");
                p.sendMessage("§8§m----------------------------------------");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
            } else {
                p.sendMessage("§cRed no disponible en este momento.");
            }
            return false;
        });
    }
}
