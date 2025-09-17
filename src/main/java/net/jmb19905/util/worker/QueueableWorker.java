package net.jmb19905.util.worker;

import net.jmb19905.charcoal_pit.block.CharringWoodBlockEntity;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitManager;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitMultiblock;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Used to execute non-threadsafe tasks in a ticking loop such that potentially non-threadsafe
 * modifications will all be executed in the place same; avoids CME's.
 *
 * <p></p> See {@link CharringWoodBlockEntity} {@link CharcoalPitMultiblock}, {@link CharcoalPitManager}
 * @param <T> The object to interact with during the queue
 */
public interface QueueableWorker<T> {
    void tryExecute(T parent, @Nullable Runnable onExecute);

    default void tryExecute(T parent) {
        tryExecute(parent, null);
    }

    boolean working();

    void setWorking(boolean working);

    boolean queued();

    void setQueued(boolean queued);

    void ifQueued(Consumer<T> consumer);

    default void ifQueued(Runnable runnable) {
        ifQueued(t -> runnable.run());
    }

    default void queue() {
        setQueued(true);
    }

    void queue(Consumer<T> task);

    default void queue(Runnable task) {
        queue(t -> task.run());
    }

    default void delay() {
        setQueued(false);
    }

    void cancel();
}
