package io.github.sefiraat.networks.slimefun.network;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.particles.XParticle;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockPlaceHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NetworkDirectional extends NetworkObject {

    private static final int NORTH_SLOT = 12;
    private static final int SOUTH_SLOT = 30;
    private static final int EAST_SLOT = 22;
    private static final int WEST_SLOT = 20;
    private static final int UP_SLOT = 15;
    private static final int DOWN_SLOT = 33;

    protected static final String DIRECTION = "direction";
    protected static final String OWNER_KEY = "uuid";

    private static final Set<BlockFace> VALID_FACES = EnumSet.of(
        BlockFace.UP,
        BlockFace.DOWN,
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST
    );

    private static final Map<Location, BlockFace> SELECTED_DIRECTION_MAP = new ConcurrentHashMap<>();

    private final ItemSetting<Integer> tickRate;

    protected NetworkDirectional(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, NodeType type) {
        super(itemGroup, item, recipeType, recipe, type);
        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);

        addItemHandler(
            // Sin esto la direccion elegida sobrevivia al bloque que la eligio.
            new com.github.drakescraft_labs.slimefun4.core.handlers.BlockBreakHandler(true, true) {
                @Override
                public void onPlayerBreak(@Nonnull org.bukkit.event.block.BlockBreakEvent event,
                                          @Nonnull org.bukkit.inventory.ItemStack item,
                                          @Nonnull java.util.List<org.bukkit.inventory.ItemStack> drops) {
                    forgetSelectedFace(event.getBlock().getLocation());
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                    NetworkStorage.removeNode(event.getBlock().getLocation());
                    // El BlockStorage se reinicia a SELF, pero getSelectedFace lee primero el mapa
                    // en memoria: sin esta limpieza un nodo nuevo hereda la direccion del que
                    // hubo antes en la misma ubicacion y apunta a donde el jugador no eligio.
                    forgetSelectedFace(event.getBlock().getLocation());
                    BlockStorage.addBlockInfo(event.getBlock(), OWNER_KEY, event.getPlayer().getUniqueId().toString());
                    BlockStorage.addBlockInfo(event.getBlock(), DIRECTION, BlockFace.SELF.name());
                    final BlockMenu blockMenu = BlockStorage.getInventory(event.getBlock());
                    if (blockMenu != null) {
                        NetworkUtils.applyConfig(NetworkDirectional.this, blockMenu, event.getPlayer());
                    }
                }
            },
            new BlockTicker() {

                private int tick = 1;

                @Override
                public boolean isSynchronized() {
                    return runSync();
                }

                @Override
                public void tick(Block block, SlimefunItem slimefunItem, Config config) {
                    if (tick <= 1) {
                        final BlockMenu blockMenu = BlockStorage.getInventory(block);
                        onTick(blockMenu, block);
                    }
                }

                @Override
                public void uniqueTick() {
                    tick = tick <= 1 ? tickRate.getValue() : tick - 1;
                    if (tick <= 1) {
                        onUniqueTick();
                    }
                }
            }
        );
    }

    private void updateGui(@Nullable BlockMenu blockMenu) {
        if (blockMenu == null || !blockMenu.hasViewer()) {
            return;
        }

        BlockFace direction = getCurrentDirection(blockMenu);

        final boolean isPusher = this.getNodeType() == NodeType.PUSHER;
        for (BlockFace blockFace : VALID_FACES) {
            final Block block = blockMenu.getBlock().getRelative(blockFace);
            final SlimefunItem slimefunItem = BlockStorage.check(block);
            if (slimefunItem != null) {
                switch (blockFace) {
                    case NORTH -> blockMenu.replaceExistingItem(getNorthSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    case SOUTH -> blockMenu.replaceExistingItem(getSouthSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    case EAST -> blockMenu.replaceExistingItem(getEastSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    case WEST -> blockMenu.replaceExistingItem(getWestSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    case UP -> blockMenu.replaceExistingItem(getUpSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    case DOWN -> blockMenu.replaceExistingItem(getDownSlot(), getDirectionalSlotPane(blockFace, slimefunItem, blockFace == direction, isPusher));
                    default -> throw new IllegalStateException("Unexpected value: " + blockFace);
                }
            } else {
                final Material material = block.getType();
                switch (blockFace) {
                    case NORTH -> blockMenu.replaceExistingItem(getNorthSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    case SOUTH -> blockMenu.replaceExistingItem(getSouthSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    case EAST -> blockMenu.replaceExistingItem(getEastSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    case WEST -> blockMenu.replaceExistingItem(getWestSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    case UP -> blockMenu.replaceExistingItem(getUpSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    case DOWN -> blockMenu.replaceExistingItem(getDownSlot(), getDirectionalSlotPane(blockFace, material, blockFace == direction));
                    default -> throw new IllegalStateException("Unexpected value: " + blockFace);
                }
            }
        }
    }

    @Nonnull
    protected BlockFace getCurrentDirection(@Nonnull BlockMenu blockMenu) {
        BlockFace direction = SELECTED_DIRECTION_MAP.get(blockMenu.getLocation());

        if (direction == null) {
            final String string = BlockStorage.getLocationInfo(blockMenu.getLocation(), DIRECTION);
            if (string == null) {
                direction = BlockFace.SELF;
                BlockStorage.addBlockInfo(blockMenu.getLocation(), DIRECTION, BlockFace.SELF.name());
            } else {
                try {
                    direction = BlockFace.valueOf(string);
                } catch (IllegalArgumentException e) {
                    direction = BlockFace.SELF;
                    BlockStorage.addBlockInfo(blockMenu.getLocation(), DIRECTION, BlockFace.SELF.name());
                }
            }
            SELECTED_DIRECTION_MAP.put(blockMenu.getLocation().clone(), direction);
        }
        return direction;
    }

    @OverridingMethodsMustInvokeSuper
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        final Location loc = blockMenu != null ? blockMenu.getLocation() : block.getLocation();
        if (!NetworkStorage.getAllNetworkObjects().containsKey(loc)) {
            addToRegistry(block);
        }
        updateGui(blockMenu);
    }

    protected void onUniqueTick() {}

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(getBackgroundSlots());

                if (getOtherBackgroundSlots() != null && getOtherBackgroundStack() != null) {
                    drawBackground(getOtherBackgroundStack(), getOtherBackgroundSlots());
                }

                addItem(getNorthSlot(), getDirectionalSlotPane(BlockFace.NORTH, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
                addItem(getSouthSlot(), getDirectionalSlotPane(BlockFace.SOUTH, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
                addItem(getEastSlot(), getDirectionalSlotPane(BlockFace.EAST, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
                addItem(getWestSlot(), getDirectionalSlotPane(BlockFace.WEST, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
                addItem(getUpSlot(), getDirectionalSlotPane(BlockFace.UP, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
                addItem(getDownSlot(), getDirectionalSlotPane(BlockFace.DOWN, Material.AIR, false), (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu blockMenu, @Nonnull Block b) {
                BlockFace direction;
                final String string = BlockStorage.getLocationInfo(blockMenu.getLocation(), DIRECTION);

                if (string == null) {
                    // This likely means a block was placed before I made it directional
                    direction = BlockFace.SELF;
                    BlockStorage.addBlockInfo(blockMenu.getLocation(), DIRECTION, BlockFace.SELF.name());
                } else {
                    try {
                        direction = BlockFace.valueOf(string);
                    } catch (IllegalArgumentException e) {
                        direction = BlockFace.SELF;
                        BlockStorage.addBlockInfo(blockMenu.getLocation(), DIRECTION, BlockFace.SELF.name());
                    }
                }
                SELECTED_DIRECTION_MAP.put(blockMenu.getLocation().clone(), direction);

                blockMenu.addMenuClickHandler(getNorthSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.NORTH));
                blockMenu.addMenuClickHandler(getSouthSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.SOUTH));
                blockMenu.addMenuClickHandler(getEastSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.EAST));
                blockMenu.addMenuClickHandler(getWestSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.WEST));
                blockMenu.addMenuClickHandler(getUpSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.UP));
                blockMenu.addMenuClickHandler(getDownSlot(), (player, i, itemStack, clickAction) ->
                    directionClick(player, clickAction, blockMenu, BlockFace.DOWN));
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return this.getSlimefunItem().canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }
        };
    }

    @ParametersAreNonnullByDefault
    public boolean directionClick(Player player, ClickAction action, BlockMenu blockMenu, BlockFace blockFace) {
        if (action.isShiftClicked()) {
            openDirection(player, blockMenu, blockFace);
        } else {
            setDirection(blockMenu, blockFace);
            if (this.getNodeType() == NodeType.PUSHER) {
                final Block target = blockMenu.getBlock().getRelative(blockFace);
                final SlimefunItem item = BlockStorage.check(target);
                if (item != null && item.getId().startsWith("NTW_QUANTUM_STORAGE")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[Networks] &eLos Quantum Storage se conectan directamente con un &bCable de Red &e(la red deposita automáticamente, sin Pusher). Si lo usas standalone fuera de la red, usa una &6Tolva vanilla &eapuntando al slot superior."));
                } else if (item != null && item.getId().startsWith("NTW_") && !io.github.sefiraat.networks.utils.NetworkTransportUtils.isExternalInventoryType(item.getId(), item.getClass())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[Networks] &eEste componente de red no admite Pushers. Los Pushers solo envían a máquinas externas o inventarios vanilla."));
                }
            }
        }
        return false;
    }

    @ParametersAreNonnullByDefault
    public void openDirection(Player player, BlockMenu blockMenu, BlockFace blockFace) {
        final BlockMenu targetMenu = BlockStorage.getInventory(blockMenu.getBlock().getRelative(blockFace));
        if (targetMenu != null) {
            final Location location = targetMenu.getLocation();
            final SlimefunItem item = BlockStorage.check(location);
            if (item != null
                && item.canUse(player, true)
                && Slimefun.getProtectionManager().hasPermission(player, blockMenu.getLocation(), Interaction.INTERACT_BLOCK)
            ) {
                targetMenu.open(player);
            }
        }
    }

    @ParametersAreNonnullByDefault
    public void setDirection(BlockMenu blockMenu, BlockFace blockFace) {
        SELECTED_DIRECTION_MAP.put(blockMenu.getLocation().clone(), blockFace);
        BlockStorage.addBlockInfo(blockMenu.getBlock(), DIRECTION, blockFace.name());
        clearIdleCooldown(blockMenu.getLocation());
    }

    @Nonnull
    protected int[] getBackgroundSlots() {
        return new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 16, 17, 18, 19, 21, 23, 24, 25, 26, 27, 28, 29, 21, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
        };
    }

    @Nullable
    protected int[] getOtherBackgroundSlots() {
        return null;
    }

    @Nullable
    protected ItemStack getOtherBackgroundStack() {
        return null;
    }

    public int getNorthSlot() {
        return NORTH_SLOT;
    }

    public int getSouthSlot() {
        return SOUTH_SLOT;
    }

    public int getEastSlot() {
        return EAST_SLOT;
    }

    public int getWestSlot() {
        return WEST_SLOT;
    }

    public int getUpSlot() {
        return UP_SLOT;
    }

    public int getDownSlot() {
        return DOWN_SLOT;
    }

    public int[] getItemSlots() {
        return new int[]{};
    }

    public int[] getInputSlots() { return new int[0]; }

    public int[] getOutputSlots() { return new int[0]; }

    @Nonnull
    public static ItemStack getDirectionalSlotPane(@Nonnull BlockFace blockFace, @Nonnull SlimefunItem slimefunItem, boolean active, boolean isPusher) {
        final ItemStack displayStack = ItemCreator.create(
            slimefunItem.getItem(),
            Theme.PASSIVE + "Direction " + blockFace.name() + " (" + ChatColor.stripColor(slimefunItem.getItemName()) + ")"
        );
        final ItemMeta itemMeta = displayStack.getItemMeta();
        if (active) {
            itemMeta.addEnchant(XEnchantment.LUCK_OF_THE_SEA.get(), 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        final List<String> lore = new java.util.ArrayList<>();
        lore.add(Theme.CLICK_INFO + "Left Click: " + Theme.PASSIVE + "Set Direction");
        lore.add(Theme.CLICK_INFO + "Shift Left Click: " + Theme.PASSIVE + "Open Target Block");
        if (isPusher && slimefunItem.getId().startsWith("NTW_QUANTUM_STORAGE")) {
            lore.add("");
            lore.add(ChatColor.RED + "⚠ No compatible con Pusher (Anti-Dupe)");
            lore.add(ChatColor.YELLOW + "💡 Conecta el Quantum Storage con Cable de Red");
            lore.add(ChatColor.GRAY + "  (o alimenta con Tolva vanilla si es standalone)");
        } else if (isPusher && slimefunItem.getId().startsWith("NTW_") && !io.github.sefiraat.networks.utils.NetworkTransportUtils.isExternalInventoryType(slimefunItem.getId(), slimefunItem.getClass())) {
            lore.add("");
            lore.add(ChatColor.RED + "⚠ No compatible con Pusher");
            lore.add(ChatColor.GRAY + "  (Pushers solo envían a máquinas externas o cofres)");
        }
        itemMeta.setLore(lore);
        displayStack.setItemMeta(itemMeta);
        return displayStack;
    }

    @Nonnull
    public static ItemStack getDirectionalSlotPane(@Nonnull BlockFace blockFace, @Nonnull SlimefunItem slimefunItem, boolean active) {
        return getDirectionalSlotPane(blockFace, slimefunItem, active, false);
    }

    @Nonnull
    public static ItemStack getDirectionalSlotPane(@Nonnull BlockFace blockFace, @Nonnull Material blockMaterial, boolean active) {
        if (blockMaterial.isItem() && !blockMaterial.isAir()) {
            final ItemStack displayStack = ItemCreator.create(
                blockMaterial,
                Theme.PASSIVE + "Direction " + blockFace.name() + " (" + blockMaterial.name() + ")"
            );
            final ItemMeta itemMeta = displayStack.getItemMeta();
            if (active) {
                itemMeta.addEnchant(XEnchantment.LUCK_OF_THE_SEA.get(), 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemMeta.setLore(List.of(
                Theme.CLICK_INFO + "Left Click: " + Theme.PASSIVE + "Set Direction",
                Theme.CLICK_INFO + "Shift Left Click: " + Theme.PASSIVE + "Open Target Block"
            ));
            displayStack.setItemMeta(itemMeta);
            return displayStack;
        } else {
            Material material = active ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            return ItemCreator.create(
                material,
                ChatColor.GRAY + "Set direction: " + blockFace.name()
            );
        }
    }

    /**
     * Olvida la direccion cacheada de una ubicacion.
     *
     * SELECTED_DIRECTION_MAP es estatico y tenia cinco put y ningun remove: crecia con cada nodo
     * direccional colocado en la historia del servidor y solo se vaciaba al reiniciar. Ademas
     * getSelectedFace lo consulta antes que a BlockStorage, asi que una entrada vieja se imponia
     * sobre la direccion real de un bloque nuevo.
     */
    private static final int DEFAULT_IDLE_BACKOFF_CYCLES = 3;
    private static final Map<Location, Integer> IDLE_COOLDOWNS = new ConcurrentHashMap<>();

    public static boolean isIdleOnCooldown(@Nonnull Location location) {
        return IDLE_COOLDOWNS.compute(location, (loc, remaining) ->
            remaining == null || remaining <= 1 ? null : remaining - 1
        ) != null;
    }

    public static void deferIdle(@Nonnull Location location) {
        IDLE_COOLDOWNS.put(location, DEFAULT_IDLE_BACKOFF_CYCLES);
    }

    public static void clearIdleCooldown(@Nonnull Location location) {
        IDLE_COOLDOWNS.remove(location);
    }

    @Override
    protected void clearCachedState(@Nonnull Location location) {
        super.clearCachedState(location);
        forgetSelectedFace(location);
    }

    public static void forgetSelectedFace(@Nonnull Location location) {
        SELECTED_DIRECTION_MAP.remove(location);
        clearIdleCooldown(location);
    }

    /** Solo para pruebas: tamano actual del cache de direcciones. */
    public static int selectedFaceCacheSize() {
        return SELECTED_DIRECTION_MAP.size();
    }

    @Nullable
    public static BlockFace getSelectedFace(@Nonnull Location location) {
        BlockFace face = SELECTED_DIRECTION_MAP.get(location);
        if (face == null) {
            final String string = BlockStorage.getLocationInfo(location, DIRECTION);
            if (string != null) {
                try {
                    face = BlockFace.valueOf(string);
                    SELECTED_DIRECTION_MAP.put(location.clone(), face);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return face;
    }

    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.RED, 1);
    }

    protected void showParticle(@Nonnull Location location, @Nonnull BlockFace blockFace) {
        final Vector faceVector = blockFace.getDirection().clone().multiply(-1);
        final Vector pushVector = faceVector.clone().multiply(2);
        final Location displayLocation = location.clone().add(0.5, 0.5, 0.5).add(faceVector);
        location.getWorld().spawnParticle(XParticle.DUST.get(), displayLocation, 0, pushVector.getX(), pushVector.getY(), pushVector.getZ(), getDustOptions());
    }
}
