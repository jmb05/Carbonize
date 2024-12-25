package net.jmb19905.util.queue;

import java.util.function.Consumer;

public interface WrappedQueuer <T> extends Queuer<T> {
    Queuer<T> getQueuer();

    @Override
    default void executeQueue(T parent) {
        getQueuer().executeQueue(parent);
    }

    @Override
    default boolean isQueued() {
        return getQueuer().isQueued();
    }

    @Override
    default void ifQueued(Consumer<T> consumer) {
        getQueuer().ifQueued(consumer);
    }

    @Override
    default void queue() {
        getQueuer().queue();
    }

    @Override
    default void queue(Consumer<T> consumer) {
        getQueuer().queue(consumer);
    }

    @Override
    default void cancelQueue() {
        getQueuer().cancelQueue();
    }

}
