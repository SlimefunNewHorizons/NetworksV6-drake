package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
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

/**
 * The native Drake port of NetworksExpansion's bulk import machine.
 * It uses a full double chest but retains Drake's synchronized transport path.
 */
public final class NetworkAdvancedImport extends NetworkObject {

    private static final int[] INPUT_SLOTS = new int[54];

    static {
        for (int slot = 0; slot < INPUT_SLOTS.length; slot++) {
            INPUT_SLOTS[slot] = slot;
        }
    }

    private final ItemSetting<Integer> tickRate;

    public NetworkAdvancedImport(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ADVANCED_IMPORT);

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);
        for (int inputSlot : INPUT_SLOTS) {
            getSlotsToDrop().add(inputSlot);
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
                importItems(menu);
            }

            @Override
            public void uniqueTick() {
                tick = tick <= 1 ? tickRate.getValue() : tick - 1;
            }
        });
    }

    private void importItems(@Nonnull BlockMenu menu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(menu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }

        boolean changed = false;
        for (int inputSlot : INPUT_SLOTS) {
            final ItemStack item = menu.getItemInSlot(inputSlot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            changed |= NetworkTransportUtils.pullIntoNetwork(
                definition.getNode().getRoot(), menu.getLocation(), menu, inputSlot) > 0;
        }
        if (changed) {
            menu.markDirty();
        }
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(54);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkAdvancedImport.this.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? INPUT_SLOTS : new int[0];
            }
        };
    }
}
