package io.github.sefiraat.networks.holograms;

import io.github.sefiraat.networks.network.NetworkRoot;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de hologramas flotantes nativos (TextDisplay de Paper/Minecraft 1.20+)
 * sobre el NetworkController para mostrar telemetría en tiempo real sin lag ni dependencias externas.
 */
public final class NetworkHologramManager {

    private static final Map<Location, UUID> HOLOGRAM_ENTITIES = new ConcurrentHashMap<>();
    private static final NamespacedKey HOLO_KEY = new NamespacedKey("networks", "controller_hologram");

    private NetworkHologramManager() {}

    public static void updateHologram(@Nonnull Location controllerLoc, @Nonnull NetworkRoot root) {
        World world = controllerLoc.getWorld();
        if (world == null || !world.isChunkLoaded(controllerLoc.getBlockX() >> 4, controllerLoc.getBlockZ() >> 4)) {
            return;
        }

        UUID entityId = HOLOGRAM_ENTITIES.get(controllerLoc);
        TextDisplay textDisplay = null;

        if (entityId != null) {
            Entity entity = world.getEntity(entityId);
            if (entity instanceof TextDisplay td && entity.isValid()) {
                textDisplay = td;
            } else {
                HOLOGRAM_ENTITIES.remove(controllerLoc);
            }
        }

        if (textDisplay == null) {
            Location spawnLoc = controllerLoc.clone().add(0.5, 1.45, 0.5);
            // Limpieza preventiva de TextDisplays huérfanos en el mismo bloque
            for (Entity nearby : world.getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(HOLO_KEY, PersistentDataType.BYTE)) {
                    nearby.remove();
                }
            }

            textDisplay = world.spawn(spawnLoc, TextDisplay.class, td -> {
                td.setBillboard(Display.Billboard.CENTER);
                td.setDefaultBackground(true);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setPersistent(false);
                td.getPersistentDataContainer().set(HOLO_KEY, PersistentDataType.BYTE, (byte) 1);
            });
            HOLOGRAM_ENTITIES.put(controllerLoc, textDisplay.getUniqueId());
        }

        double tpsRate = root.getThroughput().getItemsPerSecond();
        long totalTransferred = root.getThroughput().getTotalTransferredItems();
        int nodeCount = root.getNodeCount();
        int maxNodes = root.getMaxNodes();

        String line1 = "§6§l✦ DRAKESCRAFT NETWORKS ✦";
        String line2 = String.format(Locale.ROOT, "§7Nodos: §f%d/%d §8| §7Flujo: §a+%.1f items/s", nodeCount, maxNodes, tpsRate);
        String line3 = String.format(Locale.ROOT, "§7Enrutado: §e%s §8| §b● Operativo", NumberFormat.getInstance().format(totalTransferred));

        textDisplay.text(Component.text(line1 + "\n" + line2 + "\n" + line3));
    }

    public static void removeHologram(@Nonnull Location controllerLoc) {
        UUID entityId = HOLOGRAM_ENTITIES.remove(controllerLoc);
        if (entityId != null && controllerLoc.getWorld() != null) {
            Entity entity = controllerLoc.getWorld().getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        if (controllerLoc.getWorld() != null && controllerLoc.getWorld().isChunkLoaded(controllerLoc.getBlockX() >> 4, controllerLoc.getBlockZ() >> 4)) {
            Location spawnLoc = controllerLoc.clone().add(0.5, 1.45, 0.5);
            for (Entity nearby : controllerLoc.getWorld().getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(HOLO_KEY, PersistentDataType.BYTE)) {
                    nearby.remove();
                }
            }
        }
    }

    public static void clearAll() {
        for (Map.Entry<Location, UUID> entry : HOLOGRAM_ENTITIES.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                Entity entity = loc.getWorld().getEntity(entry.getValue());
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        HOLOGRAM_ENTITIES.clear();
    }
}
