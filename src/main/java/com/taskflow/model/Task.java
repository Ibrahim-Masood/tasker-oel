package com.taskflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A single to-do item.
 * <p>
 * The lab requires each task to have a title, a description and a status. TaskFlow
 * additionally stores a {@link Priority}, an optional due date and creation / update
 * timestamps. Identity is defined by the numeric {@link #getId() id} only, so two
 * snapshots of the same task are considered equal even if their text differs.
 * <p>
 * This class is a plain data holder: it does not validate its fields. Validation lives in
 * {@code com.taskflow.service.TaskService} so that every code path shares the same rules.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class Task {

    private final int id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Creates a task with every field supplied. Used when loading saved tasks and when copying.
     *
     * @param id          the unique identifier
     * @param title       the short title
     * @param description the longer description; {@code null} is stored as an empty string
     * @param status      the current status; {@code null} is stored as {@link TaskStatus#PENDING}
     * @param priority    the importance; {@code null} is stored as {@link Priority#MEDIUM}
     * @param dueDate     the optional due date; {@code null} means "no due date"
     * @param createdAt   when the task was created
     * @param updatedAt   when the task was last modified
     */
    public Task(int id, String title, String description, TaskStatus status, Priority priority,
                LocalDate dueDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.status = status == null ? TaskStatus.PENDING : status;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    /**
     * Creates a brand new pending task with medium priority, no due date, and both
     * timestamps set to the current time.
     *
     * @param id          the unique identifier
     * @param title       the short title
     * @param description the longer description
     */
    public Task(int id, String title, String description) {
        this(id, title, description, TaskStatus.PENDING, Priority.MEDIUM, null, LocalDateTime.now(), null);
    }

    /**
     * Returns the unique identifier of this task.
     *
     * @return the id, which never changes
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the title.
     *
     * @return the short title of the task
     */
    public String getTitle() {
        return title;
    }

    /**
     * Replaces the title.
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description.
     *
     * @return the description, never {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Replaces the description.
     *
     * @param description the new description; {@code null} becomes an empty string
     */
    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    /**
     * Returns the current status.
     *
     * @return the status, never {@code null}
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Replaces the status.
     *
     * @param status the new status; {@code null} becomes {@link TaskStatus#PENDING}
     */
    public void setStatus(TaskStatus status) {
        this.status = status == null ? TaskStatus.PENDING : status;
    }

    /**
     * Returns the priority.
     *
     * @return the priority, never {@code null}
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Replaces the priority.
     *
     * @param priority the new priority; {@code null} becomes {@link Priority#MEDIUM}
     */
    public void setPriority(Priority priority) {
        this.priority = priority == null ? Priority.MEDIUM : priority;
    }

    /**
     * Returns the due date.
     *
     * @return the due date, or {@code null} if the task has none
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Replaces the due date.
     *
     * @param dueDate the new due date, or {@code null} to remove it
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Returns the creation time.
     *
     * @return when the task was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the time of the last modification.
     *
     * @return when the task was last changed
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the time of the last modification.
     *
     * @param updatedAt the new timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Tells whether the task is late: it has a due date in the past and is not completed.
     *
     * @return {@code true} if the task is overdue
     */
    public boolean isOverdue() {
        return dueDate != null && status != TaskStatus.COMPLETED && dueDate.isBefore(LocalDate.now());
    }

    /**
     * Creates an independent copy of this task (same id, same field values).
     * The service hands out copies so callers cannot change stored data by accident.
     *
     * @return a new {@code Task} equal to this one
     */
    public Task copy() {
        return new Task(id, title, description, status, priority, dueDate, createdAt, updatedAt);
    }

    /**
     * Two tasks are equal when they have the same id.
     *
     * @param o the object to compare with
     * @return {@code true} if {@code o} is a task with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Task)) {
            return false;
        }
        return id == ((Task) o).id;
    }

    /**
     * Hash code consistent with {@link #equals(Object)}.
     *
     * @return a hash derived from the id
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a short, readable description of the task, handy for logs and test failures.
     *
     * @return a string such as {@code Task#3[Buy milk, Pending]}
     */
    @Override
    public String toString() {
        return "Task#" + id + "[" + title + ", " + status + "]";
    }
}
