package io.github.sefiraat.networks.network;

import io.github.sefiraat.networks.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportedRecipesTest extends BukkitTestSupport {

    @Test
    void rejectsNonGridRecipesBeforeMatching() {
        final ItemStack[] input = new ItemStack[8];
        final ItemStack[] recipe = new ItemStack[9];

        assertFalse(SupportedRecipes.testRecipe(input, recipe));
    }

    @Test
    void acceptsMatchingThreeByThreeRecipe() {
        final ItemStack[] input = new ItemStack[9];
        final ItemStack[] recipe = new ItemStack[9];
        input[4] = new ItemStack(Material.IRON_INGOT);
        recipe[4] = new ItemStack(Material.IRON_INGOT);

        assertTrue(SupportedRecipes.testRecipe(input, recipe));
    }
}
