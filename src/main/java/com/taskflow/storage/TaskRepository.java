package com.taskflow.storage;

import com.taskflow.model.Task;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Abstraction over permanent storage of tasks.
 * <p>
 * {@code TaskService} depends only on this interface, so the GUI, the unit tests and any future
 * database back end can share the same business logic (the <em>Dependency Inversion</em> principle).
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public interface TaskRepository {

    /**
     * Reads every stored task.
     *
     * @return the stored tasks in the order they were saved; empty if nothing has been saved yet
     * @throws IOException if the storage cannot be read
     */
    List<Task> load() throws IOException;

    /**
     * Replaces the stored data with the given tasks.
     *
     * @param tasks the complete list of tasks to store
     * @throws IOException if the storage cannot be written
     */
    void save(Collection<Task> tasks) throws IOException;
}
