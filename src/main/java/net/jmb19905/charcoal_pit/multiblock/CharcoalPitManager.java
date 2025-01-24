package net.jmb19905.charcoal_pit.multiblock;

import net.jmb19905.Carbonize;
import net.jmb19905.util.BlockPosWrapper;
import net.jmb19905.util.queue.Queuer;
import net.jmb19905.util.queue.TaskManager;
import net.jmb19905.util.queue.WrappedQueuer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *  This is the world-discriminated manager that handles all the charcoal pit data. It is what controls & ticks the multi-blocks. Most of the functionality
 *  is stored inside the multi-blocks themselves, this manager will just access it for you. The reasoning behind this structuring is to make everything more
 *  readable.
 */
public class CharcoalPitManager extends PersistentState implements WrappedQueuer<CharcoalPitManager> {
    public static final Identifier CHARCOAL_PIT_ID = new Identifier(Carbonize.MOD_ID, "charcoal_pit_data");

    protected final ServerWorld world;
    private final List<CharcoalPitMultiblock> storage;
    private final TaskManager<CharcoalPitManager> taskManager;

    public CharcoalPitManager(ServerWorld world) {
        this(world, new ArrayList<>());
    }

    public CharcoalPitManager(ServerWorld world, List<CharcoalPitMultiblock> list) {
        this.world = world;
        this.storage = list;
        this.taskManager = new TaskManager<>();
    }

    public void tick() {
        executeQueue(this);
        for (int i = 0; i < storage.size(); i++) {
            var data = storage.get(i);
            ifQueued(() -> data.queue());
            if (data.canTick()) data.tick();
            if (data.isInvalidated()) {
                queue(() -> storage.remove(data));
            }
        }
    }

    public void add(Function<CharcoalPitManager, CharcoalPitMultiblock> getter) {
        var multiblock = getter.apply(this);
        add(multiblock);

    }

    public void add(CharcoalPitMultiblock multiblock) {
        if (!exists(multiblock))
            storage.add(multiblock);
    }

    public CharcoalPitMultiblock get(BlockPos pos) {
        return storage.stream().filter(data -> data.hasPosition(pos)).findFirst().orElse(null);
    }

    /**
     *     Firstly, it gets the associated multi-block associated with the world position.
     * <p> Next, it will perform a null check as syncing isn't immediate.
     * <p> Lastly, the prioritised data will incorporate the following data into it.
     * <p>The redundant data will be deleted in the loop as it won't have blocks associated with it.
     * <p>
     * <p> <p> NOTE: Only one world instance is needed as this function should only call from neighbouring positions.
     */
    public void merge(BlockPos priorityPos, BlockPos subPos) {
        var priorityData = get(priorityPos);
        var subData = get(subPos);
        if (priorityData != null && subData != null && !priorityData.equals(subData))
            priorityData.consume(subData);
    }

    public boolean exists(BlockPos pos) {
        return get(pos) != null;
    }

    public boolean exists(CharcoalPitMultiblock data) {
        return data != null && storage.contains(data);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Size", storage.size());
        for (int index = 0; index < storage.size(); index++) {
            var data = storage.get(index);
            var dataExists = data != null;
            nbt.putBoolean(withIndex("NotNull", index), dataExists);
            if (dataExists) {
                var positions = data.positions();
                nbt.putInt(withIndex("BlockPositionsSize", index), positions.size());
                nbt.putInt(withIndex("MaxBurnTime", index), data.maxBurnTime());
                nbt.putInt(withIndex("BurnTime", index), data.burnTime());
                nbt.putBoolean(withIndex("Extinguished", index), data.extinguished());
                for (var pos = 0; pos < positions.size(); pos++) {
                    var posExists = positions.get(pos) != null;
                    nbt.putBoolean(withIndex(withIndex("BlockPositionNotNull", index), pos), posExists);
                    if (posExists)
                        nbt.putIntArray(withIndex(withIndex("BlockPositions", index), pos), positions.get(pos).toArray());
                }
            }
        }
        return nbt;
    }

    @Override
    public Queuer<CharcoalPitManager> getQueuer() {
        return taskManager;
    }

    private static CharcoalPitManager createFromNbt(NbtCompound nbt, ServerWorld world) {
        var size = nbt.getInt("Size");
        CharcoalPitManager data = new CharcoalPitManager(world, new ArrayList<>(size));
        for (int index = 0; index < size; index++) {
            if (nbt.getBoolean(withIndex("NotNull", index))) {
                var blockPositionsSize = nbt.getInt(withIndex("BlockPositionsSize", index));
                ArrayList<BlockPos> blockPositions = new ArrayList<>(blockPositionsSize);
                var maxBurnTime = nbt.getInt(withIndex("MaxBurnTime", index));
                var burnTime = nbt.getInt(withIndex("BurnTime", index));
                var extinguished = nbt.getBoolean(withIndex("Extinguished", index));
                for (var pos = 0; pos < blockPositionsSize; pos++)
                    if (nbt.getBoolean(withIndex(withIndex("BlockPositionNotNull", index), pos)))
                        blockPositions.add(BlockPosWrapper.raise(nbt.getIntArray(withIndex(withIndex("BlockPositions", index), pos))));
                data.storage.add(new CharcoalPitMultiblock(data, blockPositions, maxBurnTime, burnTime, extinguished));
            }
        }
        return data;
    }
    public static CharcoalPitManager get(ServerWorld world) {
        CharcoalPitManager data = world.getPersistentStateManager().getOrCreate(nbt -> createFromNbt(nbt, world), () -> new CharcoalPitManager(world), CHARCOAL_PIT_ID.toString());
        data.markDirty();
        return data;
    }

    private static String withIndex(String string, int index) {
        return string + index;
    }
}
