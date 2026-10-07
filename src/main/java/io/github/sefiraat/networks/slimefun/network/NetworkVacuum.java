package io.github.sefiraat.networks.slimefun.network;

import dev.drake.sefilib.misc.ParticleUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkVacuum extends NetworkObject {

    private static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    /**
     * An empty spatial query is the common case for unattended vacuums. Rechecking it every
     * server tick makes each machine traverse the world's entity lookup even when there is
     * nothing to collect. Half a second remains responsive for drops while avoiding that idle
     * main-thread cost.
     */
    private static final int EMPTY_SCAN_DELAY_TICKS = 10;

    private final ItemSetting<Integer> tickRate;
    private final ItemSetting<Integer> vacuumRange;
    private final Map<Location, Integer> emptyScanCooldowns = new ConcurrentHashMap<>();

    public NetworkVacuum(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.VACUUM);

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        this.vacuumRange = new IntRangeSetting(this, "vacuum_range", 1, 2, 5);
        addItemSetting(this.tickRate, this.vacuumRange);

        for (int inputSlot : INPUT_SLOTS) {
            this.getSlotsToDrop().add(inputSlot);
        }

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
                            if (isScanDue(block.getLocation())) {
                                findItem(blockMenu);
                            }
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

    private void findItem(@Nonnull BlockMenu blockMenu) {
        for (int inputSlot : INPUT_SLOTS) {
            final ItemStack inSlot = blockMenu.getItemInSlot(inputSlot);
            if (inSlot == null || inSlot.getType().isAir()) {
                final Location location = blockMenu.getLocation().clone().add(0.5, 0.5, 0.5);
                final int range = this.vacuumRange.getValue();
                Collection<Entity> items = location.getWorld()
                    .getNearbyEntities(location, range, range, range, Item.class::isInstance);
                Optional<Entity> optionalEntity = items.stream().findFirst();
                if (optionalEntity.isEmpty() || !(optionalEntity.get() instanceof Item item)) {
                    deferEmptyScan(blockMenu.getLocation());
                    return;
                }
                if (item.isValid() && !item.isDead() && item.getPickupDelay() <= 0 && !SlimefunUtils.hasNoPickupFlag(item)) {
                    final ItemStack itemStack = item.getItemStack().clone();
                    blockMenu.replaceExistingItem(inputSlot, itemStack);
                    blockMenu.markDirty();
                    ParticleUtils.displayParticleRandomly(item, 1, 5, new Particle.DustOptions(Color.BLUE, 1));
                    item.remove();
                } else {
                    deferEmptyScan(blockMenu.getLocation());
                }
                return;
            }
        }
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

    private void tryAddItem(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        for (int inputSlot : INPUT_SLOTS) {
            final ItemStack itemStack = blockMenu.getItemInSlot(inputSlot);

            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            final int consumed = NetworkTransportUtils.pullIntoNetwork(
                    definition.getNode().getRoot(),
                    blockMenu.getLocation(),
                    blockMenu,
                    inputSlot);
            if (consumed > 0) {
                blockMenu.markDirty();
            }
        }
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(9);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_VACUUM.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return INPUT_SLOTS;
                }
                return new int[0];
            }
        };
    }
}
