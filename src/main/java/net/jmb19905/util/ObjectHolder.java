package net.jmb19905.util;

import java.util.function.Function;

public class ObjectHolder <T> {

    private T value;

    public ObjectHolder(T def) {
        this.value = def;
    }

    public T getValue() {
        return value;
    }

    public void updateValue(Function<T, T> mutator) {
        this.value = mutator.apply(this.value);
    }
}
