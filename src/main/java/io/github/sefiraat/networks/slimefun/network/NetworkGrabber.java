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

public class NetworkGrabber extends NetworkDirectional {

    public NetworkGrabber(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
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

        final BlockFace direction = this.getCurrentDirection(blockMenu);
        final BlockMenu targetMenu = BlockStorage.getInventory(blockMenu.getBlock().getRelative(direction));

        if (!NetworkTransportUtils.isExternalInventory(targetMenu)) {
            return false;
        }

        int[] slots = NetworkTransportUtils.getTransportSlots(targetMenu, ItemTransportFlow.WITHDRAW, null);

        for (int slot : slots) {
            final ItemStack itemStack = targetMenu.getItemInSlot(slot);

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                final int consumed = NetworkTransportUtils.pullIntoNetwork(
                        definition.getNode().getRoot(),
                        blockMenu.getLocation(),
                        targetMenu,
                        slot);

                if (consumed > 0) {
                    blockMenu.markDirty();
                    if (definition.getNode().getRoot().isDisplayParticles()) {
                        showParticle(blockMenu.getLocation(), direction);
                    }
                    return true;
                }
            }
        }
        return false;
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
