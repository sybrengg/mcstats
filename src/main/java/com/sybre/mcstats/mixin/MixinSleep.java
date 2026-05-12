package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinSleep {

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void onWakeUp(boolean updateSleepCounter, boolean notifyClients, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.isSleeping()) {
            com.sybre.mcstats.StatsCollector.getInstance()
                .recordCustomStat(self, "mcstats.custom:times_slept", 1);
        }
    }
}
