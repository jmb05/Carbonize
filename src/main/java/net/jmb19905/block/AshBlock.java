package net.jmb19905.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class AshBlock extends FallingBlock {

    public static final int MAX_LAYERS = 8;
    public static final IntProperty LAYERS = Properties.LAYERS;
    protected static final VoxelShape[] LAYERS_TO_SHAPE = new VoxelShape[]{VoxelShapes.empty(), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 14.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)};

    public AshBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(LAYERS, 1));
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState downState = world.getBlockState(pos.down());
        boolean fallThrough = FallingBlock.canFallThrough(downState) && !(downState.isOf(this) && downState.get(LAYERS) == 8);
        if (!fallThrough || pos.getY() < world.getBottomY()) {
            return;
        }
        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, pos, state);
        this.configureFallingBlockEntity(fallingBlockEntity);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        switch (type) {
            case LAND -> {
                return state.get(LAYERS) < 5;
            }
            case WATER, AIR -> {
                return false;
            }
        }
        return false;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return LAYERS_TO_SHAPE[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return LAYERS_TO_SHAPE[state.get(LAYERS) - 1];
    }

    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return LAYERS_TO_SHAPE[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return LAYERS_TO_SHAPE[state.get(LAYERS)];
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return state.get(LAYERS) == MAX_LAYERS ? 0.2f : 1.0f;
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        int i = state.get(LAYERS);
        if (context.getStack().isOf(this.asItem()) && i < MAX_LAYERS) {
            if (context.canReplaceExisting()) {
                return context.getSide() == Direction.UP;
            }
            return true;
        }
        return i == 1;
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (blockState.isOf(this)) {
            int i = blockState.get(LAYERS);
            return blockState.with(LAYERS, Math.min(MAX_LAYERS, i + 1));
        }
        return super.getPlacementState(ctx);
    }

    @Override
    public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
        var state = world.getBlockState(pos);
        var fallingLayers = fallingBlockEntity.getBlockState().get(LAYERS);
        if (state.isOf(this)) {
                int layers = state.get(LAYERS) + fallingLayers;
                int more = Math.max(layers - MAX_LAYERS, 0);
            if (state.get(LAYERS) < MAX_LAYERS) {
                world.setBlockState(pos, getDefaultState().with(LAYERS, Math.min(layers, 8)));
            } else if (state.get(LAYERS) == MAX_LAYERS) {
                world.setBlockState(pos.up(), getDefaultState().with(LAYERS, Math.min(layers, 8)));
            }
            if (more > 0) {
                world.setBlockState(pos.up(), getDefaultState().with(LAYERS, Math.min(more, 8)));
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Biome biome = world.getBiome(pos).value();
        if (world.isRaining() && biome.getPrecipitation(pos) == Biome.Precipitation.RAIN) {
            int layers;
            if ((layers = state.get(LAYERS)) > 1) {
                world.setBlockState(pos, state.with(LAYERS, layers - 1));
            } else {
                world.removeBlock(pos, false);
            }
        }
    }
}
