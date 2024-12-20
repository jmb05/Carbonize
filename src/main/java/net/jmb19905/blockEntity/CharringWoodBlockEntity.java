package net.jmb19905.blockEntity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.jmb19905.Carbonize;
import net.jmb19905.persistent_data.GlobalCharcoalPits;
import net.jmb19905.persistent_data.CharcoalPitMultiblock;
import net.jmb19905.recipe.BurnRecipe;
import net.jmb19905.util.BlockHelper;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.jmb19905.block.CharringWoodBlock.STAGE;
import static net.jmb19905.block.CharringWoodBlock.Stage.IGNITING;

public class CharringWoodBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    private static final BlockState FIRE_STATE = Blocks.FIRE.getDefaultState();
    private static final CharcoalPitMultiblock DUMMY_DATA = CharcoalPitMultiblock.def(World.OVERWORLD, new BlockPos(0, 0, 0));
    public static final int SINGLE_BURN_TIME = 200;
    private List<BurnRecipe> recipeCache;
    private GlobalCharcoalPits globalDataCache;
    private CharcoalPitMultiblock dataCache;
    private BlockState parentState;
    private BlockState mediumState;
    private BlockState finalState;

    public CharringWoodBlockEntity(BlockPos pos, BlockState state) {
        super(Carbonize.CHARRING_WOOD_TYPE, pos, state);
        this.recipeCache = null;
        this.globalDataCache = null;
        this.dataCache = null;
        this.parentState = Blocks.OAK_PLANKS.getDefaultState();
        this.mediumState = state;
        this.finalState = Carbonize.CHARCOAL_PLANKS.getDefaultState();

    }

    public CharcoalPitMultiblock getDataSafely() {
        return dataCache == null ? DUMMY_DATA : dataCache;
    }

    public void join(CharringWoodBlockEntity priorityEntity) {
        getCharcoalPitData().merge(priorityEntity.pos, this.pos);
    }

    public void sync(BlockState parent, BlockState current) {
        sync(parent);
        assert world != null;
        world.setBlockState(pos, current);
    }

    public void sync(BlockState parent) {
        if (world != null && !world.isClient) {
            if (recipeCache == null)
                recipeCache = world.getRecipeManager().listAllOfType(Carbonize.BURN_RECIPE_TYPE);
            for (var burnRecipe : recipeCache)
                if (parent.isIn(burnRecipe.input())) {
                    this.parentState = parent;
                    this.mediumState = BlockHelper.transferState(burnRecipe.medium().getDefaultState(), parent);
                    this.finalState = BlockHelper.transferState(burnRecipe.result().getDefaultState(), parent);
                    if (dataCache == null)
                        getCharcoalPitData().add(new CharcoalPitMultiblock(getServerWorld().getRegistryKey(), pos, burnRecipe.burnTime(), 0, false));
                    break;
                }
            update();
        }
    }

    public BlockState getParent() {
        return parentState;
    }

    public BlockState getFinal() {
        return finalState;
    }

    /**
     * This is specifically for debugging purposes - makes it easier to track the model when the calls are seperated.
     */
    public BlockState getMimicState() {
        return switch (getCachedState().get(STAGE)) {
            case IGNITING -> parentState;
            case BURNING -> mediumState;
            case CHARRING -> finalState;
        };
    }

    public BlockState getMimicData() {
        return getMimicState();
    }

    @Override
    public BlockState getRenderData() {
        return getMimicState();
    }

    public int getBlockCount() {
        return getDataSafely().getBlockCount();
    }

    public int getRemainingBurnTime() {
        return getDataSafely().getRemainingBurnTime();
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
        nbt.put("ParentState", NbtHelper.fromBlockState(parentState));
        nbt.put("MediumState", NbtHelper.fromBlockState(mediumState));
        nbt.put("FinalState", NbtHelper.fromBlockState(finalState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.parentState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("ParentState"));
        this.mediumState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("MediumState"));
        this.finalState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("FinalState"));
    }

    public static void tick(World world, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        CharringWoodBlockEntity entity = (CharringWoodBlockEntity) blockEntity;
        if (world instanceof ServerWorld serverWorld) {
            for (Direction dir : Direction.values())
                lightSide(serverWorld, pos, dir, entity);

            if (entity.dataCache == null && entity.getCharcoalPitData().exists(pos))
                entity.dataCache = entity.getCharcoalPitData().get(pos);
        }
    }



    private static void lightSide(ServerWorld world, BlockPos pos, Direction dir, CharringWoodBlockEntity entity) {
        var sidePos = pos.offset(dir);
        var sideState = world.getBlockState(sidePos);

        if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_WALL)) return;

        if (BlockHelper.isNonFlammableFullCube(world, pos, sideState)) return;

        if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) {
            var parent = world.getBlockState(sidePos);
            world.setBlockState(sidePos, entity.getCachedState());
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> {
                blockEntity.sync(parent);
                blockEntity.join(entity);
            });
        } else if (entity.getCachedState().get(STAGE).ordinal() > IGNITING.ordinal()) {
            if (sideState.isReplaceable() || sideState.isAir()) {
                BlockState fireState;
                if (dir != Direction.UP)
                    fireState = FIRE_STATE.with(DIRECTION_PROPERTIES.get(dir.getOpposite()), true);
                else fireState = FIRE_STATE;
                //world.setBlockState(sidePos, fireState);
            } else if (world.getRandom().nextInt(SINGLE_BURN_TIME) == 0) {
                if (sideState.isOf(Blocks.TNT)) {
                    TntBlock.primeTnt(world, sidePos);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                } else if (!sideState.isFullCube(world, pos) && !sideState.isIn(BlockTags.FIRE)) {
                    //world.breakBlock(pos, false);
                    //world.setBlockState(pos, FIRE_STATE);
                }
            }
        }
    }

    private GlobalCharcoalPits getCharcoalPitData() {
        assert getServerWorld() != null;
        if (globalDataCache == null)
            globalDataCache = GlobalCharcoalPits.getServerState(getServerWorld());
        else globalDataCache.markDirty();
        return globalDataCache;
    }

    private ServerWorld getServerWorld() {
        return (ServerWorld) world;
    }

    public void update() {
        if (world == null) return;
        this.markDirty();
        getCharcoalPitData().queue();
        if(world instanceof ServerWorld serverWorld) serverWorld.getChunkManager().markForUpdate(pos);
    }
}
