package net.jmb19905.util.queue;

public interface Queuer {
    void executeQueue();

    boolean isQueued();

    void ifQueued(Runnable runnable);

    void queue();

    void queue(Runnable runnable);

    void cancelQueue();
}
