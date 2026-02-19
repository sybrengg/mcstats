package com.sybre.mcstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the MC Stats mod. Stored as JSON in config/mcstats.json.
 */
public class ModConfig {
    private static ModConfig INSTANCE;

    public String _help = "MC Stats Mod Configuration. Change database_type to switch backends. Restart server after editing.";

    // Database type selector
    public String _database_type_options = "Options: 'logging' (console only), 'mysql', 'sqlite', 'webhook'";
    public String database_type = "logging";

    // How often buffered stats are flushed to the database (in seconds)
    public int flush_interval_seconds = 30;

    public MySQLConfig mysql = new MySQLConfig();
    public SQLiteConfig sqlite = new SQLiteConfig();
    public WebhookConfig webhook = new WebhookConfig();

    public static class MySQLConfig {
        public String _help = "Connect to a MySQL or MariaDB server. The mod auto-creates the 'stat_events' table.";
        public String host = "localhost";
        public int port = 3306;
        public String database = "mcstats";
        public String username = "root";
        public String password = "";
    }

    public static class SQLiteConfig {
        public String _help = "Zero-config file database. The .db file is created in your server root folder.";
        public String file = "mcstats.db";
    }

    public static class WebhookConfig {
        public String _help = "Pushes stats to a URL via HTTP POST every flush cycle. The mod sends data TO your endpoint (one-way push, not a query). Your endpoint receives JSON with player_uuid, stats (map of stat_name to value), and a timestamp. Set api_key to secure it with a Bearer token.";
        public String _example_payload = "{\"player_uuid\":\"444be9c1-...\",\"stats\":{\"minecraft.mined:minecraft.stone\":42},\"timestamp\":1708204800000}";
        public String url = "http://localhost:8080/api/stats";
        public String api_key = "";
    }

    public static ModConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("mcstats.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                INSTANCE = gson.fromJson(json, ModConfig.class);
                StatsMod.LOGGER.info("Loaded config from {}", configFile);
            } catch (Exception e) {
                StatsMod.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            try {
                Files.createDirectories(configDir);
                Files.writeString(configFile, gson.toJson(INSTANCE));
                StatsMod.LOGGER.info("Created default config at {}", configFile);
            } catch (IOException e) {
                StatsMod.LOGGER.error("Failed to write default config", e);
            }
        }

        return INSTANCE;
    }

    public static ModConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }
}
