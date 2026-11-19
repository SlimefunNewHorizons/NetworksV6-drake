package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.slimefun.tools.NetworkCloudLink;
import io.github.sefiraat.networks.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener para NetworkCloudLink:
 * 1. Anti-dupe: bloquea drop, swap y movimiento del Cloud-Link mientras su grid remoto este abierto.
 * 2. Auto-Vacuum al minar (BlockDropItemEvent): deposita drops atomicamente en la red vinculada.
 * 3. Vaciado rapido de inventario (Shift+Clic Izquierdo al aire): transfiere slots 0..35 protegiendo equipo.
 */
public class CloudLinkListener implements Listener {

    private static final Map<UUID, Location> OPEN_CLOUD_SESSIONS = new ConcurrentHashMap<>();

    public static void markCloudGridOpen(@Nonnull UUID playerId, @Nonnull Location gridLocation) {
        OPEN_CLOUD_SESSIONS.put(playerId, gridLocation);
    }

    public static void clearCloudGridOpen(@Nonnull UUID playerId) {
        OPEN_CLOUD_SESSIONS.remove(playerId);
    }

    public static boolean isCloudGridOpen(@Nonnull UUID playerId) {
        return OPEN_CLOUD_SESSIONS.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@Nonnull InventoryCloseEvent event) {
        OPEN_CLOUD_SESSIONS.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@Nonnull PlayerQuitEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        OPEN_CLOUD_SESSIONS.remove(playerId);
        NetworkCloudLink.clearNotifyCooldown(playerId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(@Nonnull PlayerDropItemEvent event) {
        if (!isCloudGridOpen(event.getPlayer().getUniqueId())) {
            return;
        }
        if (NetworkCloudLink.isCloudLink(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerSwapHands(@Nonnull PlayerSwapHandItemsEvent event) {
        if (!isCloudGridOpen(event.getPlayer().getUniqueId())) {
            return;
        }
        if (NetworkCloudLink.isCloudLink(event.getMainHandItem())
                || NetworkCloudLink.isCloudLink(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@Nonnull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isCloudGridOpen(player.getUniqueId())) {
            return;
        }
        if (NetworkCloudLink.isCloudLink(event.getCurrentItem())
                || NetworkCloudLink.isCloudLink(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY) {
            final int hotbar = event.getHotbarButton();
            if (hotbar >= 0 && hotbar < 9) {
                final ItemStack hotbarItem = player.getInventory().getItem(hotbar);
                if (NetworkCloudLink.isCloudLink(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND
                && NetworkCloudLink.isCloudLink(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@Nonnull InventoryDragEvent event) {
        if (!isCloudGridOpen(event.getWhoClicked().getUniqueId())) {
            return;
        }
        if (NetworkCloudLink.isCloudLink(event.getOldCursor())
                || NetworkCloudLink.isCloudLink(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDropItem(@Nonnull BlockDropItemEvent event) {
        final Player player = event.getPlayer();
        final ItemStack cloudLink = findActiveCloudLink(player.getInventory());
        if (cloudLink == null) {
            return;
        }

        final Location boundLocation = NetworkCloudLink.getBoundLocation(cloudLink);
        if (boundLocation == null || !NetworkCloudLink.isChunkLoaded(boundLocation)) {
            return;
        }
        if (!NetworkCloudLink.hasInteractPermission(player, boundLocation)) {
            return;
        }

        final NetworkRoot root = NetworkCloudLink.resolveRoot(boundLocation);
        if (root == null) {
            return;
        }

        boolean anyLeftover = false;
        final Iterator<Item> iterator = event.getItems().iterator();
        while (iterator.hasNext()) {
            final Item itemEntity = iterator.next();
            final ItemStack dropStack = itemEntity.getItemStack();
            if (dropStack.getType().isAir() || dropStack.getAmount() <= 0) {
                continue;
            }

            final NetworkCloudLink.DepositResult result = NetworkCloudLink.depositAtomic(
                    dropStack,
                    incoming -> root.addItemStack0(boundLocation, incoming)
            );

            if (result.remaining() <= 0) {
                itemEntity.remove();
                iterator.remove();
            } else {
                if (result.accepted() > 0) {
                    itemEntity.setItemStack(dropStack);
                }
                anyLeftover = true;
            }
        }

        if (anyLeftover && NetworkCloudLink.shouldNotifyFull(player.getUniqueId(), System.currentTimeMillis())) {
            player.sendActionBar(Component.text(
                    "Cloud-Link: Red llena, los items sobrantes quedaron en el suelo.",
                    NamedTextColor.RED
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuickDump(@Nonnull PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }
        final Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        final ItemStack item = event.getItem();
        if (!NetworkCloudLink.isCloudLink(item)) {
            return;
        }

        event.setCancelled(true);

        final Location boundLocation = NetworkCloudLink.getBoundLocation(item);
        if (boundLocation == null) {
            player.sendMessage(Theme.ERROR + "Cloud-Link no esta vinculado a ninguna red.");
            return;
        }
        if (!NetworkCloudLink.isChunkLoaded(boundLocation)) {
            player.sendMessage(Theme.ERROR + "El chunk de la red vinculada no esta cargado.");
            return;
        }
        if (!NetworkCloudLink.hasInteractPermission(player, boundLocation)) {
            player.sendMessage(Theme.ERROR + "No tienes permiso para depositar en esta red.");
            return;
        }

        final NetworkRoot root = NetworkCloudLink.resolveRoot(boundLocation);
        if (root == null) {
            player.sendMessage(Theme.ERROR + "La red vinculada no esta activa.");
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] storage = inventory.getStorageContents();
        final NetworkCloudLink.DumpSummary summary = NetworkCloudLink.dumpInventory(
                storage,
                NetworkCloudLink::isProtectedFromDump,
                incoming -> root.addItemStack0(boundLocation, incoming)
        );

        if (summary.itemsDeposited() > 0) {
            inventory.setStorageContents(storage);
            player.sendActionBar(Component.text(
                    "Cloud-Link: " + summary.itemsDeposited() + " items transferidos a la red.",
                    NamedTextColor.GREEN
            ));
        } else if (summary.stacksRemaining() == 0) {
            player.sendActionBar(Component.text(
                    "Cloud-Link: No habia items transferibles en el inventario.",
                    NamedTextColor.YELLOW
            ));
        }

        if (summary.stacksRemaining() > 0
                && NetworkCloudLink.shouldNotifyFull(player.getUniqueId(), System.currentTimeMillis())) {
            player.sendActionBar(Component.text(
                    "Cloud-Link: Red llena, " + summary.stacksRemaining() + " stacks quedaron en tu inventario.",
                    NamedTextColor.RED
            ));
        }
    }

    @Nullable
    private static ItemStack findActiveCloudLink(@Nonnull PlayerInventory inventory) {
        final ItemStack[] storage = inventory.getStorageContents();
        final int limit = Math.min(NetworkCloudLink.STORAGE_SLOTS, storage.length);
        for (int i = 0; i < limit; i++) {
            final ItemStack candidate = storage[i];
            if (NetworkCloudLink.isCloudLink(candidate)
                    && NetworkCloudLink.isAutoVacuumEnabled(candidate)
                    && NetworkCloudLink.getBoundLocation(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }
}
