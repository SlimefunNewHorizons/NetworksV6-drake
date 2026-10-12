package io.github.sefiraat.networks.listeners;

import com.github.drakescraft_labs.slimefun4.libraries.dough.blocks.BlockPosition;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockStateRefreshListener implements Listener {
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        setNotUseSnapshot(bp(event.getBlock()));
    }

    @EventHandler
    public void onBreak2(EntityChangeBlockEvent event) {
        setNotUseSnapshot(bp(event.getBlock()));
    }

    @EventHandler
    public void onBreak3(StructureGrowEvent event) {
        for (BlockState block : event.getBlocks()) {
            setNotUseSnapshot(bp(block.getX(), block.getY(), block.getZ()));
        }
    }

    @EventHandler
    public void onBreak4(BlockFromToEvent event) {
        setNotUseSnapshot(bp(event.getBlock()));
        setNotUseSnapshot(bp(event.getToBlock()));
    }

    // contains  = use snapshot
    // !contains = not use snapshot
    // LongOpenHashSet no es thread-safe; wrappear con synchronizedLongSet para accesos desde async chunk-loader de Paper.
    static LongSet set = LongSets.synchronize(new LongOpenHashSet(4096));

    public static BlockState getState(Block block) {
        long v = bp(block.getX(), block.getY(), block.getZ());
        boolean r = set.contains(v);
        set.add(v);
        return block.getState(r);
    }

    public static BlockState getFreshState(Block block) {
        setNotUseSnapshot(bp(block.getX(), block.getY(), block.getZ()));
        return block.getState(false);
    }

    private static void setNotUseSnapshot(long position) {
        set.remove(position);
    }

    long bp(Block block) {
        return bp(block.getX(), block.getY(), block.getZ());
    }

    // Refresh profiling is intentionally kept out of the production listener.
    static long bp(int x, int y, int z) {
        return BlockPosition.getAsLong(x, y, z);
    }
}
