package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.items.CustomItemStack;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public class NetworkGreedyBlock extends NetworkObject {

    public static final int TEMPLATE_SLOT = 10;
    public static final int INPUT_SLOT = 16;
    private static final int[] BACKGROUND_SLOTS = new int[]{
        3, 4, 5, 12, 13, 14, 21, 22, 23
    };
    private static final int[] BACKGROUND_SLOTS_TEMPLATE = new int[]{
        0,1,2,9,11,18,19,20
    };
    private static final int[] BACKGROUND_SLOTS_INPUT = new int[]{
        6,7,8,15,17,24,25,26
    };

    private static final ItemStack TEMPLATE_BACKGROUND_STACK = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE,
        Theme.SUCCESS + "Store items matching"
    );

    private static final ItemStack STORAGE_BACKGROUND_STACK = ItemCreator.create(
        Material.ORANGE_STAINED_GLASS_PANE,
        Theme.SUCCESS + "Storage"
    );

    public NetworkGreedyBlock(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        this(itemGroup, item, recipeType, recipe, NodeType.GREEDY_BLOCK);
    }

    protected NetworkGreedyBlock(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
        NodeType nodeType) {
        super(itemGroup, item, recipeType, recipe, nodeType);
        for (int slot : getInputSlots()) {
            this.getSlotsToDrop().add(slot);
        }
        this.getSlotsToDrop().add(TEMPLATE_SLOT);
    }

    /** Slots whose stacks are exposed to and stored by the network root. */
    public int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(TEMPLATE_BACKGROUND_STACK, BACKGROUND_SLOTS_TEMPLATE);
                drawBackground(STORAGE_BACKGROUND_STACK, BACKGROUND_SLOTS_INPUT);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_GREEDY_BLOCK.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                }
                return new int[0];
            }

        };
    }

}
