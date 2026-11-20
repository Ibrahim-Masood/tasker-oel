package com.taskflow.service;

/**
 * Thrown when user input breaks a rule, for example an empty title.
 * It records which input field is at fault so the GUI can highlight it.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class ValidationException extends TaskException {

    private static final long serialVersionUID = 1L;

    /** Field name used for problems with the title. */
    public static final String FIELD_TITLE = "title";

    /** Field name used for problems with the description. */
    public static final String FIELD_DESCRIPTION = "description";

    /** Field name used for problems with the due date. */
    public static final String FIELD_DUE_DATE = "dueDate";

    private final String field;

    /**
     * Creates a validation exception.
     *
     * @param field   the name of the invalid field, one of the {@code FIELD_*} constants
     * @param message the message to show to the user
     */
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the name of the field that failed validation.
     *
     * @return one of {@link #FIELD_TITLE}, {@link #FIELD_DESCRIPTION} or {@link #FIELD_DUE_DATE}
     */
    public String getField() {
        return field;
    }
}
