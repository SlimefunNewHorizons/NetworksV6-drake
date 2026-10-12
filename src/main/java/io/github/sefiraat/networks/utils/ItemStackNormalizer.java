package io.github.sefiraat.networks.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;

/**
 * Converts specialized Slimefun stacks into plain Bukkit stacks before persistence.
 */
public final class ItemStackNormalizer {

    private ItemStackNormalizer() {
    }

    @Nonnull
    public static ItemStack normalize(@Nonnull ItemStack itemStack) {
        final ItemStack normalized = new ItemStack(itemStack.getType(), itemStack.getAmount());
        final ItemMeta itemMeta = itemStack.getItemMeta();

        // Reapply the full component-backed metadata to a guaranteed base Bukkit stack.
        if (itemMeta != null) {
            normalized.setItemMeta(itemMeta.clone());
        }

        return normalized;
    }

    @Nonnull
    public static ItemStack[] normalizeRecipe(@Nonnull ItemStack[] recipeItems) {
        final ItemStack[] normalized = new ItemStack[recipeItems.length];

        for (int index = 0; index < recipeItems.length; index++) {
            final ItemStack itemStack = recipeItems[index];
            normalized[index] = itemStack == null ? null : normalize(itemStack);
        }

        return normalized;
    }
}
