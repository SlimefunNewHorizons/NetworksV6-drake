package io.github.sefiraat.networks.slimefun.network.camouflage;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * [EN] Cable Camouflage for Networks V6 (parity with MultiverseNets).
 * Allows Network Bridges (cables) to adopt the appearance of any solid, occluding block
 * (quartz, stone bricks, deepslate, planks, terracotta, etc.), seamlessly blending
 * cables into player builds while preserving 100% network connectivity.
 *
 * [ES] Camuflaje de cables para Networks V6 (paridad con MultiverseNets).
 * Permite que los Network Bridges (cables) adopten el aspecto de cualquier bloque solido
 * completo (cuarzo, ladrillos de piedra, deepslate, madera, hormigon, etc.), integrando los
 * cables dentro de las construcciones de los jugadores manteniendo conectividad total.
 */
public final class CableCamouflage {

    public static final String KEY_CAMO = "camo_material";
    public static final Material DEFAULT_LOOK = Material.WHITE_STAINED_GLASS;

    private CableCamouflage() {
    }

    /**
     * Comprueba si el material es apto para camuflar un cable.
     * Solo bloques solidos, completos, no interactuables y sin gravedad ni NBT cambiante.
     */
    public static boolean allowed(@Nullable Material material) {
        if (material == null || !material.isBlock() || material.isAir() || !material.isSolid()
                || !material.isOccluding() || material.hasGravity() || material.isInteractable()) {
            return false;
        }

        String name = material.name();
        if (name.contains("COPPER") && !name.startsWith("WAXED_") && !name.startsWith("RAW_") && !name.contains("ORE")) {
            return false;
        }
        if (name.endsWith("CORAL_BLOCK") && !name.startsWith("DEAD_")) {
            return false;
        }

        return switch (material) {
            case GRASS_BLOCK, MYCELIUM, TNT, SPAWNER, BEEHIVE, BEE_NEST, SCULK_CATALYST, SCULK_SHRIEKER,
                 BUDDING_AMETHYST, REINFORCED_DEEPSLATE, BEDROCK, BARRIER, COMMAND_BLOCK,
                 CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, STRUCTURE_BLOCK, JIGSAW,
                 INFESTED_STONE, INFESTED_COBBLESTONE, INFESTED_STONE_BRICKS, INFESTED_MOSSY_STONE_BRICKS,
                 INFESTED_CRACKED_STONE_BRICKS, INFESTED_CHISELED_STONE_BRICKS, INFESTED_DEEPSLATE -> false;
            default -> true;
        };
    }

    public static boolean allowed(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        // No permitir camuflar con items especiales de Slimefun
        if (SlimefunItem.getByItem(item) != null) {
            return false;
        }
        return allowed(item.getType());
    }

    @Nullable
    public static Material getCamouflage(@Nonnull Location location) {
        String name = BlockStorage.getLocationInfo(location, KEY_CAMO);
        if (name != null) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    /**
     * Aplica el camuflaje al bloque de puente.
     * Devuelve el bloque previo que estuviera aplicado (para devolverlo al jugador), o null si era cristal normal.
     */
    @Nullable
    public static ItemStack apply(@Nonnull Block block, @Nonnull ItemStack item, @Nullable Player player) {
        Location loc = block.getLocation();
        Material prevMat = getCamouflage(loc);
        ItemStack previous = prevMat != null ? new ItemStack(prevMat, 1) : null;

        Material newMat = item.getType();
        BlockStorage.addBlockInfo(loc, KEY_CAMO, newMat.name());
        block.setType(newMat, false);

        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 0.5, 0.5), 10, 0.25, 0.25, 0.25,
                new Particle.DustOptions(Color.fromRGB(64, 200, 255), 1.0f));
            loc.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 0.8f, 1.2f);
        }

        return previous;
    }

    /**
     * Retira el camuflaje del puente y devuelve el bloque que estaba colocado.
     */
    @Nullable
    public static ItemStack remove(@Nonnull Block block, @Nullable Player player) {
        Location loc = block.getLocation();
        Material prevMat = getCamouflage(loc);
        if (prevMat == null) {
            return null;
        }

        BlockStorage.addBlockInfo(loc, KEY_CAMO, null);
        block.setType(DEFAULT_LOOK, false);

        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 15, 0.2, 0.2, 0.2,
                prevMat.createBlockData());
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.4f);
        }

        return new ItemStack(prevMat, 1);
    }
}
