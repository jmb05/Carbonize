package net.jmb19905.blockEntity;

import net.jmb19905.Carbonize;
import net.jmb19905.util.ObjectHolder;
import net.jmb19905.util.StateHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

public class CharringWoodBlockEntity extends BlockEntity {

    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    public BlockState parentState;
    public BlockPos startingPos;
    public static final int SINGLE_BURN_TIME = 200;
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
                world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE).forEach(burnRecipe -> {
                    CharringWoodBlockEntity charringEntity = (CharringWoodBlockEntity) blockEntity;
                    if (entity.parentState.isIn(burnRecipe.input())) {
                        if (pos.equals(charringEntity.startingPos))
                            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
                        //var stateHolder = burnRecipe.result().getDefaultState();
                        var stateHolder = new ObjectHolder<>(burnRecipe.result().getDefaultState());
                        entity.parentState.getProperties().forEach(value -> stateHolder.updateValue(oldState -> StateHelper.copyProperty(entity.parentState, oldState, value)));
                        world.setBlockState(pos, stateHolder.getValue());
                    }
                });
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
        } else if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) {
            var oldState = world.getBlockState(sidePos);
            world.setBlockState(sidePos, Carbonize.CHARRING_WOOD.getDefaultState());
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> {
                blockEntity.transferData(entity);
                blockEntity.parentState = oldState;
            });
        } else if (sideState.isOf(Carbonize.CHARRING_WOOD)) {
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.transferData(entity));
        }
    }

    public void transferData(CharringWoodBlockEntity parent) {
        this.maxBurnTime = parent.maxBurnTime;
        this.startingPos = parent.startingPos;
    }

    public void setLogCount(int logCount) {
        this.maxBurnTime = (int) (logCount * SINGLE_BURN_TIME * 0.8);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("MaxBurnTime", maxBurnTime);
        nbt.put("ParentState", NbtHelper.fromBlockState(parentState));
        nbt.put("StartingPos", NbtHelper.fromBlockPos(startingPos));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
        parentState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("ParentState"));
        startingPos = NbtHelper.toBlockPos(nbt.getCompound("StartingPos"));
    }
}
