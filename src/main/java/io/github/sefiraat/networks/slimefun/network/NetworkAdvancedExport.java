package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Drake port of NetworksExpansion's bulk export machine.
 * Templates define item requests; output is atomically returned to the network
 * whenever the destination cannot accept it.
 */
public final class NetworkAdvancedExport extends NetworkObject {

    private static final int[] TEMPLATE_SLOTS = createSlots(0, 18);
    private static final int[] OUTPUT_SLOTS = createSlots(27, 18);
    private static final int[] BACKGROUND_SLOTS = {
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final ItemStack TEMPLATE_BACKGROUND = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE, Theme.SUCCESS + "Export templates"
    );
    private static final ItemStack OUTPUT_BACKGROUND = ItemCreator.create(
        Material.ORANGE_STAINED_GLASS_PANE, Theme.SUCCESS + "Export output"
    );

    private final ItemSetting<Integer> tickRate;

    public NetworkAdvancedExport(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ADVANCED_EXPORT);

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);
        for (int slot : TEMPLATE_SLOTS) {
            getSlotsToDrop().add(slot);
        }
        for (int slot : OUTPUT_SLOTS) {
            getSlotsToDrop().add(slot);
        }

        addItemHandler(new BlockTicker() {
            private int tick = 1;

            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem ignored, Config config) {
                if (tick > 1) {
                    return;
                }

                final BlockMenu menu = BlockStorage.getInventory(block);
                if (menu == null) {
                    return;
                }
                addToRegistry(block);
                exportItems(menu);
            }

            @Override
            public void uniqueTick() {
                tick = tick <= 1 ? tickRate.getValue() : tick - 1;
            }
        });
    }

    private void exportItems(@Nonnull BlockMenu menu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(menu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }

        final List<ItemRequest> requests = collectRequests(menu);
        if (requests.isEmpty() || !hasOutputSpace(menu)) {
            return;
        }

        boolean changed = false;
        for (ItemRequest request : requests) {
            final ItemStack fetched = definition.getNode().getRoot().getItemStack0(menu.getLocation(), request);
            if (fetched != null && fetched.getAmount() > 0) {
                changed |= NetworkTransportUtils.pushIntoMenuOrReturn(
                    definition.getNode().getRoot(), menu.getLocation(), menu, fetched, OUTPUT_SLOTS);
            }
        }
        if (changed) {
            menu.markDirty();
        }
    }

    @Nonnull
    private static List<ItemRequest> collectRequests(@Nonnull BlockMenu menu) {
        final List<ItemRequest> requests = new ArrayList<>();
        for (int slot : TEMPLATE_SLOTS) {
            final ItemStack template = menu.getItemInSlot(slot);
            if (template != null && template.getType() != Material.AIR && template.getAmount() > 0) {
                final ItemStack requested = template.clone();
                requested.setAmount(1);
                requests.add(new ItemRequest(requested, template.getAmount()));
            }
        }
        return requests;
    }

    static boolean hasOutputSpace(@Nonnull BlockMenu menu) {
        final ItemStack[] output = new ItemStack[OUTPUT_SLOTS.length];
        for (int index = 0; index < OUTPUT_SLOTS.length; index++) {
            output[index] = menu.getItemInSlot(OUTPUT_SLOTS[index]);
        }
        return hasOutputSpace(output);
    }

    static boolean hasOutputSpace(@Nonnull ItemStack[] output) {
        for (int slot : OUTPUT_SLOTS) {
            final int index = slot - OUTPUT_SLOTS[0];
            final ItemStack item = output[index];
            if (item == null || item.getType() == Material.AIR || item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static int[] createSlots(int start, int length) {
        final int[] slots = new int[length];
        for (int index = 0; index < length; index++) {
            slots[index] = start + index;
        }
        return slots;
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(54);
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(TEMPLATE_BACKGROUND, new int[]{22});
                drawBackground(OUTPUT_BACKGROUND, new int[]{49});
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkAdvancedExport.this.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.WITHDRAW ? OUTPUT_SLOTS : new int[0];
            }
        };
    }
}
