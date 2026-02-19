package com.sybre.mcstats.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceMenu.class)
public class MixinFurnaceMenu {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onFurnaceTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            String itemId = stack.getItem().toString();
            int count = stack.getCount();
            com.sybre.mcstats.StatsCollector.getInstance()
                .recordCustomStat(serverPlayer, "mcstats.smelted:" + itemId, count);
        }
    }
}
