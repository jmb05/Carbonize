package net.jmb19905.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
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
    public static final float RAIN_FERTILIZE_CHANCE = 0.1f;
    public static final IntProperty LAYERS = Properties.LAYERS;
    protected static final VoxelShape[] LAYERS_TO_SHAPE = new VoxelShape[]{VoxelShapes.empty(), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 14.0, 16.0), Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)};

    public MapCodec<AshBlock> CODEC = createCodec(AshBlock::new);

    public AshBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(LAYERS, 1));
    }

    @Override
    protected MapCodec<? extends FallingBlock> getCodec() {
        return CODEC;
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
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
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
            System.out.println("Layers: " + (state.get(LAYERS)) + " + " + fallingLayers + " = " + layers + " (More: " + more + ")");
            if (state.get(LAYERS) < MAX_LAYERS) {
                System.out.println("Added to block: " + pos);
                world.setBlockState(pos, getDefaultState().with(LAYERS, Math.min(layers, 8)));
                if (more > 0) {
                    System.out.println("Added new block: " + pos.up());
                    world.setBlockState(pos.up(), getDefaultState().with(LAYERS, Math.min(more, 8)));
                }
            } else if (state.get(LAYERS) == MAX_LAYERS) {
                System.out.println("Added new block: " + pos.up());
                BlockState upState = world.getBlockState(pos.up());
                if (upState.isOf(this)) {
                    world.setBlockState(pos.up(), getDefaultState().with(LAYERS, Math.min(upState.get(LAYERS) + fallingLayers, 8)));
                } else {
                    world.setBlockState(pos.up(), getDefaultState().with(LAYERS, Math.min(fallingLayers, 8)));
                }
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
            //if (random.nextFloat() < RAIN_FERTILIZE_CHANCE) {
                //fertilizeGround(world, pos);
            //}
        }
    }

    private void fertilizeGround(World world, BlockPos pos) {
        Direction direction = Direction.random(world.random);
        BlockPos relPos = pos.offset(direction);
        BlockState blockState = world.getBlockState(pos);
        boolean bl = blockState.isSideSolidFullSquare(world, pos, direction);
        if (bl && useOnGround(world, relPos, direction)) {
            if (!world.isClient) {
                world.syncWorldEvent(1505, relPos, 0);
            }
        }
    }

    public static boolean useOnGround(World world, BlockPos blockPos, @Nullable Direction facing) {
        if (world.getBlockState(blockPos).isOf(Blocks.WATER) && world.getFluidState(blockPos).getLevel() == 8) {
            if (world instanceof ServerWorld) {
                Random random = world.getRandom();

                label78:
                for (int i = 0; i < 128; ++i) {
                    BlockPos blockPos2 = blockPos;
                    BlockState blockState = Blocks.SEAGRASS.getDefaultState();

                    for (int j = 0; j < i / 16; ++j) {
                        blockPos2 = blockPos2.add(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                        if (world.getBlockState(blockPos2).isFullCube(world, blockPos2)) {
                            continue label78;
                        }
                    }

                    RegistryEntry<Biome> registryEntry = world.getBiome(blockPos2);
                    if (registryEntry.isIn(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
                        if (i == 0 && facing != null && facing.getAxis().isHorizontal()) {
                            blockState = Registries.BLOCK.getEntryList(BlockTags.WALL_CORALS).flatMap((blocks) -> blocks.getRandom(world.random)).map((blockEntry) -> blockEntry.value().getDefaultState()).orElse(blockState);
                            if (blockState.contains(DeadCoralWallFanBlock.FACING)) {
                                blockState = blockState.with(DeadCoralWallFanBlock.FACING, facing);
                            }
                        } else if (random.nextInt(4) == 0) {
                            blockState = Registries.BLOCK.getEntryList(BlockTags.UNDERWATER_BONEMEALS).flatMap((blocks) -> blocks.getRandom(world.random)).map((blockEntry) -> blockEntry.value().getDefaultState()).orElse(blockState);
                        }
                    }

                    if (blockState.isIn(BlockTags.WALL_CORALS, (state) -> state.contains(DeadCoralWallFanBlock.FACING))) {
                        for (int k = 0; !blockState.canPlaceAt(world, blockPos2) && k < 4; ++k) {
                            blockState = blockState.with(DeadCoralWallFanBlock.FACING, Direction.Type.HORIZONTAL.random(random));
                        }
                    }

                    if (blockState.canPlaceAt(world, blockPos2)) {
                        BlockState blockState2 = world.getBlockState(blockPos2);
                        if (blockState2.isOf(Blocks.WATER) && world.getFluidState(blockPos2).getLevel() == 8) {
                            world.setBlockState(blockPos2, blockState, 3);
                        } else if (blockState2.isOf(Blocks.SEAGRASS) && random.nextInt(10) == 0) {
                            ((Fertilizable) Blocks.SEAGRASS).grow((ServerWorld) world, random, blockPos2, blockState2);
                        }
                    }
                }

            }
            return true;
        } else {
            return false;
        }
    }

}
