package net.jmb19905.charcoal_pit.block;

import net.jmb19905.charcoal_pit.CharcoalPitInit;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.jmb19905.charcoal_pit.block.CharringWoodBlock.Stage.IGNITING;

public class CharringWoodBlock extends BlockWithEntity {
    public static final EnumProperty<Stage> STAGE = EnumProperty.of("stage", Stage.class);
    public static final BooleanProperty SYNCED = BooleanProperty.of("synced");

    public CharringWoodBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(STAGE, IGNITING).with(SYNCED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STAGE, SYNCED);
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
        return checkType(type, CharcoalPitInit.CHARRING_WOOD_TYPE, (serverWorld, pos, blockState, blockEntity) -> blockEntity.tick(serverWorld, pos, blockState));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world instanceof ServerWorld serverWorld)
            serverWorld.getBlockEntity(pos, CharcoalPitInit.CHARRING_WOOD_TYPE).ifPresent(CharringWoodBlockEntity::update);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects() && entity instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)entity))
            entity.damage(world.getDamageSources().hotFloor(), 0.5F);
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double z;
        double y;
        double x;
        if (random.nextInt(24) == 0) {
            world.playSound((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f, false);
        }
        for (int i2 = 0; i2 < 2; ++i2) {
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

    public static Optional<CharringWoodBlockEntity> getEntity(BlockView world, BlockPos pos) {
        return world.getBlockEntity(pos, CharcoalPitInit.CHARRING_WOOD_TYPE);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private  <T> T proxy(Optional<CharringWoodBlockEntity> entity, Function<BlockState, T> proxy, Supplier<T> def) {
        if (entity.isPresent()) {
            var medium = entity.get().getMimicData();
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
