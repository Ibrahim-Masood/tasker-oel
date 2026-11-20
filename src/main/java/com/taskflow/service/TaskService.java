package com.taskflow.service;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.storage.TaskRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The business logic of the application: create, read, update and delete tasks, change their
 * status, validate input, search / filter / sort, undo deletions and report statistics.
 * <p>
 * <b>Data structures used</b>
 * <ul>
 *   <li>{@link LinkedHashMap}{@code <Integer, Task>} &ndash; primary store. Gives O(1) lookup by id
 *       while remembering insertion order for stable listing.</li>
 *   <li>{@link ArrayDeque} used as a stack &ndash; the undo history. Push and pop at the head are
 *       O(1); it is capped at {@value #UNDO_LIMIT} entries.</li>
 *   <li>{@link ArrayList} of {@link Runnable} &ndash; observers notified after every change
 *       (the <em>Observer</em> pattern), which keeps the GUI free of business logic.</li>
 * </ul>
 * The service never exposes its internal {@link Task} objects; callers always receive copies.
 * Every mutating method saves to the {@link TaskRepository} before it notifies observers.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class TaskService {

    /** Longest allowed title, in characters. */
    public static final int MAX_TITLE_LENGTH = 80;

    /** Longest allowed description, in characters. */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    /** How many delete operations can be undone. */
    public static final int UNDO_LIMIT = 20;

    private final TaskRepository repository;
    private final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private final Deque<List<Task>> undoStack = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int nextId = 1;

    /**
     * Creates a service and loads any previously saved tasks from the repository.
     *
     * @param repository where tasks are loaded from and saved to
     * @throws StorageException if the saved tasks cannot be read
     */
    public TaskService(TaskRepository repository) {
        this.repository = repository;
        try {
            for (Task t : repository.load()) {
                tasks.put(t.getId(), t);
                nextId = Math.max(nextId, t.getId() + 1);
            }
        } catch (IOException e) {
            throw new StorageException("Could not load saved tasks: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ create

    /**
     * Adds a new pending task with medium priority and no due date. This is the minimal
     * "add a task (title and description)" operation required by the lab.
     *
     * @param title       the title; must not be empty or blank
     * @param description the description; may be {@code null} or empty
     * @return a copy of the newly stored task, including its generated id
     * @throws ValidationException if the title is empty or any field is too long
     */
    public Task addTask(String title, String description) throws ValidationException {
        return addTask(title, description, TaskStatus.PENDING, Priority.MEDIUM, null);
    }

    /**
     * Adds a new task with every field specified.
     *
     * @param title       the title; must not be empty or blank
     * @param description the description; may be {@code null} or empty
     * @param status      the starting status; {@code null} means pending
     * @param priority    the importance; {@code null} means medium
     * @param dueDate     the optional due date; {@code null} means none
     * @return a copy of the newly stored task, including its generated id
     * @throws ValidationException if the title is empty or any field is too long
     */
    public Task addTask(String title, String description, TaskStatus status, Priority priority,
                        LocalDate dueDate) throws ValidationException {
        String cleanTitle = validate(title, description);
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task(nextId++, cleanTitle, clean(description), status, priority, dueDate, now, now);
        tasks.put(task.getId(), task);
        changed();
        return task.copy();
    }

    // -------------------------------------------------------------------- read

    /**
     * Returns every task in the order it was added.
     *
     * @return a new list of task copies; empty (never {@code null}) when there are no tasks
     */
    public List<Task> getAllTasks() {
        List<Task> result = new ArrayList<>(tasks.size());
        for (Task t : tasks.values()) {
            result.add(t.copy());
        }
        return result;
    }

    /**
     * Finds one task by id.
     *
     * @param id the task id
     * @return a copy of the task
     * @throws TaskNotFoundException if no task has this id
     */
    public Task getTask(int id) throws TaskNotFoundException {
        return find(id).copy();
    }

    /**
     * Returns how many tasks exist.
     *
     * @return the number of stored tasks
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Searches, filters and sorts the tasks in one step. A task matches when its title or
     * description contains the search text (ignoring case) <em>and</em> its status equals the filter.
     * The whole pass is O(n) for matching plus O(m log m) for sorting the m matches.
     *
     * @param searchText the text to look for; {@code null} or blank matches everything
     * @param status     only tasks with this status; {@code null} means all statuses
     * @param sort       the ordering of the result; {@code null} means {@link SortOption#NEWEST}
     * @return a new list of matching task copies
     */
    public List<Task> query(String searchText, TaskStatus status, SortOption sort) {
        String needle = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        List<Task> result = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (status != null && t.getStatus() != status) {
                continue;
            }
            if (!needle.isEmpty()
                    && !t.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                    && !t.getDescription().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            result.add(t.copy());
        }
        result.sort((sort == null ? SortOption.NEWEST : sort).getComparator());
        return result;
    }

    /**
     * Counts the tasks in each state.
     *
     * @return an immutable snapshot of the current numbers
     */
    public TaskStats getStats() {
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        int overdue = 0;
        for (Task t : tasks.values()) {
            switch (t.getStatus()) {
                case PENDING -> pending++;
                case IN_PROGRESS -> inProgress++;
                case COMPLETED -> completed++;
            }
            if (t.isOverdue()) {
                overdue++;
            }
        }
        return new TaskStats(tasks.size(), pending, inProgress, completed, overdue);
    }

    // ------------------------------------------------------------------ update

    /**
     * Updates the title and description of a task, leaving its other fields unchanged.
     *
     * @param id          the id of the task to change
     * @param title       the new title; must not be empty or blank
     * @param description the new description
     * @return a copy of the updated task
     * @throws ValidationException   if the title is empty or any field is too long
     * @throws TaskNotFoundException if no task has this id
     */
    public Task updateTask(int id, String title, String description)
            throws ValidationException, TaskNotFoundException {
        Task existing = find(id);
        return updateTask(id, title, description, existing.getStatus(), existing.getPriority(),
                existing.getDueDate());
    }

    /**
     * Updates every editable field of a task.
     *
     * @param id          the id of the task to change
     * @param title       the new title; must not be empty or blank
     * @param description the new description
     * @param status      the new status; {@code null} means pending
     * @param priority    the new priority; {@code null} means medium
     * @param dueDate     the new due date, or {@code null} to clear it
     * @return a copy of the updated task
     * @throws ValidationException   if the title is empty or any field is too long
     * @throws TaskNotFoundException if no task has this id
     */
    public Task updateTask(int id, String title, String description, TaskStatus status,
                           Priority priority, LocalDate dueDate)
            throws ValidationException, TaskNotFoundException {
        Task task = find(id);
        String cleanTitle = validate(title, description);
        task.setTitle(cleanTitle);
        task.setDescription(clean(description));
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        task.setUpdatedAt(LocalDateTime.now());
        changed();
        return task.copy();
    }

    /**
     * Changes only the status of a task (Pending / In Progress / Completed).
     *
     * @param id     the id of the task to change
     * @param status the new status; {@code null} means pending
     * @return a copy of the updated task
     * @throws TaskNotFoundException if no task has this id
     */
    public Task setStatus(int id, TaskStatus status) throws TaskNotFoundException {
        Task task = find(id);
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        changed();
        return task.copy();
    }

    // ------------------------------------------------------------------ delete

    /**
     * Deletes a task. The deletion can be reversed with {@link #undoDelete()}.
     *
     * @param id the id of the task to remove
     * @return a copy of the task that was removed
     * @throws TaskNotFoundException if no task has this id
     */
    public Task deleteTask(int id) throws TaskNotFoundException {
        Task removed = find(id);
        tasks.remove(id);
        pushUndo(Collections.singletonList(removed));
        changed();
        return removed.copy();
    }

    /**
     * Deletes every completed task in one step (undoable as a single operation).
     *
     * @return how many tasks were removed
     */
    public int clearCompleted() {
        List<Task> removed = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                removed.add(t);
            }
        }
        if (removed.isEmpty()) {
            return 0;
        }
        for (Task t : removed) {
            tasks.remove(t.getId());
        }
        pushUndo(removed);
        changed();
        return removed.size();
    }

    /**
     * Tells whether there is a deletion that can be undone.
     *
     * @return {@code true} if {@link #undoDelete()} would restore something
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Restores the most recently deleted task (or group of tasks) with their original ids.
     *
     * @return copies of the restored tasks; an empty list if there was nothing to undo
     */
    public List<Task> undoDelete() {
        List<Task> restored = undoStack.pollFirst();
        if (restored == null) {
            return Collections.emptyList();
        }
        List<Task> copies = new ArrayList<>();
        for (Task t : restored) {
            tasks.put(t.getId(), t);
            copies.add(t.copy());
        }
        changed();
        return copies;
    }

    // --------------------------------------------------------------- observers

    /**
     * Registers an observer that is run after every change to the task data.
     *
     * @param listener the code to run; ignored if {@code null}
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Checks the user input against the business rules.
     *
     * @param title       the title to check
     * @param description the description to check
     * @return the title with surrounding whitespace removed
     * @throws ValidationException if the title is empty/blank or any text is too long
     */
    private String validate(String title, String description) throws ValidationException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException(ValidationException.FIELD_TITLE, "Title cannot be empty.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException(ValidationException.FIELD_TITLE,
                    "Title is too long (maximum " + MAX_TITLE_LENGTH + " characters).");
        }
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException(ValidationException.FIELD_DESCRIPTION,
                    "Description is too long (maximum " + MAX_DESCRIPTION_LENGTH + " characters).");
        }
        return trimmed;
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    private Task find(int id) throws TaskNotFoundException {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    private void pushUndo(List<Task> removed) {
        undoStack.addFirst(new ArrayList<>(removed));
        while (undoStack.size() > UNDO_LIMIT) {
            undoStack.removeLast();
        }
    }

    /** Saves to the repository, then tells every observer that the data changed. */
    private void changed() {
        try {
            repository.save(tasks.values());
        } catch (IOException e) {
            throw new StorageException("Could not save tasks: " + e.getMessage(), e);
        }
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
