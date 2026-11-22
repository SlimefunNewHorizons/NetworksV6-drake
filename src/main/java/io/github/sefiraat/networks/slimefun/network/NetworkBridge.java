package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflage;
import io.github.sefiraat.networks.slimefun.network.camouflage.CableCamouflageMenu;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class NetworkBridge extends NetworkObject {

    public NetworkBridge(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput, NodeType.BRIDGE);
        addItemHandler((BlockUseHandler) this::onRightClick);
    }

    /**
     * Determina si una interaccion con Shift+Click Derecho debe cederse a la colocacion
     * vanilla/Slimefun de bloques en lugar de abrir la GUI o aplicar camuflaje.
     */
    public static boolean shouldAllowSneakPlacement(@Nonnull Player player, @Nullable ItemStack eventItem) {
        return shouldAllowSneakPlacement(
            player.isSneaking(),
            player.getInventory().getItemInMainHand(),
            player.getInventory().getItemInOffHand(),
            eventItem
        );
    }

    public static boolean shouldAllowSneakPlacement(
            boolean sneaking,
            @Nullable ItemStack mainHand,
            @Nullable ItemStack offHand,
            @Nullable ItemStack eventItem
    ) {
        if (!sneaking) {
            return false;
        }
        return isPlaceableBlock(eventItem) || isPlaceableBlock(mainHand) || isPlaceableBlock(offHand);
    }

    public static boolean isPlaceableBlock(@Nullable ItemStack item) {
        return item != null && !item.getType().isAir() && item.getType().isBlock();
    }

    private void onRightClick(@Nonnull PlayerRightClickEvent event) {
        final Optional<Block> blockOptional = event.getClickedBlock();
        if (blockOptional.isEmpty()) {
            return;
        }

        final Block block = blockOptional.get();
        final Player player = event.getPlayer();
        final ItemStack itemInHand = event.getItem();

        // Shift + Click Derecho con un bloque colocable (mainhand/offhand):
        // NO cancelar ni abrir GUI, permitir colocar bloques/bridges encima normalmente.
        if (shouldAllowSneakPlacement(player, itemInHand)) {
            return;
        }

        if (!Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK)) {
            return;
        }

        // Si el jugador (sin agacharse) hace clic derecho con un bloque solido apto para camuflaje:
        if (!player.isSneaking() && itemInHand != null && !itemInHand.getType().isAir() && CableCamouflage.allowed(itemInHand)) {
            event.cancel();
            ItemStack previous = CableCamouflage.apply(block, itemInHand, player);
            if (player.getGameMode() != GameMode.CREATIVE) {
                itemInHand.subtract(1);
            }
            if (previous != null) {
                var leftovers = player.getInventory().addItem(previous);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            player.sendMessage("§a[Networks] §7Cable camuflado con §b" + itemInHand.getType().name() + "§7.");
            return;
        }

        // Solo abrir el menu de camuflaje con mano vacia (evita secuestrar colocacion de otros bloques/nodos).
        if (itemInHand == null || itemInHand.getType().isAir()) {
            event.cancel();
            new CableCamouflageMenu(block, player).open();
        }
    }

    @Override
    protected void onBreak(@Nonnull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final Material camoMat = CableCamouflage.getCamouflage(location);

        if (camoMat != null) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                location.getWorld().dropItemNaturally(location, new ItemStack(camoMat, 1));
            }
            BlockStorage.addBlockInfo(location, CableCamouflage.KEY_CAMO, null);
        }

        super.onBreak(event);
    }
}
