package com.sybre.mcstats;

import com.sybre.mcstats.db.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.biome.Biome;

import java.util.*;
import java.util.concurrent.*;

public class StatsCollector {
    private static StatsCollector INSTANCE;

    private final int flushIntervalSeconds;
    private final DatabaseManager dbManager;

    // Buffer: playerUUID -> (statName -> accumulated delta)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Integer>> buffer = new ConcurrentHashMap<>();

    // Biome tracker: playerUUID -> set of visited biome IDs
    private final ConcurrentHashMap<UUID, Set<String>> visitedBiomes = new ConcurrentHashMap<>();

    // Track active server players for biome checking
    private final ConcurrentHashMap<UUID, ServerPlayer> activePlayers = new ConcurrentHashMap<>();

    private static final int BIOME_CHECK_INTERVAL_SECONDS = 5;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mcstats-flush");
        t.setDaemon(true);
        return t;
    });

    private StatsCollector(ModConfig config) {
        this.flushIntervalSeconds = config.flush_interval_seconds;
        this.dbManager = createDatabaseManager(config);
        this.dbManager.initialize();

        // Schedule periodic flush
        scheduler.scheduleAtFixedRate(this::flushAll, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);
        // Schedule biome checks
        scheduler.scheduleAtFixedRate(this::checkBiomes, BIOME_CHECK_INTERVAL_SECONDS, BIOME_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        StatsMod.LOGGER.info("Stats collector started (type={}, flush={}s)", config.database_type, flushIntervalSeconds);
    }

    private static DatabaseManager createDatabaseManager(ModConfig config) {
        return switch (config.database_type.toLowerCase()) {
            case "mysql", "mariadb" -> new MySQLDatabaseManager(
                config.mysql.host, config.mysql.port,
                config.mysql.database, config.mysql.username, config.mysql.password
            );
            case "sqlite" -> new SQLiteDatabaseManager(config.sqlite.file);
            case "webhook", "http" -> new WebhookDatabaseManager(config.webhook.url, config.webhook.api_key);
            default -> {
                StatsMod.LOGGER.info("Using logging database (type='{}')", config.database_type);
                yield new LoggingDatabaseManager();
            }
        };
    }

    public static void init(ModConfig config) {
        INSTANCE = new StatsCollector(config);
    }

    public static StatsCollector getInstance() {
        return INSTANCE;
    }

    // --- Event handlers ---

    public void onPlayerJoin(ServerPlayer player) {
        StatsMod.LOGGER.info("Player joined: {}", player.getName().getString());
        buffer.putIfAbsent(player.getUUID(), new ConcurrentHashMap<>());
        visitedBiomes.putIfAbsent(player.getUUID(), ConcurrentHashMap.newKeySet());
        activePlayers.put(player.getUUID(), player);
    }

    public void onPlayerLeave(ServerPlayer player) {
        StatsMod.LOGGER.info("Player left: {}", player.getName().getString());
        // Flush biome count before disconnect
        Set<String> biomes = visitedBiomes.remove(player.getUUID());
        if (biomes != null && !biomes.isEmpty()) {
            recordCustomStat(player, "mcstats.custom:unique_biomes_visited", biomes.size());
        }
        flushPlayer(player.getUUID());
        buffer.remove(player.getUUID());
        activePlayers.remove(player.getUUID());
    }

    public void onStatIncrease(ServerPlayer player, Stat<?> stat, int amount) {
        String statName = stat.getName();
        buffer.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
              .merge(statName, amount, Integer::sum);
    }

    /**
     * Record a custom (non-vanilla) stat. Used by custom Mixins.
     */
    public void recordCustomStat(ServerPlayer player, String statName, int amount) {
        buffer.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
              .merge(statName, amount, Integer::sum);
    }

    /**
     * Record a max-stat (keeps only the highest value).
     */
    public void recordMaxStat(ServerPlayer player, String statName, int value) {
        buffer.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
              .merge(statName, value, Math::max);
    }

    /**
     * Push a baseline stat snapshot directly to the database (for retroactive import).
     */
    public void pushBaseline(UUID playerUUID, Map<String, Integer> stats) {
        dbManager.pushStats(playerUUID, stats);
    }

    // --- Biome tracking ---

    private void checkBiomes() {
        for (Map.Entry<UUID, ServerPlayer> entry : activePlayers.entrySet()) {
            try {
                ServerPlayer player = entry.getValue();
                if (player.isRemoved()) continue;

                Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
                biomeHolder.unwrapKey().ifPresent(key -> {
                    String biomeId = key.toString();
                    Set<String> biomes = visitedBiomes.computeIfAbsent(entry.getKey(), k -> ConcurrentHashMap.newKeySet());
                    if (biomes.add(biomeId)) {
                        StatsMod.LOGGER.info("New biome visited by {}: {}", player.getName().getString(), biomeId);
                    }
                });
            } catch (Exception e) {
                // Safely ignore
            }
        }
    }

    // --- Flush logic ---

    private void flushAll() {
        if (buffer.isEmpty()) return;

        int totalStats = 0;
        for (Map.Entry<UUID, ConcurrentHashMap<String, Integer>> entry : buffer.entrySet()) {
            totalStats += flushPlayer(entry.getKey());
        }

        if (totalStats > 0) {
            StatsMod.LOGGER.info("Flushed {} stat entries for {} players", totalStats, buffer.size());
        }
    }

    private int flushPlayer(UUID playerUUID) {
        ConcurrentHashMap<String, Integer> playerStats = buffer.get(playerUUID);
        if (playerStats == null || playerStats.isEmpty()) return 0;

        Map<String, Integer> snapshot = new ConcurrentHashMap<>(playerStats);
        playerStats.clear();

        dbManager.pushStats(playerUUID, snapshot);
        return snapshot.size();
    }

    public void shutdown() {
        StatsMod.LOGGER.info("Shutting down stats collector, flushing remaining data...");
        scheduler.shutdown();
        for (Map.Entry<UUID, ServerPlayer> entry : activePlayers.entrySet()) {
            Set<String> biomes = visitedBiomes.get(entry.getKey());
            if (biomes != null && !biomes.isEmpty()) {
                recordCustomStat(entry.getValue(), "mcstats.custom:unique_biomes_visited", biomes.size());
            }
        }
        flushAll();
        dbManager.shutdown();
    }
}
