package com.sybre.mcstats.db;

import com.sybre.mcstats.StatsMod;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL/MariaDB database manager. Stores stat events as time-series rows.
 */
public class MySQLDatabaseManager implements DatabaseManager {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public MySQLDatabaseManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true", host, port, database);
            connection = DriverManager.getConnection(url, username, password);
            createTable();
            StatsMod.LOGGER.info("MySQL connected to {}:{}/{}", host, port, database);
        } catch (Exception e) {
            StatsMod.LOGGER.error("Failed to connect to MySQL", e);
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stat_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    stat_name VARCHAR(255) NOT NULL,
                    stat_value INT NOT NULL,
                    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_player (player_uuid),
                    INDEX idx_stat (stat_name),
                    INDEX idx_time (recorded_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
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
            StatsMod.LOGGER.error("Failed to push stats to MySQL", e);
            reconnect();
        }
    }

    private void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
            initialize();
        } catch (SQLException e) {
            StatsMod.LOGGER.error("Failed to reconnect to MySQL", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                StatsMod.LOGGER.info("MySQL connection closed");
            }
        } catch (SQLException e) {
            StatsMod.LOGGER.error("Error closing MySQL connection", e);
        }
    }
}
