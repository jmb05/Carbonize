package net.jmb19905.util.queue;

import java.util.function.Consumer;

public interface Queuer <T>{
    void executeQueue(T parent);

    boolean isQueued();

    void ifQueued(Consumer<T> consumer);
    default void ifQueued(Runnable runnable) {
        ifQueued(t -> runnable.run());
    }

    void queue();

    void queue(Consumer<T> consumer);
    default void queue(Runnable consumer) {
        queue(t -> consumer.run());
    }

    void cancelQueue();
}
