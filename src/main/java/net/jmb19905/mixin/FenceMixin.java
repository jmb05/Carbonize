package net.jmb19905.mixin;

import net.jmb19905.Carbonize;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.registry.tag.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 *  This just makes sure that charcoal fences can connect with wooden ones. This is a prime example of how making wooden-like blocks with the proper tags can cause issues.
 *  However, this must be done as charcoal has to behave the same but without breaking the recipes.
 */
@Mixin(FenceBlock.class)
public class FenceMixin {
    @Inject(method = "canConnectToFence", at = @At(value = "RETURN"), cancellable = true)
    public void carbonize$fixFences(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() || (state.isIn(BlockTags.FENCES) && (state.isIn(BlockTags.WOODEN_FENCES) || state.isIn(Carbonize.CHARCOAL_BLOCKS))));
    }
}
