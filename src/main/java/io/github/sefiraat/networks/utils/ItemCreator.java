package io.github.sefiraat.networks.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemCreator {
    public static ItemStack create(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.stream(lore).toList());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(ItemStack item, String name, String... lore) {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.stream(lore).toList());
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    public static ItemStack create(ItemStack item, String name) {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(name);
        displayItem.setItemMeta(meta);
        return displayItem;
    }
}
