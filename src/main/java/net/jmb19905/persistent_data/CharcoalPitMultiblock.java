package net.jmb19905.persistent_data;

import net.jmb19905.Carbonize;
import net.jmb19905.block.CharringWoodBlock;
import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.jmb19905.util.BlockHelper;
import net.jmb19905.util.BlockPosWrapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.jmb19905.block.CharringWoodBlock.STAGE;
import static net.jmb19905.block.CharringWoodBlock.Stage.*;

/**
 * This is a very important handler for charcoal pits. It enables it to work as one. Where this multiblock will control its members.
 * This solves many synchronization issues between charcoal pits. However, it comes at the consequence of being harder to debug. So,
 * if there are issues here let me (StockiesLad) know as I have a relatively firm grasp how the charcoal pits work by now.
 */
public class CharcoalPitMultiblock {
    public static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter(entry -> entry.getKey() != Direction.DOWN).collect(Util.toMap());
    public static final BlockState FIRE_STATE = Blocks.FIRE.getDefaultState();
    public static final int SINGLE_BURN_TIME = 200;

    private CharcoalPitManager managerCache;
    public final RegistryKey<World> worldKey;
    public final List<BlockPosWrapper> blockPositions;
    private final List<ChunkPos> chunkPositions;
    public int maxBurnTime;
    public int burnTime;
    public boolean extinguished;
    private boolean queued;

    public CharcoalPitMultiblock(CharcoalPitManager charcoalPitManager, RegistryKey<World> worldKey, int maxBurnTime, int burnTime, boolean extinguished) {
        this.managerCache = charcoalPitManager;
        this.worldKey = worldKey;
        this.blockPositions = new ArrayList<>();
        this.chunkPositions = new ArrayList<>();
        this.maxBurnTime = maxBurnTime;
        this.burnTime = burnTime;
        this.extinguished = extinguished;
        this.queued = false;
    }

    public CharcoalPitMultiblock(CharcoalPitManager charcoalPitManager, RegistryKey<World> worldKey, List<BlockPos> blockPositions, int maxBurnTime, int burnTime, boolean extinguished) {
        this(charcoalPitManager, worldKey, maxBurnTime, burnTime, extinguished);
        this.appendBlockPositions(blockPositions.stream().map(BlockPosWrapper::new).toList());
    }

    public CharcoalPitMultiblock(CharcoalPitManager charcoalPitManager, RegistryKey<World> worldKey, BlockPos pos, int maxBurnTime, int burnTime, boolean extinguished) {
        this(charcoalPitManager, worldKey, maxBurnTime, burnTime, extinguished);
        appendBlockPositions(new BlockPosWrapper(pos));
    }

    public void tick(MinecraftServer server) {
        if (!canTick(server.getWorld(worldKey))) return;

        burnTime++;

        test(server.getWorld(worldKey));

        if (burnTime == maxBurnTime / 3)
            update(server);
        else if (burnTime == maxBurnTime * 2/3)
            update(server);
        else if (burnTime >= maxBurnTime)
            update(server);
        else if (queued) {
            update(server);
            queued = false;
        } else if (blockPositions.size() == 1)
            update(server);

    }

    /**
     * This is very important. This checks if ALL the chunks are loaded. This way ticking could be ignored if it isn't.
     * This is useful for preventing:
     * <p> 1 - Charcoal pits falling out of sync on chunk borders.
     * <p> 2. - Charcoal pits ticking, setting blockStates and other things it SHOULD NOT BE DOING if its chunks aren't loaded.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTick(ServerWorld world) {
        return world != null && chunkPositions.stream().allMatch(chunkPos -> world.isChunkLoaded(chunkPos.x, chunkPos.z));
    }

    public void queue() {
        this.queued = true;
    }

    public int getRemainingBurnTime() {
        return maxBurnTime - burnTime;
    }

    public int getBlockCount() {
        return blockPositions.size();
    }

    public void setOutdated() {
        this.blockPositions.clear();
        this.chunkPositions.clear();
        this.burnTime = maxBurnTime + 1;
    }

    public boolean isOutdated() {
        return getBlockCount() == 0 || getRemainingBurnTime() < 0;
    }

    public boolean hasPosition(ServerWorld world, BlockPosWrapper pos) {
        var worldMatches = world.getRegistryKey().getValue().equals(worldKey.getValue());
        var positionMatches = blockPositions.stream().anyMatch(blockPosition -> blockPosition.equals(pos));
        return worldMatches && positionMatches;
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
        this.maxBurnTime = burnTimeFormula(this.maxBurnTime, data.maxBurnTime, totalBlockCount);
        this.burnTime = burnTimeFormula(this.burnTime, data.burnTime, totalBlockCount);
        this.extinguished = this.extinguished || data.extinguished;
        data.setOutdated();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (super.equals(obj)) return true;
        if (obj instanceof CharcoalPitMultiblock data) {
            return  this.worldKey.getValue().equals(data.worldKey.getValue()) &&
                    this.blockPositions.equals(data.blockPositions) &&
                    this.maxBurnTime == data.maxBurnTime &&
                    this.burnTime == data.burnTime &&
                    this.extinguished == data.extinguished;
        }

        return false;
    }

    private void test(ServerWorld world) {
        process(world, ctx -> {
            var pos = ctx.pos();
            //var state = ctx.state();
            var entity = ctx.entity();
            for (var dir : Direction.values())
                testSide(world, pos, dir, entity);

        });
    }

    public void testSide(ServerWorld world, BlockPos pos, Direction dir, CharringWoodBlockEntity entity) {
        var sidePos = pos.offset(dir);
        var sideState = world.getBlockState(sidePos);

        // Checks if a block is not flammable and a full cube to determine if it's a valid wall; replaces 'charcoal_pile_valid_wall'.
        if (BlockHelper.isNonFlammableFullCube(world, pos, sideState)) return;

        // Terminate loop if the side pos is already a member of this multi-block - avoids unecessary checks. One of the multi-block-exclusive optimisations.
        if (hasPosition(world, new BlockPosWrapper(sidePos))) return;

        if (sideState.isOf(Carbonize.CHARRING_WOOD)) {
            access(world).merge(world, pos, sidePos);
        } else if (sideState.isIn(Carbonize.CHARCOAL_PILE_VALID_FUEL)) {
            var parent = world.getBlockState(sidePos);
            world.setBlockState(sidePos, Carbonize.CHARRING_WOOD.getDefaultState());
            world.getBlockEntity(sidePos, Carbonize.CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.sync(parent));
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


    private void update(MinecraftServer server) {
        var world = server.getWorld(worldKey);
        if (burnTime >= maxBurnTime) {
            process(world, ctx -> ctx.world().setBlockState(ctx.pos(), ctx.entity().getFinal()));
            if (!extinguished)
                if (world != null && !blockPositions.isEmpty()) {
                    world.playSound(null, blockPositions.get(world.getRandom().nextInt(blockPositions.size())).expose(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 2f, 1f);
                    extinguished = true;
                }
        } else if (burnTime >= maxBurnTime * 2/3)
            updateStates(world, CHARRING);
        else if (burnTime >= maxBurnTime / 3)
            updateStates(world, BURNING);
        else updateStates(world, IGNITING);
    }

    private void updateStates(ServerWorld world, CharringWoodBlock.Stage stage) {
        process(world, ctx -> {
            if (!ctx.state().get(STAGE).equals(stage)) {
                ctx.world().setBlockState(ctx.pos(), ctx.state().with(STAGE, stage));
                ctx.entity().update();
            }
        });
    }

    private void process(ServerWorld world, Consumer<CharcoalPitContext> consumer) {
        for (int i = 0; i < blockPositions.size(); i++) {
            var exposedPos = blockPositions.get(i).expose();
            var state = world.getBlockState(exposedPos);
            if (state.isOf(Carbonize.CHARRING_WOOD)) {
                var entity = world.getBlockEntity(exposedPos, Carbonize.CHARRING_WOOD_TYPE);
                consumer.accept(new CharcoalPitContext(world, exposedPos, state, entity.orElseThrow()));
            } else {
                removePosition(i);
            }
        }
    }

    private void addPosition(BlockPosWrapper pos) {
        var chunkPos = new ChunkPos(pos.expose());
        if (chunkPositions.stream().noneMatch(chunkPosition -> chunkPosition.equals(chunkPos)))
            chunkPositions.add(chunkPos);
        blockPositions.add(pos);

    }

    private void removePosition(int index) {
        var chunkPos = new ChunkPos(blockPositions.get(index).expose());
        blockPositions.remove(index);
        if (blockPositions.stream().noneMatch(pos -> chunkPos.equals(new ChunkPos(pos.expose()))))
            chunkPositions.remove(chunkPos);
    }

    private CharcoalPitManager access(ServerWorld world) {
        if (managerCache == null) {
            managerCache = CharcoalPitManager.getServerState(world);
        } else managerCache.markDirty();
        return managerCache;
    }

    public static CharcoalPitMultiblock def(RegistryKey<World> world, BlockPos pos) {
        return new CharcoalPitMultiblock(null, world, pos, SINGLE_BURN_TIME, 0, false);
    }

    private static int burnTimeFormula(int firstBurnTime, int secondBurnTime, int totalBlockCount) {
        return (int) (Math.max(firstBurnTime, secondBurnTime) + Math.min(firstBurnTime, secondBurnTime) / Math.cbrt(totalBlockCount));
    }
}