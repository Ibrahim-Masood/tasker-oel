package com.taskflow.service;

/**
 * Thrown when an operation refers to a task id that does not exist, for example updating or
 * deleting a task that was already removed.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class TaskNotFoundException extends TaskException {

    private static final long serialVersionUID = 1L;

    private final int id;

    /**
     * Creates the exception for a missing task.
     *
     * @param id the id that could not be found
     */
    public TaskNotFoundException(int id) {
        super("Task #" + id + " does not exist (it may have been deleted).");
        this.id = id;
    }

    /**
     * Returns the id that could not be found.
     *
     * @return the missing task id
     */
    public int getId() {
        return id;
    }
}
