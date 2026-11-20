package com.taskflow.service;

/**
 * Base class of the checked exceptions thrown by {@link TaskService}.
 * <p>
 * They are <em>checked</em> on purpose: the compiler forces every caller (including the GUI)
 * to decide how to present a validation or lookup problem to the user instead of ignoring it.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class TaskException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-presentable message.
     *
     * @param message what went wrong, written so it can be shown directly to the user
     */
    public TaskException(String message) {
        super(message);
    }
}
