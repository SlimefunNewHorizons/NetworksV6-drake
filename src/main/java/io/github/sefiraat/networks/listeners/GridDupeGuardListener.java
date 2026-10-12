package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.slimefun.network.grid.AbstractGrid;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bloquea clics de inventario abusivos en grids (#230, dupe tipo COLLECT_TO_CURSOR / middle).
 */
public class GridDupeGuardListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@Nonnull InventoryClickEvent event) {
        if (gridOf(event.getInventory().getHolder()) == null) {
            return;
        }

        final ClickType click = event.getClick();
        final InventoryAction action = event.getAction();

        if (click == ClickType.MIDDLE
                || click == ClickType.DOUBLE_CLICK
                || click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND
                || click == ClickType.CREATIVE
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.UNKNOWN
                || action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.CLONE_STACK) {
            event.setCancelled(true);
        }
    }

    // Un drag NO pasa por el MenuClickHandler de Slimefun: si reparte/recoge sobre los display-slots
    // (clones de visualización con lore) evade retrieveItem() y puede dupear. Se cancela cualquier drag
    // que toque el inventario superior del grid EXCEPTO si afecta única y exclusivamente al input slot
    // (slot real donde el jugador deposita items para importarlos a la red = transporte legítimo).
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@Nonnull InventoryDragEvent event) {
        final Inventory topInventory = event.getView().getTopInventory();
        final AbstractGrid grid = gridOf(topInventory.getHolder());
        if (grid == null) {
            return;
        }

        final int topSize = topInventory.getSize();
        final int inputSlot = grid.getInputSlot();
        for (int rawSlot : event.getRawSlots()) {
            // rawSlot < topSize => el drag toca el inventario superior (el grid).
            // Se permite solo si todos los slots tocados del top son el input slot.
            if (rawSlot < topSize && rawSlot != inputSlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Devuelve la instancia AbstractGrid si el holder es un BlockMenu de un grid; null en otro caso.
    @Nullable
    private AbstractGrid gridOf(@Nullable InventoryHolder holder) {
        if (!(holder instanceof BlockMenu blockMenu)) {
            return null;
        }
        final SlimefunItem slimefunItem = BlockStorage.check(blockMenu.getLocation());
        return slimefunItem instanceof AbstractGrid grid ? grid : null;
    }
}
