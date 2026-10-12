package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.utils.StackUtils;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class SupportedRecipes {

    // Preserve registry order so overlapping recipes always resolve consistently.
    private static final Map<ItemStack[], ItemStack> RECIPES = new LinkedHashMap<>();

    public static void setup() {
        RECIPES.clear();
        for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            RecipeType recipeType = item.getRecipeType();
            if ((recipeType == RecipeType.ENHANCED_CRAFTING_TABLE || recipeType == RecipeType.MAGIC_WORKBENCH || recipeType == RecipeType.ARMOR_FORGE) && allowedRecipe(item)) {
                // Skip items with invalid recipe arrays (must be exactly 9 elements)
                ItemStack[] recipe = item.getRecipe();
                if (recipe == null || recipe.length != 9) {
                    continue;
                }

                ItemStack[] itemStacks = new ItemStack[9];
                int i = 0;
                for (ItemStack itemStack : recipe) {
                    if (itemStack == null) {
                        itemStacks[i] = null;
                    } else {
                        itemStacks[i] = new ItemStack(itemStack.clone());
                    }
                    i++;
                }
                addRecipe(itemStacks, item.getRecipeOutput());
            }
        }
    }

    public static Map<ItemStack[], ItemStack> getRecipes() {
        return RECIPES;
    }

    /**
     * Resolves an enhanced-crafting recipe using the same matching rules for encoders and crafters.
     */
    @Nonnull
    public static Optional<ItemStack> findRecipe(@Nonnull ItemStack[] input) {
        for (Map.Entry<ItemStack[], ItemStack> entry : RECIPES.entrySet()) {
            if (testRecipe(input, entry.getKey())) {
                return Optional.of(new ItemStack(entry.getValue()));
            }
        }

        return Optional.empty();
    }

    public static void addRecipe(@Nonnull ItemStack[] input, @Nonnull ItemStack output) {
        RECIPES.put(input, output);
    }

    public static boolean testRecipe(@Nonnull ItemStack[] input, @Nonnull ItemStack[] recipe) {
        if (input.length != 9 || recipe.length != 9) {
            return false;
        }

        for (int test = 0; test < recipe.length; test++) {
            if (!StackUtils.itemsMatch(input[test], recipe[test])) {
                return false;
            }
        }
        return true;
    }

    public static boolean allowedRecipe(@Nonnull SlimefunItem item) {
        return !(item instanceof SlimefunBackpack);
    }

}
