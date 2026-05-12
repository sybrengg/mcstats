package com.sybre.mcstats.db;

import com.sybre.mcstats.StatsMod;
import java.util.Map;
import java.util.UUID;

public class LoggingDatabaseManager implements DatabaseManager {

    @Override
    public void initialize() {
        StatsMod.LOGGER.info("LoggingDatabaseManager initialized (No active DB connection).");
    }

    @Override
    public void shutdown() {
        StatsMod.LOGGER.info("LoggingDatabaseManager shutting down.");
    }

    @Override
    public void pushStats(UUID playerUUID, Map<String, Integer> stats) {
        StatsMod.LOGGER.info("DB PUSH [{}]: {}", playerUUID, stats);
    }
}
