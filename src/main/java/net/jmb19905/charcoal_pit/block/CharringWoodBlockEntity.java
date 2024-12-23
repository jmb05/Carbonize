package net.jmb19905.charcoal_pit.block;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.jmb19905.Carbonize;
import net.jmb19905.charcoal_pit.CharcoalPitInit;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitManager;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitMultiblock;
import net.jmb19905.recipe.BurnRecipe;
import net.jmb19905.util.BlockHelper;
import net.jmb19905.util.queue.Queuer;
import net.jmb19905.util.queue.TaskManager;
import net.jmb19905.util.queue.WrappedQueuer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.jmb19905.charcoal_pit.block.CharringWoodBlock.STAGE;

/**
 * This class has no inherent functionality. Its job is purely just to sync itself with {@link CharcoalPitMultiblock}. The only thing
 * this class does it get individually specific things like searching the recipe manager for its burn time and mimic model.
 * Everything else is handled by the multi-block and any tasks will be delegated from it.
 */
public class CharringWoodBlockEntity extends BlockEntity implements RenderDataBlockEntity, WrappedQueuer {
    private static final CharcoalPitMultiblock DUMMY_DATA = CharcoalPitMultiblock.def();
    private final TaskManager taskManager;
    private List<BurnRecipe> recipeCache;
    private CharcoalPitManager managerCache;
    private CharcoalPitMultiblock dataCache;
    private BlockState parentState;
    private BlockState mediumState;
    private BlockState finalState;

    public CharringWoodBlockEntity(BlockPos pos, BlockState state) {
        super(CharcoalPitInit.CHARRING_WOOD_TYPE, pos, state);
        this.taskManager = new TaskManager();
        this.recipeCache = null;
        this.managerCache = null;
        this.dataCache = null;
        this.parentState = Blocks.OAK_PLANKS.getDefaultState();
        this.mediumState = state;
        this.finalState = Carbonize.CHARCOAL_PLANKS.getDefaultState();

    }

    public static void tick(World world, BlockPos pos, BlockState ignoredState, BlockEntity blockEntity) {
        CharringWoodBlockEntity entity = (CharringWoodBlockEntity) blockEntity;
        if (!world.isClient) {
            entity.executeQueue();

            if (entity.dataCache == null)
                if (entity.getCharcoalPitData().exists(pos))
                    entity.dataCache = entity.getCharcoalPitData().get(pos);
                else entity.sync(entity.getParent());
            else if (!entity.getCharcoalPitData().exists(pos))
                entity.dataCache = entity.getCharcoalPitData().get(pos);
            else if (entity.dataCache.isInvalidated())
                if (entity.getCharcoalPitData().exists(pos))
                    entity.dataCache = entity.getCharcoalPitData().get(pos);
                else entity.sync(entity.getParent());


        }
    }

    /**
        Queue properly and refactor queue to supply a super instance
     */
    public void invalidate() {
        this.dataCache = null;
    }

    public CharcoalPitMultiblock getDataSafely() {
        return dataCache == null ? DUMMY_DATA : dataCache;
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
                    if (dataCache == null /*|| !dataCache.hasPosition(pos)*/)
                        getCharcoalPitData().add(charcoalPitManager ->  new CharcoalPitMultiblock(
                                charcoalPitManager,
                                pos,
                                burnRecipe.burnTime(),
                                0,
                                false
                        ));
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

    @Override
    public Queuer getQueuer() {
        return taskManager;
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

        if (managerCache != null)
            managerCache.markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.parentState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("ParentState"));
        this.mediumState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("MediumState"));
        this.finalState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("FinalState"));
    }

    private CharcoalPitManager getCharcoalPitData() {
        if (managerCache == null) {
            assert getServerWorld() != null;
            managerCache = CharcoalPitManager.get(getServerWorld());
        }
        else managerCache.markDirty();
        return managerCache;
    }

    private ServerWorld getServerWorld() {
        return (ServerWorld) world;
    }

    public void update() {
        if (world != null) {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getChunkManager().markForUpdate(pos);
                getCharcoalPitData().queue();
            }
            this.markDirty();
            this.world.updateListeners(pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }
}
