package com.sybre.mcstats;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Registers /mcstats commands.
 * Only registered as a dedicated server command (OP level 4 required by default).
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("mcstats")
                .then(Commands.literal("import")
                    .executes(ctx -> runImport(ctx.getSource(), false))
                    .then(Commands.literal("force")
                        .executes(ctx -> runImport(ctx.getSource(), true))
                    )
                )
                .then(Commands.literal("status")
                    .executes(ctx -> showStatus(ctx.getSource()))
                )
        );
    }

    private static int runImport(CommandSourceStack source, boolean force) {
        MinecraftServer server = source.getServer();

        String mode = force ? "force re-import" : "import";
        source.sendSuccess(() -> Component.literal("[MC Stats] Starting baseline " + mode + "..."), true);

        // Run async to not block the server thread
        new Thread(() -> {
            try {
                if (force) {
                    BaselineImporter.forceImport(server);
                } else {
                    BaselineImporter.importIfNeeded(server);
                }
                server.execute(() ->
                    source.sendSuccess(() -> Component.literal("[MC Stats] Baseline " + mode + " complete!"), true)
                );
            } catch (Exception e) {
                server.execute(() ->
                    source.sendFailure(Component.literal("[MC Stats] Import failed: " + e.getMessage()))
                );
            }
        }, "mcstats-import").start();

        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        PlacedBlockTracker tracker = PlacedBlockTracker.getInstance();

        source.sendSuccess(() -> Component.literal(
            "[MC Stats] Status:\n" +
            "  Tracked placed blocks: " + tracker.getTrackedCount() + "\n" +
            "  Database type: " + ModConfig.get().database_type + "\n" +
            "  Flush interval: " + ModConfig.get().flush_interval_seconds + "s"
        ), false);

        return 1;
    }
}
