package io.github.sefiraat.networks.slimefun.network.camouflage;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.utils.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * [EN] Camouflage GUI for Network Bridges.
 * [ES] Menu interactivo de camuflaje para Network Bridges / cables de red.
 */
public class CableCamouflageMenu implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int STATUS_SLOT = 4;
    public static final int HELP_SLOT = 11;
    public static final int CAMO_SLOT = 13;
    public static final int REMOVE_SLOT = 15;
    public static final int CLOSE_SLOT = 22;

    private final Block block;
    private final Player player;
    private final Inventory inventory;

    public CableCamouflageMenu(@Nonnull Block block, @Nonnull Player player) {
        this.block = block;
        this.player = player;
        this.inventory = Bukkit.createInventory(
            this,
            SIZE,
            Component.text("Camuflaje de Cable // Networks", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)
        );
    }

    public void open() {
        draw();
        player.openInventory(this.inventory);
    }

    public void draw() {
        ItemStack header = ItemCreator.create(Material.CYAN_STAINED_GLASS_PANE, " ");
        ItemStack filler = ItemCreator.create(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < SIZE; i++) {
            this.inventory.setItem(i, i < 9 ? header : filler);
        }

        // Telemetria de conexion
        NodeDefinition nodeDef = NetworkStorage.getAllNetworkObjects().get(block.getLocation());
        NetworkRoot root = nodeDef != null && nodeDef.getNode() != null ? nodeDef.getNode().getRoot() : null;

        if (root != null) {
            this.inventory.setItem(STATUS_SLOT, ItemCreator.create(
                Material.LIME_STAINED_GLASS,
                "§a§l✔ Cable Conectado a la Red",
                "§7Controlador activo: §e" + root.getController().getBlockX() + ", " + root.getController().getBlockY() + ", " + root.getController().getBlockZ(),
                "§7Nodos en la red: §f" + root.getNodeCount() + " / " + root.getMaxNodes(),
                "§7Estado: §aOperativo al 100%"
            ));
        } else {
            this.inventory.setItem(STATUS_SLOT, ItemCreator.create(
                Material.RED_STAINED_GLASS,
                "§c§l✖ Cable Desconectado",
                "§7Este cable no alcanza un Network Controller.",
                "§eUne este puente con cables hasta un Controlador."
            ));
        }

        // Guia de uso
        this.inventory.setItem(HELP_SLOT, ItemCreator.create(
            Material.KNOWLEDGE_BOOK,
            "§6§lGuía de Camuflaje",
            "§7Oculta tus cables dentro de tu construcción.",
            "§7Coloca cualquier bloque sólido completo en el slot central.",
            "§7El cable tomará su textura pero seguirá funcionando al 100%.",
            "",
            "§eEl bloque NO se destruye: se almacena dentro del cable.",
            "§eAl retirar el camuflaje o picar el cable, lo recuperas.",
            "",
            "§8Válido: Cuarzo, ladrillos, piedras, madera, hormigón, etc."
        ));

        // Slot de camuflaje
        Material camo = CableCamouflage.getCamouflage(block.getLocation());
        if (camo == null) {
            this.inventory.setItem(CAMO_SLOT, ItemCreator.create(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "§e§lRanura de Camuflaje (Vacía)",
                "§7Haz clic con un bloque sólido en tu cursor,",
                "§7o pulsa Shift+Clic en un bloque de tu inventario."
            ));
            this.inventory.setItem(REMOVE_SLOT, filler);
        } else {
            this.inventory.setItem(CAMO_SLOT, ItemCreator.create(
                camo,
                "§b§lCamuflaje Activo: §f" + camo.name(),
                "§7El cable se muestra como este bloque.",
                "",
                "§e» Clic para recuperar el bloque.",
                "§e» Clic con otro bloque para cambiarlo."
            ));
            this.inventory.setItem(REMOVE_SLOT, ItemCreator.create(
                Material.GLASS,
                "§c§lQuitar Camuflaje",
                "§7Devuelve el bloque a tu inventario",
                "§7y el cable vuelve a ser de cristal blanco.",
                "",
                "§e» Clic para quitar."
            ));
        }

        this.inventory.setItem(CLOSE_SLOT, ItemCreator.create(Material.BARRIER, "§c§lCerrar"));
    }

    public void handleClick(@Nonnull InventoryClickEvent event) {
        int raw = event.getRawSlot();

        if (raw >= SIZE) {
            // Clic en el inventario del jugador
            if (event.isShiftClick()) {
                ItemStack current = event.getCurrentItem();
                if (current != null && CableCamouflage.allowed(current)) {
                    event.setCancelled(true);
                    ItemStack previous = CableCamouflage.apply(block, current, player);
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        current.subtract(1);
                    }
                    if (previous != null) {
                        giveOrDrop(previous);
                    }
                    draw();
                }
            }
            return;
        }

        event.setCancelled(true);

        switch (raw) {
            case CLOSE_SLOT -> player.closeInventory();
            case REMOVE_SLOT -> {
                ItemStack back = CableCamouflage.remove(block, player);
                if (back != null) {
                    giveOrDrop(back);
                    draw();
                }
            }
            case CAMO_SLOT -> {
                ItemStack cursor = event.getCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    ItemStack back = CableCamouflage.remove(block, player);
                    if (back != null) {
                        giveOrDrop(back);
                        draw();
                    }
                    return;
                }

                if (!CableCamouflage.allowed(cursor)) {
                    player.sendMessage("§c[Networks] Ese bloque no es apto para camuflar. Debe ser un bloque sólido completo.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }

                ItemStack previous = CableCamouflage.apply(block, cursor, player);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    cursor.subtract(1);
                    event.getView().setCursor(cursor);
                }
                if (previous != null) {
                    giveOrDrop(previous);
                }
                draw();
            }
            default -> {
            }
        }
    }

    public void handleDrag(@Nonnull InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void giveOrDrop(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        var leftovers = player.getInventory().addItem(stack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    @Nonnull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
