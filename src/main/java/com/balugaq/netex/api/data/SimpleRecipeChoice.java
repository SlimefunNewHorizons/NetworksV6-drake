package com.balugaq.netex.api.data;

import io.github.sefiraat.networks.utils.StackUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RecipeChoice que compara con StackUtils.itemsMatch (lore incluida) en vez de la igualdad exacta.
 *
 * Antes heredaba de RecipeChoice.ExactChoice, pero desde Paper 1.21.11 (y en 26.1 y 26.2) esa clase es
 * final: el plugin no compilaba contra esas APIs, y en un servidor nuevo cargar esta clase lanzaria
 * IncompatibleClassChangeError. Ahora implementa la interfaz directamente con el mismo comportamiento:
 * los mismos constructores, getChoices(), getItemStack() con la primera opcion y la misma validacion.
 */
public class SimpleRecipeChoice implements RecipeChoice {
    private final List<ItemStack> choices;

    public SimpleRecipeChoice(@NotNull ItemStack choice) {
        this(Collections.singletonList(choice));
    }

    public SimpleRecipeChoice(@NotNull ItemStack... choices) {
        this(Arrays.asList(choices));
    }

    public SimpleRecipeChoice(@NotNull List<ItemStack> choices) {
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one choice");
        }
        final List<ItemStack> copy = new ArrayList<>(choices.size());
        for (ItemStack choice : choices) {
            if (choice == null || choice.getType() == Material.AIR) {
                throw new IllegalArgumentException("Cannot have null or air choice");
            }
            copy.add(choice.clone());
        }
        this.choices = copy;
    }

    @NotNull
    public List<ItemStack> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    @NotNull
    @Override
    public ItemStack getItemStack() {
        return choices.get(0).clone();
    }

    @NotNull
    @Override
    public SimpleRecipeChoice clone() {
        return new SimpleRecipeChoice(choices);
    }

    @Override
    public boolean test(@NotNull ItemStack other) {
        for (ItemStack choice : choices) {
            if (StackUtils.itemsMatch(choice, other, true, false)) {
                return true;
            }
        }

        return false;
    }
}
