package com.sybre.mcstats.db;

import com.sybre.mcstats.StatsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite database manager. Zero-config file-based storage.
 */
public class SQLiteDatabaseManager implements DatabaseManager {
    private final String fileName;
    private Connection connection;

    public SQLiteDatabaseManager(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            Path dbPath = FabricLoader.getInstance().getGameDir().resolve(fileName);
            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Enable WAL mode for better concurrent performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
            }

            createTable();
            StatsMod.LOGGER.info("SQLite database opened at {}", dbPath);
        } catch (Exception e) {
            StatsMod.LOGGER.error("Failed to initialize SQLite", e);
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stat_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    stat_name TEXT NOT NULL,
                    stat_value INTEGER NOT NULL,
                    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player ON stat_events (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stat ON stat_events (stat_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_time ON stat_events (recorded_at)");
        }
    }

    @Override
    public void pushStats(UUID playerUUID, Map<String, Integer> stats) {
        if (connection == null || stats.isEmpty()) return;

        String sql = "INSERT INTO stat_events (player_uuid, stat_name, stat_value) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, entry.getKey());
                ps.setInt(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            StatsMod.LOGGER.error("Failed to push stats to SQLite", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                StatsMod.LOGGER.info("SQLite connection closed");
            }
        } catch (SQLException e) {
            StatsMod.LOGGER.error("Error closing SQLite connection", e);
        }
    }
}
