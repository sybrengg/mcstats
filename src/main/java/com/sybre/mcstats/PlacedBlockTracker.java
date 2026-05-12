package com.sybre.mcstats;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks block positions that were placed by players.
 * When a block is broken, we check if it was player-placed.
 * If NOT, it's considered "naturally generated" and emits a separate stat.
 *
 * Memory-safe: positions are removed when blocks are broken.
 * Resets on server restart (acceptable since natural resources don't get replanted).
 */
public class PlacedBlockTracker {
    private static final PlacedBlockTracker INSTANCE = new PlacedBlockTracker();

    // dimension -> set of placed positions
    private final ConcurrentHashMap<ResourceKey<Level>, Set<Long>> placedPositions = new ConcurrentHashMap<>();

    public static PlacedBlockTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Mark a position as player-placed.
     */
    public void markPlaced(Level level, BlockPos pos) {
        placedPositions
            .computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
            .add(pos.asLong());
    }

    /**
     * Check if a position was player-placed, and remove it from tracking.
     * Returns true if the block WAS player-placed (i.e., NOT natural).
     */
    public boolean wasPlacedAndRemove(Level level, BlockPos pos) {
        Set<Long> positions = placedPositions.get(level.dimension());
        if (positions == null) return false;
        return positions.remove(pos.asLong());
    }

    /**
     * Clear all tracking data (e.g., on server stop).
     */
    public void clear() {
        placedPositions.clear();
    }

    /**
     * Get total tracked positions (for debugging).
     */
    public int getTrackedCount() {
        return placedPositions.values().stream().mapToInt(Set::size).sum();
    }
}
