package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.SupportedRecipes;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.NetworksSlimefunItemStacks;
import io.github.sefiraat.networks.slimefun.tools.CraftingBlueprint;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.NetworkTransportUtils;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public class NetworkEncoder extends NetworkObject {

    private static final int[] BACKGROUND = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 15, 17, 18, 24, 25, 26, 27, 28, 29, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final int[] RECIPE_SLOTS = new int[]{
        12, 13, 14, 21, 22, 23, 30, 31, 32
    };

    private static final int[] BLUEPRINT_BACK = new int[]{
        10, 28
    };

    private static final int BLANK_BLUEPRINT_SLOT = 19;
    private static final int CLEAR_BUTTON_SLOT = 20;
    private static final int ENCODE_SLOT = 16;
    private static final int OUTPUT_SLOT = 34;

    private static final int CHARGE_COST = 20000;

    public static final ItemStack BLUEPRINT_BACK_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Blank Blueprint"
    );

    public static final ItemStack ENCODE_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Click to encode when valid"
    );

    public static final ItemStack CLEAR_BUTTON_STACK = ItemCreator.create(
        Material.RED_STAINED_GLASS_PANE,
        Theme.CLICK_INFO + "Limpiar / Formatear Blueprint",
        Theme.PASSIVE + "Haz clic para borrar la receta",
        Theme.PASSIVE + "del blueprint colocado en el slot y",
        Theme.PASSIVE + "devolverlo a Blueprint en blanco."
    );

    public NetworkEncoder(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ENCODER);
        for (int recipeSlot : RECIPE_SLOTS) {
            this.getSlotsToDrop().add(recipeSlot);
        }
        this.getSlotsToDrop().add(BLANK_BLUEPRINT_SLOT);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }


    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND);
                drawBackground(BLUEPRINT_BACK_STACK, BLUEPRINT_BACK);

                addItem(ENCODE_SLOT, ENCODE_STACK, (player, i, itemStack, clickAction) -> false);
                addItem(CLEAR_BUTTON_SLOT, CLEAR_BUTTON_STACK, (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuClickHandler(ENCODE_SLOT, (player, i, itemStack, clickAction) -> {
                    tryEncode(player, menu);
                    return false;
                });
                menu.addMenuClickHandler(CLEAR_BUTTON_SLOT, (player, i, itemStack, clickAction) -> {
                    tryClear(player, menu);
                    return false;
                });
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_RECIPE_ENCODER.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    public void tryClear(@Nonnull Player player, @Nonnull BlockMenu blockMenu) {
        final ItemStack blueprint = blockMenu.getItemInSlot(BLANK_BLUEPRINT_SLOT);
        if (blueprint == null || blueprint.getType() == Material.AIR
            || !(SlimefunItem.getByItem(blueprint) instanceof CraftingBlueprint)) {
            player.sendMessage(Theme.WARNING + "No hay ningún blueprint en la ranura para formatear.");
            return;
        }

        ItemStack blank = NetworksSlimefunItemStacks.CRAFTING_BLUEPRINT.clone();
        blank.setAmount(blueprint.getAmount());
        blockMenu.replaceExistingItem(BLANK_BLUEPRINT_SLOT, blank);
        blockMenu.markDirty();
        player.sendMessage(Theme.SUCCESS + "Blueprint formateado a estado en blanco.");
    }

    public void tryEncode(@Nonnull Player player, @Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        final long networkCharge = root.getRootPower();

        if (networkCharge < CHARGE_COST) {
            player.sendMessage(Theme.WARNING + "Not enough Network power to fulfill this task.");
            return;
        }

        final ItemStack outputStack = blockMenu.getItemInSlot(OUTPUT_SLOT);

        if (outputStack != null && outputStack.getType() != Material.AIR) {
            player.sendMessage(Theme.WARNING + "The output slot must be empty.");
            return;
        }

        ItemStack blueprint = blockMenu.getItemInSlot(BLANK_BLUEPRINT_SLOT);

        if (blueprint == null || blueprint.getType() == Material.AIR
            || !(SlimefunItem.getByItem(blueprint) instanceof CraftingBlueprint)) {
            player.sendMessage(Theme.WARNING + "You need to provide a crafting blueprint");
            return;
        }

        // Get the recipe input
        final ItemStack[] inputs = new ItemStack[RECIPE_SLOTS.length];
        int i = 0;
        for (int recipeSlot : RECIPE_SLOTS) {
            ItemStack stackInSlot = blockMenu.getItemInSlot(recipeSlot);
            if (stackInSlot != null) {
                inputs[i] = new ItemStack(stackInSlot);
                inputs[i].setAmount(1);
            }
            i++;
        }

        ItemStack crafted = SupportedRecipes.findRecipe(inputs).orElse(null);

        // If no slimefun recipe found, try a vanilla one
        if (crafted == null) {
            crafted = Bukkit.craftItem(inputs.clone(), player.getWorld(), player);
        }

        // If no item crafted OR result doesn't fit, escape
        if (crafted == null || crafted.getType() == Material.AIR) {
            player.sendMessage(Theme.WARNING + "Doesn't look like this is a valid recipe.");
            return;
        }

        final ItemStack blueprintClone = StackUtils.getAsQuantity(blueprint, 1);
        CraftingBlueprint.setBlueprint(blueprintClone, inputs, crafted);

        // Re-chequeo estrecho de OUTPUT justo antes de escribir: cierra el doble-click
        // reentrante en el mismo tick (el primer push ya dejó el slot ocupado).
        final ItemStack outputNow = blockMenu.getItemInSlot(OUTPUT_SLOT);
        if (outputNow != null && outputNow.getType() != Material.AIR) {
            player.sendMessage(Theme.WARNING + "The output slot must be empty.");
            return;
        }

        // Entregar PRIMERO. Solo se consume la fuente si la salida se entregó completa.
        final ItemStack leftover = NetworkTransportUtils.pushIntoMenu(blockMenu, blueprintClone, OUTPUT_SLOT);
        if (leftover != null && leftover.getType() != Material.AIR && leftover.getAmount() > 0) {
            // No cupo: nada consumido, el blueprint no se pierde.
            player.sendMessage(Theme.WARNING + "The output slot must be empty.");
            return;
        }

        // Entrega confirmada: consumimos 1 blueprint de la ranura.
        blueprint.setAmount(blueprint.getAmount() - 1);
        if (blueprint.getAmount() <= 0) {
            blockMenu.replaceExistingItem(BLANK_BLUEPRINT_SLOT, null);
        }

        // Safe ghost encoding: los ítems de receta se conservan intactos en la matriz 3x3
        blockMenu.markDirty();
        root.removeRootPower(CHARGE_COST);
    }
}
