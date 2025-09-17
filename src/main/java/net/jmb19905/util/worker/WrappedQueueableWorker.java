package net.jmb19905.util.worker;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface WrappedQueueableWorker<T> extends QueueableWorker<T> {
    QueueableWorker<T> getWorker();

    @Override
    default void tryExecute(T parent, @Nullable Runnable onExecute) {
        getWorker().tryExecute(parent, onExecute);
    }

    @Override
    default boolean working() {
        return getWorker().working();
    }

    @Override
    default void setWorking(boolean working) {
        getWorker().setWorking(working);
    }

    @Override
    default boolean queued() {
        return getWorker().queued();
    }

    @Override
    default void setQueued(boolean queued) {
        getWorker().setQueued(queued);
    }

    @Override
    default void ifQueued(Consumer<T> consumer) {
        getWorker().ifQueued(consumer);
    }

    @Override
    default void queue(Consumer<T> task) {
        getWorker().queue(task);
    }

    @Override
    default void cancel() {
        getWorker().cancel();
    }
}
