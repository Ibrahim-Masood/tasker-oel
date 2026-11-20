package com.taskflow.service;

import com.taskflow.model.Task;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * The ways the task list can be ordered. Each constant carries its own {@link Comparator},
 * so sorting is a single call to {@code List.sort(option.getComparator())}
 * (the <em>Strategy</em> pattern). Java's list sort is a stable merge sort (TimSort),
 * which runs in O(n log n) time.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public enum SortOption {

    /** Most recently created first. */
    NEWEST("Newest first",
            Comparator.comparing(Task::getCreatedAt).reversed().thenComparing(Comparator.comparingInt(Task::getId).reversed())),

    /** Oldest first. */
    OLDEST("Oldest first",
            Comparator.comparing(Task::getCreatedAt).thenComparingInt(Task::getId)),

    /** Alphabetical by title, ignoring case. */
    TITLE("Title A-Z",
            Comparator.comparing((Task t) -> t.getTitle().toLowerCase()).thenComparingInt(Task::getId)),

    /** High priority first, then earliest due date. */
    PRIORITY("Priority",
            Comparator.comparingInt((Task t) -> -t.getPriority().getWeight())
                    .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                    .thenComparingInt(Task::getId)),

    /** Earliest due date first; tasks without a due date go last. */
    DUE_DATE("Due date",
            Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                    .thenComparingInt(Task::getId)),

    /** Grouped by workflow status: Pending, In Progress, Completed. */
    STATUS("Status",
            Comparator.comparingInt((Task t) -> t.getStatus().ordinal()).thenComparingInt(Task::getId));

    private final String label;
    private final Comparator<Task> comparator;

    /**
     * Creates a sort option.
     *
     * @param label      the name shown in the sort menu
     * @param comparator the ordering to apply
     */
    SortOption(String label, Comparator<Task> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    /**
     * Returns the menu label.
     *
     * @return the human readable name of this ordering
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the comparator that implements this ordering.
     *
     * @return a comparator over {@link Task} objects
     */
    public Comparator<Task> getComparator() {
        return comparator;
    }

    /**
     * Returns the menu label.
     *
     * @return the same value as {@link #getLabel()}
     */
    @Override
    public String toString() {
        return label;
    }
}
