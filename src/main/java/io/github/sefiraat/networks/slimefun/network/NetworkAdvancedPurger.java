package io.github.sefiraat.networks.slimefun.network;

import com.cryptomorin.xseries.particles.XParticle;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.ItemSetting;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.items.settings.IntRangeSetting;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/** Bulk, multi-filter variant of the standard Network Purger. */
public final class NetworkAdvancedPurger extends NetworkObject {

    private static final int[] TEMPLATE_SLOTS = createTemplateSlots();
    private static final int[] DIVIDER_SLOTS = {8, 17, 26, 35, 44, 53};
    private static final ItemStack DIVIDER = ItemCreator.create(
        Material.RED_STAINED_GLASS_PANE, Theme.WARNING + "Purge filters"
    );

    private final ItemSetting<Integer> tickRate;

    public NetworkAdvancedPurger(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ADVANCED_PURGER);

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);
        for (int slot : TEMPLATE_SLOTS) {
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

                final BlockMenu menu = BlockStorage.getInventory(block);
                if (menu == null) {
                    return;
                }
                addToRegistry(block);
                purge(menu);
            }

            @Override
            public void uniqueTick() {
                tick = tick <= 1 ? tickRate.getValue() : tick - 1;
            }
        });
    }

    private void purge(@Nonnull BlockMenu menu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(menu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }

        boolean purged = false;
        for (int slot : TEMPLATE_SLOTS) {
            final ItemStack filter = menu.getItemInSlot(slot);
            if (filter == null || filter.getType() == Material.AIR) {
                continue;
            }

            final ItemStack requested = filter.clone();
            requested.setAmount(1);
            final ItemStack removed = definition.getNode().getRoot().getItemStack0(
                menu.getLocation(), new ItemRequest(requested, requested.getMaxStackSize()));
            purged |= removed != null && removed.getAmount() > 0;
        }

        if (purged && definition.getNode().getRoot().isDisplayParticles()) {
            final org.bukkit.Location effect = menu.getLocation().clone().add(0.5, 1.2, 0.5);
            effect.getWorld().spawnParticle(XParticle.SMOKE.get(), effect, 0, 0, 0.05, 0);
        }
    }

    private static int[] createTemplateSlots() {
        final int[] slots = new int[48];
        int target = 0;
        for (int slot = 0; slot < 54; slot++) {
            if ((slot + 1) % 9 != 0) {
                slots[target++] = slot;
            }
        }
        return slots;
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                drawBackground(DIVIDER, DIVIDER_SLOTS);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkAdvancedPurger.this.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }
}
