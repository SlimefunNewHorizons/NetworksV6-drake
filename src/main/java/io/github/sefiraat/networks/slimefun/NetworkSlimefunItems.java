package io.github.sefiraat.networks.slimefun;

import com.balugaq.netex.utils.Converter;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.slimefun.network.*;
import io.github.sefiraat.networks.slimefun.network.grid.NetworkCraftingGrid;
import io.github.sefiraat.networks.slimefun.network.grid.NetworkGrid;
import io.github.sefiraat.networks.slimefun.tools.*;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.SlimefunItems;
import com.github.drakescraft_labs.slimefun4.implementation.items.blocks.UnplaceableBlock;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class NetworkSlimefunItems {

    public static final UnplaceableBlock SYNTHETIC_EMERALD_SHARD;
    public static final UnplaceableBlock OPTIC_GLASS;
    public static final UnplaceableBlock OPTIC_CABLE;
    public static final UnplaceableBlock OPTIC_STAR;
    public static final UnplaceableBlock RADIOACTIVE_OPTIC_STAR;
    public static final UnplaceableBlock SHRINKING_BASE;
    public static final UnplaceableBlock SIMPLE_NANOBOTS;
    public static final UnplaceableBlock ADVANCED_NANOBOTS;
    public static final UnplaceableBlock AI_CORE;
    public static final UnplaceableBlock EMPOWERED_AI_CORE;
    public static final UnplaceableBlock PRISTINE_AI_CORE;
    public static final UnplaceableBlock INTERDIMENSIONAL_PRESENCE;

    public static final NetworkController NETWORK_CONTROLLER;
    public static final NetworkBridge NETWORK_BRIDGE;
    public static final NetworkMonitor NETWORK_MONITOR;
    public static final NetworkInputOnlyMonitor NETWORK_INPUT_ONLY_MONITOR;
    public static final NetworkOutputOnlyMonitor NETWORK_OUTPUT_ONLY_MONITOR;
    public static final NetworkImport NETWORK_IMPORT;
    public static final NetworkAdvancedImport NETWORK_ADVANCED_IMPORT;
    public static final NetworkExport NETWORK_EXPORT;
    public static final NetworkAdvancedExport NETWORK_ADVANCED_EXPORT;
    public static final NetworkGrabber NETWORK_GRABBER;
    public static final NetworkAdvancedGrabber NETWORK_ADVANCED_GRABBER;
    public static final NetworkPusher NETWORK_PUSHER;
    public static final NetworkAdvancedPusher NETWORK_ADVANCED_PUSHER;
    public static final NetworkControlX NETWORK_CONTROL_X;
    public static final NetworkControlV NETWORK_CONTROL_V;
    public static final NetworkVacuum NETWORK_VACUUM;
    public static final NetworkAdvancedVacuum NETWORK_ADVANCED_VACUUM;
    public static final NetworkVanillaGrabber NETWORK_VANILLA_GRABBER;
    public static final NetworkVanillaPusher NETWORK_VANILLA_PUSHER;
    public static final NetworkWirelessTransmitter NETWORK_WIRELESS_TRANSMITTER;
    public static final NetworkWirelessReceiver NETWORK_WIRELESS_RECEIVER;
    public static final NetworkPurger NETWORK_PURGER;
    public static final NetworkAdvancedPurger NETWORK_ADVANCED_PURGER;
    public static final NetworkGrid NETWORK_GRID;
    public static final NetworkCraftingGrid NETWORK_CRAFTING_GRID;
    public static final NetworkCell NETWORK_CELL;
    public static final NetworkGreedyBlock NETWORK_GREEDY_BLOCK;
    public static final NetworkAdvancedGreedyBlock NETWORK_ADVANCED_GREEDY_BLOCK;
    public static final NetworkQuantumWorkbench NETWORK_QUANTUM_WORKBENCH;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_1;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_2;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_3;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_4;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_5;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_6;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_7;
    public static final NetworkQuantumStorage NETWORK_QUANTUM_STORAGE_8;
    public static final NetworkPowerNode NETWORK_CAPACITOR_1;
    public static final NetworkPowerNode NETWORK_CAPACITOR_2;
    public static final NetworkPowerNode NETWORK_CAPACITOR_3;
    public static final NetworkPowerNode NETWORK_CAPACITOR_4;
    public static final NetworkPowerOutlet NETWORK_POWER_OUTLET_1;
    public static final NetworkPowerOutlet NETWORK_POWER_OUTLET_2;
    public static final NetworkPowerDisplay NETWORK_POWER_DISPLAY;
    public static final NetworkEncoder NETWORK_RECIPE_ENCODER;
    public static final NetworkAutoCrafter NETWORK_AUTO_CRAFTER;
    public static final NetworkAutoCrafter NETWORK_AUTO_CRAFTER_WITHHOLDING;
    public static final NetworkAutoCrafter NETWORK_ADVANCED_AUTO_CRAFTER;
    public static final NetworkAutoCrafter NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING;
    public static final NetworkCrafterMatrix NETWORK_CRAFTER_MATRIX;

    public static final CraftingBlueprint CRAFTING_BLUEPRINT;
    public static final NetworkProbe NETWORK_PROBE;
    public static final NetworkRemote NETWORK_REMOTE;
    public static final NetworkRemote NETWORK_REMOTE_EMPOWERED;
    public static final NetworkRemote NETWORK_REMOTE_PRISTINE;
    public static final NetworkRemote NETWORK_REMOTE_ULTIMATE;
    public static final NetworkCrayon NETWORK_CRAYON;
    public static final NetworkConfigurator NETWORK_CONFIGURATOR;
    public static final NetworkWirelessConfigurator NETWORK_WIRELESS_CONFIGURATOR;
    public static final NetworkRake NETWORK_RAKE_1;
    public static final NetworkRake NETWORK_RAKE_2;
    public static final NetworkRake NETWORK_RAKE_3;
    public static final NetworkAdminDebugger NETWORK_ADMIN_DEBUGGER;

    static {
        final ItemStack glass = new ItemStack(Material.GLASS);

        SYNTHETIC_EMERALD_SHARD = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.SYNTHETIC_EMERALD_SHARD,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.STONE_CHUNK.getItem().getItem(), SlimefunItems.SYNTHETIC_EMERALD.getItem().getItem(), null,
                        SlimefunItems.SYNTHETIC_EMERALD.getItem().getItem(), null, null,
                        null, null, null
                ),
                Converter.getItem(NetworksSlimefunItemStacks.SYNTHETIC_EMERALD_SHARD, 3)
        );

        OPTIC_GLASS = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.OPTIC_GLASS,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        glass, glass, glass,
                        glass, SYNTHETIC_EMERALD_SHARD.getItem(), glass,
                        glass, glass, glass
                ), // Compatibility recipe for the Drake core
                Converter.getItem(NetworksSlimefunItemStacks.OPTIC_GLASS, 8)
        );

        OPTIC_CABLE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.OPTIC_CABLE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.COPPER_WIRE.getItem().getItem(), SYNTHETIC_EMERALD_SHARD.getItem(), SlimefunItems.COPPER_WIRE.getItem().getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem()
                ),
                Converter.getItem(NetworksSlimefunItemStacks.OPTIC_CABLE, 16)
        );

        OPTIC_STAR = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.OPTIC_STAR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), new ItemStack(Material.NETHER_STAR), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        RADIOACTIVE_OPTIC_STAR = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.RADIOACTIVE_OPTIC_STAR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(),
                        OPTIC_CABLE.getItem(), OPTIC_STAR.getItem(), OPTIC_CABLE.getItem(),
                        SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem()
                )
        );

        SHRINKING_BASE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.SHRINKING_BASE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem(), SlimefunItems.ANDROID_INTERFACE_ITEMS.getItem().getItem(), SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem(),
                        OPTIC_CABLE.getItem(), RADIOACTIVE_OPTIC_STAR.getItem(), OPTIC_CABLE.getItem(),
                        SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem(), SlimefunItems.ANDROID_MEMORY_CORE.getItem().getItem(), SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem()
                )
        );

        SIMPLE_NANOBOTS = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.SIMPLE_NANOBOTS,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SHRINKING_BASE.getItem(), SlimefunItems.PROGRAMMABLE_ANDROID.getItem().getItem()
                ),
                Converter.getItem(NetworksSlimefunItemStacks.SIMPLE_NANOBOTS, 4)
        );

        ADVANCED_NANOBOTS = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.ADVANCED_NANOBOTS,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SHRINKING_BASE.getItem(), SlimefunItems.PROGRAMMABLE_ANDROID_3.getItem().getItem()
                ),
                Converter.getItem(NetworksSlimefunItemStacks.ADVANCED_NANOBOTS, 4)
        );

        AI_CORE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.AI_CORE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), SlimefunItems.ANDROID_MEMORY_CORE.getItem().getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), ADVANCED_NANOBOTS.getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), SlimefunItems.ANDROID_MEMORY_CORE.getItem().getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem()
                )
        );

        EMPOWERED_AI_CORE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.EMPOWERED_AI_CORE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(), RADIOACTIVE_OPTIC_STAR.getItem(), SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(), AI_CORE.getItem(), SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(), RADIOACTIVE_OPTIC_STAR.getItem(), SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem()
                )
        );

        PRISTINE_AI_CORE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.PRISTINE_AI_CORE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.NEPTUNIUM.getItem().getItem(), SlimefunItems.DAMASCUS_STEEL_MULTI_TOOL.getItem().getItem(), SlimefunItems.NEPTUNIUM.getItem().getItem(),
                        SlimefunItems.NEPTUNIUM.getItem().getItem(), EMPOWERED_AI_CORE.getItem(), SlimefunItems.NEPTUNIUM.getItem().getItem(),
                        SlimefunItems.NEPTUNIUM.getItem().getItem(), SlimefunItems.ELECTRIFIED_CRUCIBLE_3.getItem().getItem(), SlimefunItems.NEPTUNIUM.getItem().getItem()
                )
        );

        INTERDIMENSIONAL_PRESENCE = new UnplaceableBlock(
                NetworksItemGroups.MATERIALS,
                NetworksSlimefunItemStacks.INTERDIMENSIONAL_PRESENCE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.PLUTONIUM.getItem().getItem(), SlimefunItems.REINFORCED_ALLOY_MULTI_TOOL.getItem().getItem(), SlimefunItems.PLUTONIUM.getItem().getItem(),
                        SlimefunItems.PLUTONIUM.getItem().getItem(), PRISTINE_AI_CORE.getItem(), SlimefunItems.PLUTONIUM.getItem().getItem(),
                        SlimefunItems.PLUTONIUM.getItem().getItem(), SlimefunItems.NETHER_STAR_REACTOR.getItem().getItem(), SlimefunItems.PLUTONIUM.getItem().getItem()
                )
        );

        NETWORK_CONTROLLER = new NetworkController(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CONTROLLER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_MANAGER.getItem().getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_BRIDGE = new NetworkBridge(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_BRIDGE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_CONNECTOR_NODE.getItem().getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        ),
                Converter.getItem(NetworksSlimefunItemStacks.NETWORK_BRIDGE, 8)
        );

        NETWORK_MONITOR = new NetworkMonitor(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_MONITOR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_MOTOR.getItem().getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_INPUT_ONLY_MONITOR = new NetworkInputOnlyMonitor(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_INPUT_ONLY_MONITOR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_INPUT_NODE.getItem().getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), NETWORK_MONITOR.getItem(), NETWORK_BRIDGE.getItem()
                )
        );

        NETWORK_OUTPUT_ONLY_MONITOR = new NetworkOutputOnlyMonitor(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_OUTPUT_ONLY_MONITOR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_OUTPUT_NODE_2.getItem().getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), NETWORK_MONITOR.getItem(), NETWORK_BRIDGE.getItem()
                )
        );

        NETWORK_IMPORT = new NetworkImport(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_IMPORT,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_INPUT_NODE.getItem().getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_ADVANCED_IMPORT = new NetworkAdvancedImport(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_IMPORT,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_IMPORT.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.CARGO_MANAGER.getItem().getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_EXPORT = new NetworkExport(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_EXPORT,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_OUTPUT_NODE_2.getItem().getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_ADVANCED_EXPORT = new NetworkAdvancedExport(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_EXPORT,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_EXPORT.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.CARGO_MANAGER.getItem().getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_GRABBER = new NetworkGrabber(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_GRABBER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_IMPORT.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_ADVANCED_GRABBER = new NetworkAdvancedGrabber(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_GRABBER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_GRABBER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_PUSHER = new NetworkPusher(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_PUSHER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_EXPORT.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_ADVANCED_PUSHER = new NetworkAdvancedPusher(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_PUSHER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_PUSHER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_CONTROL_X = new NetworkControlX(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CONTROL_X,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_DISPLAY, OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NetworksSlimefunItemStacks.NETWORK_GRABBER, OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_1, OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_CONTROL_V = new NetworkControlV(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CONTROL_V,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_DISPLAY, OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NetworksSlimefunItemStacks.NETWORK_PUSHER, OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_1, OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_VACUUM = new NetworkVacuum(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_VACUUM,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_DISPLAY, OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NetworksSlimefunItemStacks.NETWORK_IMPORT, OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_1, OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_ADVANCED_VACUUM = new NetworkAdvancedVacuum(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_VACUUM,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_VACUUM.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_VANILLA_GRABBER = new NetworkVanillaGrabber(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_VANILLA_GRABBER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        new ItemStack(Material.HOPPER), NETWORK_GRABBER.getItem(), new ItemStack(Material.HOPPER),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_VANILLA_PUSHER = new NetworkVanillaPusher(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_VANILLA_PUSHER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), new ItemStack(Material.HOPPER), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_PUSHER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), new ItemStack(Material.HOPPER), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_WIRELESS_TRANSMITTER = new NetworkWirelessTransmitter(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_WIRELESS_TRANSMITTER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_PUSHER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), PRISTINE_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_WIRELESS_RECEIVER = new NetworkWirelessReceiver(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_WIRELESS_RECEIVER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SIMPLE_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_GRABBER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), AI_CORE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_PURGER = new NetworkPurger(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_PURGER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.TRASH_CAN.getItem().getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_ADVANCED_PURGER = new NetworkAdvancedPurger(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_PURGER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_PURGER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_GRID = new NetworkGrid(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_GRID,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), new ItemStack(Material.NETHER_STAR), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_CRAFTING_GRID = new NetworkCraftingGrid(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CRAFTING_GRID,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_STAR.getItem(), OPTIC_STAR.getItem(), OPTIC_STAR.getItem(),
                        OPTIC_STAR.getItem(), NETWORK_GRID.getItem(), OPTIC_STAR.getItem(),
                        OPTIC_STAR.getItem(), OPTIC_STAR.getItem(), OPTIC_STAR.getItem()
                        )
        );

        NETWORK_CELL = new NetworkCell(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CELL,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), new ItemStack(Material.CHEST), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_GREEDY_BLOCK = new NetworkGreedyBlock(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_GREEDY_BLOCK,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_CELL.getItem(), OPTIC_CABLE.getItem(),
                        NETWORK_BRIDGE.getItem(), SIMPLE_NANOBOTS.getItem(), NETWORK_BRIDGE.getItem()
                        )
        );

        NETWORK_ADVANCED_GREEDY_BLOCK = new NetworkAdvancedGreedyBlock(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_GREEDY_BLOCK,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_GREEDY_BLOCK.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_QUANTUM_WORKBENCH = new NetworkQuantumWorkbench(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_WORKBENCH,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_BRIDGE.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ADVANCED_CIRCUIT_BOARD.getItem().getItem(), OPTIC_GLASS.getItem()
                )
        );

        NETWORK_QUANTUM_STORAGE_1 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_1,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.CARGO_MOTOR.getItem().getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[0]
        );

        NETWORK_QUANTUM_STORAGE_2 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_2,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.ALUMINUM_BRASS_INGOT.getItem().getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.SYNTHETIC_SAPPHIRE.getItem().getItem(), NETWORK_QUANTUM_STORAGE_1.getItem(), SlimefunItems.SYNTHETIC_SAPPHIRE.getItem().getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ALUMINUM_BRASS_INGOT.getItem().getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[1]
        );

        NETWORK_QUANTUM_STORAGE_3 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_3,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.SYNTHETIC_DIAMOND.getItem().getItem(), NETWORK_QUANTUM_STORAGE_2.getItem(), SlimefunItems.SYNTHETIC_DIAMOND.getItem().getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.CORINTHIAN_BRONZE_INGOT.getItem().getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[2]
        );

        NETWORK_QUANTUM_STORAGE_4 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_4,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.HARDENED_METAL_INGOT.getItem().getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.SYNTHETIC_EMERALD.getItem().getItem(), NETWORK_QUANTUM_STORAGE_3.getItem(), SlimefunItems.SYNTHETIC_EMERALD.getItem().getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.HARDENED_METAL_INGOT.getItem().getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[3]
        );

        NETWORK_QUANTUM_STORAGE_5 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_5,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.REINFORCED_ALLOY_INGOT.getItem().getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.POWER_CRYSTAL.getItem().getItem(), NETWORK_QUANTUM_STORAGE_4.getItem(), SlimefunItems.POWER_CRYSTAL.getItem().getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.REINFORCED_ALLOY_INGOT.getItem().getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[4]
        );

        NETWORK_QUANTUM_STORAGE_6 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_6,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        SlimefunItems.STEEL_PLATE.getItem().getItem(), SlimefunItems.BLISTERING_INGOT.getItem().getItem(), SlimefunItems.STEEL_PLATE.getItem().getItem(),
                        SlimefunItems.CARGO_MOTOR.getItem().getItem(), NETWORK_QUANTUM_STORAGE_5.getItem(), SlimefunItems.CARGO_MOTOR.getItem().getItem(),
                        SlimefunItems.STEEL_PLATE.getItem().getItem(), SlimefunItems.BLISTERING_INGOT.getItem().getItem(), SlimefunItems.STEEL_PLATE.getItem().getItem()
                ),
                NetworkQuantumStorage.getSizes()[5]
        );

        NETWORK_QUANTUM_STORAGE_7 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_7,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        SlimefunItems.REINFORCED_PLATE.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_2.getItem().getItem(), SlimefunItems.REINFORCED_PLATE.getItem().getItem(),
                        SlimefunItems.CARGO_CONNECTOR_NODE.getItem().getItem(), NETWORK_QUANTUM_STORAGE_6.getItem(), SlimefunItems.CARGO_CONNECTOR_NODE.getItem().getItem(),
                        SlimefunItems.REINFORCED_PLATE.getItem().getItem(), SlimefunItems.BLISTERING_INGOT_2.getItem().getItem(), SlimefunItems.REINFORCED_PLATE.getItem().getItem()
                ),
                NetworkQuantumStorage.getSizes()[6]
        );

        NETWORK_QUANTUM_STORAGE_8 = new NetworkQuantumStorage(
                NetworksItemGroups.NETWORK_QUANTUMS,
                NetworksSlimefunItemStacks.NETWORK_QUANTUM_STORAGE_8,
                NetworkQuantumWorkbench.TYPE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), OPTIC_GLASS.getItem(),
                        SlimefunItems.CARGO_MANAGER.getItem().getItem(), NETWORK_QUANTUM_STORAGE_7.getItem(), SlimefunItems.CARGO_MANAGER.getItem().getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.BLISTERING_INGOT_3.getItem().getItem(), OPTIC_GLASS.getItem()
                ),
                NetworkQuantumStorage.getSizes()[7]
        );

        NETWORK_CAPACITOR_1 = new NetworkPowerNode(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CAPACITOR_1,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SlimefunItems.MEDIUM_CAPACITOR.getItem().getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem()
                        ),
                1000
        );

        NETWORK_CAPACITOR_2 = new NetworkPowerNode(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CAPACITOR_2,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_CAPACITOR_1.getItem(), NETWORK_CAPACITOR_1.getItem(), NETWORK_CAPACITOR_1.getItem(),
                        NETWORK_CAPACITOR_1.getItem(), SlimefunItems.BIG_CAPACITOR.getItem().getItem(), NETWORK_CAPACITOR_1.getItem(),
                        NETWORK_CAPACITOR_1.getItem(), NETWORK_CAPACITOR_1.getItem(), NETWORK_CAPACITOR_1.getItem()
                        ),
                10000
        );

        NETWORK_CAPACITOR_3 = new NetworkPowerNode(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CAPACITOR_3,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_CAPACITOR_2.getItem(), NETWORK_CAPACITOR_2.getItem(), NETWORK_CAPACITOR_2.getItem(),
                        NETWORK_CAPACITOR_2.getItem(), SlimefunItems.LARGE_CAPACITOR.getItem().getItem(), NETWORK_CAPACITOR_2.getItem(),
                        NETWORK_CAPACITOR_2.getItem(), NETWORK_CAPACITOR_2.getItem(), NETWORK_CAPACITOR_2.getItem()
                        ),
                100000
        );

        NETWORK_CAPACITOR_4 = new NetworkPowerNode(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CAPACITOR_4,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_CAPACITOR_3.getItem(), NETWORK_CAPACITOR_3.getItem(), NETWORK_CAPACITOR_3.getItem(),
                        NETWORK_CAPACITOR_3.getItem(), SlimefunItems.CARBONADO_EDGED_CAPACITOR.getItem().getItem(), NETWORK_CAPACITOR_3.getItem(),
                        NETWORK_CAPACITOR_3.getItem(), NETWORK_CAPACITOR_3.getItem(), NETWORK_CAPACITOR_3.getItem()
                        ),
                1000000
        );

        NETWORK_POWER_OUTLET_1 = new NetworkPowerOutlet(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_1,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ENERGY_CONNECTOR.getItem().getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem(), OPTIC_GLASS.getItem()
                        ),
                500
        );

        NETWORK_POWER_OUTLET_2 = new NetworkPowerOutlet(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_2,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_POWER_OUTLET_1.getItem(), OPTIC_GLASS.getItem(), NETWORK_POWER_OUTLET_1.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ENERGY_CONNECTOR.getItem().getItem(), OPTIC_GLASS.getItem(),
                        NETWORK_POWER_OUTLET_1.getItem(), OPTIC_GLASS.getItem(), NETWORK_POWER_OUTLET_1.getItem()
                        ),
                2000
        );

        NETWORK_POWER_DISPLAY = new NetworkPowerDisplay(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_POWER_DISPLAY,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), SlimefunItems.ENERGY_REGULATOR.getItem().getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_CAPACITOR_1.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ENERGY_CONNECTOR.getItem().getItem(), OPTIC_GLASS.getItem()
                        )
        );

        NETWORK_RECIPE_ENCODER = new NetworkEncoder(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_RECIPE_ENCODER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), SlimefunItems.ANDROID_MEMORY_CORE.getItem().getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), SlimefunItems.ENHANCED_AUTO_CRAFTER.getItem().getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(),
                        SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem(), SlimefunItems.CARGO_MOTOR.getItem().getItem(), SlimefunItems.BASIC_CIRCUIT_BOARD.getItem().getItem()
                )
        );

        NETWORK_AUTO_CRAFTER = new NetworkAutoCrafter(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_AUTO_CRAFTER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), SIMPLE_NANOBOTS.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), SlimefunItems.ENHANCED_AUTO_CRAFTER.getItem().getItem(), OPTIC_GLASS.getItem()
                        ),
                64,
                false
        );

        NETWORK_AUTO_CRAFTER_WITHHOLDING = new NetworkAutoCrafter(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_AUTO_CRAFTER_WITHHOLDING,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), OPTIC_CABLE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), NETWORK_AUTO_CRAFTER.getItem(), OPTIC_GLASS.getItem()
                        ),
                128,
                true
        );

        NETWORK_ADVANCED_AUTO_CRAFTER = new NetworkAutoCrafter(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_AUTO_CRAFTER,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), ADVANCED_NANOBOTS.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_AUTO_CRAFTER.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                ),
                96,
                false,
                true
        );

        NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING = new NetworkAutoCrafter(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_GLASS.getItem(), PRISTINE_AI_CORE.getItem(), OPTIC_GLASS.getItem(),
                        OPTIC_CABLE.getItem(), NETWORK_AUTO_CRAFTER_WITHHOLDING.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_GLASS.getItem(), EMPOWERED_AI_CORE.getItem(), OPTIC_GLASS.getItem()
                ),
                192,
                true,
                true
        );

        NETWORK_CRAFTER_MATRIX = new NetworkCrafterMatrix(
                NetworksItemGroups.NETWORK_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_CRAFTER_MATRIX,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        NETWORK_ADVANCED_AUTO_CRAFTER.getItem(), PRISTINE_AI_CORE.getItem(), NETWORK_ADVANCED_AUTO_CRAFTER.getItem(),
                        PRISTINE_AI_CORE.getItem(), SlimefunItems.ENHANCED_AUTO_CRAFTER.getItem().getItem(), PRISTINE_AI_CORE.getItem(),
                        NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING.getItem(), PRISTINE_AI_CORE.getItem(), NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING.getItem()
                ),
                500
        );

        CRAFTING_BLUEPRINT = new CraftingBlueprint(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.CRAFTING_BLUEPRINT,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        OPTIC_CABLE.getItem(), OPTIC_CABLE.getItem(), OPTIC_CABLE.getItem(),
                        OPTIC_CABLE.getItem(), new ItemStack(Material.PAPER), OPTIC_CABLE.getItem(),
                        OPTIC_CABLE.getItem(), OPTIC_CABLE.getItem(), OPTIC_CABLE.getItem()
                )
        );

        NETWORK_PROBE = new NetworkProbe(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_PROBE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, SlimefunItems.DURALUMIN_INGOT.getItem().getItem(), null,
                        null, OPTIC_CABLE.getItem(), null,
                        null, NETWORK_BRIDGE.getItem(), null
                )
        );

        NETWORK_REMOTE = new NetworkRemote(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_REMOTE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, NETWORK_GRID.getItem(), null,
                        null, AI_CORE.getItem(), null,
                        null, OPTIC_STAR.getItem(), null
                ),
                NetworkRemote.getRanges()[0]
        );

        NETWORK_REMOTE_EMPOWERED = new NetworkRemote(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_REMOTE_EMPOWERED,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, NETWORK_REMOTE.getItem(), null,
                        null, EMPOWERED_AI_CORE.getItem(), null,
                        null, NETWORK_REMOTE.getItem(), null
                ),
                NetworkRemote.getRanges()[1]
        );

        NETWORK_REMOTE_PRISTINE = new NetworkRemote(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_REMOTE_PRISTINE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, NETWORK_REMOTE_EMPOWERED.getItem(), null,
                        null, PRISTINE_AI_CORE.getItem(), null,
                        null, NETWORK_REMOTE_EMPOWERED.getItem(), null
                ),
                NetworkRemote.getRanges()[2]
        );

        NETWORK_REMOTE_ULTIMATE = new NetworkRemote(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_REMOTE_ULTIMATE,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, NETWORK_REMOTE_PRISTINE.getItem(), null,
                        null, INTERDIMENSIONAL_PRESENCE.getItem(), null,
                        null, NETWORK_REMOTE_PRISTINE.getItem(), null
                ),
                NetworkRemote.getRanges()[3]
        );

        NETWORK_CRAYON = new NetworkCrayon(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_CRAYON,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, OPTIC_CABLE.getItem(), null,
                        null, new ItemStack(Material.HONEYCOMB), null,
                        null, new ItemStack(Material.HONEYCOMB), null
                )
        );

        NETWORK_CONFIGURATOR = new NetworkConfigurator(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_CONFIGURATOR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, RADIOACTIVE_OPTIC_STAR.getItem(), null,
                        null, NETWORK_CRAYON.getItem(), null,
                        null, AI_CORE.getItem(), null
                )
        );

        NETWORK_WIRELESS_CONFIGURATOR = new NetworkWirelessConfigurator(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_WIRELESS_CONFIGURATOR,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, RADIOACTIVE_OPTIC_STAR.getItem(), null,
                        null, NETWORK_CONFIGURATOR.getItem(), null,
                        null, INTERDIMENSIONAL_PRESENCE.getItem(), null
                )
        );

        NETWORK_RAKE_1 = new NetworkRake(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_RAKE_1,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, RADIOACTIVE_OPTIC_STAR.getItem(), null,
                        null, new ItemStack(Material.DIAMOND_SWORD), null,
                        null, SYNTHETIC_EMERALD_SHARD.getItem(), null
                ),
                250
        );

        NETWORK_RAKE_2 = new NetworkRake(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_RAKE_2,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, RADIOACTIVE_OPTIC_STAR.getItem(), null,
                        null, NETWORK_RAKE_1.getItem(), null,
                        null, AI_CORE.getItem(), null
                ),
                1000
        );

        NETWORK_RAKE_3 = new NetworkRake(
                NetworksItemGroups.TOOLS,
                NetworksSlimefunItemStacks.NETWORK_RAKE_3,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                makeRecipe(
                        null, RADIOACTIVE_OPTIC_STAR.getItem(), null,
                        null, NETWORK_RAKE_2.getItem(), null,
                        null, EMPOWERED_AI_CORE.getItem(), null
                ),
                9999
        );

        NETWORK_ADMIN_DEBUGGER = new NetworkAdminDebugger(
                NetworksItemGroups.DISABLED_ITEMS,
                NetworksSlimefunItemStacks.NETWORK_DEBUG_STICK,
                RecipeType.NULL,
                new ItemStack[]{}
        );
    }

    private static ItemStack[] makeRecipe(Object... items) {
        ItemStack[] realItems = new ItemStack[9];
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if (item instanceof ItemStack is) {
                realItems[i] = is;
            } else if (item instanceof SlimefunItemStack sfis) {
                realItems[i] = Converter.getItem(sfis);
            }
        }

        return realItems;
    }

    public static void setup() {
        Networks plugin = Networks.getInstance();

        SYNTHETIC_EMERALD_SHARD.register(plugin);
        OPTIC_GLASS.register(plugin);
        OPTIC_CABLE.register(plugin);
        OPTIC_STAR.register(plugin);
        RADIOACTIVE_OPTIC_STAR.register(plugin);
        SHRINKING_BASE.register(plugin);
        SIMPLE_NANOBOTS.register(plugin);
        ADVANCED_NANOBOTS.register(plugin);
        AI_CORE.register(plugin);
        EMPOWERED_AI_CORE.register(plugin);
        PRISTINE_AI_CORE.register(plugin);
        INTERDIMENSIONAL_PRESENCE.register(plugin);

        NETWORK_CONTROLLER.register(plugin);
        NETWORK_BRIDGE.register(plugin);
        NETWORK_MONITOR.register(plugin);
        NETWORK_INPUT_ONLY_MONITOR.register(plugin);
        NETWORK_OUTPUT_ONLY_MONITOR.register(plugin);
        NETWORK_IMPORT.register(plugin);
        NETWORK_ADVANCED_IMPORT.register(plugin);
        NETWORK_EXPORT.register(plugin);
        NETWORK_ADVANCED_EXPORT.register(plugin);
        NETWORK_GRABBER.register(plugin);
        NETWORK_ADVANCED_GRABBER.register(plugin);
        NETWORK_PUSHER.register(plugin);
        NETWORK_ADVANCED_PUSHER.register(plugin);
        NETWORK_CONTROL_X.register(plugin);
        NETWORK_CONTROL_V.register(plugin);
        NETWORK_VACUUM.register(plugin);
        NETWORK_ADVANCED_VACUUM.register(plugin);
        NETWORK_VANILLA_GRABBER.register(plugin);
        NETWORK_VANILLA_PUSHER.register(plugin);
        NETWORK_WIRELESS_TRANSMITTER.register(plugin);
        NETWORK_WIRELESS_RECEIVER.register(plugin);
        NETWORK_PURGER.register(plugin);
        NETWORK_ADVANCED_PURGER.register(plugin);
        NETWORK_GRID.register(plugin);
        NETWORK_CRAFTING_GRID.register(plugin);
        NETWORK_CELL.register(plugin);
        NETWORK_GREEDY_BLOCK.register(plugin);
        NETWORK_ADVANCED_GREEDY_BLOCK.register(plugin);
        NETWORK_QUANTUM_WORKBENCH.register(plugin);
        NETWORK_QUANTUM_STORAGE_1.register(plugin);
        NETWORK_QUANTUM_STORAGE_2.register(plugin);
        NETWORK_QUANTUM_STORAGE_3.register(plugin);
        NETWORK_QUANTUM_STORAGE_4.register(plugin);
        NETWORK_QUANTUM_STORAGE_5.register(plugin);
        NETWORK_QUANTUM_STORAGE_6.register(plugin);
        NETWORK_QUANTUM_STORAGE_7.register(plugin);
        NETWORK_QUANTUM_STORAGE_8.register(plugin);
        NETWORK_CAPACITOR_1.register(plugin);
        NETWORK_CAPACITOR_2.register(plugin);
        NETWORK_CAPACITOR_3.register(plugin);
        NETWORK_CAPACITOR_4.register(plugin);
        NETWORK_POWER_OUTLET_1.register(plugin);
        NETWORK_POWER_OUTLET_2.register(plugin);
        NETWORK_POWER_DISPLAY.register(plugin);
        NETWORK_RECIPE_ENCODER.register(plugin);
        NETWORK_AUTO_CRAFTER.register(plugin);
        NETWORK_AUTO_CRAFTER_WITHHOLDING.register(plugin);
        NETWORK_ADVANCED_AUTO_CRAFTER.register(plugin);
        NETWORK_ADVANCED_AUTO_CRAFTER_WITHHOLDING.register(plugin);
        NETWORK_CRAFTER_MATRIX.register(plugin);

        CRAFTING_BLUEPRINT.register(plugin);
        NETWORK_PROBE.register(plugin);
        NETWORK_REMOTE.register(plugin);
        NETWORK_REMOTE_EMPOWERED.register(plugin);
        NETWORK_REMOTE_PRISTINE.register(plugin);
        NETWORK_REMOTE_ULTIMATE.register(plugin);
        NETWORK_CRAYON.register(plugin);
        NETWORK_CONFIGURATOR.register(plugin);
        NETWORK_WIRELESS_CONFIGURATOR.register(plugin);
        NETWORK_RAKE_1.register(plugin);
        NETWORK_RAKE_2.register(plugin);
        NETWORK_RAKE_3.register(plugin);

        NETWORK_ADMIN_DEBUGGER.register(plugin);
    }
}
