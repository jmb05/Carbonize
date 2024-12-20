package net.jmb19905.persistent_data;

import net.jmb19905.Carbonize;
import net.jmb19905.block.CharringWoodBlock;
import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.jmb19905.util.BlockPosWrapper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.jmb19905.block.CharringWoodBlock.STAGE;
import static net.jmb19905.block.CharringWoodBlock.Stage.*;

public class CharcoalPitMultiblock {
    private boolean queued;
    public final RegistryKey<World> worldKey;
    public final List<BlockPosWrapper> blockPositions;
    private final List<ChunkPos> chunkPositions;
    public int maxBurnTime;
    public int burnTime;
    public boolean extinguished;

    public CharcoalPitMultiblock(RegistryKey<World> worldKey, int maxBurnTime, int burnTime, boolean extinguished) {
        this.queued = false;
        this.worldKey = worldKey;
        this.blockPositions = new ArrayList<>();
        this.chunkPositions = new ArrayList<>();
        this.maxBurnTime = maxBurnTime;
        this.burnTime = burnTime;
        this.extinguished = extinguished;
    }

    public CharcoalPitMultiblock(RegistryKey<World> worldKey, List<BlockPos> blockPositions, int maxBurnTime, int burnTime, boolean extinguished) {
        this(worldKey, maxBurnTime, burnTime, extinguished);
        this.appendBlockPositions(blockPositions.stream().map(BlockPosWrapper::new).toList());

    }

    public CharcoalPitMultiblock(RegistryKey<World> worldKey, BlockPos pos, int maxBurnTime, int burnTime, boolean extinguished) {
        this(worldKey, maxBurnTime, burnTime, extinguished);
        appendBlockPositions(new BlockPosWrapper(pos));
    }

    public void update(MinecraftServer server) {
        var world = server.getWorld(worldKey);
        if (burnTime >= maxBurnTime) {
            process(world, ctx -> ctx.world().setBlockState(ctx.pos(), ctx.entity().getFinal()));
            if (!extinguished)
                if (world != null && !blockPositions.isEmpty()) {
                    world.playSound(null, blockPositions.get(world.getRandom().nextInt(blockPositions.size())).expose(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 2f, 1f);
                    extinguished = true;
                }
        } else if (burnTime >= maxBurnTime * 2/3)
            update(world, CHARRING);
        else if (burnTime >= maxBurnTime / 3)
            update(world, BURNING);
        else update(world, IGNITING);
    }

    public void tick(MinecraftServer server) {
        if (!chunkPositions.stream().allMatch(chunkPos -> {
            var world = server.getWorld(worldKey);
            return world != null && world.isChunkLoaded(chunkPos.x, chunkPos.z);
        })) return;

        burnTime++;

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

    public void queueUpdate() {
        this.queued = true;
    }

    public int getRemainingBurnTime() {
        return maxBurnTime - burnTime;
    }

    public int getBlockCount() {
        return blockPositions.size();
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

    public void incorp(CharcoalPitMultiblock data) {
        appendBlockPositions(data.blockPositions);
        var totalBlockCount = getBlockCount();
        this.maxBurnTime = burnTimeFormula(this.maxBurnTime, data.maxBurnTime, totalBlockCount);
        this.burnTime = burnTimeFormula(this.burnTime, data.burnTime, totalBlockCount);
        this.extinguished = this.extinguished || data.extinguished;
        data.blockPositions.clear();
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

    private void update(ServerWorld world, CharringWoodBlock.Stage stage) {
        process(world, ctx -> {
            if (!ctx.state().get(STAGE).equals(stage))
                ctx.entity().sync(ctx.entity().getParent(), ctx.state().with(STAGE, stage));
        });
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

    public static CharcoalPitMultiblock def(RegistryKey<World> world, BlockPos pos) {
        return new CharcoalPitMultiblock(world, pos, CharringWoodBlockEntity.SINGLE_BURN_TIME, 0, false);
    }

    private static int burnTimeFormula(int firstBurnTime, int secondBurnTime, int totalBlockCount) {
        return (int) (Math.max(firstBurnTime, secondBurnTime) + Math.min(firstBurnTime, secondBurnTime) / Math.sqrt(totalBlockCount));
    }
}