package com.taskflow.service;

/**
 * Thrown when tasks cannot be read from or written to permanent storage (for example when the
 * disk is full or the data file is locked).
 * <p>
 * Unlike {@link TaskException} this is an <em>unchecked</em> exception: a storage failure is not
 * something the caller can fix by changing its input, so it is reported once at the top of the
 * application rather than declared on every method.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a storage exception.
     *
     * @param message what went wrong
     * @param cause   the underlying I/O problem
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
