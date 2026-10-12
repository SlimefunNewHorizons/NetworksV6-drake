package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.slimefun.network.NetworkAutoCrafter;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumWorkbench;
import io.github.sefiraat.networks.slimefun.network.NetworkVanillaGrabber;
import io.github.sefiraat.networks.slimefun.network.NetworkVanillaPusher;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Evita dupes al mover items desde menús externos hacia la red (#240, #235 y similares).
 */
public final class NetworkTransportUtils {

    private static final String NETWORK_ID_PREFIX = "NTW_";
    private static final int[] EMPTY_SLOTS = new int[0];

    private NetworkTransportUtils() {}

    /**
     * Inserta en la red el stack del slot origen y solo cuenta éxito si el amount bajó.
     *
     * @return cantidad realmente absorbida por la red
     */
    public static int pullIntoNetwork(
            @Nonnull NetworkRoot root,
            @Nonnull org.bukkit.Location accessor,
            @Nonnull BlockMenu sourceMenu,
            int slot) {
        final ItemStack stack = sourceMenu.getItemInSlot(slot);
        if (stack == null || stack.getType() == Material.AIR) {
            return 0;
        }

        final int before = stack.getAmount();
        root.addItemStack0(accessor, stack);
        final int consumed = before - stack.getAmount();

        if (consumed > 0) {
            if (stack.getAmount() <= 0) {
                sourceMenu.replaceExistingItem(slot, null);
            }
            sourceMenu.markDirty();
            root.uncontrolAccessInput(accessor);
        }

        return consumed;
    }

    /**
     * Extrae desde un inventario vanilla (barriles, cofres) con escritura explícita en el slot.
     */
    public static int pullFromInventory(
            @Nonnull NetworkRoot root,
            @Nonnull org.bukkit.Location accessor,
            @Nonnull Inventory inventory,
            int slot) {
        final ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.getType() == Material.AIR) {
            return 0;
        }

        final int before = stack.getAmount();
        root.addItemStack0(accessor, stack);
        final int consumed = before - stack.getAmount();

        if (consumed <= 0) {
            return 0;
        }

        final int remaining = before - consumed;
        if (remaining <= 0) {
            inventory.setItem(slot, null);
        } else {
            final ItemStack updated = stack.clone();
            updated.setAmount(remaining);
            inventory.setItem(slot, updated);
        }

        if (inventory.getHolder() instanceof org.bukkit.block.BlockState state) {
            state.update(true, false);
        } else if (inventory.getHolder() instanceof org.bukkit.block.DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof org.bukkit.block.BlockState left) {
                left.update(true, false);
            }
            if (doubleChest.getRightSide() instanceof org.bukkit.block.BlockState right) {
                right.update(true, false);
            }
        }

        root.uncontrolAccessInput(accessor);
        return consumed;
    }

    /**
     * Vacía el slot de salida interno de un vanilla grabber hacia la red (#235 jam en OUTPUT).
     */
    public static int flushMenuSlotToNetwork(
            @Nonnull NetworkRoot root,
            @Nonnull org.bukkit.Location accessor,
            @Nonnull BlockMenu menu,
            int slot) {
        return pullIntoNetwork(root, accessor, menu, slot);
    }

    /**
     * Inserta una copia del stack en un BlockMenu y sincroniza el amount restante en el stack original.
     * BlockMenu#pushItem puede guardar la misma referencia recibida si el slot esta vacio.
     */
    @Nullable
    public static ItemStack pushIntoMenu(
            @Nonnull BlockMenu targetMenu,
            @Nonnull ItemStack stack,
            int... slots) {
        if (stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            stack.setAmount(0);
            return null;
        }

        final int amountBefore = stack.getAmount();
        final ItemStack leftover = targetMenu.pushItem(stack.clone(), slots);
        final int amountAfter = leftover == null ? 0 : leftover.getAmount();

        if (amountAfter < amountBefore) {
            targetMenu.markDirty();
        }

        if (leftover == null || leftover.getType() == Material.AIR || leftover.getAmount() <= 0) {
            stack.setAmount(0);
            return null;
        }

        stack.setAmount(leftover.getAmount());
        return leftover;
    }

    public static boolean pushIntoMenuOrReturn(
            @Nonnull NetworkRoot root,
            @Nonnull org.bukkit.Location accessor,
            @Nonnull BlockMenu targetMenu,
            @Nonnull ItemStack stack,
            int... slots) {
        final int before = stack.getAmount();
        final ItemStack leftover = pushIntoMenu(targetMenu, stack, slots);
        final int moved = before - (leftover == null ? 0 : leftover.getAmount());

        if (leftover != null && leftover.getAmount() > 0) {
            root.uncontrolAccessInput(accessor);
            root.addItemStack0(accessor, leftover);
            // If the network is full and still can't absorb the leftover, drop it in-world
            if (leftover.getAmount() > 0) {
                accessor.getWorld().dropItem(accessor.clone().add(0.5, 0.5, 0.5), leftover.clone());
                leftover.setAmount(0);
            }
        }

        return moved > 0;
    }

    /**
     * Resuelve slots de transporte con compatibilidad para presets antiguos.
     * Algunos addons solo sobreescriben el metodo simple por flow; el overload con menu/item puede devolver vacio.
     */
    @Nonnull
    public static int[] getTransportSlots(
            @Nonnull BlockMenu menu,
            @Nonnull ItemTransportFlow flow,
            @Nullable ItemStack stack) {
        int[] slots = menu.getPreset().getSlotsAccessedByItemTransport(menu, flow, stack);
        if (slots == null || slots.length == 0) {
            slots = menu.getPreset().getSlotsAccessedByItemTransport(flow);
        }
        return slots == null ? EMPTY_SLOTS : slots;
    }

    /**
     * Nodos NTW cuyos slots de transporte contienen items reales de la máquina y no
     * una vista del almacenamiento de la red: pueden actuar como inventario externo
     * sin abrir el vector de dupe de #230/#240 (celdas y quantum storage siguen vetados).
     */
    private static final Class<?>[] TRANSPORT_SAFE_NODES = {
        NetworkVanillaGrabber.class,
        NetworkVanillaPusher.class,
        NetworkAutoCrafter.class,
        NetworkQuantumWorkbench.class
    };

    /**
     * No extraer desde otra máquina Networks (evita estados híbridos tipo #230).
     */
    public static boolean isExternalInventory(@Nullable BlockMenu menu) {
        if (menu == null) {
            return false;
        }
        final SlimefunItem item = BlockStorage.check(menu.getBlock());
        return isExternalInventoryType(item == null ? null : item.getId(), item == null ? null : item.getClass());
    }

    public static boolean isExternalInventoryType(@Nullable String itemId, @Nullable Class<?> itemType) {
        if (itemId == null || !itemId.startsWith(NETWORK_ID_PREFIX)) {
            return true;
        }
        if (itemType == null) {
            return false;
        }
        for (Class<?> safeNode : TRANSPORT_SAFE_NODES) {
            if (safeNode.isAssignableFrom(itemType)) {
                return true;
            }
        }
        return false;
    }
}
