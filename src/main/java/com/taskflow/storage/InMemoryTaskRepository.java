package com.taskflow.storage;

import com.taskflow.model.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link TaskRepository} that keeps tasks in memory only. Nothing is written to disk, which
 * makes it ideal for fast, isolated unit tests and as a fallback when the data file is unusable.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final List<Task> stored = new ArrayList<>();

    /**
     * Creates an empty in-memory repository.
     */
    public InMemoryTaskRepository() {
        // intentionally empty
    }

    /**
     * Returns copies of the tasks saved so far.
     *
     * @return a new list of task copies
     */
    @Override
    public List<Task> load() {
        List<Task> result = new ArrayList<>();
        for (Task t : stored) {
            result.add(t.copy());
        }
        return result;
    }

    /**
     * Replaces the in-memory contents with copies of the given tasks.
     *
     * @param tasks the tasks to remember
     */
    @Override
    public void save(Collection<Task> tasks) {
        stored.clear();
        for (Task t : tasks) {
            stored.add(t.copy());
        }
    }
}
