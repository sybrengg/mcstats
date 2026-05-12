package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks highest single-hit damage dealt by players.
 * In 1.21.11, hurt(DamageSource, float) is void and declared on Entity.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "hurt", at = @At("TAIL"))
    private void onHurt(DamageSource source, float amount, CallbackInfo ci) {
        if (source.getEntity() instanceof ServerPlayer attacker && amount > 0) {
            int dmgTenths = Math.round(amount * 10);
            com.sybre.mcstats.StatsCollector.getInstance()
                .recordMaxStat(attacker, "mcstats.custom:highest_single_hit", dmgTenths);
        }
    }
}
