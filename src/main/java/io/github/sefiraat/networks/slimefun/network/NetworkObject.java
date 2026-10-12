package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.NetworkIntegrity;
import io.github.sefiraat.networks.utils.NetworkUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.events.PlayerRightClickEvent;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockBreakHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockPlaceHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemUseHandler;
import com.github.drakescraft_labs.slimefun4.implementation.items.blocks.UnplaceableBlock;

import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class NetworkObject extends SlimefunItem implements AdminDebuggable {

    @Getter
    private final NodeType nodeType;
    @Getter
    private final List<Integer> slotsToDrop = new ArrayList<>();

    protected static final Set<BlockFace> CHECK_FACES = Set.of(
        BlockFace.UP,
        BlockFace.DOWN,
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.EAST,
        BlockFace.WEST
    );


    protected NetworkObject(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, NodeType type) {
        this(itemGroup, item, recipeType, recipe, null, type);
    }

    protected NetworkObject(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput, NodeType type) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
        this.nodeType = type;
        addItemHandler(
            new BlockTicker() {

                @Override
                public boolean isSynchronized() {
                    return runSync();
                }

                @Override
                public void tick(Block b, SlimefunItem item, Config data) {
                    addToRegistry(b);
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                    preBreak(event);
                    onBreak(event);
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@Nonnull BlockPlaceEvent blockPlaceEvent) {
                    onPlace(blockPlaceEvent);
                }
            },
            new ItemUseHandler() {
                @Override
                public void onRightClick(PlayerRightClickEvent playerRightClickEvent) {
                    prePlace(playerRightClickEvent);
                }
            }
        );
    }

    protected void addToRegistry(@Nonnull Block block) {
        final Location location = block.getLocation();
        if (NetworkStorage.getAllNetworkObjects().containsKey(location)) {
            return;
        }
        final NodeDefinition nodeDefinition = new NodeDefinition(nodeType);
        if (NetworkStorage.getAllNetworkObjects().putIfAbsent(location, nodeDefinition) == null) {
            /*
             * A persisted node can reach its first ticker after the controller already rebuilt.
             * Registering it without invalidating the graph leaves the machine disconnected until
             * a player breaks or places a nearby block. World-wide invalidation is intentional:
             * a network can cross chunk boundaries, and the dirty set coalesces the startup burst.
             */
            NetworkController.markNetworksDirtyInWorld(block.getWorld());
        }
    }

    protected void preBreak(@Nonnull BlockBreakEvent event) {

    }

    protected void onBreak(@Nonnull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final BlockMenu blockMenu = BlockStorage.getInventory(event.getBlock());

        if (blockMenu != null) {
            for (int i : this.slotsToDrop) {
                blockMenu.dropItems(location, i);
            }
        }
        NetworkController.markDirty(location);
        if (this.nodeType == NodeType.CONTROLLER) {
            NetworkController.wipeNetwork(location);
        }
        NetworkStorage.removeNode(location);
        clearCachedState(location);

        BlockStorage.clearBlockInfo(location);
    }

    protected void clearCachedState(@Nonnull Location location) {
    }

    protected void prePlace(@Nonnull PlayerRightClickEvent event) {
        Optional<Block> blockOptional = event.getClickedBlock();
        Location controllerLocation = null;

        if (blockOptional.isPresent()) {
            Block block = blockOptional.get();
            Block target = block.getRelative(event.getClickedFace());

            for (BlockFace checkFace : CHECK_FACES) {
                Block checkBlock = target.getRelative(checkFace);

                // Check for node definitions. If there isn't one, we don't care
                NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(checkBlock.getLocation());
                if (definition == null) {
                    continue;
                }

                /*
                 * A stale definition beside the target must not turn an ordinary placement into
                 * a fake controller conflict. Purge it here, before the custom right-click event
                 * can cancel creation of Bukkit's BlockPlaceEvent.
                 */
                if (!NetworkIntegrity.isNetworksMachine(checkBlock.getLocation())) {
                    NetworkUtils.clearNetwork(checkBlock.getLocation());
                    continue;
                }

                // There is a definition, if it has a node, then it's part of an active network.
                if (definition.getNode() != null) {
                    NetworkRoot networkRoot = definition.getNode().getRoot();
                    if (controllerLocation == null) {
                        // First network found, store root location
                        controllerLocation = networkRoot.getController();
                    } else if (!controllerLocation.equals(networkRoot.getController())) {
                        // Location differs from that previously recorded, would result in two controllers
                        cancelPlace(event);
                    }
                }
            }
        }
    }

    protected void cancelPlace(PlayerRightClickEvent event) {
        event.getPlayer().sendMessage(Theme.ERROR.getColor() + "This placement would connect two controllers!");
        event.cancel();
    }

    protected void onPlace(@Nonnull BlockPlaceEvent event) {
        // Register first, then invalidate adjacent roots so a newly added node is
        // included without requiring players to replace the Network Controller.
        final Location location = event.getBlockPlaced().getLocation();

        // addToRegistry usa putIfAbsent, asi que una definicion persistida de la maquina anterior
        // sobreviviria con su NodeType antiguo y el grafo trataria a esta como lo que ya no es.
        // El jugador acaba de colocarla: su tipo manda sobre cualquier resto en la coordenada.
        final NodeDefinition previous = NetworkStorage.getAllNetworkObjects().get(location);
        if (previous != null && previous.getType() != this.nodeType) {
            NetworkUtils.clearNetwork(location);
        }

        addToRegistry(event.getBlockPlaced());
        NetworkController.markDirty(location);
    }

    public boolean runSync() {
        return true;
    }
}
