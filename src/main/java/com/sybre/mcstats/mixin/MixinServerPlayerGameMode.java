package com.sybre.mcstats.mixin;

import com.sybre.mcstats.PlacedBlockTracker;
import com.sybre.mcstats.StatsCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into block breaking to emit mcstats.natural_mined:block_id
 * for blocks that were NOT placed by a player.
 */
@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode {

    @Shadow
    protected ServerLevel level;

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        try {
            BlockState state = level.getBlockState(pos);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

            boolean wasPlaced = PlacedBlockTracker.getInstance().wasPlacedAndRemove(level, pos);

            if (!wasPlaced && player != null) {
                StatsCollector.getInstance()
                    .recordCustomStat(player, "mcstats.natural_mined:" + blockId, 1);
            }
        } catch (Exception e) {
            // Silently ignore errors to not crash the game
        }
    }
}
