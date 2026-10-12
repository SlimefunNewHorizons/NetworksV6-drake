package io.github.sefiraat.networks.persistence;

import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional, write-only SQLite mirror for quantum storage state.
 *
 * <p>Slimefun's {@code BlockStorage} remains authoritative. This class deliberately does not
 * restore values from SQLite, preventing a failed or stale database from creating a second source
 * of truth for items. A future explicit importer can consume this mirror after it has been
 * validated in production.</p>
 */
public final class QuantumStorageMirror implements AutoCloseable {

    private static final String MIRROR_SQL = "mirror-sql";

    private final Networks plugin;
    private final Map<String, Snapshot> pending = new ConcurrentHashMap<>();
    private final ExecutorService writer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final boolean enabled;
    private Connection connection;
    private BukkitTask flushTask;

    private QuantumStorageMirror(@Nonnull Networks plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.writer = enabled ? Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Networks-Quantum-SQL");
            thread.setDaemon(true);
            return thread;
        }) : null;
    }

    /** Creates the mirror only when it was explicitly enabled in config.yml. */
    @Nonnull
    public static QuantumStorageMirror create(@Nonnull Networks plugin) {
        String mode = plugin.getConfig().getString("persistence.quantum.mode", "slimefun");
        if (!MIRROR_SQL.equalsIgnoreCase(mode)) {
            return new QuantumStorageMirror(plugin, false);
        }

        QuantumStorageMirror mirror = new QuantumStorageMirror(plugin, true);
        try {
            mirror.open();
            int flushSeconds = Math.max(1, plugin.getConfig().getInt("persistence.quantum.flush-seconds", 5));
            mirror.flushTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                mirror::flushAsync,
                20L * flushSeconds,
                20L * flushSeconds
            );
            plugin.getLogger().info("Quantum storage SQL mirror enabled; BlockStorage remains authoritative.");
        } catch (Exception exception) {
            plugin.getLogger().severe("Quantum storage SQL mirror disabled: " + exception.getMessage());
            mirror.close();
            return new QuantumStorageMirror(plugin, false);
        }

        return mirror;
    }

    /** Queues the latest state for a location; repeated ticks are coalesced into one database write. */
    public void record(@Nonnull Location location, @Nonnull QuantumCache cache) {
        if (!enabled || closed.get() || location.getWorld() == null) {
            return;
        }

        ItemStack item = cache.getItemStack();
        Snapshot snapshot = new Snapshot(
            location.getWorld().getUID().toString(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            item == null ? null : item.clone(),
            cache.getAmount(),
            cache.isVoidExcess()
        );
        pending.put(snapshot.key(), snapshot);
    }

    private void open() throws ClassNotFoundException, IOException, SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Could not create plugin data directory");
        }

        String fileName = plugin.getConfig().getString("persistence.quantum.sqlite-file", "quantum-storage-mirror.db");
        if (fileName == null || fileName.isBlank()) {
            throw new SQLException("SQLite mirror file name cannot be empty");
        }
        File databaseFile = new File(dataFolder, fileName).getCanonicalFile();
        if (!databaseFile.getParentFile().equals(dataFolder.getCanonicalFile())) {
            throw new SQLException("SQLite mirror file must stay inside the Networks data directory");
        }

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS quantum_storage_mirror (
                    world_uuid TEXT NOT NULL,
                    block_x INTEGER NOT NULL,
                    block_y INTEGER NOT NULL,
                    block_z INTEGER NOT NULL,
                    item_data TEXT,
                    amount INTEGER NOT NULL,
                    void_excess INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (world_uuid, block_x, block_y, block_z)
                )
                """);
        }
    }

    private void flushAsync() {
        if (!enabled || closed.get() || pending.isEmpty()) {
            return;
        }

        Map<String, Snapshot> batch = drainPending();
        if (!batch.isEmpty()) {
            submit(batch);
        }
    }

    @Nonnull
    private Map<String, Snapshot> drainPending() {
        Map<String, Snapshot> batch = new HashMap<>();
        pending.forEach((key, value) -> {
            if (pending.remove(key, value)) {
                batch.put(key, value);
            }
        });
        return batch;
    }

    private void persist(@Nonnull Map<String, Snapshot> batch) {
        if (connection == null) {
            pending.putAll(batch);
            return;
        }

        String sql = """
            INSERT INTO quantum_storage_mirror
            (world_uuid, block_x, block_y, block_z, item_data, amount, void_excess, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(world_uuid, block_x, block_y, block_z) DO UPDATE SET
                item_data = excluded.item_data,
                amount = excluded.amount,
                void_excess = excluded.void_excess,
                updated_at = excluded.updated_at
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Snapshot snapshot : batch.values()) {
                statement.setString(1, snapshot.worldUuid());
                statement.setInt(2, snapshot.x());
                statement.setInt(3, snapshot.y());
                statement.setInt(4, snapshot.z());
                statement.setString(5, serialize(snapshot.item()));
                statement.setInt(6, snapshot.amount());
                statement.setInt(7, snapshot.voidExcess() ? 1 : 0);
                statement.setLong(8, System.currentTimeMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException | IOException exception) {
            plugin.getLogger().warning("Quantum storage SQL mirror write failed: " + exception.getMessage());
            batch.forEach(pending::putIfAbsent);
        }
    }

    /**
     * Keeps a scheduler tick from failing during plugin shutdown if the last asynchronous flush
     * races with the single writer being closed.
     */
    private void submit(@Nonnull Map<String, Snapshot> batch) {
        try {
            writer.execute(() -> persist(batch));
        } catch (RejectedExecutionException exception) {
            pending.putAll(batch);
        }
    }

    private String serialize(ItemStack item) throws IOException {
        if (item == null) {
            return null;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream stream = new BukkitObjectOutputStream(bytes)) {
            stream.writeObject(item);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }

    @Override
    public void close() {
        if (!enabled || !closed.compareAndSet(false, true)) {
            return;
        }
        if (flushTask != null) {
            flushTask.cancel();
        }
        Map<String, Snapshot> lastBatch = drainPending();
        if (!lastBatch.isEmpty()) {
            submit(lastBatch);
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not close quantum SQL mirror: " + exception.getMessage());
        }
    }

    private record Snapshot(
        String worldUuid,
        int x,
        int y,
        int z,
        ItemStack item,
        int amount,
        boolean voidExcess
    ) {
        private String key() {
            return worldUuid + ':' + x + ':' + y + ':' + z;
        }
    }
}
