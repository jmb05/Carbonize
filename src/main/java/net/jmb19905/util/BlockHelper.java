package net.jmb19905.util;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

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
}
