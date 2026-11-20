package com.taskflow.service;

/**
 * An immutable snapshot of how many tasks are in each state. It feeds the dashboard cards and
 * the progress ring.
 *
 * @param total      the number of tasks
 * @param pending    the number of pending tasks
 * @param inProgress the number of tasks in progress
 * @param completed  the number of completed tasks
 * @param overdue    the number of unfinished tasks whose due date has passed
 * @author Ibrahim Masood
 * @version 1.0
 */
public record TaskStats(int total, int pending, int inProgress, int completed, int overdue) {

    /**
     * Returns the fraction of tasks that are completed.
     *
     * @return a value from 0.0 to 1.0, or 0.0 when there are no tasks
     */
    public double completionRatio() {
        return total == 0 ? 0.0 : (double) completed / total;
    }

    /**
     * Returns the completion rate as a whole percentage.
     *
     * @return a value from 0 to 100
     */
    public int completionPercent() {
        return (int) Math.round(completionRatio() * 100);
    }
}
