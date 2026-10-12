package io.github.sefiraat.networks.slimefun.network;

import dev.drake.sefilib.misc.ParticleUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.utils.SlimefunUtils;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** High-throughput vacuum with persistent filters stored in native BlockStorage. */
public final class NetworkAdvancedVacuum extends NetworkObject {

    private static final String FILTER_MODE_KEY = "advanced-vacuum-filter-mode";
    private static final String MATCH_MODE_KEY = "advanced-vacuum-match-mode";
    private static final int EMPTY_SCAN_DELAY_TICKS = 10;
    private static final int[] INPUT_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15, 16, 17
    };
    private static final int[] DIVIDER_SLOTS = {18, 19, 20, 21, 22, 23, 24, 25, 26};
    private static final int[] FILTER_SLOTS = {27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final int FILTER_MODE_SLOT = 36;
    private static final int MATCH_MODE_SLOT = 37;
    private static final int[] BACKGROUND_SLOTS = {
        38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final ItemStack DIVIDER = ItemCreator.create(
        Material.LIGHT_BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Vacuum buffer"
    );

    private final ItemSetting<Integer> tickRate;
    private final ItemSetting<Integer> vacuumRange;
    private final Map<Location, Integer> emptyScanCooldowns = new ConcurrentHashMap<>();

    public NetworkAdvancedVacuum(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ADVANCED_VACUUM);
        tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        vacuumRange = new IntRangeSetting(this, "vacuum_range", 1, 2, 5);
        addItemSetting(tickRate, vacuumRange);
        for (int slot : INPUT_SLOTS) {
            getSlotsToDrop().add(slot);
        }
        for (int slot : FILTER_SLOTS) {
            getSlotsToDrop().add(slot);
        }

        addItemHandler(new BlockTicker() {
            private int tick = 1;

            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem ignored, Config config) {
                if (tick > 1) {
                    return;
                }
                BlockMenu menu = BlockStorage.getInventory(block);
                if (menu == null) {
                    return;
                }
                addToRegistry(block);
                flushBuffer(menu);
                if (isScanDue(block.getLocation())) {
                    collectOneItem(menu);
                }
            }

            @Override
            public void uniqueTick() {
                tick = tick <= 1 ? tickRate.getValue() : tick - 1;
            }
        });
    }

    private void flushBuffer(@Nonnull BlockMenu menu) {
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(menu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }
        for (int slot : INPUT_SLOTS) {
            if (NetworkTransportUtils.pullIntoNetwork(definition.getNode().getRoot(), menu.getLocation(), menu, slot) > 0) {
                menu.markDirty();
            }
        }
    }

    private void collectOneItem(@Nonnull BlockMenu menu) {
        int freeSlot = findFreeInputSlot(menu);
        if (freeSlot < 0) {
            return;
        }
        Location center = menu.getLocation().clone().add(0.5, 0.5, 0.5);
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(
            center, vacuumRange.getValue(), vacuumRange.getValue(), vacuumRange.getValue(), Item.class::isInstance
        );
        for (Entity entity : nearby) {
            Item item = (Item) entity;
            if (!item.isValid() || item.isDead()) {
                continue;
            }
            if (item.getPickupDelay() > 0 || SlimefunUtils.hasNoPickupFlag(item) || !allows(menu, item.getItemStack())) {
                continue;
            }
            ItemStack entityStack = item.getItemStack();
            int currentAmount = entityStack.getAmount();
            ItemStack stack = entityStack.clone();
            int moved = Math.min(currentAmount, stack.getMaxStackSize());
            stack.setAmount(moved);
            menu.replaceExistingItem(freeSlot, stack);
            menu.markDirty();
            if (currentAmount <= moved) {
                item.remove();
            } else {
                ItemStack remaining = entityStack.clone();
                remaining.setAmount(currentAmount - moved);
                item.setItemStack(remaining);
            }
            ParticleUtils.displayParticleRandomly(item, 1, 5, new Particle.DustOptions(Color.AQUA, 1));
            return;
        }
        deferEmptyScan(menu.getLocation());
    }

    private boolean isScanDue(@Nonnull Location location) {
        return emptyScanCooldowns.compute(location, (ignored, remaining) ->
            remaining == null || remaining <= 1 ? null : remaining - 1
        ) == null;
    }

    private void deferEmptyScan(@Nonnull Location location) {
        emptyScanCooldowns.put(location, EMPTY_SCAN_DELAY_TICKS);
    }

    @Override
    protected void clearCachedState(@Nonnull Location location) {
        emptyScanCooldowns.remove(location);
    }

    private int findFreeInputSlot(@Nonnull BlockMenu menu) {
        for (int slot : INPUT_SLOTS) {
            ItemStack existing = menu.getItemInSlot(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                return slot;
            }
        }
        return -1;
    }

    private boolean allows(@Nonnull BlockMenu menu, @Nonnull ItemStack incoming) {
        boolean hasFilter = false;
        boolean matched = false;
        boolean materialMode = getFlag(menu, MATCH_MODE_KEY);
        for (int slot : FILTER_SLOTS) {
            ItemStack filter = menu.getItemInSlot(slot);
            if (filter == null || filter.getType() == Material.AIR) {
                continue;
            }
            hasFilter = true;
            if (materialMode ? filter.getType() == incoming.getType() : StackUtils.itemsMatch(filter, incoming)) {
                matched = true;
                break;
            }
        }
        return !hasFilter || (getFlag(menu, FILTER_MODE_KEY) ? matched : !matched);
    }

    private boolean getFlag(@Nonnull BlockMenu menu, @Nonnull String key) {
        return Boolean.parseBoolean(BlockStorage.getLocationInfo(menu.getLocation(), key));
    }

    private void toggleFlag(@Nonnull BlockMenu menu, @Nonnull String key) {
        BlockStorage.addBlockInfo(menu.getLocation(), key, Boolean.toString(!getFlag(menu, key)));
        updateControls(menu);
    }

    private void updateControls(@Nonnull BlockMenu menu) {
        boolean whitelist = getFlag(menu, FILTER_MODE_KEY);
        boolean materialMode = getFlag(menu, MATCH_MODE_KEY);
        menu.replaceExistingItem(FILTER_MODE_SLOT, ItemCreator.create(
            whitelist ? Material.LIME_DYE : Material.RED_DYE,
            Theme.CLICK_INFO + (whitelist ? "Whitelist filters" : "Blacklist filters"),
            Theme.PASSIVE + "Click to switch filter mode"
        ));
        menu.replaceExistingItem(MATCH_MODE_SLOT, ItemCreator.create(
            materialMode ? Material.IRON_INGOT : Material.NAME_TAG,
            Theme.CLICK_INFO + (materialMode ? "Match material" : "Match exact item"),
            Theme.PASSIVE + "Click to switch match mode"
        ));
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                drawBackground(DIVIDER, DIVIDER_SLOTS);
                drawBackground(BACKGROUND_SLOTS);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block block) {
                updateControls(menu);
                menu.addMenuClickHandler(FILTER_MODE_SLOT, (player, slot, item, action) -> {
                    toggleFlag(menu, FILTER_MODE_KEY);
                    return false;
                });
                menu.addMenuClickHandler(MATCH_MODE_SLOT, (player, slot, item, action) -> {
                    toggleFlag(menu, MATCH_MODE_KEY);
                    return false;
                });
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkAdvancedVacuum.this.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? INPUT_SLOTS : new int[0];
            }
        };
    }
}
