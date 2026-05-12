package com.sybre.mcstats;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Imports existing vanilla stats from world/stats/*.json on first startup.
 * Creates a flag file so it only runs once per world.
 */
public class BaselineImporter {

    private static final Gson GSON = new Gson();

    /**
     * Run the baseline import if it hasn't been done yet.
     */
    public static void importIfNeeded(MinecraftServer server) {
        Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path flagFile = worldDir.resolve("mcstats_baseline_imported.flag");

        if (Files.exists(flagFile)) {
            StatsMod.LOGGER.info("Baseline stats already imported, skipping. Use /mcstats import force to re-import.");
            return;
        }

        runImport(server);
    }

    /**
     * Force a re-import, deleting the flag file first.
     */
    public static void forceImport(MinecraftServer server) {
        Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path flagFile = worldDir.resolve("mcstats_baseline_imported.flag");

        try {
            Files.deleteIfExists(flagFile);
        } catch (IOException e) {
            StatsMod.LOGGER.error("Failed to delete flag file", e);
        }

        runImport(server);
    }

    private static void runImport(MinecraftServer server) {
        Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path flagFile = worldDir.resolve("mcstats_baseline_imported.flag");
        Path statsDir = worldDir.resolve("stats");

        if (!Files.isDirectory(statsDir)) {
            StatsMod.LOGGER.info("No stats directory found at {}, skipping baseline import.", statsDir);
            return;
        }

        StatsMod.LOGGER.info("Starting baseline stat import from {}...", statsDir);
        int totalPlayers = 0;
        int totalStats = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(statsDir, "*.json")) {
            for (Path statFile : stream) {
                String fileName = statFile.getFileName().toString();
                String uuidStr = fileName.replace(".json", "");

                UUID playerUUID;
                try {
                    playerUUID = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    StatsMod.LOGGER.warn("Skipping invalid stat file: {}", fileName);
                    continue;
                }

                Map<String, Integer> parsedStats = parseStatFile(statFile);
                if (!parsedStats.isEmpty()) {
                    StatsCollector.getInstance().pushBaseline(playerUUID, parsedStats);
                    totalPlayers++;
                    totalStats += parsedStats.size();
                }
            }
        } catch (IOException e) {
            StatsMod.LOGGER.error("Failed to read stats directory", e);
            return;
        }

        // Write flag file
        try {
            Files.writeString(flagFile, "Baseline import completed at " + new Date());
            StatsMod.LOGGER.info("Baseline import complete: {} stats for {} players", totalStats, totalPlayers);
        } catch (IOException e) {
            StatsMod.LOGGER.error("Failed to write baseline flag file", e);
        }
    }

    /**
     * Parse a vanilla stat JSON file into a flat map of stat_name -> value.
     *
     * Vanilla format:
     * {
     *   "stats": {
     *     "minecraft:mined": { "minecraft:stone": 42 },
     *     "minecraft:custom": { "minecraft:jump": 100 }
     *   }
     * }
     *
     * Output format (matching Stat.getName() style):
     *   "minecraft.mined:minecraft.stone" -> 42
     *   "minecraft.custom:minecraft.jump" -> 100
     */
    private static Map<String, Integer> parseStatFile(Path file) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String content = Files.readString(file);
            JsonObject root = GSON.fromJson(content, JsonObject.class);

            JsonObject stats = root.getAsJsonObject("stats");
            if (stats == null) return result;

            for (Map.Entry<String, JsonElement> category : stats.entrySet()) {
                // category key: "minecraft:mined" -> "minecraft.mined"
                String categoryKey = category.getKey().replace(":", ".");

                JsonObject entries = category.getValue().getAsJsonObject();
                if (entries == null) continue;

                for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                    // entry key: "minecraft:stone"
                    String itemKey = entry.getKey();
                    int value = entry.getValue().getAsInt();

                    if (value > 0) {
                        // Final format: "minecraft.mined:minecraft.stone"
                        String statName = categoryKey + ":" + itemKey;
                        result.put(statName, value);
                    }
                }
            }
        } catch (Exception e) {
            StatsMod.LOGGER.error("Failed to parse stat file {}", file, e);
        }

        return result;
    }
}
