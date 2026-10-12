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
import java.util.concurrent.ConcurrentHashMap;

public class NetworkAutoCrafter extends NetworkObject {

    public enum CrafterStatus {
        STANDBY,
        OPERATIONAL,
        MISSING_MATERIALS,
        INSUFFICIENT_POWER,
        OUTPUT_FULL
    }

    private static final int[] BACKGROUND_SLOTS = new int[]{
        3, 4, 5, 12, 14, 21, 22, 23
    };
    public static final int STATUS_SLOT = 13;

    private static final int[] BLUEPRINT_BACKGROUND = new int[]{0, 1, 2, 9, 11, 18, 19, 20};
    private static final int[] OUTPUT_BACKGROUND = new int[]{6, 7, 8, 15, 17, 24, 25, 26};

    private static final int BLUEPRINT_SLOT = 10;
    private static final int OUTPUT_SLOT = 16;

    public static final ItemStack BLUEPRINT_BACKGROUND_STACK = ItemCreator.create(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "Crafting Blueprint"
    );

    public static final ItemStack OUTPUT_BACKGROUND_STACK = ItemCreator.create(
        Material.GREEN_STAINED_GLASS_PANE, Theme.PASSIVE + "Output"
    );

    private final int chargePerCraft;
    private final boolean withholding;
    private final boolean stackBlueprints;

    private static final Map<Location, BlueprintInstance> INSTANCE_MAP = new ConcurrentHashMap<>();

    public NetworkAutoCrafter(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int chargePerCraft, boolean withholding) {
        this(itemGroup, item, recipeType, recipe, chargePerCraft, withholding, false);
    }

    /**
     * Advanced crafters may process a stack of identical blueprints in one
     * atomic operation. Standard crafters deliberately retain single-stack behavior.
     */
    public NetworkAutoCrafter(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int chargePerCraft, boolean withholding, boolean stackBlueprints) {
        super(itemGroup, item, recipeType, recipe, NodeType.CRAFTER);

        this.chargePerCraft = chargePerCraft;
        this.withholding = withholding;
        this.stackBlueprints = stackBlueprints;

        this.getSlotsToDrop().add(BLUEPRINT_SLOT);
        this.getSlotsToDrop().add(OUTPUT_SLOT);

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
                        craftPreFlight(blockMenu);
                    }
                }
            }
        );
    }

    public static ItemStack getStatusIcon(@Nonnull CrafterStatus status, @Nullable String summary, @Nullable List<String> details) {
        Material mat;
        String name;
        switch (status) {
            case OPERATIONAL -> {
                mat = Material.LIME_STAINED_GLASS_PANE;
                name = Theme.SUCCESS + "🟢 Operativo";
            }
            case MISSING_MATERIALS -> {
                mat = Material.RED_STAINED_GLASS_PANE;
                name = Theme.ERROR + "🔴 Faltan Materiales";
            }
            case INSUFFICIENT_POWER -> {
                mat = Material.YELLOW_STAINED_GLASS_PANE;
                name = Theme.WARNING + "⚡ Energía Insuficiente";
            }
            case OUTPUT_FULL -> {
                mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                name = Theme.CLICK_INFO + "📦 Salida Llena / Red Saturada";
            }
            default -> {
                mat = Material.GRAY_STAINED_GLASS_PANE;
                name = Theme.PASSIVE + "⚪ En Espera";
            }
        }

        List<String> lore = new ArrayList<>();
        if (summary != null) {
            lore.add(summary);
        }
        if (details != null && !details.isEmpty()) {
            lore.add("");
            lore.addAll(details);
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

    public static List<String> checkMissingMaterials(@Nonnull NetworkRoot root, @Nonnull BlueprintInstance instance, int blueprintAmount) {
        List<String> missing = new ArrayList<>();
        Map<ItemStack, Integer> neededMap = new LinkedHashMap<>();
        for (ItemStack req : instance.getRecipeItems()) {
            if (req != null) {
                boolean matched = false;
                for (Map.Entry<ItemStack, Integer> entry : neededMap.entrySet()) {
                    if (StackUtils.itemsMatch(entry.getKey(), req)) {
                        entry.setValue(entry.getValue() + req.getAmount() * blueprintAmount);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    neededMap.put(req, req.getAmount() * blueprintAmount);
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

    private void updateStatus(@Nonnull BlockMenu blockMenu, @Nonnull CrafterStatus status, @Nullable String summary, @Nullable List<String> details) {
        // The status pane is only visible while a player has the menu open.
        // Avoid allocating lore and serializing an ItemStack for every machine tick
        // when nobody can observe the diagnostic.
        if (!blockMenu.hasViewer()) {
            return;
        }
        blockMenu.replaceExistingItem(STATUS_SLOT, getStatusIcon(status, summary, details));
    }

    protected void craftPreFlight(@Nonnull BlockMenu blockMenu) {
        releaseCache(blockMenu);

        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();

        if (!this.withholding) {
            flushOutputIntoNetwork(blockMenu, root);
        }

        final ItemStack blueprint = blockMenu.getItemInSlot(BLUEPRINT_SLOT);

        if (blueprint == null || blueprint.getType() == Material.AIR) {
            updateStatus(blockMenu, CrafterStatus.STANDBY, Theme.PASSIVE + "Inserta un Blueprint codificado.", null);
            return;
        }

        final SlimefunItem item = SlimefunItem.getByItem(blueprint);

        if (!(item instanceof CraftingBlueprint)) {
            updateStatus(blockMenu, CrafterStatus.STANDBY, Theme.WARNING + "Ítem no es un Blueprint válido.", null);
            return;
        }

        BlueprintInstance instance = INSTANCE_MAP.get(blockMenu.getLocation());

        if (instance == null) {
            final ItemMeta blueprintMeta = blueprint.getItemMeta();
            final Optional<BlueprintInstance> optional = DataTypeMethods.getOptionalCustom(blueprintMeta, Keys.BLUEPRINT_INSTANCE, PersistentCraftingBlueprintType.TYPE);

            if (optional.isEmpty()) {
                updateStatus(blockMenu, CrafterStatus.STANDBY, Theme.PASSIVE + "Blueprint en blanco o sin receta.", null);
                return;
            }

            instance = optional.get();
            setCache(blockMenu, instance);
        }

        final int blueprintAmount = getBlueprintAmount(blueprint);
        final long requiredCharge = getRequiredCharge(blueprintAmount);
        final long networkCharge = root.getRootPower();

        if (!hasSufficientPower(networkCharge, requiredCharge)) {
            List<String> powerLore = new ArrayList<>();
            powerLore.add(Theme.PASSIVE + "Energía de la Red: " + Theme.CLICK_INFO + networkCharge + " J");
            powerLore.add(Theme.PASSIVE + "Drenaje Requerido: " + Theme.ERROR + requiredCharge + " J");
            powerLore.add(Theme.PASSIVE + "Conecta más capacitores o generadores a la red.");
            updateStatus(blockMenu, CrafterStatus.INSUFFICIENT_POWER, Theme.WARNING + "Batería de red insuficiente.", powerLore);
            return;
        }

        final ItemStack output = blockMenu.getItemInSlot(OUTPUT_SLOT);

        if (!canFitOutput(output, instance.getItemStack(), blueprintAmount)) {
            List<String> fullLore = new ArrayList<>();
            fullLore.add(Theme.PASSIVE + "Ranura de salida llena.");
            fullLore.add(Theme.PASSIVE + "Retira los ítems o descarga a la red.");
            updateStatus(blockMenu, CrafterStatus.OUTPUT_FULL, Theme.CLICK_INFO + "Espacio insuficiente para salida.", fullLore);
            return;
        }

        // tryCraft already performs the authoritative, atomic extraction. The
        // preflight scan exists solely to render a player-facing explanation;
        // running it for closed menus traverses the network a second time each tick.
        if (blockMenu.hasViewer()) {
            List<String> missing = checkMissingMaterials(root, instance, blueprintAmount);
            if (!missing.isEmpty()) {
                updateStatus(blockMenu, CrafterStatus.MISSING_MATERIALS, Theme.ERROR + "Faltan ingredientes en la red:", missing);
                return;
            }
        }

        if (tryCraft(blockMenu, instance, root, blueprintAmount)) {
            root.removeRootPower(Math.toIntExact(requiredCharge));
            List<String> opLore = new ArrayList<>();
            opLore.add(Theme.PASSIVE + "Salida: " + Theme.CLICK_INFO + getItemDisplayName(instance.getItemStack()));
            opLore.add(Theme.PASSIVE + "Drenaje: " + Theme.PASSIVE + requiredCharge + " J/craft");
            updateStatus(blockMenu, CrafterStatus.OPERATIONAL, Theme.SUCCESS + "Crafteando a ritmo regular...", opLore);
        }
    }

    private void flushOutputIntoNetwork(@Nonnull BlockMenu blockMenu, @Nonnull NetworkRoot root) {
        final ItemStack stored = blockMenu.getItemInSlot(OUTPUT_SLOT);
        if (stored == null || stored.getType() == Material.AIR) {
            return;
        }

        final int previousAmount = stored.getAmount();
        root.addItemStack0(blockMenu.getLocation(), stored);
        if (stored.getAmount() <= 0) {
            blockMenu.replaceExistingItem(OUTPUT_SLOT, null);
        }
        if (stored.getAmount() != previousAmount) {
            blockMenu.markDirty();
        }
    }

    @Override
    protected void clearCachedState(@Nonnull Location location) {
        INSTANCE_MAP.remove(location);
    }

    private boolean tryCraft(@Nonnull BlockMenu blockMenu, @Nonnull BlueprintInstance instance, @Nonnull NetworkRoot root, int blueprintAmount) {
        // Get the recipe input
        final ItemStack[] inputs = new ItemStack[9];

        final ItemRequest[] requests = new ItemRequest[9];
        boolean hasInput = false;
        for (int i = 0; i < 9; i++) {
            final ItemStack requested = instance.getRecipeItems()[i];
            if (requested != null) {
                requests[i] = new ItemRequest(requested, requested.getAmount() * blueprintAmount);
                hasInput = true;
            }
        }

        // Un blueprint vacío no debe producir resultados de la nada.
        if (!hasInput) {
            return false;
        }

        final ItemStack[] extracted = root.getItemStacks0(blockMenu.getLocation(), requests);
        if (extracted == null) {
            return false;
        }
        System.arraycopy(extracted, 0, inputs, 0, inputs.length);

        ItemStack crafted = SupportedRecipes.findRecipe(inputs).orElse(null);

        // If no slimefun recipe found, try a vanilla one
        if (crafted == null) {
            instance.generateVanillaRecipe(blockMenu.getLocation().getWorld());
            if (instance.getRecipe() == null) {
                returnItems(root, inputs, blockMenu.getLocation());
                return false;
            } else if (matchesBlueprintRecipe(instance.getRecipeItems(), inputs)) {
                setCache(blockMenu, instance);
                // CRÍTICO: clonar para no mutar el singleton de Bukkit Recipe
                crafted = instance.getRecipe().getResult().clone();
            }
        }

        // If no item crafted OR result doesn't fit, escape
        if (crafted == null || crafted.getType() == Material.AIR) {
            returnItems(root, inputs, blockMenu.getLocation());
            return false;
        }

        if (blueprintAmount > 1) {
            final long totalAmount = (long) crafted.getAmount() * blueprintAmount;
            if (totalAmount > crafted.getMaxStackSize()) {
                returnItems(root, inputs, blockMenu.getLocation());
                return false;
            }
            crafted.setAmount((int) totalAmount);
        }

        // Push item
        final Location location = blockMenu.getLocation().clone().add(0.5, 1.1, 0.5);
        if (root.isDisplayParticles()) {
            location.getWorld().spawnParticle(Particle.WAX_OFF, location, 0, 0, 4, 0);
        }
        final int craftedAmount = crafted.getAmount();
        final ItemStack leftover = blockMenu.pushItem(crafted, OUTPUT_SLOT);
        if (leftover != null && leftover.getAmount() > 0) {
            if (leftover.getAmount() == craftedAmount) {
                returnItems(root, inputs, blockMenu.getLocation());
                return false;
            }
            blockMenu.getLocation().getWorld().dropItemNaturally(blockMenu.getLocation(), leftover.clone());
        }
        return true;
    }

    private void returnItems(@Nonnull NetworkRoot root, @Nonnull ItemStack[] inputs, @Nonnull Location origin) {
        for (ItemStack input : inputs) {
            if (input != null && input.getAmount() > 0) {
                root.uncontrolAccessInput(origin);
                root.addItemStack0(origin, input);
                if (input.getAmount() > 0) {
                    // Network full — drop in-world so items are not silently lost
                    final org.bukkit.Location dropLoc = origin.clone().add(0.5, 1.0, 0.5);
                    dropLoc.getWorld().dropItem(dropLoc, input.clone());
                    input.setAmount(0);
                }
            }
        }
    }

    /**
     * Recipe identity is item/meta based. Amounts may be multiplied by an
     * advanced crafter and must not prevent an otherwise valid vanilla recipe.
     */
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

    static boolean hasSufficientPower(long availablePower, long requiredPower) {
        return availablePower >= requiredPower;
    }

    static long getRequiredCharge(int chargePerCraft, int blueprintAmount) {
        return Math.multiplyExact((long) chargePerCraft, blueprintAmount);
    }

    static boolean canFitOutput(@Nullable ItemStack currentOutput, @Nonnull ItemStack craftedOutput, int blueprintAmount) {
        if (blueprintAmount < 1) {
            return false;
        }

        final long totalAmount = (long) craftedOutput.getAmount() * blueprintAmount;
        if (totalAmount > craftedOutput.getMaxStackSize()) {
            return false;
        }
        if (currentOutput == null || currentOutput.getType() == Material.AIR) {
            return true;
        }
        return StackUtils.itemsMatch(craftedOutput, currentOutput)
            && currentOutput.getAmount() + totalAmount <= currentOutput.getMaxStackSize();
    }

    static boolean canFitOutput(@Nullable ItemStack currentOutput, @Nonnull ItemStack craftedOutput) {
        return canFitOutput(currentOutput, craftedOutput, 1);
    }

    private int getBlueprintAmount(@Nonnull ItemStack blueprint) {
        return this.stackBlueprints ? blueprint.getAmount() : 1;
    }

    private long getRequiredCharge(int blueprintAmount) {
        return getRequiredCharge(this.chargePerCraft, blueprintAmount);
    }

    public void releaseCache(@Nonnull BlockMenu blockMenu) {
        if (blockMenu.hasViewer()) {
            INSTANCE_MAP.remove(blockMenu.getLocation());
        }
    }

    public void setCache(@Nonnull BlockMenu blockMenu, @Nonnull BlueprintInstance blueprintInstance) {
        if (!blockMenu.hasViewer()) {
            INSTANCE_MAP.putIfAbsent(blockMenu.getLocation().clone(), blueprintInstance);
        }
    }


    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(BLUEPRINT_BACKGROUND_STACK, BLUEPRINT_BACKGROUND);
                drawBackground(OUTPUT_BACKGROUND_STACK, OUTPUT_BACKGROUND);
                addItem(STATUS_SLOT, getStatusIcon(CrafterStatus.STANDBY, Theme.PASSIVE + "Inserta un Blueprint codificado.", null), (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuClickHandler(STATUS_SLOT, (player, i, itemStack, clickAction) -> false);
                menu.addMenuOpeningHandler(player -> craftPreFlight(menu));
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return NetworkSlimefunItems.NETWORK_AUTO_CRAFTER.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (NetworkAutoCrafter.this.withholding && flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }
                return new int[0];
            }
        };
    }
}
