package io.github.sefiraat.networks.slimefun.network.grid;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.GridItemRequest;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.utils.ChatUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractGrid extends NetworkObject {

    private static final ItemStack BLANK_SLOT_STACK = ItemCreator.create(
        Material.LIGHT_GRAY_STAINED_GLASS_PANE,
        " "
    );

    private static final ItemStack PAGE_PREVIOUS_STACK = ItemCreator.create(
        Material.RED_STAINED_GLASS_PANE,
        Theme.CLICK_INFO.getColor() + "Previous Page"
    );

    private static final ItemStack PAGE_NEXT_STACK = ItemCreator.create(
        Material.RED_STAINED_GLASS_PANE,
        Theme.CLICK_INFO.getColor() + "Next Page"
    );

    private static final ItemStack CHANGE_SORT_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE,
        Theme.CLICK_INFO.getColor() + "Change Sort Order"
    );

    private static final ItemStack FILTER_STACK = ItemCreator.create(
        Material.NAME_TAG,
        Theme.CLICK_INFO.getColor() + "Set Filter (Right Click to Clear)"
    );

    private static final Comparator<Map.Entry<ItemStack, Integer>> ALPHABETICAL_SORT = Comparator.comparing(
        itemStackIntegerEntry -> {
            ItemStack itemStack = itemStackIntegerEntry.getKey();
            SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
            if (slimefunItem != null) {
                return ChatColor.stripColor(slimefunItem.getItemName());
            } else {
                ItemMeta itemMeta = itemStackIntegerEntry.getKey().getItemMeta();
                return itemMeta != null && itemMeta.hasDisplayName()
                    ? ChatColor.stripColor(itemMeta.getDisplayName())
                    : itemStackIntegerEntry.getKey().getType().name();
            }
        }
    );

    private static final Comparator<Map.Entry<ItemStack, Integer>> NUMERICAL_SORT = Map.Entry.comparingByValue();

    private final ItemSetting<Integer> tickRate;

    protected AbstractGrid(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRID);

        this.getSlotsToDrop().add(getInputSlot());

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);

        addItemHandler(
            new BlockTicker() {

                private int tick = 1;

                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block block, SlimefunItem item, Config data) {
                    if (tick <= 1) {
                        final BlockMenu blockMenu = BlockStorage.getInventory(block);
                        addToRegistry(block);
                        if (blockMenu != null) {
                            tryAddItem(blockMenu);
                            updateDisplay(blockMenu);
                        }
                    }
                }

                @Override
                public void uniqueTick() {
                    tick = tick <= 1 ? tickRate.getValue() : tick - 1;
                }
            }
        );
    }

    protected void tryAddItem(@Nonnull BlockMenu blockMenu) {
        final ItemStack itemStack = blockMenu.getItemInSlot(getInputSlot());

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }

        final int consumed = NetworkTransportUtils.pullIntoNetwork(
                definition.getNode().getRoot(),
                blockMenu.getLocation(),
                blockMenu,
                getInputSlot());
        if (consumed > 0) {
            // pullIntoNetwork mutó el ItemStack del slot in-place (addItemStack0 hace
            // incoming.setAmount(...) sobre la referencia viva del slot). Si quedó vacío,
            // sincronizar el BlockMenu para no dejar un item-fantasma que vanilla pueda
            // recoger con click (dupe del grid/remote). Mismo patrón que tryReturnItems.
            final ItemStack remaining = blockMenu.getItemInSlot(getInputSlot());
            if (remaining == null || remaining.getType() == Material.AIR || remaining.getAmount() <= 0) {
                blockMenu.replaceExistingItem(getInputSlot(), null);
            }
            blockMenu.markDirty();
        }
    }

    protected void updateDisplay(@Nonnull BlockMenu blockMenu) {
        // No viewer - lets not bother updating
        if (!blockMenu.hasViewer()) {
            return;
        }

        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        // No node located, weird
        if (definition == null || definition.getNode() == null) {
            clearDisplay(blockMenu);
            return;
        }

        // Update Screen
        final NetworkRoot root = definition.getNode().getRoot();
        root.refreshRootItems();
        final GridCache gridCache = getCacheMap().computeIfAbsent(
            blockMenu.getLocation().clone(),
            location -> new GridCache(0, 0, GridCache.SortOrder.ALPHABETICAL)
        );
        final List<Map.Entry<ItemStack, Integer>> entries = getEntries(root, gridCache);
        final int pages = (int) Math.ceil(entries.size() / (double) getDisplaySlots().length) - 1;

        // Con la rejilla vacia `pages` vale -1 (ceil(0/slots) - 1). Publicarlo como maximo hacia
        // que el boton de pagina siguiente dejara la cache en la pagina -1, y al reaparecer items
        // el subList de mas abajo arrancaba en un indice negativo (IndexOutOfBoundsException por
        // tick). Siempre existe la pagina 0, asi que el maximo publicado nunca baja de ella.
        gridCache.setMaxPages(Math.max(0, pages));

        // Set everything to blank and return if there are no pages (no items)
        if (pages < 0) {
            clearDisplay(blockMenu);
            return;
        }

        // Reset selected page if it no longer exists due to items being removed.
        // El limite inferior tambien se comprueba para sanear caches que ya quedaron en negativo.
        if (gridCache.getPage() > pages || gridCache.getPage() < 0) {
            gridCache.setPage(0);
        }

        final int start = gridCache.getPage() * getDisplaySlots().length;
        final int end = Math.min(start + getDisplaySlots().length, entries.size());
        final List<Map.Entry<ItemStack, Integer>> validEntries = entries.subList(start, end);

        getCacheMap().put(blockMenu.getLocation(), gridCache);

        for (int i = 0; i < getDisplaySlots().length; i++) {
            if (validEntries.size() > i) {
                final Map.Entry<ItemStack, Integer> entry = validEntries.get(i);
                final ItemStack displayStack = entry.getKey().clone();
                final ItemMeta itemMeta = displayStack.getItemMeta();
                if (itemMeta == null) {
                    blockMenu.replaceExistingItem(getDisplaySlots()[i], displayStack);
                    continue;
                }
                List<String> lore = itemMeta.getLore();

                if (lore == null) {
                    lore = getLoreAddition(entry.getValue());
                } else {
                    lore.addAll(getLoreAddition(entry.getValue()));
                }

                itemMeta.setLore(lore);
                displayStack.setItemMeta(itemMeta);
                blockMenu.replaceExistingItem(getDisplaySlots()[i], displayStack);
            } else {
                blockMenu.replaceExistingItem(getDisplaySlots()[i], BLANK_SLOT_STACK);
            }
        }
    }

    protected void clearDisplay(BlockMenu blockMenu) {
        for (int displaySlot : getDisplaySlots()) {
            blockMenu.replaceExistingItem(displaySlot, BLANK_SLOT_STACK);
        }
    }

    protected boolean handleDisplayClick(Player player, ItemStack itemStack, ClickAction action,
            BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
        if (definition != null && definition.getNode() != null) {
            retrieveItem(player, definition, itemStack, action, blockMenu);
        }
        return false;
    }

    @Nonnull
    protected List<Map.Entry<ItemStack, Integer>> getEntries(@Nonnull NetworkRoot networkRoot, @Nonnull GridCache cache) {
        return networkRoot.getAllNetworkItems().entrySet().stream()
            .filter(entry -> {
                if (cache.getFilter() == null) {
                    return true;
                }

                final ItemStack itemStack = entry.getKey();
                String name = itemStack.getType().name().toLowerCase(Locale.ROOT);
                if (itemStack.hasItemMeta()) {
                    final ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta.hasDisplayName()) {
                        name = ChatColor.stripColor(itemMeta.getDisplayName().toLowerCase(Locale.ROOT));
                    }
                }
                return name.contains(cache.getFilter());
            })
            .sorted(cache.getSortOrder() == GridCache.SortOrder.ALPHABETICAL ? ALPHABETICAL_SORT : NUMERICAL_SORT.reversed())
            .toList();
    }

    protected boolean setFilter(@Nonnull Player player, @Nonnull BlockMenu blockMenu, @Nonnull GridCache gridCache, @Nonnull ClickAction action) {
        if (action.isRightClicked()) {
            gridCache.setFilter(null);
        } else {
            player.closeInventory();
            player.sendMessage(Theme.WARNING + "Type what you would like to filter this grid to");
            ChatUtils.awaitInput(player, s -> {
                if (s.isBlank()) {
                    return;
                }
                gridCache.setFilter(s.toLowerCase(Locale.ROOT));
                player.sendMessage(Theme.SUCCESS + "Filter applied");
            });
        }
        return false;
    }

    @ParametersAreNonnullByDefault
    protected void retrieveItem(Player player, NodeDefinition definition, @Nullable ItemStack itemStack, ClickAction action, BlockMenu blockMenu) {
        // Dupe guard: verify the node is still active in the network registry
        final NodeDefinition currentDef = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
        if (currentDef == null || currentDef.getNode() == null || currentDef.getNode().getRoot() == null || currentDef.getNode().getRoot().getController() == null) {
            player.sendMessage(Theme.ERROR.getColor() + "This grid is no longer connected to an active network!");
            player.closeInventory();
            return;
        }

        // Todo Item can be null here. No idea how - investigate later
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        final ItemStack clone = itemStack.clone();
        final ItemMeta cloneMeta = clone.getItemMeta();
        final List<String> cloneLore = cloneMeta.getLore();
        if (cloneLore == null || cloneLore.size() < 2 || !isGridDisplayStack(cloneLore)) {
            return;
        }

        cloneLore.remove(cloneLore.size() - 1);
        cloneLore.remove(cloneLore.size() - 1);
        cloneMeta.setLore(cloneLore);
        clone.setItemMeta(cloneMeta);
        int amount = 1;

        if (action.isRightClicked()) {
            amount = clone.getMaxStackSize();
        }

        final GridItemRequest request = new GridItemRequest(clone, amount, player);

        if (action.isShiftClicked()) {
            addToInventory(player, definition, request, blockMenu);
        } else {
            addToCursor(player, definition, request, action, blockMenu);
        }

        updateDisplay(blockMenu);
    }

    @ParametersAreNonnullByDefault
    private void addToInventory(Player player, NodeDefinition definition, GridItemRequest request, BlockMenu menu) {
        ItemStack requestingStack = definition.getNode().getRoot().getItemStack0(menu.getLocation(), request);

        if (requestingStack == null) {
            return;
        }

        HashMap<Integer, ItemStack> remnant = player.getInventory().addItem(requestingStack);
        requestingStack = remnant.values().stream().findFirst().orElse(null);
        if (requestingStack != null) {
            definition.getNode().getRoot().addItemStack0(player.getLocation(), requestingStack);
        }
    }

    @ParametersAreNonnullByDefault
    private void addToCursor(Player player, NodeDefinition definition, GridItemRequest request, ClickAction action, BlockMenu menu) {
        final ItemStack cursor = player.getItemOnCursor();

        // Quickly check if the cursor has an item and if we can add more to it
        if (cursor.getType() != Material.AIR && !canAddMore(action, cursor, request)) {
            return;
        }

        ItemStack requestingStack = definition.getNode().getRoot().getItemStack0(menu.getLocation(), request);
        setCursor(player, cursor, requestingStack);
    }

    private void setCursor(Player player, ItemStack cursor, ItemStack requestingStack) {
        if (requestingStack == null || requestingStack.getType() == Material.AIR || requestingStack.getAmount() <= 0) {
            return;
        }
        if (cursor.getType() != Material.AIR) {
            // Sumar el stack del cursor al solicitado pero sin exceder maxStackSize (#dupe-cursor-overflow).
            final int combined = cursor.getAmount() + requestingStack.getAmount();
            requestingStack.setAmount(Math.min(combined, requestingStack.getMaxStackSize()));
        }
        player.setItemOnCursor(requestingStack);
    }

    private boolean canAddMore(@Nonnull ClickAction action, @Nonnull ItemStack cursor, @Nonnull GridItemRequest request) {
        return !action.isRightClicked()
            && request.getAmount() == 1
            && cursor.getAmount() < cursor.getMaxStackSize()
            && StackUtils.itemsMatch(request, cursor, true);
    }

    @Override
    public void postRegister() {
        getPreset();
    }

    @Nonnull
    protected abstract BlockMenuPreset getPreset();

    @Nonnull
    protected abstract Map<Location, GridCache> getCacheMap();

    protected abstract int[] getBackgroundSlots();

    protected abstract int[] getDisplaySlots();

    public abstract int getInputSlot();

    protected abstract int getChangeSort();

    protected abstract int getPagePrevious();

    protected abstract int getPageNext();

    protected abstract int getFilterSlot();

    protected ItemStack getBlankSlotStack() {
        return BLANK_SLOT_STACK;
    }

    protected ItemStack getPagePreviousStack() {
        return PAGE_PREVIOUS_STACK;
    }

    protected ItemStack getPageNextStack() {
        return PAGE_NEXT_STACK;
    }

    protected ItemStack getChangeSortStack() {
        return CHANGE_SORT_STACK;
    }

    protected ItemStack getFilterStack() {
        return FILTER_STACK;
    }


    /** Solo retiros desde ítems pintados por el grid (anti-dupe #230). */
    private static boolean isGridDisplayStack(@Nonnull List<String> lore) {
        return lore.stream().anyMatch(line -> line != null && line.contains("Amount:"));
    }

    @Nonnull
    private static List<String> getLoreAddition(int amount) {
        final MessageFormat format = new MessageFormat("{0}Amount: {1}{2}", Locale.ROOT);
        return List.of(
            "",
            format.format(new Object[]{Theme.CLICK_INFO.getColor(), Theme.PASSIVE.getColor(), amount}, new StringBuffer(), null).toString()
        );
    }

    @SuppressWarnings("deprecation")
    public void receiveItem(
            Player player, ItemStack itemStack, ClickAction action, BlockMenu blockMenu) {
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }
        receiveItem(definition.getNode().getRoot(), player, itemStack, action, blockMenu);
    }

    @SuppressWarnings({"deprecation", "unused"})
    public void receiveItem(
            NetworkRoot root,
            Player player,
            @Nullable ItemStack itemStack,
            ClickAction action,
            BlockMenu blockMenu) {
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            root.addItemStack0(blockMenu.getLocation(), itemStack);
        }
    }
}
