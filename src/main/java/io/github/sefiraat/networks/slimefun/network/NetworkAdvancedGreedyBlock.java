package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/** Nine-slot network storage endpoint for a single matching item type. */
public final class NetworkAdvancedGreedyBlock extends NetworkGreedyBlock {

    private static final int[] INPUT_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26};
    private static final int[] BACKGROUND_SLOTS = {3, 4, 12, 13, 21, 22};
    private static final int[] TEMPLATE_BACKGROUND_SLOTS = {0, 1, 2, 9, 11, 18, 19, 20};
    private static final int[] STORAGE_BACKGROUND_SLOTS = {5, 14, 23};
    private static final ItemStack TEMPLATE_BACKGROUND = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE, Theme.SUCCESS + "Store items matching"
    );
    private static final ItemStack STORAGE_BACKGROUND = ItemCreator.create(
        Material.ORANGE_STAINED_GLASS_PANE, Theme.SUCCESS + "Storage"
    );

    public NetworkAdvancedGreedyBlock(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType,
        ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, io.github.sefiraat.networks.network.NodeType.GREEDY_BLOCK);
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(TEMPLATE_BACKGROUND, TEMPLATE_BACKGROUND_SLOTS);
                drawBackground(STORAGE_BACKGROUND, STORAGE_BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_ADVANCED_GREEDY_BLOCK.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? INPUT_SLOTS : new int[0];
            }
        };
    }
}
