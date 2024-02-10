package net.jmb19905.mixin;

import net.jmb19905.Carbonize;
import net.jmb19905.block.AshBlock;
import net.jmb19905.recipe.BurnRecipe;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireMixin {

    @Shadow
    protected abstract int getSpreadChance(BlockState state);

    @Shadow
    protected abstract BlockState getStateWithAge(WorldAccess world, BlockPos pos, int age);

    @Inject(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;south()Lnet/minecraft/util/math/BlockPos;"))
    private void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!Carbonize.CONFIG.increasedFireSpreadRange()) return;
        int k = world.getBiome(pos).isIn(BiomeTags.INCREASED_FIRE_BURNOUT) ? -50 : 0;
        this.trySpreadingFire(world, pos.east(2), 300 + k, random, 0);
        this.trySpreadingFire(world, pos.west(2), 300 + k, random, 0);
        this.trySpreadingFire(world, pos.down(2), 250 + k, random, 0);
        this.trySpreadingFire(world, pos.up(2), 250 + k, random, 0);
        this.trySpreadingFire(world, pos.north(2), 300 + k, random, 0);
        this.trySpreadingFire(world, pos.south(2), 300 + k, random, 0);
    }

    @Shadow
    protected abstract void trySpreadingFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge);

    @Inject(method = "trySpreadingFire", at = @At("HEAD"))
    private void tryCatchFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci){
        int i = this.getSpreadChance(world.getBlockState(pos));
        if (random.nextInt(spreadFactor) < i) {
            BlockState blockState = world.getBlockState(pos);
            if (random.nextInt(currentAge + 10) < 5 && !world.hasRain(pos)) {
                int j = Math.min(currentAge + random.nextInt(5) / 4, 15);
                if (!transformByBurning(world, pos, random)) world.setBlockState(pos, this.getStateWithAge(world, pos, j), 3);
            } else {
                if (!transformByBurning(world, pos, random)) world.removeBlock(pos, false);
            }

            Block block = blockState.getBlock();
            if (block instanceof TntBlock) {
                TntBlock.primeTnt(world, pos);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Unique
    private boolean transformByBurning(World world, BlockPos pos, Random random) {
        BlockState state = world.getBlockState(pos);
        world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE);
        if (Carbonize.CONFIG.burnCrafting()) {
            for (BurnRecipe burnRecipe : world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE)) {
                if (state.isIn(burnRecipe.input())) {
                    float exposure = getExposed(world, pos);
                    float randomVal = random.nextFloat();
                    if (randomVal > exposure) {
                        var newState = burnRecipe.result().getDefaultState();
                        if (state.contains(Properties.AXIS)) {
                            var rotation = state.get(Properties.AXIS);
                            newState = newState.with(Properties.AXIS, rotation);
                        }
                        world.setBlockState(pos, newState);
                        return true;
                    } else if (randomVal > 0.3f && Carbonize.CONFIG.createAsh()) {
                        world.setBlockState(pos, Carbonize.ASH_LAYER.getDefaultState().with(AshBlock.LAYERS, getAshLayerCount(random, state)));
                        return true;
                    }
                }
            }
        }
        if (random.nextFloat() > 0.3f && Carbonize.CONFIG.createAsh()) {
            world.setBlockState(pos, Carbonize.ASH_LAYER.getDefaultState().with(AshBlock.LAYERS, getAshLayerCount(random, state)));
            return true;
        }
        return false;
    }

    @Unique
    private int getAshLayerCount(Random random, BlockState state) {
        int layers = random.nextInt(2) + 1;
        layers += ((int) state.getBlock().getHardness());
        return MathHelper.clamp(layers, 1, 4);
    }

    @Unique
    private float getExposed(World world, BlockPos pos) {
        float val = 0.0f;
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.offset(dir)).isAir()) {
                val += 1f/6f;
            }
        }
        return val;
    }

}