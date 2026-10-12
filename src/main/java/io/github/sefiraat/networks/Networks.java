package io.github.sefiraat.networks;

import com.github.drakescraft_labs.slimefun4.api.SlimefunAddon;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import io.github.sefiraat.networks.commands.NetworksMain;
import io.github.sefiraat.networks.integrations.HudCallbacks;
import io.github.sefiraat.networks.integrations.NetheoPlants;
import io.github.sefiraat.networks.listeners.SyncListener;
import io.github.sefiraat.networks.managers.ListenerManager;
import io.github.sefiraat.networks.managers.SupportedPluginManager;
import io.github.sefiraat.networks.network.SupportedRecipes;
import io.github.sefiraat.networks.persistence.QuantumStorageMirror;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import io.github.sefiraat.networks.utils.NetworkUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class Networks extends JavaPlugin implements SlimefunAddon {

    private static Networks instance;
    private static final long SHUTDOWN_DIRTY_MARK_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(2);

    private final String username;
    private final String repo;
    private final String branch;

    private ListenerManager listenerManager;
    private SupportedPluginManager supportedPluginManager;
    private QuantumStorageMirror quantumStorageMirror;

    public Networks() {
        this.username = "DrakesCraft-Labs";
        this.repo = "NetworksV6-drake";
        this.branch = "main";
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("########################################");
        getLogger().info("      Networks DrakesCraft Edition      ");
        getLogger().info("      Original author: Sefiraat         ");
        getLogger().info(" Repo: DrakesCraft-Labs/NetworksV6-drake");
        getLogger().info("########################################");

        saveDefaultConfig();
        this.quantumStorageMirror = QuantumStorageMirror.create(this);

        this.supportedPluginManager = new SupportedPluginManager();
        setupSlimefun();

        this.listenerManager = new ListenerManager();
        final NetworksMain networksCommand = new NetworksMain();
        this.getCommand("networks").setExecutor(networksCommand);
        this.getCommand("networks").setTabCompleter(networksCommand);

        SupportedRecipes.setup();

        // Slimefun finishes restoring block storage after addon enablement. Reindex twice
        // so controllers can rebuild networks in chunks that never fire ChunkLoadEvent.
        scheduleLoadedChunkReindex(200L);
        scheduleLoadedChunkReindex(600L);

        // Fix dupe bug which breaks the network controller data without player interaction
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    Set<Location> wrongs = new HashSet<>();
                    Set<Location> controllers = new HashSet<>(
                            NetworkController.getNetworks().keySet());
                    for (Location controller : controllers) {
                        if (controller != null && controller.getWorld() != null) {
                            int chunkX = controller.getBlockX() >> 4;
                            int chunkZ = controller.getBlockZ() >> 4;
                            if (controller.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                                if (!(BlockStorage.check(controller) instanceof NetworkController)) {
                                    wrongs.add(controller);
                                }
                            }
                        }
                    }

                    for (Location wrong : wrongs) {
                        NetworkUtils.clearNetwork(wrong);
                    }
                },
                5, Slimefun.getTickerTask().getTickRate()
        );

        // Iniciar telemetría de Action Bar HUD
        new io.github.sefiraat.networks.tasks.NetworkActionBarTask().runTaskTimer(this, 20L, 10L);
    }

    private void scheduleLoadedChunkReindex(long delayTicks) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                int indexed = SyncListener.indexLoadedChunks();
                // Registrar los nodos no basta: un controlador que ya reconstruyo antes de que el
                // reindex devolviera al registro un nodo lejano conserva su topologia y no lo
                // readopta, porque solo reconstruye si su red es nula o esta sucia. Tras reindexar
                // se marcan sucias las redes cargadas para que cada controlador rehaga su grafo una
                // vez e incorpore lo que faltara. Es lo que dejaba un nodo fuera de su red tras un
                // reinicio, con todo bien conectado y la maquina sin trabajar.
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    io.github.sefiraat.networks.slimefun.network.NetworkController
                        .markNetworksDirtyInWorld(world);
                }
                getLogger().info("Startup network reindex added " + indexed + " node(s).");
            } catch (Exception exception) {
                getLogger().severe("Startup network reindex failed: " + exception.getMessage());
                exception.printStackTrace();
            }
        }, delayTicks);
    }

    @Override
    public void onDisable() {
        if (instance == null) {
            return;
        }

        try {
            io.github.sefiraat.networks.holograms.NetworkHologramManager.clearAll();
        } catch (Throwable t) {
            getLogger().warning("Error clearing holograms on disable: " + t.getMessage());
        }

        try {
            Bukkit.getScheduler().cancelTasks(this);
        } catch (Throwable t) {
            getLogger().severe("Failed to cancel scheduler tasks: " + t.getMessage());
        }
        
        saveData();
        if (quantumStorageMirror != null) {
            quantumStorageMirror.close();
        }
        NetworkQuantumStorage.clearRuntimeCache();
        NetworkController.clearRuntimeState();
        NetworkRoot.clearRuntimeHistory();
        // El cache de permisos crece con cada par de dueno y ubicacion vistos.
        io.github.sefiraat.networks.utils.OwnerAccessCache.clear();
        NetworkStorage.clearRuntimeState();
        instance = null;
    }

    private void saveData() {
        getLogger().info("Marking Networks inventories dirty before shutdown (2s budget)...");

        try {
            int marked = markNetworkInventoriesDirty(System.nanoTime() + SHUTDOWN_DIRTY_MARK_BUDGET_NANOS);
            getLogger().info("Marked " + marked + " Networks inventories dirty. Slimefun will persist shared block storage.");
        } catch (Throwable t) {
            getLogger().severe("Failed to mark all network inventories dirty: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * Marks known network menus without scanning every Slimefun block in every world.
     * Slimefun owns the subsequent global persistence pass during its own shutdown.
     */
    private int markNetworkInventoriesDirty(long deadlineNanos) {
        int marked = 0;
        try {
            for (Location location : new HashSet<>(NetworkStorage.getAllNetworkObjects().keySet())) {
                if (System.nanoTime() >= deadlineNanos) {
                    getLogger().warning("Shutdown dirty-mark budget reached; remaining menus stay in Slimefun's normal save queue.");
                    return marked;
                }

                if (markNetworkInventoryDirty(location)) {
                    marked++;
                }
            }
        } catch (Throwable t) {
            getLogger().severe("Failed to dirty-mark stored network objects: " + t.getMessage());
        }

        return marked;
    }

    private boolean markNetworkInventoryDirty(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        try {
            final SlimefunItem item = BlockStorage.check(location);
            if (item == null || !item.getId().startsWith("NTW_") || !BlockMenuPreset.isInventory(item.getId())) {
                return false;
            }

            final BlockMenu menu = BlockStorage.getInventory(location);
            if (menu != null) {
                menu.markDirty();
                return true;
            }
        } catch (Throwable t) {
            getLogger().severe("Error marking network inventory dirty at location " + location + ": " + t.getMessage());
            t.printStackTrace();
        }

        return false;
    }

    public void setupSlimefun() {
        getLogger().info("[Networks] --- Starting Slimefun Setup ---");
        NetworkSlimefunItems.setup();

        if (supportedPluginManager.isNetheopoiesis()) {
            try {
                NetheoPlants.setup();
            } catch (NoClassDefFoundError e) {
                getLogger().severe("Netheopoiesis must be updated to meet Networks' requirements.");
            }
        }
        if (supportedPluginManager.isSlimeHud()) {
            try {
                HudCallbacks.setup();
            } catch (NoClassDefFoundError e) {
                getLogger().severe("SlimeHUD must be updated to meet Networks' requirements.");
            }
        }
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nullable
    @Override
    public String getBugTrackerURL() {
        return MessageFormat.format("https://github.com/{0}/{1}/issues/", this.username, this.repo);
    }

    @Nonnull
    public static PluginManager getPluginManager() {
        return Networks.getInstance().getServer().getPluginManager();
    }

    public static Networks getInstance() {
        return Networks.instance;
    }

    public static SupportedPluginManager getSupportedPluginManager() {
        return Networks.getInstance().supportedPluginManager;
    }

    public static ListenerManager getListenerManager() {
        return Networks.getInstance().listenerManager;
    }

    /** Records a quantum state in the optional SQL mirror without changing Slimefun persistence. */
    public void recordQuantumStorage(@Nonnull Location location, @Nonnull io.github.sefiraat.networks.network.stackcaches.QuantumCache cache) {
        if (quantumStorageMirror != null) {
            quantumStorageMirror.record(location, cache);
        }
    }
}
