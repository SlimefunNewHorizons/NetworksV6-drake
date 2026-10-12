package io.github.sefiraat.networks.slimefun.network;

import org.bukkit.Location;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkPusher extends NetworkDirectional {

    private static final int[] BACKGROUND_SLOTS = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 17, 18, 20, 22, 23, 24, 26, 27, 28, 30, 31, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] TEMPLATE_BACKGROUND = new int[]{16};
    private static final int[] TEMPLATE_SLOTS = new int[]{25, 34};
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;

    public static final ItemStack TEMPLATE_BACKGROUND_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Push items matching template"
    );

    public NetworkPusher(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.PUSHER);
        for (int slot : TEMPLATE_SLOTS) {
            this.getSlotsToDrop().add(slot);
        }
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            final Location loc = blockMenu.getLocation();
            if (isIdleOnCooldown(loc)) {
                return;
            }
            if (!tryPushItem(blockMenu)) {
                deferIdle(loc);
            }
        }
    }

    private boolean tryPushItem(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return false;
        }

        final BlockFace direction = getCurrentDirection(blockMenu);
        final BlockMenu targetMenu = BlockStorage.getInventory(blockMenu.getBlock().getRelative(direction));

        if (targetMenu == null || !NetworkTransportUtils.isExternalInventory(targetMenu)) {
            return false;
        }

        boolean anyMoved = false;
        for (int itemSlot : this.getItemSlots()) {
            final ItemStack testItem = blockMenu.getItemInSlot(itemSlot);

            if (testItem == null || testItem.getType() == Material.AIR) {
                continue;
            }

            final ItemStack clone = testItem.clone();
            clone.setAmount(1);

            int[] slots = NetworkTransportUtils.getTransportSlots(targetMenu, ItemTransportFlow.INSERT, clone);
            final int insertionCapacity = getInsertionCapacity(targetMenu, slots, clone);
            if (insertionCapacity <= 0) {
                continue;
            }

            final ItemRequest itemRequest = new ItemRequest(
                    clone,
                    Math.min(insertionCapacity, clone.getMaxStackSize()));
            final ItemStack retrieved = definition.getNode().getRoot().getItemStack0(blockMenu.getLocation(), itemRequest);
            if (retrieved != null) {
                final int retrievedAmount = retrieved.getAmount();
                final ItemStack leftover = targetMenu.pushItem(retrieved, slots);
                final int insertedAmount = leftover == null ? retrievedAmount : retrievedAmount - leftover.getAmount();

                if (leftover != null && leftover.getAmount() > 0) {
                    definition.getNode().getRoot().addItemStack0(blockMenu.getLocation(), leftover);
                }

                if (insertedAmount > 0) {
                    anyMoved = true;
                    if (definition.getNode().getRoot().isDisplayParticles()) {
                        showParticle(blockMenu.getLocation(), direction);
                    }
                }
            }
        }
        return anyMoved;
    }

    private int getInsertionCapacity(@Nonnull BlockMenu targetMenu, @Nonnull int[] slots,
            @Nonnull ItemStack itemToInsert) {
        int capacity = 0;
        final ItemRequest matchRequest = new ItemRequest(itemToInsert, 1);

        for (int slot : slots) {
            final ItemStack current = targetMenu.getItemInSlot(slot);
            if (current == null || current.getType() == Material.AIR) {
                capacity += itemToInsert.getMaxStackSize();
            } else if (StackUtils.itemsMatch(matchRequest, current, true)) {
                capacity += Math.max(0, current.getMaxStackSize() - current.getAmount());
            }

            if (capacity >= itemToInsert.getMaxStackSize()) {
                return itemToInsert.getMaxStackSize();
            }
        }
        return capacity;
    }

    @Nonnull
    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Nullable
    @Override
    protected int[] getOtherBackgroundSlots() {
        return TEMPLATE_BACKGROUND;
    }

    @Nullable
    @Override
    protected ItemStack getOtherBackgroundStack() {
        return TEMPLATE_BACKGROUND_STACK;
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
    public int[] getItemSlots() {
        return TEMPLATE_SLOTS;
    }

    @Override
    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.MAROON, 1);
    }

    @Override
    public boolean runSync() {
        return true;
    }
}
