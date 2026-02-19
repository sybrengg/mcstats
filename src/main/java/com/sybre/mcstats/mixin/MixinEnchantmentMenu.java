package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public class MixinEnchantmentMenu {

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void onEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            com.sybre.mcstats.StatsCollector.getInstance()
                .recordCustomStat(serverPlayer, "mcstats.custom:enchanted", 1);
        }
    }
}
