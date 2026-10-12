package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.BlockBreakHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.items.ItemUtils;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import com.github.drakescraft_labs.slimefun4.utils.SlimefunUtils;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NetworkQuantumWorkbench extends SlimefunItem {

    private static final int[] BACKGROUND_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 14, 15, 16, 17, 18, 22, 24, 26, 27, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] RECIPE_SLOTS = {
        10, 11, 12, 19, 20, 21, 28, 29, 30
    };
    private static final int CRAFT_SLOT = 23;
    private static final int OUTPUT_SLOT = 25;

    private static final ItemStack CRAFT_BUTTON_STACK = ItemCreator.create(
        Material.CRAFTING_TABLE,
        Theme.CLICK_INFO + "Click to entangle",
        Theme.PASSIVE + "Shift-click to craft up to one stack"
    );

    private static final Map<ItemStack[], ItemStack> RECIPES = new HashMap<>();

    public static final RecipeType TYPE = new RecipeType(
        Keys.newKey("quantum-workbench"),
        Theme.themedItemStack(
            Material.BRAIN_CORAL_BLOCK,
            Theme.MACHINE,
            "Quantum Workbench",
            "Crafted using the Quantum Workbench."
        ),
        NetworkQuantumWorkbench::addRecipe
    );

    @ParametersAreNonnullByDefault
    public NetworkQuantumWorkbench(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler(getBlockBreakHandler());
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {
            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                addItem(CRAFT_SLOT, CRAFT_BUTTON_STACK, (p, slot, item, action) -> false);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                final SlimefunItem item = BlockStorage.check(block);
                return item != null
                    && item.canUse(player, false)
                    && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }
                return new int[0];
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuClickHandler(CRAFT_SLOT, (p, slot, item, action) -> {
                    craft(menu, action.isShiftClicked());
                    return false;
                });
            }
        };
    }

    public void craft(@Nonnull BlockMenu menu) {
        craft(menu, false);
    }

    public void craft(@Nonnull BlockMenu menu, boolean shiftClicked) {
        // Inventory changes and Slimefun's persistence snapshots share the server thread.
        if (!Bukkit.isPrimaryThread()) {
            return;
        }

        final ItemStack[] before = new ItemStack[RECIPE_SLOTS.length];
        final ItemStack[] remaining = new ItemStack[RECIPE_SLOTS.length];
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            before[i] = copy(menu.getItemInSlot(RECIPE_SLOTS[i]));
            remaining[i] = copy(before[i]);
        }
        final ItemStack outputBefore = copy(menu.getItemInSlot(OUTPUT_SLOT));
        ItemStack output = copy(outputBefore);
        int crafts = 0;

        for (Map.Entry<ItemStack[], ItemStack> entry : RECIPES.entrySet()) {
            if (!testRecipe(remaining, entry.getKey())) {
                continue;
            }
            while (crafts < (shiftClicked ? 64 : 1) && testRecipe(remaining, entry.getKey())) {
                final ItemStack crafted = prepareOutput(remaining, entry.getKey(), entry.getValue());
                if (isEmpty(crafted)) {
                    break;
                }
                final int capacity = Math.min(crafted.getMaxStackSize(), menu.toInventory().getMaxStackSize());
                final int currentAmount = isEmpty(output) ? 0 : output.getAmount();
                if ((!isEmpty(output) && !output.isSimilar(crafted))
                    || crafted.getAmount() > capacity - currentAmount) {
                    break;
                }

                // Stateful cells are upgraded individually and never merged with another cell.
                final boolean stateful = hasQuantumData(remaining[4]);
                if (stateful && !isEmpty(output)) {
                    break;
                }
                output = crafted.clone();
                output.setAmount(currentAmount + crafted.getAmount());
                for (int i = 0; i < remaining.length; i++) {
                    if (!isEmpty(entry.getKey()[i])) {
                        ItemUtils.consumeItem(remaining[i], entry.getKey()[i].getAmount(), true);
                        if (isEmpty(remaining[i])) {
                            remaining[i] = null;
                        }
                    }
                }
                crafts++;
                if (stateful) {
                    break;
                }
            }
            break;
        }

        if (crafts == 0 || !Objects.equals(outputBefore, menu.getItemInSlot(OUTPUT_SLOT))) {
            return;
        }
        for (int i = 0; i < before.length; i++) {
            if (!Objects.equals(before[i], menu.getItemInSlot(RECIPE_SLOTS[i]))) {
                return;
            }
        }
        // Everything is prepared before mutating live slots. This preset has no change hook;
        // bypass hooks during commit so callbacks cannot observe or interrupt a partial craft.
        for (int i = 0; i < remaining.length; i++) {
            menu.replaceExistingItem(RECIPE_SLOTS[i], remaining[i], false);
        }
        menu.replaceExistingItem(OUTPUT_SLOT, output, false);
        menu.markDirty();
    }

    private ItemStack prepareOutput(ItemStack[] inputs, ItemStack[] recipe, ItemStack result) {
        if (isEmpty(result)) {
            return null;
        }
        final ItemStack crafted = result.clone();
        for (int i = 0; i < inputs.length; i++) {
            if (i != 4 && hasQuantumData(inputs[i])) {
                return null;
            }
        }
        final ItemStack core = inputs[4];
        if (!hasQuantumData(core)) {
            return crafted;
        }
        if (core.getAmount() != 1 || recipe[4].getAmount() != 1 || crafted.getAmount() != 1
            || !(SlimefunItem.getByItem(core) instanceof NetworkQuantumStorage)
            || !(SlimefunItem.getByItem(crafted) instanceof NetworkQuantumStorage upgraded)) {
            return null;
        }
        try {
            final ItemMeta oldMeta = core.getItemMeta();
            final QuantumCache oldCache = DataTypeMethods.getCustom(
                oldMeta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);
            if (oldCache == null || oldCache.getAmount() < 0 || oldCache.getAmount() > upgraded.getMaxAmount()
                || (isEmpty(oldCache.getItemStack()) && oldCache.getAmount() != 0)) {
                return null;
            }
            final ItemMeta newMeta = crafted.getItemMeta();
            // Copy the complete nested PDC, including flags and extension keys, changing only capacity.
            final PersistentDataContainer data = oldMeta.getPersistentDataContainer().get(
                Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.TAG_CONTAINER);
            final PersistentDataContainer copy = newMeta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
            data.copyTo(copy, true);
            copy.set(PersistentQuantumStorageType.MAX_AMOUNT, PersistentDataType.INTEGER, upgraded.getMaxAmount());
            newMeta.getPersistentDataContainer().set(Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.TAG_CONTAINER, copy);
            if (!isEmpty(oldCache.getItemStack())) {
                oldCache.addMetaLore(newMeta);
            }
            crafted.setItemMeta(newMeta);
            return crafted;
        } catch (RuntimeException exception) {
            // Invalid or incompatible PDC must leave the original cell and ingredients untouched.
            return null;
        }
    }

    private static boolean hasQuantumData(ItemStack stack) {
        return !isEmpty(stack) && stack.hasItemMeta()
            && stack.getItemMeta().getPersistentDataContainer().has(Keys.QUANTUM_STORAGE_INSTANCE);
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    private boolean testRecipe(ItemStack[] input, ItemStack[] recipe) {
        if (recipe.length != input.length) {
            return false;
        }
        boolean hasIngredient = false;
        for (int test = 0; test < recipe.length; test++) {
            final ItemStack inputItem = input[test];
            final ItemStack recipeItem = recipe[test];
            final boolean inputEmpty = isEmpty(inputItem);
            final boolean recipeEmpty = isEmpty(recipeItem);
            if (inputEmpty && recipeEmpty) {
                continue;
            }
            if (inputEmpty || recipeEmpty) {
                return false;
            }
            hasIngredient = true;
            if (!SlimefunUtils.isItemSimilar(inputItem, recipeItem, true, true, false)) {
                return false;
            }
        }
        return hasIngredient;
    }

    private BlockBreakHandler getBlockBreakHandler() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack itemStack, List<ItemStack> drops) {
                BlockMenu menu = BlockStorage.getInventory(event.getBlock());
                if (menu != null) {
                    menu.dropItems(menu.getLocation(), RECIPE_SLOTS);
                    menu.dropItems(menu.getLocation(), OUTPUT_SLOT);
                }
            }
        };
    }

    public static void addRecipe(ItemStack[] input, ItemStack output) {
        RECIPES.put(input, output);
    }
}
