package net.jmb19905.blockEntity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.jmb19905.Carbonize;
import net.jmb19905.util.ObjectHolder;
import net.jmb19905.util.StateHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CharringWoodBlockEntity extends BlockEntity implements RenderDataBlockEntity {

    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    private static final int SINGLE_BURN_TIME = 200;
    private BlockState parentState;
    private BlockPos startingPos;
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
                    if (entity.getModel().isIn(burnRecipe.input())) {
                        if (pos.equals(charringEntity.getStartingPos()))
                            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
                        //var stateHolder = burnRecipe.result().getDefaultState();
                        var stateHolder = new ObjectHolder<>(burnRecipe.result().getDefaultState());
                        entity.getModel().getProperties().forEach(value -> stateHolder.updateValue(oldState -> StateHelper.copyProperty(entity.getModel(), oldState, value)));
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
                blockEntity.updateModel(oldState);
            });
        } else if (sideState.isOf(Carbonize.CHARRING_WOOD)) {
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.transferData(entity));
        }
    }

    public void transferData(CharringWoodBlockEntity parent) {
        this.maxBurnTime = parent.maxBurnTime;
        this.setStartingPos(parent.getStartingPos());
    }

    public void setLogCount(int logCount) {
        this.maxBurnTime = (int) (logCount * SINGLE_BURN_TIME * 0.8);
    }

    public void updateModel(BlockState newParent) {
        this.parentState = newParent;
        markDirty();
        if(world instanceof ServerWorld serverWorld) serverWorld.getChunkManager().markForUpdate(pos);
        if(world != null) world.setBlockState(pos, getCachedState());
    }

    public BlockState getModel() {
        var data = getRenderData();
        return data != null ? data : parentState;
    }

    public void setStartingPos(BlockPos pos) {
        this.startingPos = pos;
    }

    public BlockPos getStartingPos() {
        return startingPos;
    }

    @Override
    public BlockState getRenderData() {
        return parentState;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("MaxBurnTime", maxBurnTime);
        nbt.put("ParentState", NbtHelper.fromBlockState(getModel()));
        nbt.put("StartingPos", NbtHelper.fromBlockPos(getStartingPos()));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
        updateModel(NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("ParentState")));
        setStartingPos(NbtHelper.toBlockPos(nbt.getCompound("StartingPos")));
    }
}
