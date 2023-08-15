package net.jmb19905.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

public class FallingPillarBlock extends FlammableFallingBlock {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;
    public FallingPillarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(AXIS, Direction.Axis.Y));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FallingPillarBlock.changeRotation(state, rotation);
    }

    public static BlockState changeRotation(BlockState state, BlockRotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> {
                switch (state.get(AXIS)) {
                    case X -> {
                        return state.with(AXIS, Direction.Axis.Z);
                    }
                    case Z -> {
                        return state.with(AXIS, Direction.Axis.X);
                    }
                }
                return state;
            }
        }
        return state;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
    }
}
