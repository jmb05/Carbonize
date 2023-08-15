package net.jmb19905.blockEntity;

import net.jmb19905.Carbonize;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

public class CharringWoodBlockEntity extends BlockEntity {

    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    public static final int SINGLE_BURN_TIME = 800;
    private int burnTime = 0;
    private int maxBurnTime = SINGLE_BURN_TIME;

    public CharringWoodBlockEntity(BlockPos pos, BlockState state) {
        super(Carbonize.CHARRING_WOOD_TYPE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        CharringWoodBlockEntity entity = (CharringWoodBlockEntity) blockEntity;
        if (!world.isClient) {
            for (Direction dir : Direction.values()) {
                lightSide(world, pos, dir, entity);
            }
            entity.burnTime++;
            if (entity.burnTime >= entity.maxBurnTime) {
                world.setBlockState(pos, Carbonize.CHARCOAL_LOG.getDefaultState());
            }
        }
    }

    private static void lightSide(World world, BlockPos pos, Direction dir, CharringWoodBlockEntity entity) {
        var sidePos = pos.offset(dir);
        var sideState = world.getBlockState(sidePos);
        if (sideState.isReplaceable() || sideState.isAir()) {
            var fireState = Blocks.FIRE.getDefaultState();
            if (dir != Direction.UP) {
                fireState = fireState.with(DIRECTION_PROPERTIES.get(dir.getOpposite()), true);
            }
            world.setBlockState(sidePos, fireState);
        } else if (sideState.isIn(BlockTags.LOGS)) {
            world.setBlockState(sidePos, Carbonize.CHARRING_WOOD.getDefaultState());
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.maxBurnTime = entity.maxBurnTime);
        } else if (sideState.isOf(Carbonize.CHARRING_WOOD)) {
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.maxBurnTime = entity.maxBurnTime);
        }
    }

    public void setLogCount(int maxBurnTime) {
        this.maxBurnTime = maxBurnTime * SINGLE_BURN_TIME;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("MaxBurnTime", maxBurnTime);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
    }
}
