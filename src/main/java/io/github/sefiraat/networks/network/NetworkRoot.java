package io.github.sefiraat.networks.network;

import com.cryptomorin.xseries.particles.XParticle;
import com.github.drakescraft_labs.infinityexpansion.items.storage.StorageCache;
import com.github.drakescraft_labs.infinityexpansion.items.storage.StorageUnit;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.barrel.InfinityBarrel;
import io.github.sefiraat.networks.network.barrel.NetworkStorage;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkAutoCrafter;
import io.github.sefiraat.networks.slimefun.network.NetworkCell;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.sefiraat.networks.slimefun.network.NetworkGreedyBlock;
import io.github.sefiraat.networks.slimefun.network.NetworkMonitor;
import io.github.sefiraat.networks.slimefun.network.NetworkPowerNode;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.NetworkIntegrity;
import io.github.sefiraat.networks.utils.NetworkStackAggregator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.StackUtils;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import lombok.Getter;
import lombok.Setter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkRoot extends NetworkNode {
    public static final int persistentThreshold = 15;
    public static final int cacheMissThreshold = 15;
    public static final int reduceMs = 8000;
    public static final int transportMissThreshold = 15;
    public static final Map<Location, Map<Location, Integer /* Access times */>> observingAccessHistory =
            new ConcurrentHashMap<>();
    public static final Map<Location, Map<Location, Integer /* Cache miss times */>> persistentAccessHistory =
            new ConcurrentHashMap<>();
    public static final Map<Location, Integer /* Transport miss times */> transportMissInputHistory =
            new ConcurrentHashMap<>();
    public static final Map<Location, Integer /* Transport miss times */> transportMissOutputHistory =
            new ConcurrentHashMap<>();
    public static final Map<Location, Long> controlledAccessInputHistory = new ConcurrentHashMap<>();
    public static final Map<Location, Long> controlledAccessOutputHistory = new ConcurrentHashMap<>();

    /** Clears adaptive runtime history before a plugin reload or server restart. */
    public static void clearRuntimeHistory() {
        observingAccessHistory.clear();
        persistentAccessHistory.clear();
        transportMissInputHistory.clear();
        transportMissOutputHistory.clear();
        controlledAccessInputHistory.clear();
        controlledAccessOutputHistory.clear();
    }
    @Getter
    private final NetworkThroughputTracker throughput = new NetworkThroughputTracker();
    private final Map<Material, List<BarrelIdentity>> inputBarrelsByMaterial = new ConcurrentHashMap<>();
    private final Map<Material, List<BarrelIdentity>> outputBarrelsByMaterial = new ConcurrentHashMap<>();
    @Getter
    private final long CREATED_TIME = System.currentTimeMillis();
    @Getter
    private final Set<Location> nodeLocations = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> conflictingControllers = ConcurrentHashMap.newKeySet();

    /**
     * Inventario agregado de la red, reutilizado durante una ventana muy corta.
     *
     * getAllNetworkItems() recorre barriles, greedy blocks, crafters y celdas, y cada uno de esos
     * cuatro pasos repite su propio pruneStaleLocations sobre la red entera. Con varias rejillas
     * abiertas sobre la misma red ese trabajo se repetia integro por cada una y en cada tick.
     *
     * Medio segundo de cache es imperceptible para quien mira la pantalla --la rejilla ya solo se
     * refresca cuando alguien la tiene abierta-- y recorta el recorrido a una vez por ventana en
     * lugar de una por rejilla.
     */
    private volatile Map<ItemStack, Integer> cachedNetworkItems;
    private volatile long cachedNetworkItemsAt;
    private static final long CACHE_ITEMS_MS = 500L;

    /**
     * Menus de la red resueltos, reutilizados durante un instante.
     *
     * Cada uno de estos getters lanza un pruneStaleLocations sobre todas sus ubicaciones y
     * consulta BlockStorage una por una. El problema no es el coste en si, sino donde ocurre:
     * addItemStack0 esta sincronizado sobre la raiz y llama a getGreedyBlockMenus dentro del
     * bloqueo, de modo que cada item que entra a la red retiene el lock mientras recorre nodos.
     * Con varios importadores trabajando a la vez, todos esperan a que el anterior termine su
     * recorrido y la red entera parece congelarse.
     *
     * 250 ms es mas corto que un ciclo de Slimefun, asi que una maquina recien puesta entra en la
     * siguiente pasada y el jugador no percibe demora.
     */
    private final Map<String, Set<BlockMenu>> cachedMenus = new ConcurrentHashMap<>();
    private final Map<String, Long> cachedMenusAt = new ConcurrentHashMap<>();
    private static final long CACHE_MENUS_MS = 250L;
    private final int[] CELL_AVAILABLE_SLOTS = NetworkCell.SLOTS;
    @Getter
    private final Set<Location> bridges = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> monitors = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> inputOnlyMonitors = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> outputOnlyMonitors = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> importers = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> exporters = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> grids = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> cells = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> grabbers = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> pushers = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> purgers = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> crafters = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> powerNodes = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> powerDisplays = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> encoders = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> greedyBlocks = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> cutters = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> pasters = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> vacuums = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> wirelessTransmitters = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> wirelessReceivers = ConcurrentHashMap.newKeySet();
    @Getter
    private final Set<Location> powerOutlets = ConcurrentHashMap.newKeySet();
    @Getter
    private final int maxNodes;
    @Getter
    private final boolean recordFlow;
    private int cellsSize = -1;
    @Getter
    private @Nullable Location controller = null;

    @Getter
    private boolean isOverburdened = false;

    @Deprecated
    private @Nullable Set<BarrelIdentity> barrels = null;

    private @Nullable Set<BarrelIdentity> inputAbleBarrels = null;
    private @Nullable Set<BarrelIdentity> outputAbleBarrels = null;
    
    private volatile @Nullable Map<Location, BarrelIdentity> mapInputAbleBarrels = null;
    private volatile @Nullable Map<Location, BarrelIdentity> mapOutputAbleBarrels = null;

    @Setter
    private long rootPower = 0;

    @Setter
    @Getter
    private boolean displayParticles = false;

    public NetworkRoot(@NotNull Location location, @NotNull NodeType type, int maxNodes) {
        this(location, type, maxNodes, false);
    }

    public NetworkRoot(
            @NotNull Location location,
            @NotNull NodeType type,
            int maxNodes,
            boolean recordFlow
        ) {
        super(location, type);
        this.maxNodes = maxNodes;
        this.root = this;
        this.recordFlow = recordFlow;

        registerNode(location, type);
    }

    public static void addPersistentAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.put(accessLocation, 0);
        persistentAccessHistory.put(location, locations);
    }

    public static void addCacheMiss(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        int value = locations.getOrDefault(accessLocation, 0) + 1;
        if (value > cacheMissThreshold) {
            removePersistentAccessHistory(location, accessLocation);
            return;
        }
        locations.put(accessLocation, value);
        persistentAccessHistory.put(location, locations);
    }

    public static void minusCacheMiss(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        int value = Math.max(locations.getOrDefault(accessLocation, 0) - 1, 0);
        locations.put(accessLocation, value);
    }

    public static Map<Location, Integer> getPersistentAccessHistory(Location location) {
        return persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
    }

    public static void removePersistentAccessHistory(Location location) {
        persistentAccessHistory.remove(location);
    }

    public static void removePersistentAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.remove(accessLocation);
        persistentAccessHistory.put(location, locations);
    }

    public static void addCountObservingAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        Integer count = locations.getOrDefault(accessLocation, 0);
        if (count >= persistentThreshold) {
            removeCountObservingAccessHistory(location, accessLocation);
            addPersistentAccessHistory(location, accessLocation);
            return;
        }
        locations.put(accessLocation, count + 1);
        observingAccessHistory.put(location, locations);
    }

    public static Map<Location, Integer> getCountObservingAccessHistory(Location location) {
        return observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
    }

    public static void removeCountObservingAccessHistory(Location location) {
        observingAccessHistory.remove(location);
    }

    public static void removeCountObservingAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.remove(accessLocation);
        observingAccessHistory.put(location, locations);
    }

    @Nullable
    public static InfinityBarrel getInfinityBarrel(@NotNull BlockMenu blockMenu, @NotNull StorageUnit storageUnit) {
        return getInfinityBarrel(blockMenu, storageUnit, false);
    }

    @Nullable
    public static InfinityBarrel getInfinityBarrel(
            @NotNull BlockMenu blockMenu, @NotNull StorageUnit storageUnit, boolean includeEmpty) {
        final ItemStack itemStack = blockMenu.getItemInSlot(16);
        final Config data = BlockStorage.getLocationInfo(blockMenu.getLocation());
        // Guard: getLocationInfo puede retornar null si el chunk acaba de cargar o no tiene datos (#NPE-blockdata).
        if (data == null) {
            return null;
        }
        final String storedString = data.getString("stored");

        if (storedString == null) {
            return null;
        }

        final int storedInt;
        try {
            storedInt = Integer.parseInt(storedString);
        } catch (NumberFormatException e) {
            return null;
        }

        if (!includeEmpty && (itemStack == null || itemStack.getType() == Material.AIR)) {
            return null;
        }

        final StorageCache cache = storageUnit.getCache(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        final ItemStack clone;
        if (itemStack == null) {
            clone = null;
        } else {
            clone = itemStack.clone();
            clone.setAmount(1);
        }

        return new InfinityBarrel(
                blockMenu.getLocation(), clone, storedInt, cache);
    }

    @Nullable
    public static NetworkStorage getNetworkStorage(@NotNull BlockMenu blockMenu) {
        return getNetworkStorage(blockMenu, false);
    }

    @Nullable
    public static NetworkStorage getNetworkStorage(@NotNull BlockMenu blockMenu, boolean includeEmpty) {
        final QuantumCache cache = NetworkQuantumStorage.getDatabaseCache(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        final ItemStack itemStack = cache.getItemStack();
        if ((itemStack == null || itemStack.getType() == Material.AIR) && !includeEmpty) {
            return null;
        }

        final ItemStack output = blockMenu.getItemInSlot(NetworkQuantumStorage.OUTPUT_SLOT);
        int storedInt = cache.getAmount();
        if (output != null && output.getType() != Material.AIR && StackUtils.itemsMatch(cache.getItemStack(), output)) {
            storedInt = storedInt + output.getAmount();
        }

        final ItemStack clone;

        if (itemStack != null) {
            clone = itemStack.clone();
            clone.setAmount(1);
        } else {
            clone = null;
        }

        return new NetworkStorage(blockMenu.getLocation(), clone, storedInt);
    }

    public void registerNode(@NotNull Location location, @NotNull NodeType type) {
        nodeLocations.add(location);
        switch (type) {
            case CONTROLLER -> this.controller = location;
            case BRIDGE -> bridges.add(location);
            case STORAGE_MONITOR -> monitors.add(location);
            case INPUT_ONLY_MONITOR -> inputOnlyMonitors.add(location);
            case OUTPUT_ONLY_MONITOR -> outputOnlyMonitors.add(location);
            case IMPORT -> importers.add(location);
            case ADVANCED_IMPORT -> importers.add(location);
            case EXPORT -> exporters.add(location);
            case ADVANCED_EXPORT -> exporters.add(location);
            case GRID -> grids.add(location);
            case CELL -> cells.add(location);
            case GRABBER -> grabbers.add(location);
            case PUSHER -> pushers.add(location);
            case PURGER -> purgers.add(location);
            case ADVANCED_PURGER -> purgers.add(location);
            case CRAFTER -> crafters.add(location);
            case POWER_NODE -> powerNodes.add(location);
            case POWER_DISPLAY -> powerDisplays.add(location);
            case ENCODER -> encoders.add(location);
            case GREEDY_BLOCK -> greedyBlocks.add(location);
            case CUTTER -> cutters.add(location);
            case PASTER -> pasters.add(location);
            case VACUUM, ADVANCED_VACUUM -> vacuums.add(location);
            case WIRELESS_TRANSMITTER -> wirelessTransmitters.add(location);
            case WIRELESS_RECEIVER -> wirelessReceivers.add(location);
            case POWER_OUTLET -> powerOutlets.add(location);
        }
        invalidateBarrelCaches();
    }

    /**
     * Records a controller reached through this graph without mutating either machine.
     * The foreign controller is deliberately excluded from this root so both networks remain safe.
     */
    public void recordControllerConflict(@NotNull Location location) {
        conflictingControllers.add(location);
    }

    /**
     * Quita la ubicación de los sets internos al desmontar un nodo (#229 / #230).
     */
    public void unregisterNode(@NotNull Location location, @NotNull NodeType type) {
        nodeLocations.remove(location);
        switch (type) {
            case CONTROLLER -> {
                if (location.equals(this.controller)) {
                    this.controller = null;
                }
            }
            case BRIDGE -> bridges.remove(location);
            case STORAGE_MONITOR -> monitors.remove(location);
            case INPUT_ONLY_MONITOR -> inputOnlyMonitors.remove(location);
            case OUTPUT_ONLY_MONITOR -> outputOnlyMonitors.remove(location);
            case IMPORT -> importers.remove(location);
            case ADVANCED_IMPORT -> importers.remove(location);
            case EXPORT -> exporters.remove(location);
            case ADVANCED_EXPORT -> exporters.remove(location);
            case GRID -> grids.remove(location);
            case CELL -> cells.remove(location);
            case GRABBER -> grabbers.remove(location);
            case PUSHER -> pushers.remove(location);
            case PURGER -> purgers.remove(location);
            case ADVANCED_PURGER -> purgers.remove(location);
            case CRAFTER -> crafters.remove(location);
            case POWER_NODE -> powerNodes.remove(location);
            case POWER_DISPLAY -> powerDisplays.remove(location);
            case ENCODER -> encoders.remove(location);
            case GREEDY_BLOCK -> greedyBlocks.remove(location);
            case CUTTER -> cutters.remove(location);
            case PASTER -> pasters.remove(location);
            case VACUUM, ADVANCED_VACUUM -> vacuums.remove(location);
            case WIRELESS_TRANSMITTER -> wirelessTransmitters.remove(location);
            case WIRELESS_RECEIVER -> wirelessReceivers.remove(location);
            case POWER_OUTLET -> powerOutlets.remove(location);
            default -> {
            }
        }
        invalidateBarrelCaches();
        // Limpiar historial de transporte para evitar memory leak (#229)
        observingAccessHistory.remove(location);
        persistentAccessHistory.remove(location);
        transportMissInputHistory.remove(location);
        transportMissOutputHistory.remove(location);
    }

    /**
     * Expulsa una coordenada del grafo cuando un bloque ajeno ocupa una posición de nodo NTW (#230).
     */
    public void evictStaleLocation(@NotNull Location location) {
        if (NetworkIntegrity.isNetworksMachine(location)) {
            return;
        }
        nodeLocations.remove(location);
        cells.remove(location);
        monitors.remove(location);
        inputOnlyMonitors.remove(location);
        outputOnlyMonitors.remove(location);
        bridges.remove(location);
        importers.remove(location);
        exporters.remove(location);
        grids.remove(location);
        grabbers.remove(location);
        pushers.remove(location);
        purgers.remove(location);
        crafters.remove(location);
        powerNodes.remove(location);
        powerDisplays.remove(location);
        encoders.remove(location);
        greedyBlocks.remove(location);
        cutters.remove(location);
        pasters.remove(location);
        vacuums.remove(location);
        wirelessTransmitters.remove(location);
        wirelessReceivers.remove(location);
        powerOutlets.remove(location);
        if (location.equals(this.controller)) {
            this.controller = null;
        }
        invalidateBarrelCaches();
    }

    private void invalidateBarrelCaches() {
        this.barrels = null;
        this.inputAbleBarrels = null;
        this.outputAbleBarrels = null;
        this.mapInputAbleBarrels = null;
        this.mapOutputAbleBarrels = null;
        this.inputBarrelsByMaterial.clear();
        this.outputBarrelsByMaterial.clear();
    }

    public int getNodeCount() {
        return this.nodeLocations.size();
    }

    public void setOverburdened(boolean overburdened) {
        if (overburdened && !isOverburdened) {
            final Location loc = this.nodePosition.clone();
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        loc.getWorld()
                                .spawnParticle(
                                        XParticle.EXPLOSION.get(),
                                        loc.clone().add(x, y, z),
                                        0);
                    }
                }
            }
        }
        this.isOverburdened = overburdened;
    }

    public @NotNull Map<ItemStack, Integer> getAllNetworkItems() {
        final long ahora = System.currentTimeMillis();
        final Map<ItemStack, Integer> reciente = cachedNetworkItems;
        if (reciente != null && ahora - cachedNetworkItemsAt < CACHE_ITEMS_MS) {
            return reciente;
        }

        final NetworkStackAggregator aggregator = new NetworkStackAggregator();

        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            final ItemStack sample = barrelIdentity.getItemStack();
            if (sample != null && !sample.getType().isAir()) {
                aggregator.add(sample, (int) Math.min(barrelIdentity.getAmount(), Integer.MAX_VALUE));
            }
        }

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            for (int slot : getGreedyBlockSlots(blockMenu)) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    aggregator.add(itemStack, itemStack.getAmount());
                }
            }
        }

        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    aggregator.add(itemStack, itemStack.getAmount());
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    aggregator.add(itemStack, itemStack.getAmount());
                }
            }
        }

        final Map<ItemStack, Integer> resultado = aggregator.asMap();
        cachedNetworkItems = resultado;
        cachedNetworkItemsAt = ahora;
        return resultado;
    }

    /**
     * Descarta el inventario cacheado.
     *
     * Se llama cuando algo saca o mete items por codigo, para que la siguiente lectura no
     * devuelva una foto anterior al cambio.
     */
    public void invalidateNetworkItemsCache() {
        cachedNetworkItems = null;
        cachedMenus.clear();
        cachedMenusAt.clear();
    }

    /** Menus guardados para esa clave, o null si no hay o ya caducaron. */
    private @Nullable Set<BlockMenu> menusCacheados(@NotNull String clave) {
        final Long momento = cachedMenusAt.get(clave);
        if (momento == null || System.currentTimeMillis() - momento >= CACHE_MENUS_MS) {
            return null;
        }
        return cachedMenus.get(clave);
    }

    private void guardarMenus(@NotNull String clave, @NotNull Set<BlockMenu> menus) {
        cachedMenus.put(clave, menus);
        cachedMenusAt.put(clave, System.currentTimeMillis());
    }

    @Deprecated
    @NotNull
    public Set<BarrelIdentity> getBarrels() {
        if (this.barrels != null) {
            return this.barrels;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        for (Location cellLocation : this.monitors) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = BlockStorage.check(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                    && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
            } else if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        this.barrels = barrelSet;
        return barrelSet;
    }

    @NotNull
    public Set<BlockMenu> getCellMenus() {
        final Set<BlockMenu> reciente = menusCacheados("cells");
        if (reciente != null) {
            return reciente;
        }
        NetworkIntegrity.pruneStaleLocations(this.cells, NetworkCell.class);
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location cellLocation : this.cells) {
            if (!NetworkIntegrity.isExpectedMachine(cellLocation, NetworkCell.class)) {
                continue;
            }
            final BlockMenu menu = BlockStorage.getInventory(cellLocation);
            if (menu != null) {
                menus.add(menu);
            }
        }
        guardarMenus("cells", menus);
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getCrafterOutputs() {
        final Set<BlockMenu> reciente = menusCacheados("crafters");
        if (reciente != null) {
            return reciente;
        }
        NetworkIntegrity.pruneStaleLocations(this.crafters, NetworkAutoCrafter.class);
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location location : this.crafters) {
            final BlockMenu menu = BlockStorage.getInventory(location);
            if (menu != null) {
                menus.add(menu);
            }
        }
        guardarMenus("crafters", menus);
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getGreedyBlockMenus() {
        final Set<BlockMenu> reciente = menusCacheados("greedy");
        if (reciente != null) {
            return reciente;
        }
        NetworkIntegrity.pruneStaleLocations(this.greedyBlocks, NetworkGreedyBlock.class);
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location location : this.greedyBlocks) {
            final BlockMenu menu = BlockStorage.getInventory(location);
            if (menu != null) {
                menus.add(menu);
            }
        }
        guardarMenus("greedy", menus);
        return menus;
    }

    /** Resolves the active storage slots from the placed greedy block variant. */
    private int[] getGreedyBlockSlots(@NotNull BlockMenu menu) {
        SlimefunItem item = BlockStorage.check(menu.getLocation());
        return item instanceof NetworkGreedyBlock greedyBlock ? greedyBlock.getInputSlots() : new int[0];
    }

    public synchronized boolean contains(@NotNull ItemStack itemStack) {
        return contains(new ItemRequest(itemStack, 1));
    }

    public synchronized boolean contains(@NotNull ItemRequest request) {

        long found = 0;

        // Barrels
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            final ItemStack itemStack = barrelIdentity.getItemStack();

            if (itemStack == null || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                continue;
            }

            if (barrelIdentity instanceof InfinityBarrel) {
                if (barrelIdentity.getItemStack().getMaxStackSize() > 1) {
                    found += barrelIdentity.getAmount() - 2;
                }
            } else {
                found += barrelIdentity.getAmount();
            }

            // Escape if found all we need
            if (found >= request.getAmount()) {
                return true;
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            for (int slot : getGreedyBlockSlots(blockMenu)) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || itemStack.getAmount() <= 0
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        // Cells
        for (BlockMenu blockMenu : getCellMenus()) {
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || itemStack.getAmount() <= 0
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getAmount(@NotNull ItemStack itemStack) {
        long totalAmount = 0;

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            for (int slot : getGreedyBlockSlots(blockMenu)) {
                ItemStack inputSlotItem = blockMenu.getItemInSlot(slot);
                if (inputSlotItem != null && StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                    totalAmount += inputSlotItem.getAmount();
                }
            }
        }

        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            if (StackUtils.itemsMatch(barrelIdentity.getItemStack(), itemStack)) {
                totalAmount += barrelIdentity.getAmount();
                if (barrelIdentity instanceof InfinityBarrel) {
                    totalAmount -= 2;
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack cellItem = blockMenu.getItemInSlot(slot);
                if (cellItem != null && StackUtils.itemsMatch(cellItem, itemStack)) {
                    totalAmount += cellItem.getAmount();
                }
            }
        }
        if (totalAmount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) totalAmount;
        }
    }

    public @NotNull HashMap<ItemStack, Long> getAmount(@NotNull Set<ItemStack> itemStacks) {
        HashMap<ItemStack, Long> totalAmounts = new HashMap<>();

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            for (int slot : getGreedyBlockSlots(blockMenu)) {
                ItemStack inputSlotItem = blockMenu.getItemInSlot(slot);
                if (inputSlotItem != null) {
                    for (ItemStack itemStack : itemStacks) {
                        if (StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                            totalAmounts.put(
                                    itemStack, totalAmounts.getOrDefault(itemStack, 0L) + inputSlotItem.getAmount());
                        }
                    }
                }
            }
        }

        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            for (ItemStack itemStack : itemStacks) {
                if (StackUtils.itemsMatch(barrelIdentity.getItemStack(), itemStack)) {
                    long totalAmount = barrelIdentity.getAmount();
                    if (barrelIdentity instanceof InfinityBarrel) {
                        totalAmount -= 2;
                    }
                    totalAmounts.put(itemStack, totalAmounts.getOrDefault(itemStack, 0L) + totalAmount);
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack cellItem = blockMenu.getItemInSlot(slot);
                if (cellItem != null) {
                    for (ItemStack itemStack : itemStacks) {
                        if (StackUtils.itemsMatch(cellItem, itemStack)) {
                            totalAmounts.put(
                                    itemStack, totalAmounts.getOrDefault(itemStack, 0L) + cellItem.getAmount());
                        }
                    }
                }
            }
        }

        return totalAmounts;
    }

    @Override
    public long retrieveBlockCharge() {
        return 0;
    }

    public long getRootPower() {
        long current = 0;
        for (Location node : powerNodes) {
            final SlimefunItem item = BlockStorage.check(node);
            if (item instanceof NetworkPowerNode powerNode) {
                current += powerNode.getCharge(node);
            }
        }
        this.rootPower = current;
        return current;
    }

    public void addRootPower(long power) {
        this.rootPower += power;
    }

    public void removeRootPower(long power) {
        if (power <= 0) {
            return;
        }

        long removed = 0;
        for (Location node : powerNodes) {
            final SlimefunItem item = BlockStorage.check(node);
            if (item instanceof NetworkPowerNode powerNode) {
                final int charge = powerNode.getCharge(node);
                if (charge <= 0) {
                    continue;
                }
                final int toRemove = (int) Math.min(power - removed, charge);
                powerNode.removeCharge(node, toRemove);
                // Restar solo lo que realmente se extrajo de este nodo, no el total pedido (#rootPower-underflow).
                this.rootPower = Math.max(0, this.rootPower - toRemove);
                removed = removed + toRemove;
            }
            if (removed >= power) {
                return;
            }
        }
    }

    @NotNull
    public synchronized List<ItemStack> getItemStacks0(@NotNull Location location, @NotNull List<ItemRequest> itemRequests) {
        List<ItemStack> retrievedItems = new ArrayList<>();
        for (ItemRequest request : itemRequests) {
            ItemStack retrieved = getItemStack0(location, request);
            if (retrieved != null) {
                retrievedItems.add(retrieved);
            }
        }
        return retrievedItems;
    }

    @Nullable
    public synchronized ItemStack[] getItemStacks0(@NotNull Location location, @NotNull ItemRequest[] requests) {
        final ItemStack[] extracted = new ItemStack[requests.length];

        for (int i = 0; i < requests.length; i++) {
            final ItemRequest request = requests[i];
            if (request == null) {
                continue;
            }

            final int requestedAmount = request.getAmount();
            final ItemRequest transactionRequest = new ItemRequest(request.getItemStack(), requestedAmount);
            final ItemStack fetched = getItemStack0(location, transactionRequest);
            if (fetched == null || fetched.getAmount() != requestedAmount) {
                if (fetched != null) {
                    addItemStack0(location, fetched);
                }
                rollbackExtracted(location, extracted);
                return null;
            }
            extracted[i] = fetched;
        }
        return extracted;
    }

    private void rollbackExtracted(@NotNull Location location, ItemStack[] extracted) {
        for (ItemStack itemStack : extracted) {
            if (itemStack != null) {
                addItemStack0(location, itemStack);
            }
        }
    }

    @NotNull
    public Set<BarrelIdentity> getInputAbleBarrels() {
        if (this.inputAbleBarrels != null) {
            return this.inputAbleBarrels;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.monitors);
        monitor.addAll(this.inputOnlyMonitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = BlockStorage.check(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                    && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
                continue;
            }
            if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        // Durante carga/reconstruccion BlockStorage puede conocer el monitor antes de que
        // QuantumCache hidrate el storage. No cacheamos ese falso vacio: el siguiente tick
        // vuelve a resolverlo y la granja se recupera sin recolocar el controller.
        if (barrelSet.isEmpty() && !monitor.isEmpty()) {
            return barrelSet;
        }

        this.inputAbleBarrels = barrelSet;
        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        final Map<Material, List<BarrelIdentity>> byMat = new ConcurrentHashMap<>();
        for (BarrelIdentity storage : barrelSet) {
            if (storage != null && storage.getLocation() != null) {
                map.put(storage.getLocation(), storage);
                if (storage.getItemStack() != null && storage.getItemStack().getType() != Material.AIR) {
                    byMat.computeIfAbsent(storage.getItemStack().getType(), k -> new ArrayList<>()).add(storage);
                }
            }
        }
        this.mapInputAbleBarrels = map;
        this.inputBarrelsByMaterial.clear();
        this.inputBarrelsByMaterial.putAll(byMat);

        return barrelSet;
    }

    @NotNull
    public Set<BarrelIdentity> getOutputAbleBarrels() {

        if (this.outputAbleBarrels != null) {
            return this.outputAbleBarrels;
        }

        NetworkIntegrity.pruneStaleLocations(this.monitors, NetworkMonitor.class);
        NetworkIntegrity.pruneStaleLocations(this.outputOnlyMonitors, NetworkDirectional.class);

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.monitors);
        monitor.addAll(this.outputOnlyMonitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = BlockStorage.check(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                    && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
                continue;
            }
            if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = BlockStorage.getInventory(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        // Misma regla para salidas: una carga parcial no debe congelar una red vacia.
        if (barrelSet.isEmpty() && !monitor.isEmpty()) {
            return barrelSet;
        }

        this.outputAbleBarrels = barrelSet;
        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        final Map<Material, List<BarrelIdentity>> byMatOut = new ConcurrentHashMap<>();
        for (BarrelIdentity storage : barrelSet) {
            if (storage != null && storage.getLocation() != null) {
                map.put(storage.getLocation(), storage);
                if (storage.getItemStack() != null && storage.getItemStack().getType() != Material.AIR) {
                    byMatOut.computeIfAbsent(storage.getItemStack().getType(), k -> new ArrayList<>()).add(storage);
                }
            }
        }
        this.mapOutputAbleBarrels = map;
        this.outputBarrelsByMaterial.clear();
        this.outputBarrelsByMaterial.putAll(byMatOut);
        return barrelSet;
    }

    public boolean refreshRootItems() {
        invalidateBarrelCaches();

        getBarrels();
        getInputAbleBarrels();
        getOutputAbleBarrels();
        return true;
    }

    @Nullable
    public BarrelIdentity accessInputAbleBarrel(@Nullable Location barrelLocation) {
        if (barrelLocation == null) {
            return null;
        }
        final Map<Location, BarrelIdentity> map = getMapInputAbleBarrels();
        return map != null ? map.get(barrelLocation) : null;
    }

    @Nullable
    public BarrelIdentity accessOutputAbleBarrel(@Nullable Location barrelLocation) {
        if (barrelLocation == null) {
            return null;
        }
        final Map<Location, BarrelIdentity> map = getMapOutputAbleBarrels();
        return map != null ? map.get(barrelLocation) : null;
    }

    @Nullable
    public ItemStack requestItem(@NotNull Location accessor, @NotNull ItemRequest request) {
        return getItemStack0(accessor, request);
    }

    @Nullable
    public ItemStack requestItem(@NotNull Location accessor, @NotNull ItemStack itemStack) {
        return requestItem(accessor, new ItemRequest(itemStack, itemStack.getAmount()));
    }

    public void tryRecord(@NotNull Location accessor, @NotNull ItemRequest request) {
    }

    public void tryRecordOutput(@NotNull Location accessor, @Nullable ItemStack retrieved) {
        if (retrieved != null && retrieved.getAmount() > 0) {
            throughput.recordFlow(accessor, retrieved.getAmount());
        }
    }

    public synchronized ItemStack getItemStack0(@NotNull Location accessor, @NotNull ItemRequest request) {
        ItemStack stackToReturn = null;

        if (request.getAmount() <= 0) {
            return null;
        }

        if (!allowAccessOutput(accessor)) {
            return null;
        }

        //true here
        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            // Netex - Cache start
            boolean found = false;
            List<Location> misses = new ArrayList<>();
            // Netex - Cache end
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                // try cache first
                BarrelIdentity barrelIdentity = accessOutputAbleBarrel(entry.getKey());
                if (barrelIdentity != null) {
                    final ItemStack itemStack = barrelIdentity.getItemStack();

                    if (itemStack == null || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                        // Netex - Cache start
                        misses.add(entry.getKey());
                        // Netex - Cache end
                        continue;
                    }

                    // Netex - Cache start
                    minusCacheMiss(accessor, entry.getKey());
                    found = true; // -> false
                    // Netex - Cache end

                    boolean infinity = barrelIdentity instanceof InfinityBarrel;
                    final ItemStack fetched = barrelIdentity.requestItem(request);
                    if (fetched == null
                            || fetched.getType() == Material.AIR
                            || (infinity && fetched.getAmount() == 1)) {
                        continue;
                    }

                    // Stack is null, so we can fill it here
                    if (stackToReturn == null) {
                        stackToReturn = fetched.clone();
                        stackToReturn.setAmount(0);
                    }

                    final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();

                    if (request.getAmount() <= preserveAmount) {
                        // Netex - Reduce start
                        uncontrolAccessOutput(accessor);
                        // Netex - Reduce end
                        stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                        fetched.setAmount(fetched.getAmount() - request.getAmount());
                        // Netex - Record start
                        tryRecord(accessor, request);
                        tryRecordOutput(accessor, stackToReturn);
                        // Netex - Record end
                        return stackToReturn;
                    } else {
                        stackToReturn.setAmount(stackToReturn.getAmount() + preserveAmount);
                        request.receiveAmount(preserveAmount);
                        fetched.setAmount(fetched.getAmount() - preserveAmount);
                    }
                }
            }

            // Netex - Cache start
            if (!found) {
                for (Location miss : misses) {
                    minusCacheMiss(accessor, miss);
                }
            }
            // Netex - Cache end
        }

        // Barrels first - fast-path por material
        List<BarrelIdentity> candidateOutputBarrels = outputBarrelsByMaterial.get(request.getItemStack().getType());
        Iterable<BarrelIdentity> barrelsToIterate = candidateOutputBarrels != null ? candidateOutputBarrels : getOutputAbleBarrels();
        for (BarrelIdentity barrelIdentity : barrelsToIterate) {
            // <editor-fold desc="do barrel">
            final ItemStack itemStack = barrelIdentity.getItemStack();

            if (itemStack == null || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                continue;
            }

            // Netex - Cache start
            addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
            // Netex - Cache end

            boolean infinity = barrelIdentity instanceof InfinityBarrel;
            final ItemStack fetched = barrelIdentity.requestItem(request);
            if (fetched == null || fetched.getType() == Material.AIR || (infinity && fetched.getAmount() == 1)) {
                continue;
            }

            // Stack is null, so we can fill it here
            if (stackToReturn == null) {
                stackToReturn = fetched.clone();
                stackToReturn.setAmount(0);
            }

            final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();

            if (request.getAmount() <= preserveAmount) {
                // Netex - Reduce start
                uncontrolAccessOutput(accessor);
                // Netex - Reduce end
                stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                fetched.setAmount(fetched.getAmount() - request.getAmount());
                // Netex - Record start
                tryRecord(accessor, request);
                tryRecordOutput(accessor, stackToReturn);
                // Netex - Record end
                return stackToReturn;
            } else {
                stackToReturn.setAmount(stackToReturn.getAmount() + preserveAmount);
                request.receiveAmount(preserveAmount);
                fetched.setAmount(fetched.getAmount() - preserveAmount);
            }
            // </editor-fold>
        }

        // Cell
        for (BlockMenu blockMenu : getCellMenus()) {
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || itemStack.getAmount() <= 0
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    if (itemStack != null && itemStack.getAmount() <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    }
                    continue;
                }

                // Mark the Cell as dirty otherwise the changes will not save on shutdown
                blockMenu.markDirty();

                // If the return stack is null, we need to set it up
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    uncontrolAccessOutput(accessor);
                    // Netex - Reduce end
                    // We can't take more than this stack. Level to request amount, remove items and then return
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    final int remaining = itemStack.getAmount() - request.getAmount();
                    if (remaining <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    } else {
                        itemStack.setAmount(remaining);
                    }
                    blockMenu.markDirty();
                    // Netex - Record start
                    tryRecord(accessor, request);
                    tryRecordOutput(accessor, stackToReturn);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    // We can take more than what is here, consume before trying to take more
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    blockMenu.replaceExistingItem(slot, null);
                    blockMenu.markDirty();
                }
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || itemStack.getAmount() <= 0
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    if (itemStack != null && itemStack.getAmount() <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    }
                    continue;
                }

                // Stack is null, so we can fill it here
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    uncontrolAccessOutput(accessor);
                    // Netex - Reduce end
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    final int remaining = itemStack.getAmount() - request.getAmount();
                    if (remaining <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    } else {
                        itemStack.setAmount(remaining);
                    }
                    blockMenu.markDirty();
                    // Netex - Record start
                    tryRecord(accessor, request);
                    tryRecordOutput(accessor, stackToReturn);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    blockMenu.replaceExistingItem(slot, null);
                    blockMenu.markDirty();
                }
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            for (int slot : getGreedyBlockSlots(blockMenu)) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                        || itemStack.getType() == Material.AIR
                        || itemStack.getAmount() <= 0
                        || !StackUtils.itemsMatch(request.getItemStack(), itemStack)) {
                    if (itemStack != null && itemStack.getAmount() <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    }
                    continue;
                }

                // Mark the storage block as dirty otherwise the changes will not save on shutdown
                blockMenu.markDirty();

                // If the return stack is null, we need to set it up
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    uncontrolAccessOutput(accessor);
                    // Netex - Reduce end
                    // We can't take more than this stack. Level to request amount, remove items and then return
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    final int remaining = itemStack.getAmount() - request.getAmount();
                    if (remaining <= 0) {
                        blockMenu.replaceExistingItem(slot, null);
                    } else {
                        itemStack.setAmount(remaining);
                    }
                    // Netex - Record start
                    tryRecord(accessor, request);
                    tryRecordOutput(accessor, stackToReturn);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    // We can take more than what is here, consume before trying to take more
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    blockMenu.replaceExistingItem(slot, null);
                }
            }
        }

        if (stackToReturn == null || stackToReturn.getAmount() == 0) {
            addTransportOutputMiss(accessor);
            return null;
        }

        // Netex - Reduce start
        uncontrolAccessOutput(accessor);
        // Netex - Reduce end
        // Netex - Record start
        tryRecord(accessor, request);
        tryRecordOutput(accessor, stackToReturn);
        // Netex - Record end

        return stackToReturn;
    }

    public void addItem(@NotNull Location accessor, @NotNull ItemStack incoming) {
        addItemStack0(accessor, incoming);
    }

    public void tryRecord(@NotNull Location accessor, @Nullable ItemStack before, int after) {
    }

    public void tryRecordInput(@NotNull Location accessor, int amount) {
        if (amount > 0) {
            throughput.recordFlow(accessor, amount);
        }
    }

    public synchronized void addItemStack0(@NotNull Location accessor, @NotNull ItemStack incoming) {
        if (!allowAccessInput(accessor)) {
            return;
        }

        ItemStack beforeItemStack = null;
        if (recordFlow) {
            beforeItemStack = incoming.clone();
        }

        int before = incoming.getAmount();

        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            // Netex - Cache start
            boolean found = false;
            List<Location> misses = new ArrayList<>();
            // Netex - Cache end
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                BarrelIdentity barrelIdentity = accessInputAbleBarrel(entry.getKey());
                if (barrelIdentity != null) {
                    // <editor-fold desc="do barrel">
                    if (StackUtils.itemsMatch(barrelIdentity.getItemStack(), incoming)) {
                        // Netex - Cache start
                        minusCacheMiss(accessor, entry.getKey());
                        found = true;
                        // Netex - Cache end

                        barrelIdentity.depositItemStack(incoming);

                        // All distributed, can escape
                        if (incoming.getAmount() == 0) {
                            // Netex - Reduce start
                            uncontrolAccessInput(accessor);
                            // Netex - Reduce end
                            // Netex - Record start
                            tryRecord(accessor, beforeItemStack, 0);
                            tryRecordInput(accessor, before - incoming.getAmount());
                            // Netex - Record end
                            return;
                        }
                    } else {
                        // Netex - Cache start
                        misses.add(entry.getKey());
                        // Netex - Cache end
                    }
                    // </editor-fold>
                }
            }

            // Netex - Cache start
            if (!found) {
                for (Location miss : misses) {
                    addCacheMiss(accessor, miss);
                }
            }
            // Netex - Cache end
        }

        // Run for matching greedy blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            NetworkTransportUtils.pushIntoMenu(blockMenu, incoming, getGreedyBlockSlots(blockMenu));
            if (incoming.getAmount() == 0) {
                uncontrolAccessInput(accessor);
                tryRecord(accessor, beforeItemStack, 0);
                tryRecordInput(accessor, before - incoming.getAmount());
                return;
            }
            // Plantilla coincide pero el greedy no absorbió todo: seguir con barriles/celdas
        }

        // Run for matching barrels - fast-path por material
        List<BarrelIdentity> candidateInputBarrels = inputBarrelsByMaterial.get(incoming.getType());
        Iterable<BarrelIdentity> inputBarrelsToIterate = candidateInputBarrels != null ? candidateInputBarrels : getInputAbleBarrels();
        for (BarrelIdentity barrelIdentity : inputBarrelsToIterate) {
            // <editor-fold desc="do barrel">
            if (StackUtils.itemsMatch(barrelIdentity.getItemStack(), incoming)) {
                // Netex - Cache start
                addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
                // Netex - Cache end

                barrelIdentity.depositItemStack(incoming);

                // All distributed, can escape
                if (incoming.getAmount() == 0) {
                    // Netex - Reduce start
                    uncontrolAccessInput(accessor);
                    // Netex - Reduce end
                    // Netex - Record start
                    tryRecord(accessor, beforeItemStack, 0);
                    tryRecordInput(accessor, before - incoming.getAmount());
                    // Netex - Record end
                    return;
                }
            }
            // </editor-fold>
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            blockMenu.markDirty();
            NetworkTransportUtils.pushIntoMenu(blockMenu, incoming, CELL_AVAILABLE_SLOTS);
            if (incoming.getAmount() == 0) {
                // Netex - Reduce start
                uncontrolAccessInput(accessor);
                // Netex - Reduce end
                // Netex - Record start
                tryRecord(accessor, beforeItemStack, 0);
                tryRecordInput(accessor, before - incoming.getAmount());
                // Netex - Record end
                return;
            }
        }

        // Netex - Reduce start
        if (before == incoming.getAmount()) {
            // No item moved, limit the accessor
            addTransportInputMiss(accessor);
        } else {
            uncontrolAccessInput(accessor);
        }
        // Netex - Reduce end
        // Netex - Record start
        tryRecord(accessor, beforeItemStack, incoming.getAmount());
        tryRecordInput(accessor, before - incoming.getAmount());
        // Netex - Record end
    }

    @NotNull
    public Map<Location, BarrelIdentity> getMapInputAbleBarrels() {
        final Map<Location, BarrelIdentity> cached = this.mapInputAbleBarrels;
        if (cached != null) {
            return cached;
        }

        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        final Set<BarrelIdentity> barrels = getInputAbleBarrels();
        if (barrels != null) {
            for (BarrelIdentity barrel : barrels) {
                if (barrel != null && barrel.getLocation() != null) {
                    map.put(barrel.getLocation(), barrel);
                }
            }
        }
        this.mapInputAbleBarrels = map;
        return map;
    }

    @NotNull
    public Map<Location, BarrelIdentity> getMapOutputAbleBarrels() {
        final Map<Location, BarrelIdentity> cached = this.mapOutputAbleBarrels;
        if (cached != null) {
            return cached;
        }

        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        final Set<BarrelIdentity> barrels = getOutputAbleBarrels();
        if (barrels != null) {
            for (BarrelIdentity barrel : barrels) {
                if (barrel != null && barrel.getLocation() != null) {
                    map.put(barrel.getLocation(), barrel);
                }
            }
        }
        this.mapOutputAbleBarrels = map;
        return map;
    }


    public boolean allowAccessInput(@NotNull Location accessor) {
        Long lastTime = controlledAccessInputHistory.get(accessor);
        if (lastTime == null) {
            return true;
        } else {
            return System.currentTimeMillis() - lastTime > reduceMs;
        }
    }

    public boolean allowAccessOutput(@NotNull Location accessor) {
        Long lastTime = controlledAccessOutputHistory.get(accessor);
        if (lastTime == null) {
            return true;
        } else {
            return System.currentTimeMillis() - lastTime > reduceMs;
        }
    }

    public void addTransportInputMiss(@NotNull Location location) {
        transportMissInputHistory.merge(location, 1, (a, b) -> {
            if (a + b > transportMissThreshold) {
                controlAccessInput(location);
                return transportMissThreshold;
            } else {
                return a + b;
            }
        });
    }

    public void addTransportOutputMiss(@NotNull Location location) {
        transportMissOutputHistory.merge(location, 1, (a, b) -> {
            if (a + b > transportMissThreshold) {
                controlAccessOutput(location);
                return transportMissThreshold;
            } else {
                return a + b;
            }
        });
    }

    public void reduceTransportInputMiss(@NotNull Location location) {
        transportMissInputHistory.merge(location, -1, (a, b) -> Math.max(a + b, 0));
    }

    public void reduceTransportOutputMiss(@NotNull Location location) {
        transportMissOutputHistory.merge(location, -1, (a, b) -> Math.max(a + b, 0));
    }

    public void controlAccessInput(@NotNull Location accessor) {
        controlledAccessInputHistory.put(accessor, System.currentTimeMillis());
    }

    public void controlAccessOutput(@NotNull Location accessor) {
        controlledAccessOutputHistory.put(accessor, System.currentTimeMillis());
    }

    public void uncontrolAccessInput(@NotNull Location accessor) {
        controlledAccessInputHistory.remove(accessor);
        reduceTransportInputMiss(accessor);
    }

    public void uncontrolAccessOutput(@NotNull Location accessor) {
        controlledAccessOutputHistory.remove(accessor);
        reduceTransportOutputMiss(accessor);
    }

    public int getCellsSize() {
        if (cellsSize != -1) {
            return cellsSize;
        }

        cellsSize = getCells().size();
        return cellsSize;
    }

    @Warning(
            reason =
                    "This method is deprecated and will be removed in the future. Use addItemStack0(Location, ItemStack) instead.")
    @Deprecated(forRemoval = true)
    public void addItemStack(@NotNull ItemStack incoming) {
        // Run for matching greedy blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            NetworkTransportUtils.pushIntoMenu(blockMenu, incoming, getGreedyBlockSlots(blockMenu));
            // Given we have found a match, it doesn't matter if the item moved or not, we will not bring it in
            return;
        }

        // Run for matching barrels
        for (BarrelIdentity barrelIdentity : getInputAbleBarrels()) {
            if (StackUtils.itemsMatch(barrelIdentity, incoming, true)) {
                barrelIdentity.depositItemStack(incoming);

                // All distributed, can escape
                if (incoming.getAmount() == 0) {
                    return;
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            blockMenu.markDirty();
            NetworkTransportUtils.pushIntoMenu(blockMenu, incoming, CELL_AVAILABLE_SLOTS);
            if (incoming.getAmount() == 0) {
                return;
            }
        }
    }
}
