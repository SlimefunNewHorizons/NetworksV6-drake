package io.github.sefiraat.networks.commands;

import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NetworksMain implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("doctor", "inspect", "reload", "repair", "fillquantum");

    private static final Map<Integer, NetworkQuantumStorage> QUANTUM_REPLACEMENT_MAP = new HashMap<>();

    static {
        QUANTUM_REPLACEMENT_MAP.put(4096, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_1);
        QUANTUM_REPLACEMENT_MAP.put(32768, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_2);
        QUANTUM_REPLACEMENT_MAP.put(262144, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_3);
        QUANTUM_REPLACEMENT_MAP.put(2097152, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_4);
        QUANTUM_REPLACEMENT_MAP.put(16777216, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_5);
        QUANTUM_REPLACEMENT_MAP.put(134217728, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_6);
        QUANTUM_REPLACEMENT_MAP.put(1073741824, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_7);
        QUANTUM_REPLACEMENT_MAP.put(Integer.MAX_VALUE, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_8);
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (args.length == 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase("doctor")) {
            runDoctor(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadNetworks(sender);
            return true;
        }

        if (sender instanceof Player player) {
            if (args[0].equalsIgnoreCase("inspect")) {
                inspectNetwork(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("fillquantum")) {
                if ((player.isOp() || player.hasPermission("networks.admin")) && args.length >= 2) {
                    try {
                        int number = Integer.parseInt(args[1]);
                        fillQuantum(player, number);
                        return true;
                    } catch (NumberFormatException exception) {
                        return false;
                    }
                }
            }

            if (args[0].equalsIgnoreCase("repair")) {
                if (args.length > 1 && args[1].equalsIgnoreCase("chunk")) {
                    repairChunk(player);
                    return true;
                }
                repairNetwork(player);
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command,
                                      @Nonnull String alias, @Nonnull String[] args) {
        if (!sender.isOp() && !sender.hasPermission("networks.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("repair")
                && "chunk".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return List.of("chunk");
        }
        return List.of();
    }

    /**
     * Diagnostico de salud de las redes cargadas. No cambia nada.
     *
     * Cuenta los nodos que existen para Networks pero no para ninguna red -- sin NetworkNode, o
     * apuntando a una raiz que su controlador ya sustituyo -- que es la causa de "lo tengo todo
     * conectado y la maquina no trabaja". Da tambien el numero de redes activas y de nodos.
     */
    private void runDoctor(@Nonnull CommandSender sender) {
        if (!sender.isOp() && !sender.hasPermission("networks.admin") && sender instanceof Player) {
            sender.sendMessage(Theme.ERROR + "No tienes permiso.");
            return;
        }

        final Map<org.bukkit.Location, io.github.sefiraat.networks.network.NetworkRoot> networks =
            NetworkController.getNetworks();
        int orphansLoaded = 0;
        int orphansUnloaded = 0;
        int stale = 0;
        int conflicts = 0;
        int overburdened = 0;
        int total = 0;

        for (NetworkRoot root : networks.values()) {
            conflicts += root.getConflictingControllers().size();
            if (root.isOverburdened()) {
                overburdened++;
            }
        }

        for (Map.Entry<org.bukkit.Location, io.github.sefiraat.networks.network.NodeDefinition> entry
                : NetworkStorage.getAllNetworkObjects().entrySet()) {
            total++;
            final org.bukkit.Location location = entry.getKey();
            final io.github.sefiraat.networks.network.NetworkNode node = entry.getValue().getNode();
            if (node == null) {
                /*
                 * Separar los huerfanos por si su chunk esta cargado es la diferencia entre un
                 * numero que sirve y uno que no.
                 *
                 * Un controlador solo entra en NETWORKS cuando tickea, y Slimefun solo tickea
                 * chunks cargados. Asi que TODA base sin nadie cerca aporta huerfanos, y son
                 * normales: en cuanto alguien se acerca, el controlador tickea y los adopta.
                 * Contarlos juntos daba cifras enormes que no distinguian "no hay nadie ahi" de
                 * "esto esta roto", que es justo lo que hay que saber.
                 */
                if (location.getWorld() != null
                        && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    orphansLoaded++;
                } else {
                    orphansUnloaded++;
                }
                continue;
            }
            final org.bukkit.Location controller = node.getRoot().getController();
            if (controller == null || networks.get(controller) != node.getRoot()) {
                stale++;
            }
        }

        sender.sendMessage(Theme.MAIN + "===== Networks Doctor =====");
        sender.sendMessage(Theme.PASSIVE + "Redes activas: " + Theme.SUCCESS + networks.size());
        sender.sendMessage(Theme.PASSIVE + "Nodos registrados: " + Theme.SUCCESS + total);
        sender.sendMessage(Theme.PASSIVE + "Huerfanos en chunk CARGADO: "
                + (orphansLoaded > 0 ? Theme.ERROR : Theme.SUCCESS) + orphansLoaded
                + Theme.PASSIVE + "  <- los que importan");
        sender.sendMessage(Theme.PASSIVE + "Huerfanos en chunk descargado: "
                + Theme.SUCCESS + orphansUnloaded
                + Theme.PASSIVE + "  (normal: nadie cerca de esa base)");
        sender.sendMessage(Theme.PASSIVE + "Nodos con raiz obsoleta: "
                + (stale > 0 ? Theme.WARNING : Theme.SUCCESS) + stale);
        sender.sendMessage(Theme.PASSIVE + "Controladores conectados entre si: "
                + (conflicts > 0 ? Theme.ERROR : Theme.SUCCESS) + conflicts);
        sender.sendMessage(Theme.PASSIVE + "Redes sobrecargadas: "
                + (overburdened > 0 ? Theme.WARNING : Theme.SUCCESS) + overburdened);
        if (conflicts > 0) {
            sender.sendMessage(Theme.ERROR + "Separa los controladores: Networks los aisló y no eliminó ningún bloque.");
        }
        if (orphansLoaded > 0 || stale > 0) {
            sender.sendMessage(Theme.WARNING + "Usa /networks reload para reconciliar todas las redes en caliente.");
        } else if (conflicts == 0 && overburdened == 0) {
            sender.sendMessage(Theme.SUCCESS + "Todas las redes estan sanas.");
        }
    }

    /** Shows the topology of the controller currently targeted without changing runtime state. */
    private void inspectNetwork(@Nonnull Player player) {
        if (!player.isOp() && !player.hasPermission("networks.admin")) {
            player.sendMessage(Theme.ERROR + "No tienes permiso.");
            return;
        }
        final Block block = player.getTargetBlockExact(8);
        if (block == null) {
            player.sendMessage(Theme.ERROR + "Mira un Network Controller a menos de 8 bloques.");
            return;
        }
        final NetworkRoot root = NetworkController.getNetworks().get(block.getLocation());
        if (root == null) {
            player.sendMessage(Theme.ERROR + "Ese bloque no es un controlador activo.");
            return;
        }
        player.sendMessage(Theme.MAIN + "===== Network Inspect =====");
        player.sendMessage(Theme.PASSIVE + "Nodos: " + Theme.SUCCESS + root.getNodeCount()
                + Theme.PASSIVE + "/" + root.getMaxNodes());
        player.sendMessage(Theme.PASSIVE + "Energía: " + Theme.SUCCESS + root.getRootPower());
        player.sendMessage(Theme.PASSIVE + "Almacenamiento: " + Theme.SUCCESS
                + (root.getCells().size() + root.getGreedyBlocks().size())
                + Theme.PASSIVE + " | E/S: " + Theme.SUCCESS
                + (root.getImporters().size() + root.getExporters().size()));
        player.sendMessage(Theme.PASSIVE + "Grids: " + Theme.SUCCESS + root.getGrids().size()
                + Theme.PASSIVE + " | Crafters: " + Theme.SUCCESS + root.getCrafters().size());
        player.sendMessage(Theme.PASSIVE + "Conflictos: "
                + (root.getConflictingControllers().isEmpty() ? Theme.SUCCESS : Theme.ERROR)
                + root.getConflictingControllers().size()
                + Theme.PASSIVE + " | Sobrecargada: "
                + (root.isOverburdened() ? Theme.ERROR + "sí" : Theme.SUCCESS + "no"));
    }

    /**
     * Reconcilia en caliente el estado de todas las redes cargadas, sin reiniciar el servidor.
     *
     * Devuelve al registro los nodos de los chunks cargados y marca sucias todas las redes para
     * que cada controlador rehaga su grafo una vez y readopte lo que estuviera desligado. No
     * recarga el jar ni toca datos persistentes: solo repara la topologia en memoria. Es el
     * "aplicar el fix del net sin reinicio" -- sirve para reparar redes rotas al vuelo.
     */
    private void reloadNetworks(@Nonnull CommandSender sender) {
        if (!sender.isOp() && !sender.hasPermission("networks.admin") && sender instanceof Player) {
            sender.sendMessage(Theme.ERROR + "No tienes permiso.");
            return;
        }

        final int indexed = io.github.sefiraat.networks.listeners.SyncListener.indexLoadedChunks();
        int worlds = 0;
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            NetworkController.markNetworksDirtyInWorld(world);
            worlds++;
        }
        sender.sendMessage(Theme.SUCCESS + "Reconciliacion en caliente lanzada: "
                + indexed + " nodo(s) reindexado(s), " + worlds + " mundo(s) marcado(s).");
        sender.sendMessage(Theme.PASSIVE + "Las redes se reconstruyen en los proximos ticks. "
                + "Comprueba con /networks doctor.");
    }

    /**
     * Restores only the in-memory topology of the controller a staff member is targeting.
     * A non-controller or a vanilla block is reported and left untouched.
     */
    private void repairNetwork(@Nonnull Player player) {
        if (!player.isOp() && !player.hasPermission("networks.admin")) {
            player.sendMessage(Theme.ERROR + "You do not have permission to repair a network.");
            return;
        }

        Block block = player.getTargetBlockExact(8);
        if (block == null) {
            player.sendMessage(Theme.ERROR + "Look directly at a Network Controller within 8 blocks.");
            return;
        }

        SlimefunItem item = com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage.check(block);
        if (item instanceof NetworkController controller) {
            int nodes = controller.rebuildNetwork(block).getNodeCount();
            player.sendMessage(Theme.SUCCESS + "Network topology rebuilt: " + nodes + " node(s).");
        } else if (item instanceof NetworkObject) {
            player.sendMessage(Theme.WARNING + "Look directly at this network's controller, not a child node.");
        } else {
            player.sendMessage(Theme.ERROR + "This block has no valid Networks identity. Nothing was changed.");
        }
    }

    /** Repairs the current chunk's already persisted Networks blocks without rewriting Slimefun data. */
    private void repairChunk(@Nonnull Player player) {
        if (!player.isOp() && !player.hasPermission("networks.admin")) {
            player.sendMessage(Theme.ERROR + "You do not have permission to repair a network.");
            return;
        }

        NetworkStorage.RepairSummary summary = NetworkStorage.repairChunk(player.getLocation().getChunk());
        player.sendMessage(Theme.SUCCESS + "Chunk repair completed: " + summary.discoveredNodes()
                + " node(s), " + summary.recoveredNodes() + " restored, "
                + summary.rebuiltControllers() + " controller(s) rebuilt.");
        if (summary.invalidEntries() > 0) {
            player.sendMessage(Theme.WARNING + "Detected " + summary.invalidEntries()
                    + " invalid Slimefun record(s); they were not modified.");
        }
    }

    public void fillQuantum(Player player, int amount) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            player.sendMessage(Theme.ERROR + "Item in hand must be a Quantum Storage.");
            return;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);

        if (!(slimefunItem instanceof NetworkQuantumStorage)) {
            player.sendMessage(Theme.ERROR + "Item in hand must be a Quantum Storage.");
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        final QuantumCache quantumCache = DataTypeMethods.getCustom(
            meta,
            Keys.QUANTUM_STORAGE_INSTANCE,
            PersistentQuantumStorageType.TYPE
        );

        if (quantumCache == null || quantumCache.getItemStack() == null) {
            player.sendMessage(Theme.ERROR + "This card has either not been set to an item yet or is a corrupted Quantum Storage.");
            return;
        }

        quantumCache.setAmount(amount);
        DataTypeMethods.setCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, quantumCache);
        quantumCache.updateMetaLore(meta);
        itemStack.setItemMeta(meta);
        player.sendMessage(Theme.SUCCESS + "Item updated");
    }
}
