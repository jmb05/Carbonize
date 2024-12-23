package net.jmb19905.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface IFireAccess {
    @Invoker(value = "isFlammable") boolean carbonize$isFlammable(BlockState state);
    @Invoker(value = "getSpreadChance") int carbonize$getSpreadChance(BlockState state);
}
