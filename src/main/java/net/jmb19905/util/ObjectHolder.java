package net.jmb19905.util;

import java.util.function.Function;

public class ObjectHolder<T> {
    private T value;
    private boolean locked;

    public ObjectHolder(T def) {
        this.value = def;
    }

    public T getValue() {
        return value;
    }

    public ObjectHolder<T> updateValue(Function<T, T> mutator) {
        this.setValue(mutator.apply(this.value));
        return this;
    }

    public ObjectHolder<T> setValue(T newValue) {
        if (!locked)
            this.value = newValue;
        return this;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public ObjectHolder<T> lock() {
        this.locked = true;
        return this;
    }
}
