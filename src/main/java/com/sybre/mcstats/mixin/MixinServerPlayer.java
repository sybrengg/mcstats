package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer {

    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        String killerName = source.getEntity() != null ? source.getEntity().getName().getString() : "none";
        String damageType = source.getMsgId();
        
        com.sybre.mcstats.StatsCollector.getInstance()
            .recordCustomStat(player, "mcstats.death:" + damageType + ":" + killerName, 1);
    }
}
