package com.sybre.mcstats;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsMod implements ModInitializer {
    public static final String MOD_ID = "mcstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MC Stats Mod");

        // Load config and initialize stats collector
        ModConfig config = ModConfig.load();
        StatsCollector.init(config);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            StatsCollector.getInstance().onPlayerJoin(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            StatsCollector.getInstance().onPlayerLeave(handler.getPlayer());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            BaselineImporter.importIfNeeded(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            StatsCollector.getInstance().shutdown();
        });
    }
}
