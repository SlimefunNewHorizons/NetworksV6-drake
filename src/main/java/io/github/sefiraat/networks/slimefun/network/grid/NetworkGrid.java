package io.github.sefiraat.networks.slimefun.network.grid;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkGrid extends AbstractGrid {

    private static final int[] BACKGROUND_SLOTS = {
        17, 26
    };

    private static final int[] DISPLAY_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7,
        9, 10, 11, 12, 13, 14, 15, 16,
        18, 19, 20, 21, 22, 23, 24, 25,
        27, 28, 29, 30, 31, 32, 33, 34,
        36, 37, 38, 39, 40, 41, 42, 43,
        45, 46, 47, 48, 49, 50, 51, 52
    };

    private static final int INPUT_SLOT = 8;

    private static final int CHANGE_SORT = 26;
    private static final int FILTER = 35;
    private static final int PAGE_PREVIOUS = 44;
    private static final int PAGE_NEXT = 53;

    private static final Map<Location, GridCache> CACHE_MAP = new ConcurrentHashMap<>();

    public NetworkGrid(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        this.getSlotsToDrop().add(getInputSlot());
    }

    @Override
    @Nonnull
    protected BlockMenuPreset getPreset() {
        return new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(getBackgroundSlots());
                setSize(54);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                final var definition = NetworkStorage.getAllNetworkObjects().get(block.getLocation());
                if (definition != null && definition.getNode() != null) {
                    definition.getNode().getRoot().getCellMenus();
                }
                return NetworkSlimefunItems.NETWORK_GRID.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                getCacheMap().put(menu.getLocation(), new GridCache(0, 0, GridCache.SortOrder.ALPHABETICAL));

                menu.replaceExistingItem(getPagePrevious(), getPagePreviousStack());
                menu.addMenuClickHandler(getPagePrevious(), (p, slot, item, action) -> {
                    GridCache gridCache = getCache(menu);
                    gridCache.setPage(GridCache.previousPage(gridCache.getPage()));
                    getCacheMap().put(menu.getLocation(), gridCache);
                    return false;
                });

                menu.replaceExistingItem(getPageNext(), getPageNextStack());
                menu.addMenuClickHandler(getPageNext(), (p, slot, item, action) -> {
                    GridCache gridCache = getCache(menu);
                    gridCache.setPage(GridCache.nextPage(gridCache.getPage(), gridCache.getMaxPages()));
                    getCacheMap().put(menu.getLocation(), gridCache);
                    return false;
                });

                menu.replaceExistingItem(getChangeSort(), getChangeSortStack());
                menu.addMenuClickHandler(getChangeSort(), (p, slot, item, action) -> {
                    GridCache gridCache = getCache(menu);
                    if (gridCache.getSortOrder() == GridCache.SortOrder.ALPHABETICAL) {
                        gridCache.setSortOrder(GridCache.SortOrder.NUMBER);
                    } else {
                        gridCache.setSortOrder(GridCache.SortOrder.ALPHABETICAL);
                    }
                    getCacheMap().put(menu.getLocation(), gridCache);
                    return false;
                });

                menu.replaceExistingItem(getFilterSlot(), getFilterStack());
                menu.addMenuClickHandler(getFilterSlot(), (p, slot, item, action) -> {
                    GridCache gridCache = getCache(menu);
                    return setFilter(p, menu, gridCache, action);
                });

                for (int displaySlot : getDisplaySlots()) {
                    menu.replaceExistingItem(displaySlot, getBlankSlotStack());
                    menu.addMenuClickHandler(displaySlot,
                            (player, slot, item, action) -> handleDisplayClick(player, item, action, menu));
                }

                menu.addPlayerInventoryClickHandler((p, s, i, a) -> {
                    if (!a.isShiftClicked() || a.isRightClicked()) {
                        return true;
                    }

                    if (i == null || i.getType() == org.bukkit.Material.AIR) {
                        return false;
                    }

                    // Remover del inventario preventivamente contra dupes concurrentes
                    final ItemStack toInsert = i.clone();
                    p.getInventory().setItem(s, null);
                    receiveItem(p, toInsert, a, menu);

                    // Si la red no pudo absorber todo o parte del ítem (ej. red llena, sin celda/barril
                    // compatible o ítem especial no almacenable como Flight Gem), devolver el remanente (#emilio-flight-gem).
                    if (toInsert.getAmount() > 0) {
                        final ItemStack current = p.getInventory().getItem(s);
                        if (current == null || current.getType() == org.bukkit.Material.AIR) {
                            p.getInventory().setItem(s, toInsert);
                        } else {
                            final java.util.Map<Integer, ItemStack> overflow = p.getInventory().addItem(toInsert);
                            for (ItemStack leftover : overflow.values()) {
                                if (leftover != null && leftover.getAmount() > 0) {
                                    p.getWorld().dropItemNaturally(p.getLocation(), leftover);
                                }
                            }
                        }
                    }
                    return false;
                });
            }
        };
    }

    @Nonnull
    public Map<Location, GridCache> getCacheMap() {
        return CACHE_MAP;
    }

    private GridCache getCache(@Nonnull BlockMenu menu) {
        return CACHE_MAP.computeIfAbsent(
            menu.getLocation().clone(),
            location -> new GridCache(0, 0, GridCache.SortOrder.ALPHABETICAL)
        );
    }

    public int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    public int[] getDisplaySlots() {
        return DISPLAY_SLOTS;
    }

    public int getInputSlot() {
        return INPUT_SLOT;
    }

    public int getChangeSort() {
        return CHANGE_SORT;
    }

    public int getPagePrevious() {
        return PAGE_PREVIOUS;
    }

    public int getPageNext() {
        return PAGE_NEXT;
    }

    @Override
    protected int getFilterSlot() {
        return FILTER;
    }

    @Override
    protected void clearCachedState(@Nonnull Location location) {
        CACHE_MAP.remove(location);
    }
}
