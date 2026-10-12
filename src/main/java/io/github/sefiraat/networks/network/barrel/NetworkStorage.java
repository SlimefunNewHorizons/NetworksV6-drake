package io.github.sefiraat.networks.network.barrel;

import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkStorage extends BarrelIdentity {

    public NetworkStorage(Location location, ItemStack itemStack, int amount) {
        super(location, itemStack, amount, BarrelType.NETWORKS);
    }

    @Nullable
    @Override
    public ItemStack getItemStack() {
        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(getLocation());
        if (cache == null || cache.getItemStack() == null) {
            return null;
        }
        final ItemStack clone = cache.getItemStack().clone();
        clone.setAmount(1);
        return clone;
    }

    @Override
    public int getAmount() {
        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(getLocation());
        if (cache == null) {
            return 0;
        }

        long amount = cache.getAmount();
        final BlockMenu blockMenu = BlockStorage.getInventory(getLocation());
        if (blockMenu != null) {
            final ItemStack output = blockMenu.getItemInSlot(NetworkQuantumStorage.OUTPUT_SLOT);
            if (output != null && getItemStack() != null
                    && io.github.sefiraat.networks.utils.StackUtils.itemsMatch(this, output, true)) {
                amount += output.getAmount();
            }
        }
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    @Override
    @Nullable
    public ItemStack requestItem(@Nonnull ItemRequest itemRequest) {
        final BlockMenu blockMenu = BlockStorage.getInventory(this.getLocation());

        if (blockMenu == null) {
            return null;
        }

        final QuantumCache cache = NetworkQuantumStorage.getDatabaseCache(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        return NetworkQuantumStorage.getItemStack(cache, blockMenu, itemRequest.getAmount());
    }

    @Override
    public void depositItemStack(ItemStack[] itemsToDeposit) {
        if (BlockStorage.check(this.getLocation()) instanceof NetworkQuantumStorage) {
            final BlockMenu blockMenu = BlockStorage.getInventory(this.getLocation());
            if (blockMenu == null) {
                return;
            }
            final QuantumCache cache = NetworkQuantumStorage.getDatabaseCache(this.getLocation());
            if (cache != null) {
                NetworkQuantumStorage.tryInputItem(blockMenu.getLocation(), itemsToDeposit, cache);
            }
        }
    }


    @Override
    public int getInputSlot() {
        return NetworkQuantumStorage.INPUT_SLOT;
    }

    @Override
    public int getOutputSlot() {
        return NetworkQuantumStorage.OUTPUT_SLOT;
    }
}
