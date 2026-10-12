package io.github.sefiraat.networks.tasks;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.slimefun.tools.NetworkCard;
import io.github.sefiraat.networks.slimefun.tools.NetworkConfigurator;
import io.github.sefiraat.networks.slimefun.tools.NetworkCrayon;
import io.github.sefiraat.networks.slimefun.tools.NetworkProbe;
import io.github.sefiraat.networks.slimefun.tools.NetworkRemote;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tarea periódica de bajo costo (ejecutada cada 10 ticks = 0.5s) que muestra telemetría
 * en la barra de acción del jugador cuando sostiene herramientas de red o inspecciona agachado.
 */
public class NetworkActionBarTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid()) {
                continue;
            }

            boolean isHoldingTool = isHoldingNetworkTool(player);
            boolean isSneaking = player.isSneaking();

            if (!isHoldingTool && !isSneaking) {
                continue;
            }

            RayTraceResult trace = player.rayTraceBlocks(6.0);
            if (trace == null || trace.getHitBlock() == null) {
                continue;
            }

            Block block = trace.getHitBlock();
            Location loc = block.getLocation();
            NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(loc);
            if (definition == null || definition.getNode() == null) {
                continue;
            }

            NetworkNode node = definition.getNode();
            NetworkRoot root = node.getRoot();
            if (root == null) {
                continue;
            }

            double rootFlow = root.getThroughput().getItemsPerSecond();
            long totalTransferred = root.getThroughput().getTotalTransferredItems();
            double nodeFlow = root.getThroughput().getNodeItemsPerSecond(loc);
            int nodeCount = root.getNodeCount();
            int maxNodes = root.getMaxNodes();

            String typeName = definition.getType() != null ? definition.getType().name() : "NODO";
            String msg;

            if (nodeFlow > 0.05) {
                msg = String.format(Locale.ROOT,
                    "§6§lNETWORKS §8» §f%s §8| §dNodo: §a+%.1f/s §8| §fRed: §a+%.1f/s §8| §bNodos: §f%d/%d §8| §e%s enrutados",
                    typeName, nodeFlow, rootFlow, nodeCount, maxNodes, NumberFormat.getInstance().format(totalTransferred));
            } else {
                msg = String.format(Locale.ROOT,
                    "§6§lNETWORKS §8» §f%s §8| §fFlujo Red: §a+%.1f items/s §8| §bNodos: §f%d/%d §8| §e%s enrutados",
                    typeName, rootFlow, nodeCount, maxNodes, NumberFormat.getInstance().format(totalTransferred));
            }

            player.sendActionBar(Component.text(msg));
        }
    }

    private boolean isHoldingNetworkTool(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isTool(main) || isTool(off);
    }

    private boolean isTool(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        return sfItem instanceof NetworkProbe
            || sfItem instanceof NetworkConfigurator
            || sfItem instanceof NetworkCrayon
            || sfItem instanceof NetworkRemote
            || sfItem instanceof NetworkCard;
    }
}
