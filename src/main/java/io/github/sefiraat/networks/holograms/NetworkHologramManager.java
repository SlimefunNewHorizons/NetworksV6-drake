package io.github.sefiraat.networks.holograms;

import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de hologramas flotantes nativos (TextDisplay de Paper/Minecraft 1.20+)
 * sobre NetworkController y NetworkQuantumStorage para mostrar telemetria en tiempo
 * real sin lag ni dependencias externas.
 */
public final class NetworkHologramManager {

    private static final Map<Location, UUID> CONTROLLER_HOLOGRAMS = new ConcurrentHashMap<>();
    private static final Map<Location, UUID> QUANTUM_HOLOGRAMS = new ConcurrentHashMap<>();

    public static final float QUANTUM_SCALE = 0.50f;
    public static final int QUANTUM_LINE_WIDTH = 120;
    public static final float QUANTUM_VIEW_RANGE = 0.20f;
    public static final double QUANTUM_Y_OFFSET = 1.15;
    public static final int QUANTUM_MAX_NAME_LENGTH = 16;

    private static final NamespacedKey HOLO_KEY = new NamespacedKey("networks", "controller_hologram");
    private static final NamespacedKey QUANTUM_HOLO_KEY = new NamespacedKey("networks", "quantum_hologram");

    private NetworkHologramManager() {}

    public static void updateHologram(@Nonnull Location controllerLoc, @Nonnull NetworkRoot root) {
        World world = controllerLoc.getWorld();
        if (world == null || !world.isChunkLoaded(controllerLoc.getBlockX() >> 4, controllerLoc.getBlockZ() >> 4)) {
            return;
        }

        UUID entityId = CONTROLLER_HOLOGRAMS.get(controllerLoc);
        TextDisplay textDisplay = null;

        if (entityId != null) {
            Entity entity = world.getEntity(entityId);
            if (entity instanceof TextDisplay td && entity.isValid()) {
                textDisplay = td;
            } else {
                CONTROLLER_HOLOGRAMS.remove(controllerLoc);
            }
        }

        if (textDisplay == null) {
            Location spawnLoc = controllerLoc.clone().add(0.5, 1.45, 0.5);
            // Limpieza preventiva de TextDisplays huerfanos en el mismo bloque
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
            CONTROLLER_HOLOGRAMS.put(controllerLoc, textDisplay.getUniqueId());
        }

        double tpsRate = root.getThroughput().getItemsPerSecond();
        long totalTransferred = root.getThroughput().getTotalTransferredItems();
        int nodeCount = root.getNodeCount();
        int maxNodes = root.getMaxNodes();

        String line1 = "§6§l✦ DRAKESCRAFT NETWORKS ✦";
        String line2 = String.format(Locale.ROOT, "§7Nodos: §f%d/%d §8| §7Flujo: §a+%.1f it/s", nodeCount, maxNodes, tpsRate);
        String line3 = String.format(Locale.ROOT, "§7Enrutado: §e%s §8| §b● Operativo", NumberFormat.getInstance().format(totalTransferred));

        textDisplay.text(Component.text(line1 + "\n" + line2 + "\n" + line3));

        // Particula visual sutil cuando el controlador tiene trafico activo
        if (tpsRate > 0) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, controllerLoc.clone().add(0.5, 0.8, 0.5), 2, 0.15, 0.15, 0.15, 0.02);
        }
    }

    public static void removeHologram(@Nonnull Location controllerLoc) {
        UUID entityId = CONTROLLER_HOLOGRAMS.remove(controllerLoc);
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

    public static void updateQuantumHologram(@Nonnull Location location, @Nullable ItemStack storedItem, long amount, long maxCapacity) {
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        UUID entityId = QUANTUM_HOLOGRAMS.get(location);
        TextDisplay textDisplay = null;

        if (entityId != null) {
            Entity entity = world.getEntity(entityId);
            if (entity instanceof TextDisplay td && entity.isValid()) {
                textDisplay = td;
            } else {
                QUANTUM_HOLOGRAMS.remove(location);
            }
        }

        if (textDisplay == null) {
            Location spawnLoc = location.clone().add(0.5, QUANTUM_Y_OFFSET, 0.5);
            for (Entity nearby : world.getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(QUANTUM_HOLO_KEY, PersistentDataType.BYTE)) {
                    nearby.remove();
                }
            }

            textDisplay = world.spawn(spawnLoc, TextDisplay.class, td -> {
                td.setBillboard(Display.Billboard.CENTER);
                td.setDefaultBackground(true);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setPersistent(false);
                configureQuantumDisplay(td);
                td.getPersistentDataContainer().set(QUANTUM_HOLO_KEY, PersistentDataType.BYTE, (byte) 1);
            });
            QUANTUM_HOLOGRAMS.put(location, textDisplay.getUniqueId());
        } else {
            configureQuantumDisplay(textDisplay);
        }

        textDisplay.text(Component.text(formatQuantumText(storedItem, amount, maxCapacity)));
    }

    static void configureQuantumDisplay(@Nonnull TextDisplay td) {
        Transformation current = td.getTransformation();
        td.setTransformation(new Transformation(
            current.getTranslation(),
            current.getLeftRotation(),
            new Vector3f(QUANTUM_SCALE, QUANTUM_SCALE, QUANTUM_SCALE),
            current.getRightRotation()
        ));
        td.setLineWidth(QUANTUM_LINE_WIDTH);
        td.setViewRange(QUANTUM_VIEW_RANGE);
        td.setAlignment(TextDisplay.TextAlignment.CENTER);
    }

    @Nonnull
    public static String formatQuantumText(@Nullable ItemStack storedItem, long amount, long maxCapacity) {
        if (storedItem == null) {
            return "§7Vacío\n§80/" + formatCompactNumber(maxCapacity);
        }

        String rawName;
        if (storedItem.getItemMeta() != null && storedItem.getItemMeta().hasDisplayName()) {
            rawName = storedItem.getItemMeta().getDisplayName();
        } else {
            rawName = StringUtils.toTitleCase(storedItem.getType().name().replace('_', ' '));
        }

        String compactName = truncateItemName(rawName, QUANTUM_MAX_NAME_LENGTH);
        return String.format(
            Locale.ROOT,
            "§b%s\n§e%s§8/§7%s",
            compactName,
            formatCompactNumber(amount),
            formatCompactNumber(maxCapacity)
        );
    }

    @Nonnull
    public static String truncateItemName(@Nonnull String rawName, int maxLength) {
        String plain = rawName.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "").trim();
        if (plain.isEmpty()) {
            return "Item";
        }
        if (plain.length() <= maxLength) {
            return plain;
        }
        if (maxLength <= 1) {
            return plain.substring(0, Math.max(0, maxLength));
        }
        return plain.substring(0, maxLength - 1).trim() + "…";
    }

    @Nonnull
    public static String formatCompactNumber(long value) {
        if (value < 0) {
            return "0";
        }
        if (value < 1_000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return formatUnit(value / 1_000.0, "K");
        }
        if (value < 1_000_000_000L) {
            return formatUnit(value / 1_000_000.0, "M");
        }
        return formatUnit(value / 1_000_000_000.0, "B");
    }

    private static String formatUnit(double scaled, @Nonnull String suffix) {
        double rounded = Math.round(scaled * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.ROOT, "%.0f%s", rounded, suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", rounded, suffix);
    }

    public static void removeQuantumHologram(@Nonnull Location location) {
        UUID entityId = QUANTUM_HOLOGRAMS.remove(location);
        if (entityId != null && location.getWorld() != null) {
            Entity entity = location.getWorld().getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        if (location.getWorld() != null && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            Location spawnLoc = location.clone().add(0.5, QUANTUM_Y_OFFSET, 0.5);
            for (Entity nearby : location.getWorld().getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(QUANTUM_HOLO_KEY, PersistentDataType.BYTE)) {
                    nearby.remove();
                }
            }
        }
    }

    public static void clearAll() {
        for (Map.Entry<Location, UUID> entry : CONTROLLER_HOLOGRAMS.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                Entity entity = loc.getWorld().getEntity(entry.getValue());
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        CONTROLLER_HOLOGRAMS.clear();

        for (Map.Entry<Location, UUID> entry : QUANTUM_HOLOGRAMS.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                Entity entity = loc.getWorld().getEntity(entry.getValue());
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        QUANTUM_HOLOGRAMS.clear();
    }
}
