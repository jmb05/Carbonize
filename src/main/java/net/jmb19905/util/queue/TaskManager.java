package net.jmb19905.util.queue;

import java.util.LinkedList;
import java.util.List;

public class TaskManager implements Queueable {
    private boolean isQueued;
    private boolean isExecuting;
    private final List<Runnable> tasks;
    private final List<Runnable> insuredTasks;

    public TaskManager() {
        isQueued = false;
        isExecuting = true;
        tasks = new LinkedList<>();
        insuredTasks = new LinkedList<>();

    }

    public void executeQueue() {
        synchronized (this) {
            if (isQueued) {
                isExecuting = true;
                isQueued = false;
                if (!insuredTasks.isEmpty()) {
                    tasks.addAll(insuredTasks);
                    insuredTasks.clear();
                }
                if (!tasks.isEmpty()) {
                    tasks.forEach(Runnable::run);
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
    public void ifQueued(Runnable runnable) {
        if (isQueued())
            queue(runnable);
    }

    @Override
    public void queue() {
        isQueued = true;
    }

    @Override
    public void queue(Runnable runnable) {
        isQueued = true;
        if (isExecuting)
            insuredTasks.add(runnable);
        else tasks.add(runnable);
    }

    @Override
    public void cancelQueue() {
        isQueued = false;
        tasks.clear();
    }
}
