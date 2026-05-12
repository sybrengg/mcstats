package com.sybre.mcstats.mixin;

import com.sybre.mcstats.PlacedBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into block placement to track which positions were placed by players.
 */
@Mixin(Block.class)
public class MixinBlock {

    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void onBlockPlaced(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (!level.isClientSide() && placer != null) {
            PlacedBlockTracker.getInstance().markPlaced(level, pos);
        }
    }
}
