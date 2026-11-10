package io.github.sefiraat.networks.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * [ES] Resuelve los fallos especificos que ocurren exclusivamente en modo Creativo:
 * 1. Romper bloques con Interaction/Display groups en creativo que dejaban bloques invisibles/fantasmas.
 * 2. Robo o duplicacion de paneles de fondo/iconos en interfaces graficas mediante el inventario creativo.
 */
public class CreativeModeGuardListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreativeEntityDamage(@Nonnull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        Entity target = event.getEntity();
        if (target.getType() == EntityType.INTERACTION || target instanceof Display) {
            Block block = target.getLocation().getBlock();
            SlimefunItem item = BlockStorage.check(block);

            if (item != null) {
                event.setCancelled(true);
                BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                org.bukkit.Bukkit.getPluginManager().callEvent(breakEvent);

                if (!breakEvent.isCancelled()) {
                    target.remove();
                    for (Entity nearby : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
                        if (nearby instanceof Display || nearby instanceof Interaction) {
                            nearby.remove();
                        }
                    }
                    block.setType(Material.AIR);
                    BlockStorage.clearBlockInfo(block);
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreativeInventoryClick(@Nonnull InventoryCreativeEvent event) {
        if (event.getInventory().getHolder() instanceof BlockMenu blockMenu) {
            SlimefunItem item = BlockStorage.check(blockMenu.getLocation());
            if (item != null && item.getId().startsWith("NTW_")) {
                int rawSlot = event.getRawSlot();
                if (rawSlot >= 0 && rawSlot < event.getInventory().getSize()) {
                    ItemStack clicked = event.getCurrentItem();
                    if (clicked != null && (clicked.getType().name().endsWith("_PANE") || clicked.getType() == Material.BARRIER)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
