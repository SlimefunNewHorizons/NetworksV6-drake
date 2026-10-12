package io.github.sefiraat.networks.listeners;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.utils.NetworkIntegrity;
import io.github.sefiraat.networks.utils.NetworkUtils;
import com.github.drakescraft_labs.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Impide que picos/shovels explosivos rompan máquinas NTW sin limpiar el grafo (#229).
 */
public class ExplosiveToolListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosiveBlockBreak(@Nonnull ExplosiveToolBreakBlocksEvent event) {
        final List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : event.getAdditionalBlocks()) {
            final Location location = block.getLocation();
            if (NetworkIntegrity.isNetworksMachine(location)
                    || NetworkStorage.getAllNetworkObjects().containsKey(location)) {
                NetworkUtils.clearNetwork(location);
                blocksToRemove.add(block);
            }
        }

        event.getAdditionalBlocks().removeAll(blocksToRemove);
    }
}
