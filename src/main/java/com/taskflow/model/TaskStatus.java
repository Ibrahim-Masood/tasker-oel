package com.taskflow.model;

/**
 * The lifecycle state of a {@link Task}.
 * <p>
 * The declaration order is significant: it is the natural workflow order
 * (Pending, then In Progress, then Completed) and is used by {@link #next()}
 * and by the "sort by status" option.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public enum TaskStatus {

    /** The task has not been started yet. */
    PENDING("Pending"),

    /** The task is currently being worked on. */
    IN_PROGRESS("In Progress"),

    /** The task is finished. */
    COMPLETED("Completed");

    private final String label;

    /**
     * Creates a status constant.
     *
     * @param label the human readable name shown in the user interface
     */
    TaskStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the human readable name of this status.
     *
     * @return the display label, for example {@code "In Progress"}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the next status in the workflow, wrapping around after
     * {@link #COMPLETED}. Used by the quick "cycle status" button on a task card.
     *
     * @return the following status in declaration order
     */
    public TaskStatus next() {
        TaskStatus[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /**
     * Parses a status from either its display label or its constant name.
     * Matching ignores case, surrounding spaces, underscores and hyphens, so
     * {@code "in progress"}, {@code "IN_PROGRESS"} and {@code "In-Progress"} all work.
     *
     * @param text the text to parse; may be {@code null}
     * @return the matching status
     * @throws IllegalArgumentException if the text is {@code null} or matches no status
     */
    public static TaskStatus parse(String text) {
        if (text != null) {
            String wanted = normalise(text);
            for (TaskStatus status : values()) {
                if (normalise(status.name()).equals(wanted) || normalise(status.label).equals(wanted)) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + text);
    }

    private static String normalise(String s) {
        return s.trim().replace("_", "").replace("-", "").replace(" ", "").toLowerCase();
    }

    /**
     * Returns the display label, so a status can be placed directly in a combo box or message.
     *
     * @return the same value as {@link #getLabel()}
     */
    @Override
    public String toString() {
        return label;
    }
}
