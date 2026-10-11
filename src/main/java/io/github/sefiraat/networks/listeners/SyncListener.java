package io.github.sefiraat.networks.listeners;

import com.balugaq.netex.api.events.NetworksBlockBreakEvent;
import com.balugaq.netex.api.events.NetworksBlockPlaceEvent;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.utils.NetworkUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

/*
 * Fix https://github.com/Sefiraat/Networks/issues/188
 * Fix https://github.com/Sefiraat/Networks/issues/192
 * Fix https://github.com/ytdd9527/NetworksExpansion/issues/119
 */
/*
 * Estos listeners reciben cada bloque roto o colocado y cada chunk descargado de todo el servidor,
 * no solo los de Networks. Antes armaban el mensaje de debug con MessageFormat.format (que parsea el
 * patron en cada llamada) aunque el debug estuviera apagado. Ahora solo se arma si esta encendido, y
 * el Location del bloque se pide una vez en vez de tres.
 */
public class SyncListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        final Location location = e.getBlock().getLocation();
        if (Networks.getConfigManager().isDebug()) {
            Networks.getInstance().debug(MessageFormat.format("Listened BlockBreakEvent at {0}", location));
        }
        NetworkUtils.clearNetwork(location);
        SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(location);
        if (slimefunItem != null && slimefunItem.getAddon() instanceof Networks) {
            NetworksBlockBreakEvent event = new NetworksBlockBreakEvent(e.getBlock(), e.getPlayer());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent e) {
        final Location location = e.getBlock().getLocation();
        if (Networks.getConfigManager().isDebug()) {
            Networks.getInstance().debug(MessageFormat.format("Listened BlockPlaceEvent at {0}", location));
        }
        NetworkUtils.clearNetwork(location);
        SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(location);
        if (slimefunItem != null && slimefunItem.getAddon() instanceof Networks) {
            NetworksBlockPlaceEvent event = new NetworksBlockPlaceEvent(e.getBlock(), e.getPlayer());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkUnload(@NotNull ChunkUnloadEvent e) {
        if (Networks.getConfigManager().isDebug()) {
            Networks.getInstance()
                .debug(MessageFormat.format(
                    "Listened ChunkUnloadEvent at world: {0}, x: {1}, z: {2}",
                    e.getWorld().getName(),
                    e.getChunk().getX(),
                    e.getChunk().getZ()));
        }
        NetworkStorage.unregisterChunk(e.getChunk());
    }
}
