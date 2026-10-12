package io.github.sefiraat.networks.slimefun.network;

import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class NetworkQuantumWorkbenchTest extends BukkitTestSupport {

    private static final int[] INPUTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private NetworkQuantumWorkbench workbench;
    private BlockMenu menu;
    private ItemGroup group;

    public static class TestNetworks extends Networks {
        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @BeforeAll
    static void initializeNamespace() throws Exception {
        var instance = Networks.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, MockBukkit.loadSimple(TestNetworks.class));
    }

    @AfterAll
    static void clearNamespace() throws Exception {
        var instance = Networks.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        clearRecipes();
    }

    private static void clearRecipes() throws Exception {
        var recipes = NetworkQuantumWorkbench.class.getDeclaredField("RECIPES");
        recipes.setAccessible(true);
        ((Map<?, ?>) recipes.get(null)).clear();
    }

    @BeforeEach
    void createWorkbench() throws Exception {
        clearRecipes();
        group = new ItemGroup(Keys.newKey("test_group"), new ItemStack(Material.CHEST));
        workbench = new NetworkQuantumWorkbench(group,
            new SlimefunItemStack("TEST_WORKBENCH", new ItemStack(Material.CRAFTING_TABLE)),
            RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[9]);
        workbench.postRegister();
        menu = new BlockMenu(Slimefun.getRegistry().getMenuPresets().get("TEST_WORKBENCH"),
            MockBukkit.getMock().addSimpleWorld("quantum_" + System.nanoTime()).getSpawnLocation());
    }

    private void recipe(int ingredientAmount, int outputAmount) {
        ItemStack[] recipe = new ItemStack[9];
        recipe[0] = new ItemStack(Material.IRON_INGOT, ingredientAmount);
        NetworkQuantumWorkbench.addRecipe(recipe, new ItemStack(Material.DIAMOND, outputAmount));
    }

    private ItemStack[] snapshot() {
        ItemStack[] items = menu.getContents();
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i] == null ? null : items[i].clone();
        }
        return items;
    }

    @Test
    void normalClickCraftsOneAndFurtherClicksStackCompatibleOutput() {
        recipe(1, 1);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 4));
        workbench.craft(menu);
        assertEquals(1, menu.getItemInSlot(25).getAmount());
        assertEquals(3, menu.getItemInSlot(10).getAmount());
        workbench.craft(menu);
        assertEquals(2, menu.getItemInSlot(25).getAmount());
        assertEquals(2, menu.getItemInSlot(10).getAmount());
    }

    @Test
    void shiftCraftsUntilIngredientOrOutputLimitAndDoesNotAliasTemplates() {
        recipe(2, 3);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 64));
        workbench.craft(menu, true);
        assertEquals(63, menu.getItemInSlot(25).getAmount());
        assertEquals(22, menu.getItemInSlot(10).getAmount());
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
        menu.replaceExistingItem(25, null);
        workbench.craft(menu, true);
        assertEquals(33, menu.getItemInSlot(25).getAmount());
        assertNull(menu.getItemInSlot(10));
    }

    @Test
    void partialOutputOnlyConsumesWhatFits() {
        recipe(1, 1);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 20));
        menu.replaceExistingItem(25, new ItemStack(Material.DIAMOND, 61));
        workbench.craft(menu, true);
        assertEquals(64, menu.getItemInSlot(25).getAmount());
        assertEquals(17, menu.getItemInSlot(10).getAmount());
    }

    @Test
    void incompatibleMetadataCannotBeMerged() {
        recipe(1, 1);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 20));
        ItemStack output = new ItemStack(Material.DIAMOND);
        ItemMeta meta = output.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.newKey("owner"), PersistentDataType.STRING, "different");
        output.setItemMeta(meta);
        menu.replaceExistingItem(25, output);
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
    }

    @Test
    void insufficientIngredientsAndExtraItemsLeaveInventoryUntouched() {
        recipe(2, 1);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT));
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 10));
        menu.replaceExistingItem(11, new ItemStack(Material.STONE));
        before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
    }

    @Test
    void itemAndInventoryStackLimitsAreRespected() {
        ItemStack[] input = new ItemStack[9];
        input[0] = new ItemStack(Material.IRON_INGOT);
        NetworkQuantumWorkbench.addRecipe(input, new ItemStack(Material.ENDER_PEARL));
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 64));
        menu.toInventory().setMaxStackSize(8);
        workbench.craft(menu, true);
        assertEquals(8, menu.getItemInSlot(25).getAmount());
        assertEquals(56, menu.getItemInSlot(10).getAmount());
        menu.toInventory().setMaxStackSize(64);
        workbench.craft(menu, true);
        assertEquals(16, menu.getItemInSlot(25).getAmount());
        assertEquals(48, menu.getItemInSlot(10).getAmount());
    }

    @Test
    void consumableBucketIsRetainedAndStopsFurtherCrafting() {
        ItemStack[] input = new ItemStack[9];
        input[0] = new ItemStack(Material.WATER_BUCKET);
        NetworkQuantumWorkbench.addRecipe(input, new ItemStack(Material.DIAMOND));
        menu.replaceExistingItem(10, new ItemStack(Material.WATER_BUCKET));
        workbench.craft(menu, true);
        assertEquals(Material.BUCKET, menu.getItemInSlot(10).getType());
        assertEquals(1, menu.getItemInSlot(25).getAmount());
    }

    @Test
    void emptyRecipeCannotGenerateFreeItems() {
        NetworkQuantumWorkbench.addRecipe(new ItemStack[9], new ItemStack(Material.DIAMOND));
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
    }

    @Test
    void asyncInvocationCannotMutateInventory() {
        recipe(1, 1);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 64));
        ItemStack[] before = snapshot();
        CompletableFuture.runAsync(() -> workbench.craft(menu, true)).join();
        assertArrayEquals(before, snapshot());
    }

    private ItemStack upgradeRecipe(int amount, int count) {
        SlimefunItemStack oldCell = new SlimefunItemStack("TEST_CELL_1", new ItemStack(Material.CHEST));
        SlimefunItemStack newCell = new SlimefunItemStack("TEST_CELL_2", new ItemStack(Material.CHEST));
        var oldStorage = new NetworkQuantumStorage(group, oldCell,
            RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[9], 4096);
        var newStorage = new NetworkQuantumStorage(group, newCell,
            RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[9], 32768);
        Slimefun.getRegistry().getSlimefunItemIds().put(oldCell.getItemId(), oldStorage);
        Slimefun.getRegistry().getSlimefunItemIds().put(newCell.getItemId(), newStorage);
        ItemStack[] recipe = new ItemStack[9];
        recipe[0] = new ItemStack(Material.IRON_INGOT);
        recipe[4] = oldCell;
        NetworkQuantumWorkbench.addRecipe(recipe, newCell);
        ItemStack core = oldCell.clone();
        core.setAmount(count);
        ItemMeta meta = core.getItemMeta();
        ItemStack stored = new ItemStack(Material.DIAMOND);
        ItemMeta storedMeta = stored.getItemMeta();
        storedMeta.getPersistentDataContainer().set(Keys.newKey("stored_identity"), PersistentDataType.STRING, "keep");
        stored.setItemMeta(storedMeta);
        DataTypeMethods.setCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE,
            new QuantumCache(stored, amount, 4096, true, true));
        PersistentDataContainer data = meta.getPersistentDataContainer().get(
            Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.TAG_CONTAINER);
        data.set(Keys.newKey("extension"), PersistentDataType.STRING, "preserved");
        meta.getPersistentDataContainer().set(Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.TAG_CONTAINER, data);
        core.setItemMeta(meta);
        menu.replaceExistingItem(20, core);
        menu.replaceExistingItem(10, new ItemStack(Material.IRON_INGOT, 8));
        return core.clone();
    }

    @Test
    void statefulUpgradePreservesContentsFlagsAndUnknownPdcKeys() {
        ItemStack oldCore = upgradeRecipe(1234, 1);
        workbench.craft(menu, true);
        assertNull(menu.getItemInSlot(20));
        assertEquals(7, menu.getItemInSlot(10).getAmount());
        ItemStack upgraded = menu.getItemInSlot(25);
        assertEquals(1, upgraded.getAmount());
        QuantumCache oldCache = DataTypeMethods.getCustom(oldCore.getItemMeta(),
            Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);
        QuantumCache cache = DataTypeMethods.getCustom(upgraded.getItemMeta(),
            Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);
        assertEquals(oldCache.getItemStack(), cache.getItemStack());
        assertEquals(1234, cache.getAmount());
        assertEquals(32768, cache.getLimit());
        assertTrue(cache.isVoidExcess());
        assertTrue(cache.supportsCustomMaxAmount());
        assertEquals(4096, oldCache.getLimit());
        PersistentDataContainer data = upgraded.getItemMeta().getPersistentDataContainer().get(
            Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.TAG_CONTAINER);
        assertEquals("preserved", data.get(Keys.newKey("extension"), PersistentDataType.STRING));
        assertNotEquals(oldCore.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey("slimefun", "slimefun_item"), PersistentDataType.STRING),
            upgraded.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey("slimefun", "slimefun_item"), PersistentDataType.STRING));
    }

    @Test
    void stackedStatefulCellsAndCapacityOverflowAreRejected() {
        upgradeRecipe(100, 2);
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
        upgradeRecipe(40000, 1);
        before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
    }

    @Test
    void corruptQuantumPdcLeavesCellAndIngredientsUntouched() {
        ItemStack core = upgradeRecipe(100, 1);
        ItemMeta meta = core.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.QUANTUM_STORAGE_INSTANCE, PersistentDataType.STRING, "corrupt");
        core.setItemMeta(meta);
        menu.replaceExistingItem(20, core);
        ItemStack[] before = snapshot();
        assertDoesNotThrow(() -> workbench.craft(menu, true));
        assertArrayEquals(before, snapshot());
    }

    @Test
    void quantumDataOutsideUpgradeSlotIsNotConsumed() {
        ItemStack core = upgradeRecipe(100, 1);
        ItemStack iron = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = iron.getItemMeta();
        core.getItemMeta().getPersistentDataContainer().copyTo(meta.getPersistentDataContainer(), true);
        iron.setItemMeta(meta);
        ItemStack[] input = new ItemStack[9];
        input[0] = iron.clone();
        NetworkQuantumWorkbench.addRecipe(input, new ItemStack(Material.DIAMOND));
        menu.replaceExistingItem(10, iron);
        menu.replaceExistingItem(20, null);
        ItemStack[] before = snapshot();
        workbench.craft(menu, true);
        assertArrayEquals(before, snapshot());
    }
}
