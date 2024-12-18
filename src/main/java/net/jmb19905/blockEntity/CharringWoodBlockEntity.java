package net.jmb19905.blockEntity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.jmb19905.Carbonize;
import net.jmb19905.block.CharringWoodBlock;
import net.jmb19905.util.BlockHelper;
import net.jmb19905.util.ObjectHolder;
import net.minecraft.block.Block;
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
import net.minecraft.registry.tag.BlockTags;
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

import static net.jmb19905.block.CharringWoodBlock.STAGE;
import static net.jmb19905.block.CharringWoodBlock.Stage.IGNITING;

public class CharringWoodBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    public static final int SINGLE_BURN_TIME = 200;
    private BlockState parentState;
    private BlockState mediumState;
    private BlockState finalState;
    private ObjectHolder<Boolean> extinguished = new ObjectHolder<>(false);
    private int burnTime = 0;
    private int maxBurnTime = SINGLE_BURN_TIME;

    public CharringWoodBlockEntity(BlockPos pos, BlockState state) {
        super(Carbonize.CHARRING_WOOD_TYPE, pos, state);
        parentState = Blocks.OAK_PLANKS.getDefaultState();
        mediumState = state;
        finalState = Carbonize.CHARCOAL_PLANKS.getDefaultState();
    }

    public void createData(int blockCount, int burnTimeAverage, BlockState parentState) {
        this.maxBurnTime = (int) (4 * Math.cbrt(blockCount) * burnTimeAverage);
        updateModel(parentState);
    }

    public void transferData(CharringWoodBlockEntity parent) {
        this.maxBurnTime = parent.maxBurnTime;
        this.extinguished = parent.extinguished;
    }

    public void updateModel(BlockState parent) {
        updateModel(parent, getCachedState());
    }

    public void updateModel(BlockState parent, BlockState current) {
        assert world != null;
        world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE).forEach(burnRecipe -> {
            if (parent.isIn(burnRecipe.input())) {
                this.parentState = parent;
                this.mediumState = BlockHelper.transferState(burnRecipe.medium().getDefaultState(), parent);
                this.finalState = BlockHelper.transferState(burnRecipe.result().getDefaultState(), parent);
            }
        });
        updateListeners();
        world.setBlockState(pos, current);
    }

    private void updateListeners() {
        assert this.world != null;
        this.markDirty();
        if(world instanceof ServerWorld serverWorld) serverWorld.getChunkManager().markForUpdate(pos);
        this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
    }

    /**
     * This is specifically for CharringWoodBlock#proxy for debugging purposes - makes it easier to track the model when the calls are seperated.
     */
    public BlockState getMedium() {
        return mediumState;
    }

    @Override
    public BlockState getRenderData() {
        return switch (getCachedState().get(STAGE)) {
            case IGNITING -> parentState;
            case BURNING -> mediumState;
            case CHARRING -> finalState;
        };
    }

    public int getRemainingBurnTime() {
        return maxBurnTime - burnTime;
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
        nbt.putBoolean("Extinguished", extinguished.getValue());
        nbt.put("ParentState", NbtHelper.fromBlockState(parentState));
        nbt.put("MediumState", NbtHelper.fromBlockState(mediumState));
        nbt.put("FinalState", NbtHelper.fromBlockState(finalState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
        extinguished = new ObjectHolder<>(nbt.getBoolean("Extinguished"));
        parentState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("ParentState"));
        mediumState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("MediumState"));
        finalState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("FinalState"));
    }

    public static void tick(World world, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        CharringWoodBlockEntity entity = (CharringWoodBlockEntity) blockEntity;
        if (!world.isClient) {
            for (Direction dir : Direction.values())
                lightSide(world, pos, dir, entity);

            entity.burnTime++;

            if (entity.burnTime == entity.maxBurnTime / 3) {
                entity.updateModel(entity.parentState, state.with(STAGE, CharringWoodBlock.Stage.BURNING));
            }

            if (entity.burnTime == entity.maxBurnTime * 2/3) {
                entity.updateModel(entity.parentState, state.with(STAGE, CharringWoodBlock.Stage.CHARRING));
            }

            if (entity.burnTime >= entity.maxBurnTime) {
                if (!entity.extinguished.getValue()) {
                    world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 2f, 1f);
                    entity.extinguished.setValue(true);
                }
                world.setBlockState(pos, entity.finalState);
            }
        }
    }



    private static void lightSide(World world, BlockPos pos, Direction dir, CharringWoodBlockEntity entity) {
        var sidePos = pos.offset(dir);
        var sideState = world.getBlockState(sidePos);

        if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_WALL)) return;

        if (sideState.isOf(Carbonize.CHARRING_WOOD)) {
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.transferData(entity));
        } else if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) {
            var parent = world.getBlockState(sidePos);
            world.setBlockState(sidePos, Carbonize.CHARRING_WOOD.getDefaultState());
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> {
                blockEntity.transferData(entity);
                blockEntity.updateModel(parent);
            });
        } else if (!entity.getCachedState().get(STAGE).equals(IGNITING)) {
            if (sideState.isReplaceable() || sideState.isAir()) {
                var fireState = Blocks.FIRE.getDefaultState();
                if (dir != Direction.UP)
                    fireState = fireState.with(DIRECTION_PROPERTIES.get(dir.getOpposite()), true);
                world.setBlockState(sidePos, fireState);
            } else if (!sideState.isFullCube(world, sidePos) && !sideState.isIn(BlockTags.FIRE) && world.getRandom().nextInt(SINGLE_BURN_TIME) == 0) {
                var fireState = Blocks.FIRE.getDefaultState();
                if (dir != Direction.UP)
                    fireState = fireState.with(DIRECTION_PROPERTIES.get(dir.getOpposite()), true);
                world.setBlockState(pos, fireState);
            }
        }
    }
}
