package net.jmb19905.util.queue;

public interface WrappedQueuer extends Queuer {
    Queuer getQueuer();

    @Override
    default void executeQueue() {
        getQueuer().executeQueue();
    }

    @Override
    default boolean isQueued() {
        return getQueuer().isQueued();
    }

    @Override
    default void ifQueued(Runnable runnable) {
        getQueuer().ifQueued(runnable);
    }

    @Override
    default void queue() {
        getQueuer().queue();
    }

    @Override
    default void queue(Runnable runnable) {
        getQueuer().queue(runnable);
    }

    @Override
    default void cancelQueue() {
        getQueuer().cancelQueue();
    }

}
