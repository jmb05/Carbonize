package net.jmb19905.util.queue;

public interface WrappedQueueable extends Queueable {
    TaskManager getManager();

    @Override
    default void executeQueue() {
        getManager().executeQueue();
    }

    @Override
    default boolean isQueued() {
        return getManager().isQueued();
    }

    @Override
    default void ifQueued(Runnable runnable) {
        getManager().ifQueued(runnable);
    }

    @Override
    default void queue() {
        getManager().queue();
    }

    @Override
    default void queue(Runnable runnable) {
        getManager().queue(runnable);
    }

    @Override
    default void cancelQueue() {
        getManager().cancelQueue();
    }

}
