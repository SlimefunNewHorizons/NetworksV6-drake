package io.github.sefiraat.networks.network.barrel;

import com.github.drakescraft_labs.infinityexpansion.items.storage.StorageCache;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

public class InfinityBarrel extends BarrelIdentity {

    @Nonnull
    private final StorageCache cache;

    @ParametersAreNonnullByDefault
    public InfinityBarrel(Location location, ItemStack itemStack, int amount, StorageCache cache) {
        super(location, itemStack, amount, BarrelType.INFINITY);
        this.cache = cache;
    }

    @Nullable
    @Override
    public ItemStack requestItem(@Nonnull ItemRequest itemRequest) {
        BlockMenu blockMenu = BlockStorage.getInventory(this.getLocation());
        return blockMenu == null ? null : blockMenu.getItemInSlot(this.getOutputSlot());
    }

    @Override
    public void depositItemStack(ItemStack[] itemsToDeposit) {
        cache.depositAll(itemsToDeposit, true);
    }

    @Nullable
    @Override
    public ItemStack getItemStack() {
        final BlockMenu blockMenu = BlockStorage.getInventory(getLocation());
        if (blockMenu == null) {
            return null;
        }
        final ItemStack output = blockMenu.getItemInSlot(getOutputSlot());
        if (output == null) {
            return null;
        }
        final ItemStack clone = output.clone();
        clone.setAmount(1);
        return clone;
    }

    @Override
    public int getAmount() {
        final me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config config = BlockStorage.getLocationInfo(getLocation());
        final String stored = config == null ? null : config.getString("stored");
        if (stored == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(stored));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public int getInputSlot() {
        return 10;
    }

    @Override
    public int getOutputSlot() {
        return 16;
    }
}
