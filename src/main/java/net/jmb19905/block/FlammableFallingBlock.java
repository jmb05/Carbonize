package net.jmb19905.block;

import net.jmb19905.Carbonize;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class FlammableFallingBlock extends FallingBlock {
    public FlammableFallingBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!FallingBlock.canFallThrough(world.getBlockState(pos.down())) || pos.getY() < world.getBottomY()) {
            return;
        }

        boolean burning = false;
        for (Direction dir : Direction.values()) {
            BlockState fireState = world.getBlockState(pos.offset(dir));
            Carbonize.LOGGER.info("Block: " + fireState + " Dir: " + dir);
            if (fireState.isIn(BlockTags.FIRE)) {
                if (dir == Direction.UP || (fireState.contains(ConnectingBlock.FACING_PROPERTIES.get(dir.getOpposite())) && fireState.get(ConnectingBlock.FACING_PROPERTIES.get(dir.getOpposite())))) {
                    burning = true;
                    break;
                }
            }
        }

        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, pos, state);
        fallingBlockEntity.setOnFire(burning);
        this.configureFallingBlockEntity(fallingBlockEntity);
    }

    @Override
    protected void configureFallingBlockEntity(FallingBlockEntity entity) {
        entity.setHurtEntities(1.0f, 10);
    }

    @Override
    public void onLanding(World world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity) {
        if (fallingBlockEntity.isOnFire()) {
            world.setBlockState(pos.up(), Blocks.FIRE.getDefaultState());
        }
    }

    @Override
    public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
        if (fallingBlockEntity.isOnFire()) {
            world.setBlockState(pos.up(), Blocks.FIRE.getDefaultState());
        }
    }

    @Override
    public DamageSource getDamageSource(Entity attacker) {
        if (attacker instanceof FallingBlockEntity entity && entity.isOnFire()) {
            return attacker.getDamageSources().onFire();
        }
        return super.getDamageSource(attacker);
    }
}
