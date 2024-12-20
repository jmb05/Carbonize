package net.jmb19905.persistent_data;

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

public class GlobalCharcoalPits extends PersistentState {
    public static final Identifier CHARCOAL_PIT_ID = new Identifier(Carbonize.MOD_ID, "charcoal_pit_data");
    private final List<CharcoalPitMultiblock> storage;
    private boolean queued;

    public GlobalCharcoalPits() {
        this(new ArrayList<>());
    }

    public GlobalCharcoalPits(List<CharcoalPitMultiblock> list) {
        this.storage = list;
        this.queued = false;
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    public void tick(MinecraftServer server) {
        for (int i = 0; i < storage.size(); i++) {
            var data = storage.get(i);
            data.tick(server);
            if (queued) {
                data.queueUpdate();
                queued = false;
            }
            if (data.getRemainingBurnTime() < 0 || data.blockPositions.isEmpty())
                storage.remove(i);
        }
    }

    public void queue() {
        queued = true;
    }

    public void add(CharcoalPitMultiblock data) {
        if (!exists(data))
            storage.add(data);
    }

    public CharcoalPitMultiblock get(BlockPos pos) {
        return storage.stream().filter(data -> data.hasPosition(new BlockPosWrapper(pos))).findFirst().orElse(null);
    }

    public void merge(BlockPos priorityPos, BlockPos subPos) {
        get(priorityPos).incorp(get(subPos));
    }

    public boolean exists(BlockPos pos) {
        return get(pos) != null;
    }

    public boolean exists(CharcoalPitMultiblock data) {
        //var test0 = storage.stream().anyMatch(storageData -> storageData.equals(data));
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

    private static GlobalCharcoalPits createFromNbt(NbtCompound nbt) {
        var size = nbt.getInt("Size");
        GlobalCharcoalPits data = new GlobalCharcoalPits(new ArrayList<>(size));
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
                data.storage.add(new CharcoalPitMultiblock(world, blockPositions, maxBurnTime, burnTime, extinguished));
            }
        }
        return data;
    }

    public static GlobalCharcoalPits getServerState(ServerWorld world) {
        return getServerState(world.getServer());
    }

    public static GlobalCharcoalPits getServerState(MinecraftServer server) {
        GlobalCharcoalPits data = server.getOverworld().getPersistentStateManager().getOrCreate(GlobalCharcoalPits::createFromNbt, GlobalCharcoalPits::new, CHARCOAL_PIT_ID.toString());
        data.markDirty();
        return data;
    }

    private static String withIndex(String string, int index) {
        return string + index;
    }
}
