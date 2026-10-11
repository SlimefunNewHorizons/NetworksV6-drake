package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.data.ItemFlowRecord;
import com.balugaq.netex.api.events.NetworkRootReadyEvent;
import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NetworkController extends NetworkObject {
    @Getter
    private static final Map<Location, ItemFlowRecord> records = new ConcurrentHashMap<>();

    @Getter
    private static final Map<Location, Boolean> recordFlow = new ConcurrentHashMap<>();

    private static final String CRAYON = "crayon";
    private static final Map<Location, NetworkRoot> NETWORKS = new ConcurrentHashMap<>();
    private static final Set<Location> CRAYONS = ConcurrentHashMap.newKeySet();
    protected final Map<Location, Boolean> firstTickMap = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull ItemSetting<Integer> maxNodes;

    public NetworkController(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CONTROLLER);

        maxNodes = new IntRangeSetting(this, "max_nodes", 10, 8000, 50000);
        addItemSetting(maxNodes);

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public void tick(@NotNull Block block, SlimefunItem item, @NotNull SlimefunBlockData data) {
                // Block.getLocation() crea un Location nuevo en cada llamada; aqui se pedia 8 veces.
                final Location location = block.getLocation();
                if (!firstTickMap.containsKey(location)) {
                    onFirstTick(block, data);
                    firstTickMap.put(location, true);
                }

                addToRegistry(block);
                NetworkRoot networkRoot = new NetworkRoot(
                    location,
                    NodeType.CONTROLLER,
                    maxNodes.getValue(),
                    recordFlow.getOrDefault(location, false),
                    records.get(location));
                networkRoot.addAllChildren();
                networkRoot.markReady();

                boolean crayon = CRAYONS.contains(location);
                if (crayon) {
                    networkRoot.setDisplayParticles(true);
                }

                NETWORKS.put(location, networkRoot);

                NodeDefinition definition = NetworkStorage.getNode(location);
                if (definition != null) {
                    definition.setNode(networkRoot);
                }
                NetworkRootReadyEvent event = new NetworkRootReadyEvent(networkRoot);
                Bukkit.getPluginManager().callEvent(event);
            }
        });
    }

    public static void enableRecord(Location root) {
        recordFlow.put(root, true);
        records.putIfAbsent(root, new ItemFlowRecord());
    }

    public static void disableRecord(Location root) {
        recordFlow.put(root, false);
        ItemFlowRecord record = records.get(root);
        if (record != null) {
            record.forceGC();
        }
    }

    public static @NotNull Map<Location, NetworkRoot> getNetworks() {
        return NETWORKS;
    }

    public static @NotNull Set<Location> getCrayons() {
        return CRAYONS;
    }

    public static void addCrayon(@NotNull Location location) {
        StorageCacheUtils.setData(location, CRAYON, String.valueOf(true));
        CRAYONS.add(location);
    }

    public static void removeCrayon(@NotNull Location location) {
        StorageCacheUtils.removeData(location, CRAYON);
        CRAYONS.remove(location);
    }

    public static boolean hasCrayon(@NotNull Location location) {
        return CRAYONS.contains(location);
    }

    public static void wipeNetwork(@NotNull Location location) {
        NetworkRoot networkRoot = NETWORKS.remove(location);
        if (networkRoot != null) {
            for (NetworkNode node : networkRoot.getChildrenNodes()) {
                NetworkStorage.removeNode(node.getNodePosition());
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    protected void cancelPlace(@NotNull BlockPlaceEvent event) {
        event.getPlayer().sendMessage(Lang.getString("messages.unsupported-operation.controller.cancel_place"));
        event.setCancelled(true);
    }

    private void onFirstTick(@NotNull Block block, @NotNull SlimefunBlockData data) {
        final String crayon = data.getData(CRAYON);
        if (Boolean.parseBoolean(crayon)) {
            CRAYONS.add(block.getLocation());
        }
    }

    /**
     * Vacia el estado estatico del controlador.
     *
     * NETWORKS, CRAYONS, records y recordFlow son estaticos y sobreviven a una
     * recarga del plugin: al volver quedan llenos de Location de un mundo que ya no esta cargado,
     * apuntando a NetworkRoot muertos. La red parece existir y no responde, y solo se arregla
     * reiniciando el servidor entero.
     *
     * Es la misma familia de fallo que la fuga de SELECTED_DIRECTION_MAP: estado estatico que
     * nadie limpia porque "total, el servidor se reinicia".
     */
    public static void clearRuntimeState() {
        NETWORKS.clear();
        CRAYONS.clear();
        records.clear();
        recordFlow.clear();
    }

}
