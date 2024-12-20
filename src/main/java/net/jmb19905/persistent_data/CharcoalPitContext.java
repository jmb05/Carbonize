package net.jmb19905.persistent_data;

import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record CharcoalPitContext(ServerWorld world, BlockPos pos, BlockState state, CharringWoodBlockEntity entity) {}
