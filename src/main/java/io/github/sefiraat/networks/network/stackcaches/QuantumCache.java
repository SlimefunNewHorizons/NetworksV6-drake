package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.utils.Theme;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class QuantumCache extends ItemStackCache {

    @Nullable
    private final ItemMeta storedItemMeta;
    private final boolean supportsCustomMaxAmount;
    private final int limit;
    private int amount;
    private boolean voidExcess;

    public QuantumCache(@Nullable ItemStack storedItem, int amount, int limit, boolean voidExcess) {
        this(storedItem, amount, limit, voidExcess, false);
    }

    public QuantumCache(@Nullable ItemStack storedItem, int amount, int limit, boolean voidExcess, boolean supportsCustomMaxAmount) {
        super(storedItem);
        this.storedItemMeta = storedItem == null ? null : storedItem.getItemMeta();
        this.amount = amount;
        this.limit = limit;
        this.voidExcess = voidExcess;
        this.supportsCustomMaxAmount = supportsCustomMaxAmount;
    }

    @Nullable
    public ItemMeta getStoredItemMeta() {
        return this.storedItemMeta;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public synchronized void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Applies an insertion atomically for this storage cell. Quantum storage can be reached by
     * menus, ticks and network requests in close succession; keeping the amount mutation together
     * prevents a stale read from producing a duplicate withdrawal.
     */
    public synchronized int increaseAmount(int amount) {
        long total = (long) this.amount + (long) amount;
        if (total > this.limit) {
            this.amount = this.limit;
            if (!this.voidExcess) {
                return (int) (total - this.limit);
            }
        } else {
            this.amount = this.amount + amount;
        }
        return 0;
    }

    public synchronized void reduceAmount(int amount) {
        this.amount = Math.max(0, this.amount - amount);
    }

    public int getLimit() {
        return limit;
    }

    public synchronized boolean isVoidExcess() {
        return voidExcess;
    }

    public boolean supportsCustomMaxAmount() {
        return supportsCustomMaxAmount;
    }

    public synchronized void setVoidExcess(boolean voidExcess) {
        this.voidExcess = voidExcess;
    }

    @Nullable
    public synchronized ItemStack withdrawItem(int amount) {
        if (this.getItemStack() == null || this.amount <= 0) {
            return null;
        }
        final ItemStack clone = this.getItemStack().clone();
        final int toGive = Math.max(0, Math.min(this.amount, amount));
        clone.setAmount(toGive);
        reduceAmount(toGive);
        return toGive > 0 ? clone : null;
    }

    @Nullable
    public synchronized ItemStack withdrawItem() {
        if (this.getItemStack() == null) {
            return null;
        }
        return withdrawItem(this.getItemStack().getMaxStackSize());
    }

    public void addMetaLore(ItemMeta itemMeta) {
        final List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        lore.add("");
        lore.add(Theme.CLICK_INFO + "Holding: " +
                     (this.getItemMeta() != null && this.getItemMeta().hasDisplayName() ? this.getItemMeta().getDisplayName() : this.getItemStack().getType().name())
        );
        lore.add(Theme.CLICK_INFO + "Amount: " + this.getAmount());
        itemMeta.setLore(lore);
    }

    public void updateMetaLore(ItemMeta itemMeta) {
        final List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        final String holding = Theme.CLICK_INFO + "Holding: " +
            (this.getItemMeta() != null && this.getItemMeta().hasDisplayName() ? this.getItemMeta().getDisplayName() : this.getItemStack().getType().name());
        final String amount = Theme.CLICK_INFO + "Amount: " + this.getAmount();

        if (lore.size() >= 2
            && lore.get(lore.size() - 2).contains("Holding:")
            && lore.get(lore.size() - 1).contains("Amount:")
        ) {
            lore.set(lore.size() - 2, holding);
            lore.set(lore.size() - 1, amount);
        } else {
            lore.add("");
            lore.add(holding);
            lore.add(amount);
        }
        itemMeta.setLore(lore);
    }
}
