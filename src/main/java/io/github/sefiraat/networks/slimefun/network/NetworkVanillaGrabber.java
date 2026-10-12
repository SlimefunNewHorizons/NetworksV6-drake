package io.github.sefiraat.networks.slimefun.network;

import com.bgsoftware.wildchests.api.WildChestsAPI;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.listeners.BlockStateRefreshListener;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import com.github.drakescraft_labs.slimefun4.api.MinecraftVersion;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Extrae de inventarios vanilla hacia la red. Evita atasco en OUTPUT (#235) inyectando directo cuando hay nodo.
 */
public class NetworkVanillaGrabber extends NetworkDirectional {

    private static final int[] BACKGROUND_SLOTS = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 16, 17, 18, 20, 22, 23, 24, 26, 27, 28, 30, 31, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int OUTPUT_SLOT = 25;
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;

    public NetworkVanillaGrabber(ItemGroup itemGroup,
                                 SlimefunItemStack item,
                                 RecipeType recipeType,
                                 ItemStack[] recipe
    ) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            final Location loc = blockMenu.getLocation();
            if (isIdleOnCooldown(loc)) {
                return;
            }
            if (!tryGrabItem(blockMenu)) {
                deferIdle(loc);
            }
        }
    }

    private boolean tryGrabItem(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return false;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        final Location accessor = blockMenu.getLocation();

        // Desatascar buffer interno antes de extraer mas (#235)
        boolean flushed = flushOutputBuffer(blockMenu, root, accessor);

        final ItemStack pending = blockMenu.getItemInSlot(OUTPUT_SLOT);
        if (pending != null && pending.getType() != Material.AIR) {
            return flushed;
        }

        final BlockFace direction = getCurrentDirection(blockMenu);
        final Block block = blockMenu.getBlock();
        final Block targetBlock = block.getRelative(direction);
        final String owner = BlockStorage.getLocationInfo(block.getLocation(), OWNER_KEY);
        if (owner == null) {
            return false;
        }
        final UUID uuid;
        try {
            uuid = UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            return false;
        }
        // Cacheado unos segundos: esta comprobacion consulta a WorldGuard y se repetia en cada
        // tick de cada nodo para responder casi siempre lo mismo.
        if (!io.github.sefiraat.networks.utils.OwnerAccessCache.canInteract(uuid, targetBlock)) {
            return false;
        }

        final BlockState blockState = BlockStateRefreshListener.getFreshState(targetBlock);

        if (!(blockState instanceof InventoryHolder holder)) {
            return false;
        }

        if (Networks.getSupportedPluginManager().isWildChests()
                && WildChestsAPI.getChest(targetBlock.getLocation()) != null) {
            return false;
        }

        final Inventory inventory = holder.getInventory();

        if (inventory instanceof FurnaceInventory furnaceInventory) {
            if (tryPullFromInventory(blockMenu, root, accessor, furnaceInventory, 2) > 0) {
                return true;
            }
            final ItemStack fuel = furnaceInventory.getFuel();
            if (fuel != null && fuel.getType() == Material.BUCKET) {
                if (tryPullFromInventory(blockMenu, root, accessor, furnaceInventory, 1) > 0) {
                    return true;
                }
            }
        } else if (inventory instanceof BrewerInventory brewerInventory) {
            for (int i = 0; i < 3; i++) {
                final ItemStack stack = brewerInventory.getItem(i);
                if (stack == null || stack.getType() == Material.AIR) {
                    continue;
                }
                // Guard: solo procesar items con PotionMeta; ingredientes de addons pueden no tenerla (#NPE-brewer).
                if (!(stack.getItemMeta() instanceof PotionMeta potionMeta)) {
                    if (tryPullFromInventory(blockMenu, root, accessor, brewerInventory, i) > 0) {
                        return true;
                    }
                    continue;
                }
                if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_20_5)) {
                    if (potionMeta.getBasePotionType() != PotionType.WATER
                            && tryPullFromInventory(blockMenu, root, accessor, brewerInventory, i) > 0) {
                        return true;
                    }
                } else {
                    PotionData bpd = potionMeta.getBasePotionData();
                    if (bpd != null && bpd.getType() != PotionType.WATER
                            && tryPullFromInventory(blockMenu, root, accessor, brewerInventory, i) > 0) {
                        return true;
                    }
                }
            }
        } else {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (tryPullFromInventory(blockMenu, root, accessor, inventory, slot) > 0) {
                    return true;
                }
            }
        }
        return flushed;
    }

    private boolean flushOutputBuffer(@Nonnull BlockMenu blockMenu, @Nonnull NetworkRoot root, @Nonnull Location accessor) {
        final ItemStack pending = blockMenu.getItemInSlot(OUTPUT_SLOT);
        if (pending == null || pending.getType() == Material.AIR) {
            return false;
        }
        if (NetworkTransportUtils.flushMenuSlotToNetwork(root, accessor, blockMenu, OUTPUT_SLOT) > 0) {
            blockMenu.markDirty();
            return true;
        }
        return false;
    }

    private int tryPullFromInventory(
            @Nonnull BlockMenu blockMenu,
            @Nonnull NetworkRoot root,
            @Nonnull Location accessor,
            @Nonnull Inventory inventory,
            int slot) {
        final int consumed = NetworkTransportUtils.pullFromInventory(root, accessor, inventory, slot);
        if (consumed > 0) {
            blockMenu.markDirty();
            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(accessor);
            if (definition != null
                    && definition.getNode() != null
                    && definition.getNode().getRoot().isDisplayParticles()) {
                showParticle(accessor, getCurrentDirection(blockMenu));
            }
        }
        return consumed;
    }

    @Nonnull
    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Override
    public int getNorthSlot() {
        return NORTH_SLOT;
    }

    @Override
    public int getSouthSlot() {
        return SOUTH_SLOT;
    }

    @Override
    public int getEastSlot() {
        return EAST_SLOT;
    }

    @Override
    public int getWestSlot() {
        return WEST_SLOT;
    }

    @Override
    public int getUpSlot() {
        return UP_SLOT;
    }

    @Override
    public int getDownSlot() {
        return DOWN_SLOT;
    }

    @Override
    public boolean runSync() {
        return true;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.MAROON, 1);
    }
}
