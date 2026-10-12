package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reconcilia automáticamente las redes de Networks en islas de SkyBlock y OneBlock
 * cuando un jugador llega a su base.
 *
 * En mundos de islas (bskyblock_world, oneblock_world), los chunks periféricos de una red
 * suelen cargarse un instante después que el bloque controlador. Al teletransportarse o entrar,
 * esta rutina espera 40 ticks (2 segundos) para que Purpur estabilice y despierte los chunks,
 * reindexa localmente los chunks en un radio de 3 chunks (7x7 chunks) alrededor del jugador,
 * y marca sucia la red si detecta nodos desligados para reincorporarlos automáticamente.
 */
public class IslandSyncListener implements Listener {

    private static final long STABILIZATION_DELAY_TICKS = 40L; // 2 segundos
    private static final int CHUNK_RADIUS = 3; // Radio de 3 chunks (7x7 chunks)
    private static final long COOLDOWN_MS = 8000L; // 8 segundos por jugador

    private final Map<UUID, Long> lastSync = new ConcurrentHashMap<>();

    private boolean isIslandWorld(@Nonnull World world) {
        final String name = world.getName().toLowerCase();
        return name.startsWith("bskyblock_") || name.startsWith("oneblock_");
    }

    private void scheduleIslandSync(@Nonnull Player player) {
        final World world = player.getWorld();
        if (!isIslandWorld(world)) {
            return;
        }

        final UUID uuid = player.getUniqueId();
        final long now = System.currentTimeMillis();
        final Long previous = lastSync.get(uuid);
        if (previous != null && (now - previous) < COOLDOWN_MS) {
            return;
        }
        lastSync.put(uuid, now);

        Bukkit.getScheduler().runTaskLater(Networks.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }

            final Location loc = player.getLocation();
            final World targetWorld = loc.getWorld();
            if (targetWorld == null || !isIslandWorld(targetWorld)) {
                return;
            }

            final int centerChunkX = loc.getBlockX() >> 4;
            final int centerChunkZ = loc.getBlockZ() >> 4;
            boolean needsRebuild = false;

            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    final int cx = centerChunkX + dx;
                    final int cz = centerChunkZ + dz;

                    if (targetWorld.isChunkLoaded(cx, cz)) {
                        final Chunk chunk = targetWorld.getChunkAt(cx, cz);
                        final int reindexed = SyncListener.indexChunk(chunk);
                        if (reindexed > 0 || SyncListener.chunkHasDetachedNodes(chunk)) {
                            needsRebuild = true;
                        }
                    }
                }
            }

            if (needsRebuild) {
                NetworkController.markNetworksDirtyInWorld(targetWorld);
            }
        }, STABILIZATION_DELAY_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(@Nonnull PlayerTeleportEvent event) {
        final Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }

        final Location from = event.getFrom();
        // Si el movimiento es muy pequeño dentro del mismo chunk, omitir
        if (from.getWorld() == to.getWorld() && from.distanceSquared(to) < 64.0) {
            return;
        }

        scheduleIslandSync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(@Nonnull PlayerChangedWorldEvent event) {
        scheduleIslandSync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@Nonnull PlayerJoinEvent event) {
        scheduleIslandSync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@Nonnull PlayerQuitEvent event) {
        lastSync.remove(event.getPlayer().getUniqueId());
    }
}
