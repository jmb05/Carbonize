package net.jmb19905.mixin;

import net.jmb19905.block.FlammableFallingStairsBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StairsBlock.class)
public class StairsBlockMixin {

    @Inject(method = "isStairs", at = @At("HEAD"), cancellable = true)
    private static void isStairs(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.getBlock() instanceof StairsBlock || state.getBlock() instanceof FlammableFallingStairsBlock);
    }

}
