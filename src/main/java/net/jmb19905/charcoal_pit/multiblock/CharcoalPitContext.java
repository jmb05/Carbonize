package net.jmb19905.charcoal_pit.multiblock;

import net.jmb19905.charcoal_pit.block.CharringWoodBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record CharcoalPitContext(ServerWorld world, BlockPos pos, BlockState state, CharringWoodBlockEntity entity) {}
