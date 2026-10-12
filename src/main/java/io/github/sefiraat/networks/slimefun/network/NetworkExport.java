package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockBreakHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.items.CustomItemStack;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class NetworkExport extends NetworkObject {

    private static final int[] BACKGROUND_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 17, 18, 22, 26, 27, 31, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int TEST_ITEM_SLOT = 20;
    private static final int[] TEST_ITEM_BACKDROP = {10, 11, 12, 19, 21, 28, 29, 30};
    private static final int OUTPUT_ITEM_SLOT = 24;
    private static final int[] OUTPUT_ITEM_BACKDROP = {14, 15, 16, 23, 25, 32, 33, 34};

    private static final ItemStack TEST_BACKDROP_STACK = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE,
        Theme.SUCCESS + "Export Item Matching"
    );

    private static final ItemStack OUTPUT_BACKDROP_STACK = ItemCreator.create(
        Material.ORANGE_STAINED_GLASS_PANE,
        Theme.SUCCESS + "Output Slot"
    );

    private final ItemSetting<Integer> tickRate;

    public NetworkExport(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.EXPORT);
        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);

        this.getSlotsToDrop().add(TEST_ITEM_SLOT);
        this.getSlotsToDrop().add(OUTPUT_ITEM_SLOT);

        addItemHandler(
            new BlockTicker() {

                private int tick = 1;

                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block block, SlimefunItem item, Config data) {
                    if (tick <= 1) {
                        final BlockMenu blockMenu = BlockStorage.getInventory(block);
                        addToRegistry(block);
                        if (blockMenu != null) {
                            tryFetchItem(blockMenu);
                        }
                    }
                }

                @Override
                public void uniqueTick() {
                    tick = tick <= 1 ? tickRate.getValue() : tick - 1;
                }
            },
            new BlockBreakHandler(true, true) {
                @Override
                public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                    BlockMenu blockMenu = BlockStorage.getInventory(e.getBlock());
                    if (blockMenu != null) {
                        blockMenu.dropItems(blockMenu.getLocation(), TEST_ITEM_SLOT);
                        blockMenu.dropItems(blockMenu.getLocation(), OUTPUT_ITEM_SLOT);
                    }
                }
            }
        );
    }

    private void tryFetchItem(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        final ItemStack testItem = blockMenu.getItemInSlot(TEST_ITEM_SLOT);
        if (testItem == null || testItem.getType() == Material.AIR) {
            return;
        }

        final ItemStack itemInOutput = blockMenu.getItemInSlot(OUTPUT_ITEM_SLOT);

        // Skip if output is completely full (full stack or incompatible item)
        if (itemInOutput != null && itemInOutput.getType() != Material.AIR) {
            if (itemInOutput.getAmount() >= itemInOutput.getMaxStackSize()) {
                return;
            }
            // Has a partial stack — only continue if it's the same item type
            if (!io.github.sefiraat.networks.utils.StackUtils.itemsMatch(testItem, itemInOutput)) {
                return;
            }
        }

        final int spaceInOutput = itemInOutput == null || itemInOutput.getType() == Material.AIR
            ? testItem.getMaxStackSize()
            : testItem.getMaxStackSize() - itemInOutput.getAmount();

        if (spaceInOutput <= 0) {
            return;
        }

        final ItemStack clone = testItem.clone();
        final ItemRequest itemRequest = new ItemRequest(clone, spaceInOutput);
        final ItemStack retrieved = definition.getNode().getRoot().getItemStack0(blockMenu.getLocation(), itemRequest);
        if (retrieved != null) {
            NetworkTransportUtils.pushIntoMenuOrReturn(definition.getNode().getRoot(), blockMenu.getLocation(), blockMenu, retrieved, OUTPUT_ITEM_SLOT);
            blockMenu.markDirty();
        }
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(TEST_BACKDROP_STACK, TEST_ITEM_BACKDROP);
                drawBackground(OUTPUT_BACKDROP_STACK, OUTPUT_ITEM_BACKDROP);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_GRID.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_ITEM_SLOT};
                }
                return new int[0];
            }
        };
    }
}
