package com.sybre.mcstats.db;

import com.google.gson.Gson;
import com.sybre.mcstats.StatsMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP Webhook database manager. POSTs stats as JSON to a configurable endpoint.
 */
public class WebhookDatabaseManager implements DatabaseManager {
    private final String url;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public WebhookDatabaseManager(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public void initialize() {
        StatsMod.LOGGER.info("Webhook configured to POST to {}", url);
    }

    @Override
    public void pushStats(UUID playerUUID, Map<String, Integer> stats) {
        if (stats.isEmpty()) return;

        // Build JSON payload
        StatsPayload payload = new StatsPayload();
        payload.player_uuid = playerUUID.toString();
        payload.stats = stats;
        payload.timestamp = System.currentTimeMillis();

        String json = gson.toJson(payload);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));

        // Add API key header if configured
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        // Send asynchronously to not block the flush thread
        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    StatsMod.LOGGER.debug("Webhook push success for {}", playerUUID);
                } else {
                    StatsMod.LOGGER.warn("Webhook returned status {} for {}", response.statusCode(), playerUUID);
                }
            })
            .exceptionally(e -> {
                StatsMod.LOGGER.error("Webhook push failed for {}", playerUUID, e);
                return null;
            });
    }

    @Override
    public void shutdown() {
        StatsMod.LOGGER.info("Webhook manager shut down");
    }

    /**
     * JSON payload structure sent to the webhook endpoint.
     */
    private static class StatsPayload {
        String player_uuid;
        Map<String, Integer> stats;
        long timestamp;
    }
}
