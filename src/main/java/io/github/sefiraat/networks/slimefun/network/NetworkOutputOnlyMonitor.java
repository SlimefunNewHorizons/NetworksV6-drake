package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.NodeType;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

/** A directional storage monitor that only exposes items from the network. */
public final class NetworkOutputOnlyMonitor extends NetworkDirectional {

    public NetworkOutputOnlyMonitor(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.OUTPUT_ONLY_MONITOR);
    }
}
