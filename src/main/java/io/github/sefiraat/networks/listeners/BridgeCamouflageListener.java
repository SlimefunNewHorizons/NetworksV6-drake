package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.slimefun.network.NetworkBridge;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflage;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflageMenu;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * [ES] Escuchador de interacciones y eventos de GUI para el camuflaje de NetworkBridge.
 */
public class BridgeCamouflageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(@Nonnull InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CableCamouflageMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(@Nonnull InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CableCamouflageMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreativeClick(@Nonnull InventoryCreativeEvent event) {
        if (event.getInventory().getHolder() instanceof CableCamouflageMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockInteract(@Nonnull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Block block = event.getClickedBlock();
        final SlimefunItem sfItem = BlockStorage.check(block);

        if (sfItem instanceof NetworkBridge) {
            final Player player = event.getPlayer();
            if (!Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK)) {
                return;
            }

            final ItemStack inHand = event.getItem();
            if (inHand != null && !inHand.getType().isAir() && CableCamouflage.allowed(inHand)) {
                event.setCancelled(true);
                ItemStack previous = CableCamouflage.apply(block, inHand, player);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    inHand.subtract(1);
                }
                if (previous != null) {
                    var leftovers = player.getInventory().addItem(previous);
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                player.sendMessage("§a[Networks] §7Cable camuflado como §b" + inHand.getType().name() + "§7.");
            } else if (inHand == null || inHand.getType().isAir() || player.isSneaking()) {
                event.setCancelled(true);
                new CableCamouflageMenu(block, player).open();
            }
        }
    }
}
