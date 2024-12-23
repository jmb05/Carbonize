package net.jmb19905.util.queue;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class TaskManager <T> implements Queuer<T> {
    private boolean isQueued;
    private boolean isExecuting;
    private final List<Consumer<T>> tasks;
    private final List<Consumer<T>> insuredTasks;

    public TaskManager() {
        isQueued = false;
        isExecuting = true;
        tasks = new LinkedList<>();
        insuredTasks = new LinkedList<>();

    }

    public void executeQueue(T parent) {
        synchronized (this) {
            if (isQueued) {
                isExecuting = true;
                isQueued = false;
                if (!insuredTasks.isEmpty()) {
                    tasks.addAll(insuredTasks);
                    insuredTasks.clear();
                }
                if (!tasks.isEmpty()) {
                    tasks.forEach(consumer -> consumer.accept(parent));
                    tasks.clear();
                }
                isExecuting = false;
            }
        }
    }

    @Override
    public boolean isQueued() {
        return isQueued;
    }

    @Override
    public void ifQueued(Consumer<T> consumer) {
        if (isQueued())
            queue(consumer);
    }

    @Override
    public void queue() {
        isQueued = true;
    }

    @Override
    public void queue(Consumer<T> consumer) {
        isQueued = true;
        if (isExecuting)
            insuredTasks.add(consumer);
        else tasks.add(consumer);
    }

    @Override
    public void cancelQueue() {
        isQueued = false;
        tasks.clear();
    }
}
