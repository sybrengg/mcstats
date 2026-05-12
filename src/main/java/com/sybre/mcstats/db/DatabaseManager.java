package com.sybre.mcstats.db;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for pushing player statistics to an external database.
 * Implementations can use JDBC, HTTP webhooks, or any other transport.
 */
public interface DatabaseManager {
    void initialize();
    void shutdown();

    /**
     * Push a batch of stat changes for a player.
     * @param playerUUID the player's UUID
     * @param stats map of stat name to delta value
     */
    void pushStats(UUID playerUUID, Map<String, Integer> stats);
}
