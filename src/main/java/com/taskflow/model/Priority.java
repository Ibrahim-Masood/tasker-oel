package com.taskflow.model;

/**
 * How important a {@link Task} is. This is an extra feature that goes beyond the
 * lab's minimum requirements (title, description and status).
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public enum Priority {

    /** Can wait. */
    LOW("Low", 1),

    /** The default importance. */
    MEDIUM("Medium", 2),

    /** Should be done first. */
    HIGH("High", 3);

    private final String label;
    private final int weight;

    /**
     * Creates a priority constant.
     *
     * @param label  the human readable name shown in the user interface
     * @param weight a number used for sorting; a larger weight means more important
     */
    Priority(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    /**
     * Returns the human readable name of this priority.
     *
     * @return the display label, for example {@code "High"}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the sorting weight of this priority.
     *
     * @return {@code 1} for low, {@code 2} for medium and {@code 3} for high
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Parses a priority from its label or constant name, ignoring case and surrounding spaces.
     *
     * @param text the text to parse; may be {@code null}
     * @return the matching priority
     * @throws IllegalArgumentException if the text is {@code null} or matches no priority
     */
    public static Priority parse(String text) {
        if (text != null) {
            String wanted = text.trim();
            for (Priority p : values()) {
                if (p.name().equalsIgnoreCase(wanted) || p.label.equalsIgnoreCase(wanted)) {
                    return p;
                }
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + text);
    }

    /**
     * Returns the display label.
     *
     * @return the same value as {@link #getLabel()}
     */
    @Override
    public String toString() {
        return label;
    }
}
