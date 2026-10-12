package io.github.sefiraat.networks.integrations;

import io.github.schntgaispock.slimehud.SlimeHUD;
import io.github.schntgaispock.slimehud.util.HudBuilder;
import io.github.schntgaispock.slimehud.waila.HudController;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.sefiraat.networks.slimefun.network.NetworkGrabber;
import io.github.sefiraat.networks.slimefun.network.NetworkGreedyBlock;
import io.github.sefiraat.networks.slimefun.network.NetworkPusher;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.Locale;

public final class HudCallbacks {

    private static final String EMPTY = "&7| Vacío";

    private HudCallbacks() {
    }

    public static void setup() {
        HudController controller = SlimeHUD.getHudController();
        if (controller == null) {
            return;
        }

        // NetworkController HUD
        controller.registerCustomHandler(NetworkController.class, request -> {
            Location loc = request.getLocation();
            NetworkRoot root = NetworkController.getNetworks().get(loc);
            if (root == null) {
                return "&7| &cRed no inicializada";
            }
            double flow = root.getThroughput().getItemsPerSecond();
            int nodes = root.getNodeCount();
            int max = root.getMaxNodes();
            long total = root.getThroughput().getTotalTransferredItems();

            return String.format(Locale.ROOT,
                "&7| &fNodos: &a%d&7/&b%d &7| &fFlujo: &e+%.1f/s &7| &6%s enrutados",
                nodes, max, flow, NumberFormat.getInstance().format(total));
        });

        // NetworkQuantumStorage HUD
        controller.registerCustomHandler(NetworkQuantumStorage.class, request -> {
            Location location = request.getLocation();
            QuantumCache cache = NetworkQuantumStorage.getCaches().get(location);
            if (cache == null || cache.getItemStack() == null) {
                return EMPTY;
            }

            NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(location);
            double flow = 0.0;
            if (def != null && def.getNode() != null && def.getNode().getRoot() != null) {
                flow = def.getNode().getRoot().getThroughput().getNodeItemsPerSecond(location);
            }

            return formatQuantum(cache.getItemStack(), cache.getAmount(), cache.getLimit(), flow);
        });

        // NetworkGreedyBlock HUD
        controller.registerCustomHandler(NetworkGreedyBlock.class, request -> {
            Location location = request.getLocation();
            BlockMenu menu = BlockStorage.getInventory(location);
            if (menu == null) {
                return EMPTY;
            }

            ItemStack templateStack = menu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);
            if (templateStack == null || templateStack.getType().isAir()) {
                return EMPTY;
            }

            ItemStack itemStack = menu.getItemInSlot(NetworkGreedyBlock.INPUT_SLOT);
            int amount = itemStack == null || itemStack.getType() != templateStack.getType() ? 0 : itemStack.getAmount();
            return formatSimple(templateStack, amount, templateStack.getMaxStackSize());
        });

        // NetworkPusher HUD
        controller.registerCustomHandler(NetworkPusher.class, request -> {
            Location location = request.getLocation();
            BlockFace face = NetworkDirectional.getSelectedFace(location);
            NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(location);
            double flow = 0.0;
            if (def != null && def.getNode() != null && def.getNode().getRoot() != null) {
                flow = def.getNode().getRoot().getThroughput().getNodeItemsPerSecond(location);
            }
            String dirStr = face != null ? face.name() : "NONE";
            return String.format(Locale.ROOT, "&7| &bPusher: &e%s &7| &fFlujo: &a+%.1f/s", dirStr, flow);
        });

        // NetworkGrabber HUD
        controller.registerCustomHandler(NetworkGrabber.class, request -> {
            Location location = request.getLocation();
            BlockFace face = NetworkDirectional.getSelectedFace(location);
            NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(location);
            double flow = 0.0;
            if (def != null && def.getNode() != null && def.getNode().getRoot() != null) {
                flow = def.getNode().getRoot().getThroughput().getNodeItemsPerSecond(location);
            }
            String dirStr = face != null ? face.name() : "NONE";
            return String.format(Locale.ROOT, "&7| &dGrabber: &e%s &7| &fFlujo: &a+%.1f/s", dirStr, flow);
        });
    }

    private static String formatQuantum(ItemStack itemStack, int amount, int limit, double flow) {
        ItemMeta meta = itemStack.getItemMeta();
        String amountStr = HudBuilder.getAbbreviatedNumber(amount);
        String limitStr = HudBuilder.getAbbreviatedNumber(limit);
        String itemName = meta != null && meta.hasDisplayName()
                ? meta.getDisplayName()
                : ChatUtils.humanize(itemStack.getType().name());

        double pct = limit > 0 ? (amount * 100.0 / limit) : 0.0;
        if (flow > 0.05) {
            return String.format(Locale.ROOT, "&7| &f%s &7| &a%s&7/&e%s &7(&b%.1f%%&7) &7| &f+%.1f/s",
                itemName, amountStr, limitStr, pct, flow);
        }
        return String.format(Locale.ROOT, "&7| &f%s &7| &a%s&7/&e%s &7(&b%.1f%%&7)",
            itemName, amountStr, limitStr, pct);
    }

    private static String formatSimple(ItemStack itemStack, int amount, int limit) {
        ItemMeta meta = itemStack.getItemMeta();
        String amountStr = HudBuilder.getAbbreviatedNumber(amount);
        String limitStr = HudBuilder.getAbbreviatedNumber(limit);
        String itemName = meta != null && meta.hasDisplayName()
                ? meta.getDisplayName()
                : ChatUtils.humanize(itemStack.getType().name());

        return "&7| &f" + itemName + " &7| " + amountStr + "/" + limitStr;
    }
}
