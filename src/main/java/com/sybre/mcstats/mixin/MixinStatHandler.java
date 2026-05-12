package com.sybre.mcstats.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatsCounter.class)
public class MixinStatHandler {

    @Inject(method = "increment", at = @At("HEAD"))
    private void onIncrement(Player player, Stat<?> stat, int value, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            com.sybre.mcstats.StatsCollector.getInstance().onStatIncrease(serverPlayer, stat, value);
        }
    }
}
