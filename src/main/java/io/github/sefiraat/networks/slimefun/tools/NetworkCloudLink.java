package io.github.sefiraat.networks.slimefun.tools;

import dev.sefiraat.sefilib.persistence.PersistenceTypes;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.listeners.CloudLinkListener;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.slimefun.network.grid.AbstractGrid;
import io.github.sefiraat.networks.slimefun.network.grid.NetworkGrid;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Herramienta avanzada tipo Nube/Drive para Networks:
 * - Shift+Clic Derecho sobre un NetworkGrid, bloque de red o cofre conectado: vincula la red.
 * - Shift+Clic Derecho al aire: activa/desactiva el modo Auto-Vacuum al minar.
 * - Clic Derecho al aire: abre remotamente el NetworkGrid vinculado (con proteccion anti-dupe).
 * - Shift+Clic Izquierdo al aire: vaciado rapido del inventario (0..35) protegiendo armadura y herramientas.
 */
public class NetworkCloudLink extends SlimefunItem {

    public static final int STORAGE_SLOTS = 36;
    public static final long FULL_NOTIFY_COOLDOWN_MS = 5_000L;

    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private static final Map<UUID, Long> LAST_FULL_NOTIFY = new ConcurrentHashMap<>();

    public record DepositResult(int accepted, int remaining) {
    }

    public record DumpSummary(int itemsDeposited, int stacksRemaining) {
    }

    public NetworkCloudLink(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe
    ) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler((ItemUseHandler) this::onRightClick);
    }

    private void onRightClick(@Nonnull PlayerRightClickEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (player.isSneaking()) {
            final Optional<Block> clickedBlock = event.getClickedBlock();
            if (clickedBlock.isPresent()) {
                bindBlock(item, clickedBlock.get(), player);
            } else {
                toggleAutoVacuum(item, player);
            }
        } else {
            tryOpenGrid(item, player);
        }
        event.cancel();
    }

    public static void bindBlock(@Nonnull ItemStack itemStack, @Nonnull Block block, @Nonnull Player player) {
        if (!hasInteractPermission(player, block)) {
            player.sendMessage(Theme.ERROR + "No tienes permiso para vincular este bloque.");
            return;
        }

        final Location targetLocation = resolveBindableNetworkLocation(block);
        if (targetLocation == null) {
            player.sendMessage(Theme.ERROR + "Debes apuntar a un Network Grid, bloque de red o cofre conectado.");
            return;
        }

        if (!targetLocation.equals(block.getLocation()) && !hasInteractPermission(player, targetLocation)) {
            player.sendMessage(Theme.ERROR + "No tienes permiso sobre el nodo de red conectado.");
            return;
        }

        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        DataTypeMethods.setCustom(meta, Keys.CLOUD_LINK_LOCATION, PersistenceTypes.LOCATION, targetLocation);
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(Keys.CLOUD_LINK_VACUUM, PersistentDataType.BYTE)) {
            pdc.set(Keys.CLOUD_LINK_VACUUM, PersistentDataType.BYTE, (byte) 1);
        }
        itemStack.setItemMeta(meta);
        player.sendMessage(Theme.SUCCESS + "Cloud-Link vinculado a la red en "
                + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ() + ".");
    }

    @Nullable
    public static Location resolveBindableNetworkLocation(@Nonnull Block block) {
        final Location loc = block.getLocation();
        final SlimefunItem sfItem = BlockStorage.check(block);

        if (sfItem instanceof NetworkGrid || sfItem instanceof NetworkObject) {
            final NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(loc);
            if (def != null && def.getNode() != null) {
                return loc;
            }
            if (sfItem instanceof NetworkGrid) {
                return loc;
            }
        }

        // Cofre/contenedor conectado a un nodo de red adyacente
        if (block.getState() instanceof Container || sfItem != null) {
            for (BlockFace face : ADJACENT_FACES) {
                final Block relative = block.getRelative(face);
                final Location relLoc = relative.getLocation();
                final NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(relLoc);
                if (def != null && def.getNode() != null && def.getNode().getRoot() != null) {
                    return relLoc;
                }
            }
        }

        return null;
    }

    public static void toggleAutoVacuum(@Nonnull ItemStack itemStack, @Nonnull Player player) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        final boolean next = !isAutoVacuumEnabled(itemStack);
        meta.getPersistentDataContainer().set(Keys.CLOUD_LINK_VACUUM, PersistentDataType.BYTE, (byte) (next ? 1 : 0));
        itemStack.setItemMeta(meta);
        player.sendMessage((next ? Theme.SUCCESS : Theme.WARNING)
                + "Cloud-Link Auto-Vacuum: " + (next ? "ACTIVADO" : "DESACTIVADO") + ".");
    }

    public static boolean isAutoVacuumEnabled(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        final Byte val = meta.getPersistentDataContainer().get(Keys.CLOUD_LINK_VACUUM, PersistentDataType.BYTE);
        return val == null || val == 1;
    }

    @Nullable
    public static Location getBoundLocation(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return DataTypeMethods.getCustom(meta, Keys.CLOUD_LINK_LOCATION, PersistenceTypes.LOCATION);
    }

    public static boolean isChunkLoaded(@Nullable Location location) {
        if (location == null) {
            return false;
        }
        final World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    @Nullable
    public static NetworkRoot resolveRoot(@Nullable Location boundLocation) {
        if (!isChunkLoaded(boundLocation)) {
            return null;
        }
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(boundLocation);
        if (definition == null || definition.getNode() == null) {
            return null;
        }
        return definition.getNode().getRoot();
    }

    public static void tryOpenGrid(@Nonnull ItemStack itemStack, @Nonnull Player player) {
        final Location boundLocation = getBoundLocation(itemStack);
        if (boundLocation == null) {
            player.sendMessage(Theme.ERROR + "Cloud-Link no esta vinculado a ninguna red.");
            return;
        }
        if (!isChunkLoaded(boundLocation)) {
            player.sendMessage(Theme.ERROR + "El chunk de la red vinculada no esta cargado.");
            return;
        }
        if (!hasInteractPermission(player, boundLocation)) {
            player.sendMessage(Theme.ERROR + "No tienes permiso para acceder a esta red.");
            return;
        }

        final Location gridLocation = resolveGridLocation(boundLocation, player);
        if (gridLocation == null) {
            player.sendMessage(Theme.ERROR + "No se encontro un Network Grid cargado en la red vinculada.");
            return;
        }

        final BlockMenu menu = BlockStorage.getInventory(gridLocation);
        if (menu == null) {
            player.sendMessage(Theme.ERROR + "El Network Grid vinculado ya no esta disponible.");
            return;
        }

        CloudLinkListener.markCloudGridOpen(player.getUniqueId(), gridLocation);
        menu.open(player);
    }

    @Nullable
    private static Location resolveGridLocation(@Nonnull Location boundLocation, @Nonnull Player player) {
        final SlimefunItem directItem = BlockStorage.check(boundLocation);
        if (directItem instanceof AbstractGrid && BlockStorage.getInventory(boundLocation) != null) {
            return boundLocation;
        }

        final NetworkRoot root = resolveRoot(boundLocation);
        if (root == null) {
            return null;
        }

        for (Location gridLoc : root.getGrids()) {
            if (!isChunkLoaded(gridLoc)) {
                continue;
            }
            if (!hasInteractPermission(player, gridLoc)) {
                continue;
            }
            final SlimefunItem gridItem = BlockStorage.check(gridLoc);
            if (gridItem instanceof AbstractGrid && BlockStorage.getInventory(gridLoc) != null) {
                return gridLoc;
            }
        }
        return null;
    }

    public static boolean hasInteractPermission(@Nonnull Player player, @Nonnull Block block) {
        return Slimefun.getProtectionManager().hasPermission(
                Bukkit.getOfflinePlayer(player.getUniqueId()),
                block,
                Interaction.INTERACT_BLOCK
        );
    }

    public static boolean hasInteractPermission(@Nonnull Player player, @Nonnull Location location) {
        return Slimefun.getProtectionManager().hasPermission(
                Bukkit.getOfflinePlayer(player.getUniqueId()),
                location,
                Interaction.INTERACT_BLOCK
        );
    }

    public static boolean isCloudLink(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        final SlimefunItem sfItem = SlimefunItem.getByItem(itemStack);
        if (sfItem instanceof NetworkCloudLink) {
            return true;
        }
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(Keys.CLOUD_LINK_LOCATION, PersistenceTypes.LOCATION)
                || pdc.has(Keys.CLOUD_LINK_VACUUM, PersistentDataType.BYTE);
    }

    /**
     * Determina si un ItemStack esta protegido contra el vaciado rapido de inventario.
     * Excluye estrictamente el propio Cloud-Link, armadura, herramientas, armas, arcos, escudos y equipo.
     */
    public static boolean isProtectedFromDump(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return true;
        }
        if (isCloudLink(itemStack)) {
            return true;
        }
        return isProtectedMaterial(itemStack.getType());
    }

    public static boolean isProtectedMaterial(@Nonnull Material material) {
        if (material.isAir()) {
            return true;
        }
        final String name = material.name();
        if (name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.endsWith("_SWORD")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_SPEAR")) {
            return true;
        }
        return switch (material) {
            case BOW, CROSSBOW, TRIDENT, MACE, SHIELD, ELYTRA,
                 SHEARS, FLINT_AND_STEEL, FISHING_ROD, BRUSH, SPYGLASS,
                 COMPASS, RECOVERY_COMPASS, CLOCK, TOTEM_OF_UNDYING,
                 CARROT_ON_A_STICK, WARPED_FUNGUS_ON_A_STICK, LEAD, BUNDLE -> true;
            default -> false;
        };
    }

    /**
     * Calcula la cantidad realmente aceptada por la red de forma segura contra valores anomalos.
     */
    public static int calculateAcceptedAmount(int initialAmount, int remainingAmount) {
        if (initialAmount <= 0) {
            return 0;
        }
        final int clampedRemaining = Math.max(0, Math.min(initialAmount, remainingAmount));
        return initialAmount - clampedRemaining;
    }

    /**
     * Deposita un stack de forma atomica: clona antes de insertar en la red y solo reduce el stack
     * original por la cantidad realmente aceptada. Si la red no acepta nada, el stack queda intacto.
     */
    @Nonnull
    public static DepositResult depositAtomic(
            @Nullable ItemStack source,
            @Nonnull Consumer<ItemStack> networkInserter
    ) {
        if (source == null || source.getType().isAir() || source.getAmount() <= 0) {
            return new DepositResult(0, 0);
        }
        final int initialAmount = source.getAmount();
        final ItemStack probe = source.clone();
        networkInserter.accept(probe);

        final int accepted = calculateAcceptedAmount(initialAmount, probe.getAmount());
        final int remaining = initialAmount - accepted;

        if (accepted > 0) {
            source.setAmount(remaining);
        }
        return new DepositResult(accepted, remaining);
    }

    /**
     * Vacia unicamente los slots de almacenamiento 0..35, protegiendo estrictamente armadura (36..39),
     * offhand (40), el propio Cloud-Link y herramientas/armas/escudos/equipo.
     */
    @Nonnull
    public static DumpSummary dumpInventory(
            @Nonnull ItemStack[] storageContents,
            @Nonnull Predicate<ItemStack> isProtected,
            @Nonnull Consumer<ItemStack> networkInserter
    ) {
        final int limit = Math.min(STORAGE_SLOTS, storageContents.length);
        int deposited = 0;
        int stacksRemaining = 0;

        for (int slot = 0; slot < limit; slot++) {
            final ItemStack stack = storageContents[slot];
            if (stack == null || stack.getType().isAir() || isProtected.test(stack)) {
                continue;
            }
            final DepositResult result = depositAtomic(stack, networkInserter);
            deposited += result.accepted();
            if (result.remaining() <= 0) {
                storageContents[slot] = null;
            } else {
                stacksRemaining++;
            }
        }
        return new DumpSummary(deposited, stacksRemaining);
    }

    public static boolean shouldNotifyFull(@Nonnull UUID playerId, long nowMs) {
        return shouldNotifyFull(playerId, nowMs, FULL_NOTIFY_COOLDOWN_MS);
    }

    public static boolean shouldNotifyFull(@Nonnull UUID playerId, long nowMs, long cooldownMs) {
        final Long last = LAST_FULL_NOTIFY.get(playerId);
        if (last != null && (nowMs - last) < cooldownMs) {
            return false;
        }
        LAST_FULL_NOTIFY.put(playerId, nowMs);
        return true;
    }

    public static void clearNotifyCooldown(@Nonnull UUID playerId) {
        LAST_FULL_NOTIFY.remove(playerId);
    }
}
