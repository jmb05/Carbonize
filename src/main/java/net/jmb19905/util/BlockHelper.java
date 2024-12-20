package net.jmb19905.util;

import net.jmb19905.mixin.IFireAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The following methods copy one state to another. This had to be done as the provided method for this in {@link BlockState} just doesn't work properly for some reason...
 */
public class BlockHelper {
    public static <T extends Comparable<T>> BlockState transferStateProperty(BlockState from, BlockState to, Property<T> property) {
        return to.withIfExists(property, from.get(property));
    }

    public static BlockState transferState(BlockState parent, BlockState child) {
        var stateHolder = new ObjectHolder<>(parent);
        child.getProperties().forEach(value -> stateHolder.updateValue(oldState -> transferStateProperty(child, oldState, value)));
        return stateHolder.getValue();
    }

    public static boolean isNonFlammableFullCube(World world, BlockPos pos, BlockState state) {
        var isAir = state.isAir();
        var isCube = state.isFullCube(world, pos);
        var isFlammable = ((IFireAccess) Blocks.FIRE).carbonize$isFlammable(state);
        return !isAir && isCube && !isFlammable;
    }
}
