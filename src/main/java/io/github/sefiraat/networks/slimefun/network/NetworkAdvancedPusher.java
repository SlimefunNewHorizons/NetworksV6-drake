package io.github.sefiraat.networks.slimefun.network;

import org.bukkit.Location;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.ItemCreator;
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

/** Directional bulk exporter with nine independent item templates. */
public final class NetworkAdvancedPusher extends NetworkDirectional {

    private static final int TRANSFER_LIMIT = 3456;
    private static final int[] TEMPLATE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] BACKGROUND_SLOTS = {
        9, 10, 12, 13, 15, 16, 17, 18, 20, 22, 23, 24, 25, 26, 27, 28, 30, 31, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44
    };
    private static final ItemStack BACKGROUND = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Request templates"
    );

    public NetworkAdvancedPusher(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.PUSHER);
        for (int slot : TEMPLATE_SLOTS) {
            getSlotsToDrop().add(slot);
        }
    }

    @Override
    protected void onTick(@Nullable BlockMenu menu, @Nonnull Block block) {
        super.onTick(menu, block);
        if (menu == null) {
            return;
        }
        final Location loc = menu.getLocation();
        if (isIdleOnCooldown(loc)) {
            return;
        }
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(loc);
        if (definition == null || definition.getNode() == null) {
            return;
        }
        BlockFace direction = getCurrentDirection(menu);
        BlockMenu target = BlockStorage.getInventory(block.getRelative(direction));
        if (!NetworkTransportUtils.isExternalInventory(target)) {
            deferIdle(loc);
            return;
        }

        int moved = 0;
        for (int templateSlot : TEMPLATE_SLOTS) {
            ItemStack template = menu.getItemInSlot(templateSlot);
            if (template == null || template.getType() == Material.AIR || moved >= TRANSFER_LIMIT) {
                continue;
            }
            ItemStack request = template.clone();
            request.setAmount(1);
            int capacity = insertionCapacity(target, request, TRANSFER_LIMIT - moved);
            if (capacity <= 0) {
                continue;
            }
            ItemStack withdrawn = definition.getNode().getRoot().getItemStack0(
                loc, new ItemRequest(request, capacity)
            );
            if (withdrawn == null) {
                continue;
            }
            int before = withdrawn.getAmount();
            boolean transferred = NetworkTransportUtils.pushIntoMenuOrReturn(
                definition.getNode().getRoot(), loc, target, withdrawn,
                NetworkTransportUtils.getTransportSlots(target, ItemTransportFlow.INSERT, request)
            );
            // Count the requested batch conservatively. A partial insertion consumes
            // the remaining budget instead of risking an uncontrolled high-throughput loop.
            if (transferred) {
                moved += before;
            }
        }
        if (moved > 0) {
            if (definition.getNode().getRoot().isDisplayParticles()) {
                target.markDirty();
                showParticle(loc, direction);
            }
        } else {
            deferIdle(loc);
        }
    }

    private int insertionCapacity(@Nonnull BlockMenu target, @Nonnull ItemStack template, int remainingBudget) {
        int capacity = 0;
        for (int slot : NetworkTransportUtils.getTransportSlots(target, ItemTransportFlow.INSERT, template)) {
            ItemStack existing = target.getItemInSlot(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                capacity += template.getMaxStackSize();
            } else if (StackUtils.itemsMatch(template, existing)) {
                capacity += Math.max(0, existing.getMaxStackSize() - existing.getAmount());
            }
            if (capacity >= remainingBudget) {
                return remainingBudget;
            }
        }
        return Math.min(capacity, remainingBudget);
    }

    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Override
    public int[] getInputSlots() {
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
