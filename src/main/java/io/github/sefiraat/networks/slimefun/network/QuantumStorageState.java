package io.github.sefiraat.networks.slimefun.network;

import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class QuantumStorageState {

    private QuantumStorageState() {
    }

    static int parseStoredAmount(@Nullable String amountString, int maxAmount) {
        if (amountString == null) {
            return 0;
        }

        try {
            return Math.max(0, Math.min(Integer.parseInt(amountString), maxAmount));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static void stripDisplayLore(@Nonnull ItemMeta itemMeta) {
        if (!itemMeta.hasLore()) {
            return;
        }

        final List<String> lore = new ArrayList<>(itemMeta.getLore());
        final int size = lore.size();
        if (size < 2
            || !lore.get(size - 2).contains("Voiding:")
            || !lore.get(size - 1).contains("Amount:")
        ) {
            return;
        }

        lore.remove(size - 1);
        lore.remove(size - 2);
        if (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) {
            lore.remove(lore.size() - 1);
        }
        itemMeta.setLore(lore.isEmpty() ? null : lore);
    }
}
