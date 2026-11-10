package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflage;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflageMenu;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

public class NetworkBridge extends NetworkObject {

    public NetworkBridge(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput, NodeType.BRIDGE);
        addItemHandler((BlockUseHandler) this::onRightClick);
    }

    private void onRightClick(@Nonnull PlayerRightClickEvent event) {
        final Optional<Block> blockOptional = event.getClickedBlock();
        if (blockOptional.isEmpty()) {
            return;
        }

        final Block block = blockOptional.get();
        final Player player = event.getPlayer();

        if (!Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK)) {
            return;
        }

        final ItemStack itemInHand = event.getItem();

        // Si el jugador hace clic derecho con un bloque solido apto para camuflaje:
        if (itemInHand != null && !itemInHand.getType().isAir() && CableCamouflage.allowed(itemInHand)) {
            event.cancel();
            ItemStack previous = CableCamouflage.apply(block, itemInHand, player);
            if (player.getGameMode() != GameMode.CREATIVE) {
                itemInHand.subtract(1);
            }
            if (previous != null) {
                var leftovers = player.getInventory().addItem(previous);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            player.sendMessage("§a[Networks] §7Cable camuflado con §b" + itemInHand.getType().name() + "§7.");
            return;
        }

        // Si tiene la mano vacia o esta agachado o interactua con cualquier otro item:
        event.cancel();
        new CableCamouflageMenu(block, player).open();
    }

    @Override
    protected void onBreak(@Nonnull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final Material camoMat = CableCamouflage.getCamouflage(location);

        if (camoMat != null) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                location.getWorld().dropItemNaturally(location, new ItemStack(camoMat, 1));
            }
            BlockStorage.addBlockInfo(location, CableCamouflage.KEY_CAMO, null);
        }

        super.onBreak(event);
    }
}
