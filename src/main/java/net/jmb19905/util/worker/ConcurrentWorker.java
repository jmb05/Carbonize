package net.jmb19905.util.worker;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ConcurrentWorker<T> implements QueueableWorker<T> {
    private boolean queued;
    private boolean working;
    private final List<Consumer<T>> tasks;
    private final List<Consumer<T>> insuredTasks;
    private final List<Runnable> internalTasks;

    public ConcurrentWorker() {
        queued = false;
        working = true;
        tasks = new LinkedList<>();
        insuredTasks = new LinkedList<>();
        internalTasks = new LinkedList<>();
    }

    @Override
    public void tryExecute(T parent, @Nullable Runnable onExecute) {
        synchronized (this) {
            if (queued()) {
                setQueued(false);
                if (!internalTasks.isEmpty()) {
                    internalTasks.forEach(Runnable::run);
                    internalTasks.clear();
                }
                if (!insuredTasks.isEmpty()) {
                    setWorking(true);
                    tasks.addAll(insuredTasks);
                    setWorking(false);
                    insuredTasks.clear();
                }
                setWorking(true);
                if (!tasks.isEmpty()) {
                    tasks.forEach(consumer -> consumer.accept(parent));
                    tasks.clear();
                }
                setWorking(false);
                if (onExecute != null)
                   onExecute.run();
            }
        }
    }

    @Override
    public boolean working() {
        return working;
    }

    @Override
    public void setWorking(boolean working) {
        synchronized (this) {
            this.working = working;
        }
    }

    @Override
    public boolean queued() {
        return queued;
    }

    @Override
    public void setQueued(boolean queued) {
        synchronized (this) {
            this.queued = queued;
        }
    }

    @Override
    public void ifQueued(Consumer<T> consumer) {
        if (queued())
            queue(consumer);
    }

    @Override
    public void queue(Consumer<T> task) {
        queue();
        if (working())
            insuredTasks.add(task);
        else tasks.add(task);
    }

    @Override
    public void cancel() {
        internalTasks.add(() -> {
            tasks.clear();
            insuredTasks.clear();
        });
    }
}
