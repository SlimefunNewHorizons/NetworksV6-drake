package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.SupportedRecipes;
import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.tools.CraftingBlueprint;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.StringUtils;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentCraftingBlueprintType;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.handlers.BlockTicker;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NetworkCrafterMatrix extends NetworkObject {

    public static final int[] BLUEPRINT_SLOTS = new int[]{
        10, 11, 12, 19, 20, 21, 28, 29, 30
    };

    public static final int[] OUTPUT_SLOTS = new int[]{
        14, 15, 16, 23, 24, 25, 32, 33, 34
    };

    public static final int STATUS_SLOT = 22;

    public static final int[] BLUEPRINT_BORDER = new int[]{
        0, 1, 2, 3,
        9,
        18,
        27,
        36, 37, 38, 39,
        45, 46, 47, 48
    };

    public static final int[] OUTPUT_BORDER = new int[]{
        5, 6, 7, 8,
        17,
        26,
        35,
        41, 42, 43, 44,
        50, 51, 52, 53
    };

    public static final int[] DIVIDER_BORDER = new int[]{
        4, 13, 31, 40, 49
    };

    public static final ItemStack BLUEPRINT_BORDER_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Matriz de Blueprints (9 Slots)"
    );

    public static final ItemStack OUTPUT_BORDER_STACK = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE, Theme.PASSIVE + "Buffer de Salida Matrix (9 Slots)"
    );

    public static final ItemStack DIVIDER_STACK = ItemCreator.create(
        Material.PURPLE_STAINED_GLASS_PANE, Theme.CLICK_INFO + "Matrix Core Divider"
    );

    private final int chargePerCraft;

    public NetworkCrafterMatrix(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int chargePerCraft) {
        super(itemGroup, item, recipeType, recipe, NodeType.CRAFTER);

        this.chargePerCraft = chargePerCraft;

        for (int slot : BLUEPRINT_SLOTS) {
            this.getSlotsToDrop().add(slot);
        }
        for (int slot : OUTPUT_SLOTS) {
            this.getSlotsToDrop().add(slot);
        }

        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block block, SlimefunItem slimefunItem, Config config) {
                    BlockMenu blockMenu = BlockStorage.getInventory(block);
                    if (blockMenu != null) {
                        addToRegistry(block);
                        processMatrix(blockMenu);
                    }
                }
            }
        );
    }

    public static ItemStack getMatrixStatusIcon(int activeBlueprints, int craftsProcessed, long networkPower, long requiredPower, @Nullable List<String> missing) {
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (activeBlueprints == 0) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            name = Theme.PASSIVE + "⚪ Matrix en Espera";
            lore.add(Theme.PASSIVE + "Coloca hasta 9 Blueprints codificados");
            lore.add(Theme.PASSIVE + "en la matriz izquierda para comenzar.");
        } else if (networkPower < requiredPower) {
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            name = Theme.WARNING + "⚡ Energía Insuficiente";
            lore.add(Theme.PASSIVE + "Energía de la Red: " + Theme.CLICK_INFO + networkPower + " J");
            lore.add(Theme.PASSIVE + "Drenaje Requerido: " + Theme.ERROR + requiredPower + " J");
            lore.add(Theme.PASSIVE + "Conecta más capacitores o reactores a la red.");
        } else if (missing != null && !missing.isEmpty()) {
            mat = Material.RED_STAINED_GLASS_PANE;
            name = Theme.ERROR + "🔴 Faltan Materiales (" + activeBlueprints + " Blueprints)";
            lore.add(Theme.PASSIVE + "Ingredientes requeridos no encontrados:");
            lore.add("");
            lore.addAll(missing);
        } else if (craftsProcessed > 0) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            name = Theme.SUCCESS + "🟢 Matrix Operativa";
            lore.add(Theme.PASSIVE + "Blueprints Activos: " + Theme.CLICK_INFO + activeBlueprints + "/9");
            lore.add(Theme.PASSIVE + "Crafteos en Ciclo: " + Theme.CLICK_INFO + craftsProcessed);
            lore.add(Theme.PASSIVE + "Consumo: " + Theme.PASSIVE + (requiredPower * craftsProcessed) + " J");
        } else {
            mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            name = Theme.CLICK_INFO + "📦 Salida Saturada / En Espera";
            lore.add(Theme.PASSIVE + "Blueprints Activos: " + Theme.CLICK_INFO + activeBlueprints + "/9");
            lore.add(Theme.PASSIVE + "Espacio insuficiente en buffer de salida.");
        }

        return ItemCreator.create(mat, name, lore.toArray(new String[0]));
    }

    public static String getItemDisplayName(@Nullable ItemStack item) {
        if (item == null) {
            return "Aire";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        return StringUtils.toTitleCase(item.getType().name());
    }

    public void processMatrix(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();

        // 1. Descarga automática de outputs a la red
        flushOutputsIntoNetwork(blockMenu, root);

        int activeBlueprints = 0;
        int craftsProcessed = 0;
        List<String> missingMaterials = new ArrayList<>();

        for (int blueprintSlot : BLUEPRINT_SLOTS) {
            final ItemStack blueprint = blockMenu.getItemInSlot(blueprintSlot);
            if (blueprint == null || blueprint.getType() == Material.AIR) {
                continue;
            }

            final SlimefunItem item = SlimefunItem.getByItem(blueprint);
            if (!(item instanceof CraftingBlueprint)) {
                continue;
            }

            final ItemMeta blueprintMeta = blueprint.getItemMeta();
            final Optional<BlueprintInstance> optional = DataTypeMethods.getOptionalCustom(blueprintMeta, Keys.BLUEPRINT_INSTANCE, PersistentCraftingBlueprintType.TYPE);
            if (optional.isEmpty()) {
                continue;
            }

            activeBlueprints++;
            final BlueprintInstance instance = optional.get();
            final long networkCharge = root.getRootPower();

            if (networkCharge < this.chargePerCraft) {
                continue;
            }

            // Chequeo previo de espacio en outputs
            if (!canFitOutputInAnySlot(blockMenu, instance.getItemStack())) {
                continue;
            }

            // Comprobación de ingredientes
            List<String> slotMissing = checkMissingMaterials(root, instance);
            if (!slotMissing.isEmpty()) {
                for (String m : slotMissing) {
                    if (!missingMaterials.contains(m) && missingMaterials.size() < 6) {
                        missingMaterials.add(m);
                    }
                }
                continue;
            }

            if (tryCraftMatrix(blockMenu, instance, root)) {
                root.removeRootPower(this.chargePerCraft);
                craftsProcessed++;
            }
        }

        // Actualizar Monitor Central (Slot 22)
        blockMenu.replaceExistingItem(
            STATUS_SLOT,
            getMatrixStatusIcon(activeBlueprints, craftsProcessed, root.getRootPower(), this.chargePerCraft, missingMaterials)
        );
    }

    private void flushOutputsIntoNetwork(@Nonnull BlockMenu blockMenu, @Nonnull NetworkRoot root) {
        for (int outputSlot : OUTPUT_SLOTS) {
            final ItemStack stored = blockMenu.getItemInSlot(outputSlot);
            if (stored != null && stored.getType() != Material.AIR) {
                final int prev = stored.getAmount();
                root.addItemStack0(blockMenu.getLocation(), stored);
                if (stored.getAmount() <= 0) {
                    blockMenu.replaceExistingItem(outputSlot, null);
                }
                if (stored.getAmount() != prev) {
                    blockMenu.markDirty();
                }
            }
        }
    }

    private boolean canFitOutputInAnySlot(@Nonnull BlockMenu blockMenu, @Nonnull ItemStack result) {
        for (int outputSlot : OUTPUT_SLOTS) {
            final ItemStack current = blockMenu.getItemInSlot(outputSlot);
            if (current == null || current.getType() == Material.AIR) {
                return true;
            }
            if (StackUtils.itemsMatch(result, current) && current.getAmount() + result.getAmount() <= current.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private List<String> checkMissingMaterials(@Nonnull NetworkRoot root, @Nonnull BlueprintInstance instance) {
        List<String> missing = new ArrayList<>();
        Map<ItemStack, Integer> neededMap = new LinkedHashMap<>();
        for (ItemStack req : instance.getRecipeItems()) {
            if (req != null) {
                boolean matched = false;
                for (Map.Entry<ItemStack, Integer> entry : neededMap.entrySet()) {
                    if (StackUtils.itemsMatch(entry.getKey(), req)) {
                        entry.setValue(entry.getValue() + req.getAmount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    neededMap.put(req, req.getAmount());
                }
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : neededMap.entrySet()) {
            int needed = entry.getValue();
            int available = root.getAmount(entry.getKey());
            if (available < needed) {
                missing.add(Theme.PASSIVE + "• Falta: " + Theme.ERROR + (needed - available) + "x " + getItemDisplayName(entry.getKey()));
            }
        }
        return missing;
    }

    private boolean tryCraftMatrix(@Nonnull BlockMenu blockMenu, @Nonnull BlueprintInstance instance, @Nonnull NetworkRoot root) {
        final ItemStack[] inputs = new ItemStack[9];
        final ItemRequest[] requests = new ItemRequest[9];
        boolean hasInput = false;

        for (int i = 0; i < 9; i++) {
            final ItemStack requested = instance.getRecipeItems()[i];
            if (requested != null) {
                requests[i] = new ItemRequest(requested, requested.getAmount());
                hasInput = true;
            }
        }

        if (!hasInput) {
            return false;
        }

        final ItemStack[] extracted = root.getItemStacks0(blockMenu.getLocation(), requests);
        if (extracted == null) {
            return false;
        }
        System.arraycopy(extracted, 0, inputs, 0, inputs.length);

        ItemStack crafted = SupportedRecipes.findRecipe(inputs).orElse(null);

        if (crafted == null) {
            instance.generateVanillaRecipe(blockMenu.getLocation().getWorld());
            if (instance.getRecipe() == null) {
                returnItems(root, inputs, blockMenu.getLocation());
                return false;
            } else if (matchesBlueprintRecipe(instance.getRecipeItems(), inputs)) {
                crafted = instance.getRecipe().getResult().clone();
            }
        }

        if (crafted == null || crafted.getType() == Material.AIR) {
            returnItems(root, inputs, blockMenu.getLocation());
            return false;
        }

        final Location location = blockMenu.getLocation().clone().add(0.5, 1.1, 0.5);
        if (root.isDisplayParticles()) {
            location.getWorld().spawnParticle(Particle.WAX_OFF, location, 0, 0, 4, 0);
        }

        return pushIntoOutputs(blockMenu, crafted, root, inputs);
    }

    private boolean pushIntoOutputs(@Nonnull BlockMenu blockMenu, @Nonnull ItemStack item, @Nonnull NetworkRoot root, @Nonnull ItemStack[] inputs) {
        final int originalAmount = item.getAmount();
        ItemStack leftover = item;

        for (int slot : OUTPUT_SLOTS) {
            leftover = blockMenu.pushItem(leftover, slot);
            if (leftover == null || leftover.getAmount() <= 0) {
                return true;
            }
        }

        if (leftover.getAmount() == originalAmount) {
            returnItems(root, inputs, blockMenu.getLocation());
            return false;
        }

        blockMenu.getLocation().getWorld().dropItemNaturally(blockMenu.getLocation(), leftover.clone());
        return true;
    }

    private void returnItems(@Nonnull NetworkRoot root, @Nonnull ItemStack[] inputs, @Nonnull Location origin) {
        for (ItemStack input : inputs) {
            if (input != null && input.getAmount() > 0) {
                root.uncontrolAccessInput(origin);
                root.addItemStack0(origin, input);
                if (input.getAmount() > 0) {
                    final org.bukkit.Location dropLoc = origin.clone().add(0.5, 1.0, 0.5);
                    dropLoc.getWorld().dropItem(dropLoc, input.clone());
                    input.setAmount(0);
                }
            }
        }
    }

    private static boolean matchesBlueprintRecipe(@Nonnull ItemStack[] recipeItems, @Nonnull ItemStack[] inputs) {
        if (recipeItems.length != inputs.length) {
            return false;
        }
        for (int slot = 0; slot < recipeItems.length; slot++) {
            if (!StackUtils.itemsMatch(recipeItems[slot], inputs[slot])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BLUEPRINT_BORDER_STACK, BLUEPRINT_BORDER);
                drawBackground(OUTPUT_BORDER_STACK, OUTPUT_BORDER);
                drawBackground(DIVIDER_STACK, DIVIDER_BORDER);
                addItem(STATUS_SLOT, getMatrixStatusIcon(0, 0, 0, chargePerCraft, null), (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuClickHandler(STATUS_SLOT, (player, i, itemStack, clickAction) -> false);
                menu.addMenuOpeningHandler(player -> processMatrix(menu));
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_CRAFTER_MATRIX.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return OUTPUT_SLOTS;
                }
                return new int[0];
            }
        };
    }
}
