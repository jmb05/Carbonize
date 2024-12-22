package net.jmb19905.charcoal_pit.multiblock;

import net.jmb19905.Carbonize;
import net.jmb19905.util.BlockPosWrapper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CharcoalPitManager extends PersistentState {
    public static final Identifier CHARCOAL_PIT_ID = new Identifier(Carbonize.MOD_ID, "charcoal_pit_data");
    private final List<CharcoalPitMultiblock> storage;
    private boolean queued;

    public CharcoalPitManager() {
        this(new ArrayList<>());
    }

    public CharcoalPitManager(List<CharcoalPitMultiblock> list) {
        this.storage = list;
        this.queued = false;
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    public void tick(MinecraftServer server) {
        for (int i = 0; i < storage.size(); i++) {
            var data = storage.get(i);
            data.tick(server);
            if (queued) {
                data.queue();
                queued = false;
            }
            if (data.isOutdated())
                storage.remove(i);
        }
    }

    public void queue() {
        queued = true;
    }

    public void add(Function<CharcoalPitManager, CharcoalPitMultiblock> dataGetter) {
        var data = dataGetter.apply(this);
        if (!exists(data))
            storage.add(data);
    }

    public CharcoalPitMultiblock get(ServerWorld world, BlockPos pos) {
        return storage.stream().filter(data -> data.hasPosition(world, new BlockPosWrapper(pos))).findFirst().orElse(null);
    }

    /**
     *     Firstly, it gets the associated multi-block associated with the world position.
     * <p> Next, it will perform a null check as syncing isn't immediate.
     * <p> Lastly, the prioritised data will incorporate the following data into it.
     * <p>The redundant data will be deleted in the loop as it won't have blocks associated with it.
     * <p>
     * <p> <p> NOTE: Only one world instance is needed as this function should only call from neighbouring positions.
     */
    public void merge(ServerWorld world, BlockPos priorityPos, BlockPos subPos) {
        var priorityData = get(world, priorityPos);
        var subData = get(world, subPos);
        if (priorityData != null && subData != null)
            priorityData.consume(subData);
    }

    public boolean exists(ServerWorld world, BlockPos pos) {
        return get(world, pos) != null;
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
                var positions = data.blockPositions;
                nbt.putInt(withIndex("BlockPositionsSize", index), positions.size());
                nbt.putString(withIndex("World", index), data.worldKey.getValue().toString());
                nbt.putInt(withIndex("MaxBurnTime", index), data.maxBurnTime);
                nbt.putInt(withIndex("BurnTime", index), data.burnTime);
                nbt.putBoolean(withIndex("Extinguished", index), data.extinguished);
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

    private static CharcoalPitManager createFromNbt(NbtCompound nbt) {
        var size = nbt.getInt("Size");
        CharcoalPitManager data = new CharcoalPitManager(new ArrayList<>(size));
        for (int index = 0; index < size; index++) {
            if (nbt.getBoolean(withIndex("NotNull", index))) {
                var blockPositionsSize = nbt.getInt(withIndex("BlockPositionsSize", index));
                ArrayList<BlockPos> blockPositions = new ArrayList<>(blockPositionsSize);
                var world = RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString(withIndex("World", index))));
                var maxBurnTime = nbt.getInt(withIndex("MaxBurnTime", index));
                var burnTime = nbt.getInt(withIndex("BurnTime", index));
                var extinguished = nbt.getBoolean(withIndex("Extinguished", index));
                for (var pos = 0; pos < blockPositionsSize; pos++)
                    if (nbt.getBoolean(withIndex(withIndex("BlockPositionNotNull", index), pos)))
                        blockPositions.add(BlockPosWrapper.raise(nbt.getIntArray(withIndex(withIndex("BlockPositions", index), pos))));
                data.storage.add(new CharcoalPitMultiblock(data, world, blockPositions, maxBurnTime, burnTime, extinguished));
            }
        }
        return data;
    }

    public static CharcoalPitManager getServerState(ServerWorld world) {
        return getServerState(world.getServer());
    }

    public static CharcoalPitManager getServerState(MinecraftServer server) {
        CharcoalPitManager data = server.getOverworld().getPersistentStateManager().getOrCreate(CharcoalPitManager::createFromNbt, CharcoalPitManager::new, CHARCOAL_PIT_ID.toString());
        data.markDirty();
        return data;
    }

    private static String withIndex(String string, int index) {
        return string + index;
    }
}
