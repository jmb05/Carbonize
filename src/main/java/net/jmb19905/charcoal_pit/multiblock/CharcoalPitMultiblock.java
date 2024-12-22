package net.jmb19905.charcoal_pit.multiblock;

import net.jmb19905.Carbonize;
import net.jmb19905.charcoal_pit.block.CharringWoodBlock;
import net.jmb19905.charcoal_pit.block.CharringWoodBlockEntity;
import net.jmb19905.util.*;
import net.jmb19905.util.queue.Queueable;
import net.jmb19905.util.queue.TaskManager;
import net.jmb19905.util.queue.WrappedQueueable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.jmb19905.charcoal_pit.CharcoalPitInit.CHARRING_WOOD;
import static net.jmb19905.charcoal_pit.CharcoalPitInit.CHARRING_WOOD_TYPE;
import static net.jmb19905.charcoal_pit.block.CharringWoodBlock.STAGE;
import static net.jmb19905.charcoal_pit.block.CharringWoodBlock.SYNCED;
import static net.jmb19905.charcoal_pit.block.CharringWoodBlock.Stage.*;

/**
 * This class is practically just a giant block entity. So just treat it as one. {@link  CharcoalPitManager} will handle everything else.
 * Every {@link CharringWoodBlockEntity} must always try to upload its data to here for everything to work properly. This multi-block
 * must be the only one to either execute a task directly or delegate it to it's block entities.
 */
public class CharcoalPitMultiblock implements WrappedQueueable {
    public static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    public static final BlockState FIRE_STATE = Blocks.FIRE.getDefaultState();
    public static final int SINGLE_BURN_TIME = 200;

    /**
     * Use {@link CharcoalPitMultiblock#getPitManager(ServerWorld)} to access it safely.
     */
    @Nullable private CharcoalPitManager pitManager;
    @Nullable private ServerWorld world;
    private final TaskManager taskManager;
    public final List<BlockPosWrapper> blockPositions;
    private final List<ChunkPos> chunkPositions;
    public int maxBurnTime;
    public int burnTime;
    public boolean extinguished;

    public CharcoalPitMultiblock(@Nullable CharcoalPitManager charcoalPitManager, int maxBurnTime, int burnTime, boolean extinguished) {
        this.pitManager = charcoalPitManager;
        this.world = null;
        this.taskManager = new TaskManager();
        this.blockPositions = new ArrayList<>();
        this.chunkPositions = new ArrayList<>();
        this.maxBurnTime = maxBurnTime;
        this.burnTime = burnTime;
        this.extinguished = extinguished;
    }

    public CharcoalPitMultiblock(CharcoalPitManager charcoalPitManager, List<BlockPos> blockPositions, int maxBurnTime, int burnTime, boolean extinguished) {
        this(charcoalPitManager, maxBurnTime, burnTime, extinguished);
        this.appendBlockPositions(blockPositions.stream().map(BlockPosWrapper::new).toList());
    }

    public CharcoalPitMultiblock(CharcoalPitManager charcoalPitManager, BlockPos pos, int maxBurnTime, int burnTime, boolean extinguished) {
        this(charcoalPitManager, maxBurnTime, burnTime, extinguished);
        appendBlockPositions(new BlockPosWrapper(pos));
    }

    public static CharcoalPitMultiblock def(BlockPos pos) {
        return new CharcoalPitMultiblock(null, pos, SINGLE_BURN_TIME, 0, false);
    }

    /**
     * Ticks are managed supplied by {@link CharcoalPitManager}. It works in conjunction with {@link Queueable} to manage synchronise
     * everything to execute here. Theoretically from here, multi-threading support could be added with a couple changes. Just as long
     * as world modifications are queued properly.
     */
    public void tick(ServerWorld world) {
        cacheWorld(world);

        burnTime++;

        testSides();

        executeQueue();

        if (burnTime == maxBurnTime / 3)
            update();
        else if (burnTime == maxBurnTime * 2/3)
            update();
        else if (burnTime >= maxBurnTime)
            update();
        else if (blockPositions.size() == 1)
            update();

        testUnity();

    }

    /**
     * This is very important. This checks if ALL the chunks are loaded. This way ticking could be ignored if it isn't.
     * This is useful for preventing:
     * <p> 1 - Charcoal pits falling out of sync on chunk borders.
     * <p> 2. - Charcoal pits ticking, setting blockStates and other things it SHOULD NOT BE DOING if its chunks aren't loaded.
     * @param world  world requires a supplied instance as it could be called before ticking even begins.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTick(ServerWorld world) {
        return world != null && chunkPositions.stream().allMatch(chunkPos -> world.isChunkLoaded(chunkPos.x, chunkPos.z));
    }

    public int getRemainingBurnTime() {
        return maxBurnTime - burnTime;
    }

    public int getBlockCount() {
        return blockPositions.size();
    }

    public CharringWoodBlock.Stage getStage() {
        if (burnTime >= maxBurnTime * 2/3)
            return CHARRING;
        else if (burnTime >= maxBurnTime / 3)
            return BURNING;
        else return IGNITING;
    }

    public void setOutdated() {
        this.blockPositions.clear();
        this.chunkPositions.clear();
        this.burnTime = maxBurnTime + 1;
    }

    public boolean isOutdated() {
        return getBlockCount() == 0 || getRemainingBurnTime() < 0;
    }

    public boolean hasPosition(BlockPos pos) {
        return hasPosition(new BlockPosWrapper(pos));
    }

    public boolean hasPosition(BlockPosWrapper pos) {
        return blockPositions.stream().anyMatch(blockPosition -> blockPosition.equals(pos));
    }

    public void appendBlockPositions(BlockPosWrapper pos) {
        if (blockPositions.stream().noneMatch(blockPosition -> blockPosition.equals(pos)))
            addPosition(pos);
    }

    public void appendBlockPositions(List<BlockPosWrapper> wrappers) {
        wrappers.forEach(this::appendBlockPositions);
    }

    public void consume(CharcoalPitMultiblock data) {
        appendBlockPositions(data.blockPositions);
        var totalBlockCount = getBlockCount();
        this.maxBurnTime = calculateBurnTime(this.maxBurnTime, data.maxBurnTime, totalBlockCount);
        this.burnTime =  calculateBurnTime(this.burnTime, data.burnTime, totalBlockCount);
        this.extinguished = this.extinguished || data.extinguished;
        data.setOutdated();
        queueUpdate();
    }

    public void queueUpdate() {
        queue(this::update);
    }

    public void cacheWorld(ServerWorld world) {
        if (this.world == null)
            this.world = world;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (super.equals(obj)) return true;
        if (obj instanceof CharcoalPitMultiblock data) {
            return  this.blockPositions.equals(data.blockPositions) &&
                    this.maxBurnTime == data.maxBurnTime &&
                    this.burnTime == data.burnTime &&
                    this.extinguished == data.extinguished;
        }

        return false;
    }

    private void testUnity() {
        var blockCount = getBlockCount();

        if (blockCount <= 1) return;

        var testedPositions = collectExisting(world, blockPositions.get(0).expose());
        var testedBlockCount = testedPositions.size();

        if (testedBlockCount >= blockCount) return;

        var remainingBlockCount = getBlockCount() - testedBlockCount;
        var manager = getPitManager(world);

        manager.add(new CharcoalPitMultiblock(
                manager,
                maxBurnTime * testedBlockCount / blockCount,
                burnTime * testedBlockCount / blockCount,
                extinguished
        ));

        this.burnTime = burnTime * remainingBlockCount / blockCount;
        this.maxBurnTime = maxBurnTime * remainingBlockCount / blockCount;

        testedPositions.forEach(pos -> removePosition(new BlockPosWrapper(pos)));
    }

    private void testSides() {
        process(ctx -> {
            var pos = ctx.pos();
            var state = ctx.state();
            var entity = ctx.entity();
            for (var dir : Direction.values())
                testSide(pos, dir, state, entity);
        });
    }

    public void testSide(BlockPos pos, Direction dir, BlockState state, CharringWoodBlockEntity entity) {
        assert world != null;

        var sidePos = pos.offset(dir);
        var sideState = world.getBlockState(sidePos);
        // Checks if a block is not flammable and a full cube to determine if it's a valid wall; replaces 'charcoal_pile_valid_wall'.
        if (BlockHelper.isNonFlammableFullCube(world, pos, sideState)) return;

        // Terminate loop if the side pos is already a member of this multi-block - avoids unecessary checks. One of the multi-block-exclusive optimisations.
        if (hasPosition(sidePos)) return;

        if (sideState.isOf(CHARRING_WOOD)) {
            queue(() -> getPitManager(world).merge(pos, sidePos));
        } else if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) {
            var parent = world.getBlockState(sidePos);
            world.setBlockState(sidePos, CHARRING_WOOD.getDefaultState());
            world.getBlockEntity(sidePos, CHARRING_WOOD_TYPE).ifPresent(blockEntity -> {
                blockEntity.sync(parent);
                world.setBlockState(sidePos, blockEntity.getCachedState().with(SYNCED, true));
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

    private void update() {
        assert world != null;
        if (burnTime >= maxBurnTime) {
            process(ctx -> world.setBlockState(ctx.pos(), ctx.entity().getFinal()));
            if (!extinguished)
                if (!blockPositions.isEmpty()) {
                    world.playSound(null, blockPositions.get(world.getRandom().nextInt(blockPositions.size())).expose(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 2f, 1f);
                    extinguished = true;
                }
        } else if (burnTime >= maxBurnTime * 2/3)
            updateStates(CHARRING);
        else if (burnTime >= maxBurnTime / 3)
            updateStates(BURNING);
        else updateStates(IGNITING);
    }

    private void updateStates(CharringWoodBlock.Stage stage) {
        process(ctx -> {
            if (!ctx.state().get(STAGE).equals(stage)) {
                assert world != null;
                world.setBlockState(ctx.pos(), ctx.state().with(STAGE, stage));
                ctx.entity().update();
            }
        });
    }

    private void process(Consumer<CharcoalPitContext> consumer) {
        for (BlockPosWrapper blockPosition : blockPositions) {
            var exposedPos = blockPosition.expose();
            assert world != null;
            var state = world.getBlockState(exposedPos);
            if (state.isOf(CHARRING_WOOD)) {
                var entity = world.getBlockEntity(exposedPos, CHARRING_WOOD_TYPE);
                consumer.accept(new CharcoalPitContext(exposedPos, state, entity.orElseThrow()));
            } else {
                var blockCount = getBlockCount();
                this.burnTime = this.burnTime * (blockCount - 1) / blockCount;
                this.maxBurnTime = this.maxBurnTime * (blockCount - 1) / blockCount;
                queue(() -> removePosition(blockPosition));
            }
        }
    }

    private void addPosition(BlockPosWrapper pos) {
        var chunkPos = new ChunkPos(pos.expose());
        if (chunkPositions.stream().noneMatch(chunkPosition -> chunkPosition.equals(chunkPos)))
            chunkPositions.add(chunkPos);
        blockPositions.add(pos);

    }

    public void removePosition(BlockPosWrapper posWrapper) {
        var chunkPos = new ChunkPos(posWrapper.expose());
        blockPositions.remove(posWrapper);
        if (blockPositions.stream().noneMatch(pos -> chunkPos.equals(new ChunkPos(pos.expose()))))
            chunkPositions.remove(chunkPos);
    }

    private CharcoalPitManager getPitManager(ServerWorld world) {
        if (pitManager == null) {
            pitManager = CharcoalPitManager.get(world);
        } else pitManager.markDirty();
        return pitManager;
    }

    @Override
    public TaskManager getManager() {
        return taskManager;
    }

    public static List<BlockPos> collectFuels(ServerWorld world, BlockPos startingPos, Direction directionFrom) {
        List<BlockPos> alreadyChecked = new ArrayList<>();
        alreadyChecked.add(startingPos.offset(directionFrom));
        return collect(world, startingPos, new ObjectHolder<>(new ArrayList<>()), alreadyChecked, false).getValue();
    }

    public static List<BlockPos> collectExisting(ServerWorld world, BlockPos startingPos) {
        return collect(world, startingPos, new ObjectHolder<>(new ArrayList<>()), new ArrayList<>(), true).getValue();
    }

    private static ObjectHolder<List<BlockPos>> collect(ServerWorld world, BlockPos pos,
            ObjectHolder<List<BlockPos>> parsedPositions, List<BlockPos> checkedPositions, boolean checkExisting) {
        if (checkedPositions.contains(pos)) return parsedPositions;
        if (parsedPositions.isLocked()) return parsedPositions;

        checkedPositions.add(pos);
        BlockState state = world.getBlockState(pos);

        if (BlockHelper.isNonFlammableFullCube(world, pos, state))
             return parsedPositions;

        if (!checkExisting && !state.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL))
            return parsedPositions.getValue(List::clear).lock();

        if (checkExisting && !state.isOf(CHARRING_WOOD))
            return parsedPositions;

        parsedPositions.getValue().add(pos);

        for (Direction direction : Direction.values()) {
                collect(world, pos.offset(direction), parsedPositions, checkedPositions, checkExisting);
            if (parsedPositions.isLocked())
                break;
        }

        return parsedPositions;
    }

    private static int calculateBurnTime(int firstBurnTime, int secondBurnTime, int totalBlockCount) {
        return (int) (Math.max(firstBurnTime, secondBurnTime) + Math.min(firstBurnTime, secondBurnTime) / Math.cbrt(Math.min(totalBlockCount, 1)));
    }
}