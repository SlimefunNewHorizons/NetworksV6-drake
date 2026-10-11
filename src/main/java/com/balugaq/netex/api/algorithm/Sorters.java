package com.balugaq.netex.api.algorithm;

import com.balugaq.netex.api.data.StorageUnitData;
import com.balugaq.netex.api.helpers.Icon;
import com.ytdd9527.networksexpansion.implementation.machines.managers.DrawerManager;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import cl.jackstar.networks.compat.TextoItems;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@NullMarked
public class Sorters {
    /*
     * Un Collator por hilo. Antes cada comparador tenia un unico Collator compartido, y
     * RuleBasedCollator.compare es synchronized: todos los grids y managers que ordenaban a la vez
     * desde los tickers async se esperaban unos a otros.
     */
    private static final ThreadLocal<Collator> COLLATOR =
        ThreadLocal.withInitial(() -> Collator.getInstance(Locale.CHINA));

    public static final String NO_ITEM = TextoItems.nombreVisible(Icon.QUANTUM_STORAGE_NO_ITEM);

    public static final Comparator<Map.Entry<ItemStack, Long>> ITEMSTACK_ALPHABETICAL_SORT = Comparator.comparing(
        entry -> alphabeticalKey(entry.getKey()),
        Sorters::compareText);
    public static final Comparator<Map.Entry<ItemStack, Long>> ITEMSTACK_NUMERICAL_SORT = Map.Entry.comparingByValue();
    public static final Comparator<Map.Entry<ItemStack, Long>> ITEMSTACK_ADDON_SORT = Comparator.comparing(
        entry -> addonKey(entry.getKey()),
        Sorters::compareText);

    public static final Comparator<? super BarrelIdentity> BARREL_ALPHABETICAL_SORT = Comparator.comparing(
        barrel -> {
            ItemStack itemStack = barrel.getItemStack();
            if (itemStack == null) {
                return NO_ITEM;
            }
            return alphabeticalKey(itemStack);
        },
        Sorters::compareText);
    public static final Comparator<BarrelIdentity> BARREL_NUMERICAL_SORT =
        Comparator.comparingLong(BarrelIdentity::getAmount);
    public static final Comparator<? super StorageUnitData> DRAWER_ALPHABETICAL_SORT = Comparator.comparing(
        data -> {
            ItemStack itemStack = DrawerManager.getItemStack(data);
            if (itemStack == null) {
                return NO_ITEM;
            }
            return alphabeticalKey(itemStack);
        },
        Sorters::compareText);
    public static final Comparator<StorageUnitData> DRAWER_NUMERICAL_SORT =
        Comparator.comparingLong(StorageUnitData::getTotalAmountLong);

    private static int compareText(String a, String b) {
        return COLLATOR.get().compare(a, b);
    }

    private static String alphabeticalKey(ItemStack itemStack) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
        if (slimefunItem != null) {
            return TextUtil.stripColor(slimefunItem.getItemName());
        } else {
            return TextUtil.stripColor(TextoItems.nombreVisible(itemStack));
        }
    }

    private static String addonKey(ItemStack itemStack) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
        if (slimefunItem != null) {
            return TextUtil.stripColor(slimefunItem.getAddon().getName());
        } else {
            return "Minecraft";
        }
    }

    /**
     * Ordena entradas de items con el mismo resultado que {@code entries.sorted(comparator).toList()}.
     *
     * Con los comparadores alfabetico y por addon, Comparator.comparing recalcula la clave en cada
     * comparacion: SlimefunItem.getByItem, stripColor y el nombre visible, unas n*log(n) veces. En un
     * grid con 2000 items son decenas de miles por actualizacion. Aqui la clave de cada entrada se
     * calcula una sola vez y se convierte a CollationKey, que compara igual que el Collator.
     */
    public static List<Map.Entry<ItemStack, Long>> sortItemEntries(
        Stream<Map.Entry<ItemStack, Long>> entries, Comparator<? super Map.Entry<ItemStack, Long>> comparator) {
        final Function<ItemStack, String> keyOf;
        if (comparator == ITEMSTACK_ALPHABETICAL_SORT) {
            keyOf = Sorters::alphabeticalKey;
        } else if (comparator == ITEMSTACK_ADDON_SORT) {
            keyOf = Sorters::addonKey;
        } else {
            return entries.sorted(comparator).toList();
        }

        final Collator collator = COLLATOR.get();
        record Keyed(Map.Entry<ItemStack, Long> entry, CollationKey key) {
        }
        return entries
            .map(entry -> new Keyed(entry, collator.getCollationKey(keyOf.apply(entry.getKey()))))
            .sorted(Comparator.comparing(Keyed::key))
            .map(Keyed::entry)
            .toList();
    }
}
