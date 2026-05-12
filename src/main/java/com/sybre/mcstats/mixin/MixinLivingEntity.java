package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks totem of undying usage.
 * checkTotemDeathProtection() is declared on LivingEntity.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onTotemUse(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object) this instanceof ServerPlayer serverPlayer) {
            com.sybre.mcstats.StatsCollector.getInstance()
                .recordCustomStat(serverPlayer, "mcstats.custom:totems_used", 1);
        }
    }
}
