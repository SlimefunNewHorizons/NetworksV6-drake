package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agrega cantidades por tipo de ítem real (evita colisiones de {@link ItemStack} como clave HashMap — #226 terracotta y variantes).
 */
public final class NetworkStackAggregator {

    private final Map<ItemStack, Integer> totals = new LinkedHashMap<>();

    public NetworkStackAggregator() {}

    /**
     * Suma {@code amount} al stack canónico que coincida, o crea entrada nueva (amount=1 en la clave).
     */
    public void add(@Nonnull ItemStack stack, int amount) {
        if (stack.getType().isAir() || amount <= 0) {
            return;
        }

        for (Map.Entry<ItemStack, Integer> entry : totals.entrySet()) {
            if (StackUtils.itemsMatch(new ItemStackCache(entry.getKey()), stack, true)) {
                long sum = (long) entry.getValue() + amount;
                entry.setValue(sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum);
                return;
            }
        }

        totals.put(StackUtils.getAsQuantity(stack, 1), amount);
    }

    @Nonnull
    public Map<ItemStack, Integer> asMap() {
        return totals;
    }
}
