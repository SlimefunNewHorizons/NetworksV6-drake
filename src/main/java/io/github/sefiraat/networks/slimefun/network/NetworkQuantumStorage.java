package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.*;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.attributes.DistinctiveItem;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockBreakHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockPlaceHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkQuantumStorage extends SlimefunItem implements DistinctiveItem {

    private static final int[] SIZES = new int[]{
        4096,
        32768,
        262144,
        2097152,
        16777216,
        134217728,
        1073741824,
        Integer.MAX_VALUE
    };

    public static final String BS_AMOUNT = "stored_amount";
    public static final String BS_VOID = "void_excess";

    public static final int INPUT_SLOT = 1;
    public static final int ITEM_SLOT = 4;
    public static final int ITEM_SET_SLOT = 13;
    public static final int OUTPUT_SLOT = 7;

    private static final ItemStack BACK_INPUT = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE,
        Theme.PASSIVE + "Input"
    );

    private static final ItemStack BACK_ITEM = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE,
        Theme.PASSIVE + "Item Stored"
    );

    private static final ItemStack NO_ITEM = ItemCreator.create(
        Material.RED_STAINED_GLASS_PANE,
        Theme.ERROR + "No Registered Item",
        Theme.PASSIVE + "Click the icon below while",
        Theme.PASSIVE + "holding an item to register it."
    );

    private static final ItemStack SET_ITEM = ItemCreator.create(
        Material.LIME_STAINED_GLASS_PANE,
        Theme.SUCCESS + "Set Item",
        Theme.PASSIVE + "Drag an item on top of this pane to register it.",
        Theme.PASSIVE + "Shift Click to change voiding"
    );

    private static final ItemStack BACK_OUTPUT = ItemCreator.create(
        Material.ORANGE_STAINED_GLASS_PANE,
        Theme.PASSIVE + "Output"
    );

    private static final int[] INPUT_SLOTS = new int[]{0, 2};
    private static final int[] ITEM_SLOTS = new int[]{3, 5};
    private static final int[] OUTPUT_SLOTS = new int[]{6, 8};
    private static final int[] BACKGROUND_SLOTS = new int[]{9, 10, 11, 12, 14, 15, 16, 17};

    private static final Map<Location, QuantumCache> CACHES = new ConcurrentHashMap<>();

    static {
        final ItemMeta itemMeta = NO_ITEM.getItemMeta();
        PersistentDataAPI.setBoolean(itemMeta, Keys.newKey("display"), true);
        NO_ITEM.setItemMeta(itemMeta);
    }

    private final List<Integer> slotsToDrop = new ArrayList<>();
    private final int maxAmount;

    public NetworkQuantumStorage(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int maxAmount) {
        super(itemGroup, item, recipeType, recipe);
        this.maxAmount = maxAmount;
        slotsToDrop.add(INPUT_SLOT);
        slotsToDrop.add(OUTPUT_SLOT);
    }

    @Override
    public void preRegister() {
        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block b, SlimefunItem item, Config data) {
                    onTick(b);
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                    onBreak(event);
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                    onPlace(event);
                }
            }
        );
    }

    private void onTick(Block block) {
        final BlockMenu blockMenu = BlockStorage.getInventory(block);

        if (blockMenu == null) {
            CACHES.remove(block.getLocation());
            return;
        }

        QuantumCache cache = CACHES.get(blockMenu.getLocation());
        if (cache == null) {
            cache = addCache(blockMenu);
        }

        // addCache devuelve null cuando el BlockMenu aún no cargó su .sfi pero BS_AMOUNT>0.
        // Saltamos este tick para no sobreescribir el contenido persistido con un estado vacío.
        if (cache == null) {
            return;
        }

        final ItemStack display = blockMenu.getItemInSlot(ITEM_SLOT);
        if (display == null || display.getType() == Material.AIR) {
            updateDisplayItem(blockMenu, cache);
            blockMenu.markDirty();
        } else if (blockMenu.hasViewer()) {
            updateDisplayItem(blockMenu, cache);
        }

        // Standalone hopper support: pull matching items from a vanilla hopper above or pointing into the storage
        pullFromAdjacentHoppers(block, cache, blockMenu);

        // Move items from the input slot into the card
        final ItemStack input = blockMenu.getItemInSlot(INPUT_SLOT);
        if (input != null && input.getType() != Material.AIR) {
            tryInputItem(blockMenu.getLocation(), new ItemStack[]{input}, cache);
        }

        // Output items
        final ItemStack output = blockMenu.getItemInSlot(OUTPUT_SLOT);
        ItemStack fetched = null;
        if (output == null || output.getType() == Material.AIR) {
            // No item in output, try output
            fetched = cache.withdrawItem();
        } else if (StackUtils.itemsMatch(cache, output, true) && output.getAmount() < output.getMaxStackSize()) {
            // There is an item, but it's not filled so lets top it up if we can
            final int requestAmount = output.getMaxStackSize() - output.getAmount();
            fetched = cache.withdrawItem(requestAmount);
        }

        if (fetched != null && fetched.getType() != Material.AIR) {
            final ItemStack leftover = NetworkTransportUtils.pushIntoMenu(blockMenu, fetched, OUTPUT_SLOT);
            if (leftover != null && leftover.getAmount() > 0) {
                cache.increaseAmount(leftover.getAmount());
            }
            syncBlock(blockMenu.getLocation(), cache);
            blockMenu.markDirty();
        }

        CACHES.put(blockMenu.getLocation(), cache);
        blockMenu.markDirty();
    }

    private void pullFromAdjacentHoppers(@Nonnull Block block, @Nonnull QuantumCache cache, @Nonnull BlockMenu blockMenu) {
        if (cache.getItemStack() == null) {
            return;
        }
        // Check block directly above (standard standalone hopper input)
        final Block above = block.getRelative(0, 1, 0);
        if (above.getType() == Material.HOPPER) {
            final BlockState state = above.getState();
            if (state instanceof Hopper hopper) {
                final org.bukkit.inventory.Inventory inv = hopper.getInventory();
                boolean changed = false;
                for (int slot = 0; slot < inv.getSize(); slot++) {
                    final ItemStack item = inv.getItem(slot);
                    if (item == null || item.getType() == Material.AIR) {
                        continue;
                    }
                    if (isBlacklisted(item) || !StackUtils.itemsMatch(cache, item, true)) {
                        continue;
                    }
                    final int count = item.getAmount();
                    final int leftover = cache.increaseAmount(count);
                    if (leftover <= 0) {
                        inv.setItem(slot, null);
                        changed = true;
                    } else if (leftover < count) {
                        item.setAmount(leftover);
                        changed = true;
                    }
                    if (leftover > 0 && !cache.isVoidExcess()) {
                        break;
                    }
                }
                if (changed) {
                    syncBlock(blockMenu.getLocation(), cache);
                    blockMenu.markDirty();
                }
            }
        }
    }

    private void toggleVoid(@Nonnull BlockMenu blockMenu) {
        final QuantumCache cache = CACHES.get(blockMenu.getLocation());
        if (cache == null) {
            return;
        }
        cache.setVoidExcess(!cache.isVoidExcess());
        updateDisplayItem(blockMenu, cache);
        syncBlock(blockMenu, cache);
        CACHES.put(blockMenu.getLocation(), cache);
    }

    private void setItem(@Nonnull BlockMenu blockMenu, @Nonnull Player player) {
        final ItemStack cursor = player.getItemOnCursor();

        if (isBlacklisted(cursor)) {
            return;
        }

        final QuantumCache cache = CACHES.get(blockMenu.getLocation());
        if (cache == null || cache.getAmount() > 0) {
            player.sendMessage(Theme.WARNING + "Quantum Storage must be empty before changing the set item.");
            return;
        }
        final ItemStack itemStack = cursor.clone();
        itemStack.setAmount(1);
        cache.setItemStack(itemStack);
        updateDisplayItem(blockMenu, cache);
        syncBlock(blockMenu, cache);
        CACHES.put(blockMenu.getLocation(), cache);
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                for (int i : INPUT_SLOTS) {
                    addItem(i, BACK_INPUT, (p, slot, item, action) -> false);
                }
                for (int i : ITEM_SLOTS) {
                    addItem(i, BACK_ITEM, (p, slot, item, action) -> false);
                }
                for (int i : OUTPUT_SLOTS) {
                    addItem(i, BACK_OUTPUT, (p, slot, item, action) -> false);
                }
                addItem(ITEM_SET_SLOT, SET_ITEM, (p, slot, item, action) -> false);
                addMenuClickHandler(ITEM_SLOT, ChestMenuUtils.getEmptyClickHandler());
                drawBackground(BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return new int[]{INPUT_SLOT};
                } else if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }
                return new int[0];
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block block) {
                menu.addMenuClickHandler(ITEM_SET_SLOT, (p, slot, item, action) -> {
                    if (action.isShiftClicked()) {
                        toggleVoid(menu);
                    } else {
                        setItem(menu, p);
                    }
                    return false;
                });

                // Cache may exist if placed with items held inside.
                QuantumCache cache = CACHES.get(block.getLocation());
                if (cache == null) {
                    cache = addCache(menu);
                }
                // Inventario a medio cargar con contenido persistido: no tocamos el display
                // todavía; un tick posterior reconstruirá el cache real desde el .sfi + BS_AMOUNT.
                if (cache == null) {
                    return;
                }
                updateDisplayItem(menu, cache);
                menu.markDirty();
            }
        };
    }

    @Nullable
    private QuantumCache addCache(@Nonnull BlockMenu blockMenu) {
        final Location location = blockMenu.getLocation();
        final String amountString = BlockStorage.getLocationInfo(location, BS_AMOUNT);
        final String voidString = BlockStorage.getLocationInfo(location, BS_VOID);
        final int amount = QuantumStorageState.parseStoredAmount(amountString, this.maxAmount);
        final boolean voidExcess = voidString == null || Boolean.parseBoolean(voidString);
        final ItemStack itemStack = blockMenu.getItemInSlot(ITEM_SLOT);

        QuantumCache cache = createCache(itemStack, blockMenu, amount, voidExcess);

        // Estado transitorio (inventario a medio cargar con amount>0): NO cacheamos null
        // para no fijar un estado vacío sobre el contenido real persistido.
        if (cache == null) {
            return null;
        }

        CACHES.put(location, cache);
        return cache;
    }

    @Nullable
    private QuantumCache createCache(@Nullable ItemStack itemStack, @Nonnull BlockMenu menu, int amount, boolean voidExcess) {
        if (itemStack == null || itemStack.getType() == Material.AIR || isDisplayItem(itemStack)) {
            // ITEM_SLOT llega vacío o con el placeholder NO_ITEM. Si BS_AMOUNT > 0, el storage
            // TENÍA contenido y el BlockMenu aún no restauró su .sfi (getInventory()->loadInventory
            // usa el constructor de 2 args que no lee el Config). NO reseteamos ni persistimos un
            // cache en 0: devolvemos null para saltar este ciclo y dejar que un tick posterior
            // (con el .sfi ya cargado) reconstruya el cache real. Evita la pérdida silenciosa.
            if (amount > 0) {
                return null;
            }
            menu.addItem(ITEM_SLOT, NO_ITEM);
            menu.markDirty();
            return new QuantumCache(null, 0, this.maxAmount, true, false);
        } else {
            final ItemStack clone = itemStack.clone();
            final ItemMeta itemMeta = clone.getItemMeta();
            QuantumStorageState.stripDisplayLore(itemMeta);
            clone.setItemMeta(itemMeta);

            final QuantumCache cache = new QuantumCache(clone, amount, this.maxAmount, voidExcess);

            updateDisplayItem(menu, cache);
            menu.markDirty();
            return cache;
        }
    }

    private boolean isDisplayItem(@Nonnull ItemStack itemStack) {
        return PersistentDataAPI.getBoolean(itemStack.getItemMeta(), Keys.newKey("display"));
    }

    protected void onBreak(@Nonnull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final BlockMenu blockMenu = BlockStorage.getInventory(event.getBlock());

        if (blockMenu != null) {
            final QuantumCache cache = CACHES.remove(blockMenu.getLocation());

            if (cache != null && cache.getAmount() > 0 && cache.getItemStack() != null) {
                final ItemStack itemToDrop = this.getItem().clone();
                final ItemMeta itemMeta = itemToDrop.getItemMeta();

                DataTypeMethods.setCustom(itemMeta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, cache);
                cache.addMetaLore(itemMeta);
                itemToDrop.setItemMeta(itemMeta);
                location.getWorld().dropItem(location.clone().add(0.5, 0.5, 0.5), itemToDrop);
                event.setDropItems(false);
            }

            for (int i : this.slotsToDrop) {
                blockMenu.dropItems(location, i);
            }
        }
    }

    protected void onPlace(@Nonnull BlockPlaceEvent event) {
        final ItemStack itemStack = event.getItemInHand();
        final ItemMeta itemMeta = itemStack.getItemMeta();
        final QuantumCache cache = DataTypeMethods.getCustom(itemMeta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

        if (cache == null) {
            return;
        }

        syncBlock(event.getBlock().getLocation(), cache);
        CACHES.put(event.getBlock().getLocation(), cache);

        final BlockMenu blockMenu = BlockStorage.getInventory(event.getBlock().getLocation());
        if (blockMenu != null) {
            updateDisplayItem(blockMenu, cache);
            blockMenu.markDirty();
        }
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    @ParametersAreNonnullByDefault
    public static void tryInputItem(Location location, ItemStack[] input, QuantumCache cache) {
        if (cache.getItemStack() == null) {
            return;
        }
        for (ItemStack itemStack : input) {
            if (isBlacklisted(itemStack)) {
                continue;
            }
            if (StackUtils.itemsMatch(cache, itemStack, true)) {
                final int incoming = itemStack.getAmount();
                int leftover = cache.increaseAmount(incoming);
                itemStack.setAmount(Math.max(leftover, 0));
            }
        }
        final BlockMenu blockMenu = BlockStorage.getInventory(location);
        if (blockMenu != null) {
            final ItemStack inSlot = blockMenu.getItemInSlot(INPUT_SLOT);
            if (inSlot == null || inSlot.getType() == Material.AIR || inSlot.getAmount() <= 0) {
                blockMenu.replaceExistingItem(INPUT_SLOT, null);
            }
            blockMenu.markDirty();
            syncBlock(blockMenu, cache);
        } else {
            syncBlock(location, cache);
        }
    }

    private static boolean isBlacklisted(@Nullable ItemStack itemStack) {
        // getMaxDurability() < 0 nunca es verdad (es un short, mínimo 0) — condición eliminada.
        return itemStack == null
            || itemStack.getType() == Material.AIR
            || itemStack.getItemMeta() instanceof BundleMeta
            || Tag.SHULKER_BOXES.isTagged(itemStack.getType())
            || SlimefunItem.getByItem(itemStack) instanceof NetworkQuantumStorage;
    }

    @ParametersAreNonnullByDefault
    @Nullable
    public static ItemStack getItemStack(@Nonnull QuantumCache cache, @Nonnull BlockMenu blockMenu) {
        if (cache.getItemStack() == null || cache.getAmount() <= 0) {
            return null;
        }
        return getItemStack(cache, blockMenu, cache.getItemStack().getMaxStackSize());
    }

    @ParametersAreNonnullByDefault
    @Nullable
    public static ItemStack getItemStack(@Nonnull QuantumCache cache, @Nonnull BlockMenu blockMenu, int amount) {
        if (cache.getAmount() < amount) {
            // Storage has no content or not enough, mix and match!
            ItemStack output = blockMenu.getItemInSlot(OUTPUT_SLOT);
            ItemStack fetched = cache.withdrawItem(amount);

            if (output != null
                && output.getType() != Material.AIR
                && StackUtils.itemsMatch(cache, output, true)
            ) {
                // We have an output item we can use also
                if (fetched == null || fetched.getType() == Material.AIR) {
                    // Storage is totally empty - just use output slot
                    fetched = output.clone();
                    if (fetched.getAmount() > amount) {
                        fetched.setAmount(amount);
                    }
                    output.setAmount(output.getAmount() - fetched.getAmount());
                } else {
                    // Storage has content, lets add on top of it
                    int additional = Math.min(amount - fetched.getAmount(), output.getAmount());
                    output.setAmount(output.getAmount() - additional);
                    fetched.setAmount(fetched.getAmount() + additional);
                }
                if (output.getAmount() <= 0) {
                    blockMenu.replaceExistingItem(OUTPUT_SLOT, null);
                }
            }
            syncBlock(blockMenu, cache);
            return fetched;
        } else {
            // Storage has everything we need
            final ItemStack withdrawn = cache.withdrawItem(amount);
            syncBlock(blockMenu, cache);
            return withdrawn;
        }
    }

    private static void updateDisplayItem(@Nonnull BlockMenu menu, @Nonnull QuantumCache cache) {
        if (cache.getItemStack() == null) {
            menu.replaceExistingItem(ITEM_SLOT, NO_ITEM);
        } else {
            final ItemStack itemStack = cache.getItemStack().clone();
            final ItemMeta itemMeta = itemStack.getItemMeta();
            final List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(Theme.CLICK_INFO + "Voiding: " + Theme.PASSIVE + StringUtils.toTitleCase(String.valueOf(cache.isVoidExcess())));
            lore.add(Theme.CLICK_INFO + "Amount: " + Theme.PASSIVE + cache.getAmount());
            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
            itemStack.setAmount(1);
            menu.replaceExistingItem(ITEM_SLOT, itemStack);
        }
    }

    private static void syncBlock(@Nonnull BlockMenu blockMenu, @Nonnull QuantumCache cache) {
        syncBlock(blockMenu.getLocation(), cache);
        blockMenu.markDirty();
    }

    private static void syncBlock(@Nonnull Location location, @Nonnull QuantumCache cache) {
        BlockStorage.addBlockInfo(location, BS_AMOUNT, String.valueOf(cache.getAmount()));
        BlockStorage.addBlockInfo(location, BS_VOID, String.valueOf(cache.isVoidExcess()));
        Networks.getInstance().recordQuantumStorage(location, cache);
    }

    public static Map<Location, QuantumCache> getCaches() {
        return CACHES;
    }

    /** Clears cached storage state after BlockStorage has been persisted. */
    public static void clearRuntimeCache() {
        CACHES.clear();
    }

    public static int[] getSizes() {
        return SIZES;
    }

    @Override
    public boolean canStack(@Nonnull ItemMeta sfItemMeta, @Nonnull ItemMeta itemMeta) {
        return sfItemMeta.getPersistentDataContainer().equals(itemMeta.getPersistentDataContainer());
    }

    @Nullable
    public static QuantumCache getDatabaseCache(@Nonnull Location location) {
        QuantumCache cache = CACHES.get(location);
        if (cache != null) {
            return cache;
        }

        final SlimefunItem item = BlockStorage.check(location);
        if (item instanceof NetworkQuantumStorage quantumStorage) {
            final BlockMenu blockMenu = BlockStorage.getInventory(location);
            if (blockMenu != null) {
                return quantumStorage.addCache(blockMenu);
            }
        }
        return null;
    }
}
