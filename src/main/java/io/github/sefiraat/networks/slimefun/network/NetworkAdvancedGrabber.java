package io.github.sefiraat.networks.slimefun.network;

import org.bukkit.Location;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
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

/** Directional bulk extractor with a bounded per-tick transfer budget. */
public final class NetworkAdvancedGrabber extends NetworkDirectional {

    private static final int TRANSFER_LIMIT = 3456;

    public NetworkAdvancedGrabber(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
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
        for (int slot : NetworkTransportUtils.getTransportSlots(target, ItemTransportFlow.WITHDRAW, null)) {
            ItemStack source = target.getItemInSlot(slot);
            if (source == null || source.getType() == Material.AIR) {
                continue;
            }
            moved += NetworkTransportUtils.pullIntoNetwork(definition.getNode().getRoot(), loc, target, slot);
            if (moved >= TRANSFER_LIMIT) {
                break;
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

    @Override
    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.FUCHSIA, 1);
    }

    @Override
    public boolean runSync() {
        return true;
    }
}
