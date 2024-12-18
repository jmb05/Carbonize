package net.jmb19905.block;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.jmb19905.Carbonize;
import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.jmb19905.recipe.BurnRecipe;
import net.jmb19905.util.ObjectHolder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.jmb19905.block.CharringWoodBlock.Stage.IGNITING;

public class CharringWoodBlock extends BlockWithEntity {
    public static final EnumProperty<Stage> STAGE = EnumProperty.of("stage", Stage.class);

    public CharringWoodBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(STAGE, IGNITING));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CharringWoodBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }


    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return proxy(getEntity(world, pos), (pState) -> pState.isTransparent(world, pos), () -> super.isTransparent(state, world, pos));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return proxy(getEntity(world, pos), (pState) -> pState.getCullingShape(world, pos), () -> super.getCullingShape(state, world, pos));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return proxy(getEntity(world, pos), (pState) -> pState.getOutlineShape(world, pos, context), () -> super.getOutlineShape(state, world, pos, context));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return proxy(getEntity(world, pos), (pState) -> pState.getCollisionShape(world, pos, context), () -> super.getCollisionShape(state, world, pos, context));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, Carbonize.CHARRING_WOOD_TYPE, CharringWoodBlockEntity::tick);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects() && entity instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)entity)) {
            entity.damage(world.getDamageSources().hotFloor(), 0.5F);
        }

        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double z;
        double y;
        double x;
        int i;
        if (random.nextInt(24) == 0) {
            world.playSound((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f, false);
        }
        for (int i2 = 0; i2 < 3; ++i2) {
            x = (double) pos.getX() + random.nextDouble();
            y = (double) pos.getY() + random.nextDouble() * 0.5 + 0.5;
            z = (double) pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.FLAME, x, pos.getY() + random.nextDouble(), z, - 0.01 + random.nextFloat() / 50, random.nextFloat() / 50, - 0.01 + random.nextFloat() / 50);
            if (random.nextFloat() > 0.95f) {
                world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 0.0, 0.07, 0.0);
            } else {
                world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.0, 0.0);
            }
        }
        if (world.getBlockState(pos.up()).isAir()) return;
        for (i = 0; i < 2; ++i) {
            x = (double)pos.getX() + random.nextDouble();
            y = (double)(pos.getY() + 1) - random.nextDouble() * (double)0.1f;
            z = (double)pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.FLAME, x, pos.getY() + random.nextDouble(), z, - 0.01 + random.nextFloat() / 50, random.nextFloat() / 50, - 0.01 + random.nextFloat() / 50);
            if (random.nextFloat() > 0.95f) {
                world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 0.0, 0.07, 0.0);
            } else {
                world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.0, 0.0);
            }
        }
    }

    private boolean isFlammable(BlockState state) {
        var entry = FlammableBlockRegistry.getDefaultInstance().get(state.getBlock());
        return entry != null && entry.getBurnChance() > 0;
    }

    public static Optional<CharringWoodBlockEntity> getEntity(BlockView world, BlockPos pos) {
        return world.getBlockEntity(pos, Carbonize.CHARRING_WOOD_TYPE);
    }


    public static int checkValid(World world, BlockPos pos, Direction direction, ObjectHolder<Integer> burnTimeAverage) {
        List<BlockPos> alreadyChecked = new ArrayList<>();
        List<BurnRecipe> recipes = world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE);
        alreadyChecked.add(pos.offset(direction));
        return check(world, pos, alreadyChecked, recipes, burnTimeAverage);
    }

    private static int check(World world, BlockPos pos, List<BlockPos> alreadyChecked, List<BurnRecipe> recipes, ObjectHolder<Integer> burnTimeAverage) {
        alreadyChecked.add(pos);
        BlockState state = world.getBlockState(pos);
        if (!state.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) return 0;
        int i = 1;
        for (var burnRecipe : recipes) {
            if (state.isIn(burnRecipe.input())) {
                burnTimeAverage.updateValue(deviation -> deviation + burnRecipe.burnTime());
                break;
            }
        }
        for (Direction dir : Direction.values()) {
            BlockPos side = pos.offset(dir);
            if (alreadyChecked.contains(side)) continue;
            BlockState sideState = world.getBlockState(side);
            if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_WALL)) {
                alreadyChecked.add(side);
                continue;
            }
            int a = check(world, side, alreadyChecked, recipes, burnTimeAverage);
            if (a == 0) {
                return 0;
            }
            i += a;
        }
        return i;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private  <T> T proxy(Optional<CharringWoodBlockEntity> entity, Function<BlockState, T> proxy, Supplier<T> def) {
        if (entity.isPresent()) {
            var medium = entity.get().getMedium();
            if (medium != null && !medium.getBlock().equals(this))
                return proxy.apply(medium);
        }
        return def.get();
    }

    public enum Stage implements StringIdentifiable {
        IGNITING,
        BURNING,
        CHARRING;

        @Override
        public String asString() {
            return this.name().toLowerCase();
        }
    }
}
