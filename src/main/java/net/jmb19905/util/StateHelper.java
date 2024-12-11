package net.jmb19905.util;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

public class StateHelper {
    public static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.withIfExists(property, from.get(property));
    }
}
