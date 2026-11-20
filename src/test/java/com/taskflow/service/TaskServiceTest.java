package com.taskflow.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.storage.InMemoryTaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

/**
 * Automated tests for {@link TaskService}. The first seven tests (TC-01 .. TC-07) are exactly the
 * test cases listed in the lab handout; the rest cover extra features and edge cases.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class TaskServiceTest {

    private TaskService service;

    /** Creates a fresh, empty service before every test. */
    @Before
    public void setUp() {
        service = new TaskService(new InMemoryTaskRepository());
    }

    // ---- the seven test cases from the handout ----

    /** TC-01: Add a valid task -> task is added. */
    @Test
    public void tc01_addValidTask() throws Exception {
        Task t = service.addTask("Write report", "OEL report");
        assertEquals(1, service.getTaskCount());
        assertEquals("Write report", t.getTitle());
        assertEquals("OEL report", t.getDescription());
        assertEquals(TaskStatus.PENDING, t.getStatus());
    }

    /** TC-02: Add a task with an empty title -> validation message shown. */
    @Test
    public void tc02_addEmptyTitleRejected() {
        for (String bad : new String[] {"", "   ", null}) {
            try {
                service.addTask(bad, "desc");
                fail("Expected ValidationException for title: " + bad);
            } catch (ValidationException e) {
                assertEquals("Title cannot be empty.", e.getMessage());
                assertEquals(ValidationException.FIELD_TITLE, e.getField());
            }
        }
        assertEquals(0, service.getTaskCount());
    }

    /** TC-03: View all tasks -> all tasks displayed. */
    @Test
    public void tc03_viewAllTasks() throws Exception {
        service.addTask("A", "");
        service.addTask("B", "");
        service.addTask("C", "");
        List<Task> all = service.getAllTasks();
        assertEquals(3, all.size());
        assertEquals("A", all.get(0).getTitle());
        assertEquals("C", all.get(2).getTitle());
    }

    /** TC-04: View an empty list -> appropriate message (empty list, never null). */
    @Test
    public void tc04_viewEmptyList() {
        assertTrue(service.getAllTasks().isEmpty());
        assertEquals(0, service.getStats().total());
        assertEquals(0, service.getStats().completionPercent());
    }

    /** TC-05: Update an existing task -> details updated. */
    @Test
    public void tc05_updateExistingTask() throws Exception {
        Task t = service.addTask("Old", "old desc");
        Task u = service.updateTask(t.getId(), "New", "new desc");
        assertEquals("New", u.getTitle());
        assertEquals("new desc", service.getTask(t.getId()).getDescription());
    }

    /** TC-06: Update a non-existing task -> handled gracefully. */
    @Test
    public void tc06_updateNonExistingTask() throws Exception {
        try {
            service.updateTask(999, "X", "Y");
            fail("Expected TaskNotFoundException");
        } catch (TaskNotFoundException e) {
            assertEquals(999, e.getId());
            assertTrue(e.getMessage().contains("999"));
        }
        assertEquals(0, service.getTaskCount());
    }

    /** TC-07: Delete an existing task -> task removed. */
    @Test
    public void tc07_deleteExistingTask() throws Exception {
        Task t = service.addTask("Temp", "");
        service.deleteTask(t.getId());
        assertEquals(0, service.getTaskCount());
        try {
            service.getTask(t.getId());
            fail("Task should be gone");
        } catch (TaskNotFoundException expected) {
            // ok
        }
    }

    // ---- extra tests ----

    /** The three statuses can be set on a task. */
    @Test
    public void setStatusCyclesThroughAllStatuses() throws Exception {
        Task t = service.addTask("S", "");
        assertEquals(TaskStatus.IN_PROGRESS, service.setStatus(t.getId(), TaskStatus.IN_PROGRESS).getStatus());
        assertEquals(TaskStatus.COMPLETED, service.setStatus(t.getId(), TaskStatus.COMPLETED).getStatus());
        assertEquals(TaskStatus.PENDING, service.setStatus(t.getId(), TaskStatus.PENDING).getStatus());
    }

    /** Setting the status of a missing task fails cleanly. */
    @Test(expected = TaskNotFoundException.class)
    public void setStatusOnMissingTask() throws Exception {
        service.setStatus(42, TaskStatus.COMPLETED);
    }

    /** Deleting a missing task fails cleanly. */
    @Test(expected = TaskNotFoundException.class)
    public void deleteMissingTask() throws Exception {
        service.deleteTask(42);
    }

    /** Updating with an empty title is rejected and leaves the task unchanged. */
    @Test
    public void updateWithEmptyTitleKeepsOldData() throws Exception {
        Task t = service.addTask("Keep me", "d");
        try {
            service.updateTask(t.getId(), "  ", "x");
            fail("Expected ValidationException");
        } catch (ValidationException expected) {
            assertEquals("Keep me", service.getTask(t.getId()).getTitle());
        }
    }

    /** Over-long title and description are rejected. */
    @Test
    public void tooLongInputRejected() {
        String longTitle = "x".repeat(TaskService.MAX_TITLE_LENGTH + 1);
        String longDesc = "y".repeat(TaskService.MAX_DESCRIPTION_LENGTH + 1);
        try {
            service.addTask(longTitle, "");
            fail();
        } catch (ValidationException e) {
            assertEquals(ValidationException.FIELD_TITLE, e.getField());
        }
        try {
            service.addTask("ok", longDesc);
            fail();
        } catch (ValidationException e) {
            assertEquals(ValidationException.FIELD_DESCRIPTION, e.getField());
        }
    }

    /** Title is trimmed; null description becomes empty. */
    @Test
    public void inputIsTrimmed() throws Exception {
        Task t = service.addTask("  padded  ", null);
        assertEquals("padded", t.getTitle());
        assertEquals("", t.getDescription());
    }

    /** Ids are unique and never reused after deletion. */
    @Test
    public void idsAreNeverReused() throws Exception {
        Task a = service.addTask("A", "");
        service.deleteTask(a.getId());
        Task b = service.addTask("B", "");
        assertNotEquals(a.getId(), b.getId());
    }

    /** Returned tasks are copies: changing them does not change stored data. */
    @Test
    public void returnedTasksAreCopies() throws Exception {
        Task t = service.addTask("Original", "");
        t.setTitle("Hacked");
        assertEquals("Original", service.getTask(t.getId()).getTitle());
    }

    /** Search matches title and description, ignoring case. */
    @Test
    public void searchIgnoresCaseAndChecksDescription() throws Exception {
        service.addTask("Buy MILK", "");
        service.addTask("Read", "chapter about milk");
        service.addTask("Gym", "");
        assertEquals(2, service.query("milk", null, SortOption.TITLE).size());
        assertEquals(3, service.query("  ", null, null).size());
        assertEquals(0, service.query("zzz", null, null).size());
    }

    /** Filtering by status works and combines with search. */
    @Test
    public void filterByStatus() throws Exception {
        Task a = service.addTask("A task", "");
        service.addTask("B task", "");
        service.setStatus(a.getId(), TaskStatus.COMPLETED);
        assertEquals(1, service.query(null, TaskStatus.COMPLETED, null).size());
        assertEquals(1, service.query("b", TaskStatus.PENDING, null).size());
        assertEquals(0, service.query("a", TaskStatus.IN_PROGRESS, null).size());
    }

    /** Sorting by priority puts HIGH first; title sort is alphabetical. */
    @Test
    public void sorting() throws Exception {
        service.addTask("Charlie", "", TaskStatus.PENDING, Priority.LOW, null);
        service.addTask("alpha", "", TaskStatus.PENDING, Priority.HIGH, null);
        service.addTask("Bravo", "", TaskStatus.PENDING, Priority.MEDIUM, null);
        assertEquals("alpha", service.query(null, null, SortOption.PRIORITY).get(0).getTitle());
        assertEquals("alpha", service.query(null, null, SortOption.TITLE).get(0).getTitle());
        assertEquals("Charlie", service.query(null, null, SortOption.TITLE).get(2).getTitle());
    }

    /** Due-date sort lists earliest first and tasks without a date last. */
    @Test
    public void dueDateSortPutsUndatedLast() throws Exception {
        LocalDate today = LocalDate.now();
        service.addTask("none", "");
        service.addTask("later", "", TaskStatus.PENDING, Priority.MEDIUM, today.plusDays(9));
        service.addTask("soon", "", TaskStatus.PENDING, Priority.MEDIUM, today.plusDays(1));
        List<Task> r = service.query(null, null, SortOption.DUE_DATE);
        assertEquals("soon", r.get(0).getTitle());
        assertEquals("later", r.get(1).getTitle());
        assertEquals("none", r.get(2).getTitle());
    }

    /** Statistics count each state and overdue tasks (completed tasks are never overdue). */
    @Test
    public void statsAndOverdue() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        service.addTask("late", "", TaskStatus.PENDING, Priority.HIGH, yesterday);
        service.addTask("done late", "", TaskStatus.COMPLETED, Priority.HIGH, yesterday);
        service.addTask("working", "", TaskStatus.IN_PROGRESS, Priority.LOW, null);
        TaskStats s = service.getStats();
        assertEquals(3, s.total());
        assertEquals(1, s.pending());
        assertEquals(1, s.inProgress());
        assertEquals(1, s.completed());
        assertEquals(1, s.overdue());
        assertEquals(33, s.completionPercent());
    }

    /** Undo restores a deleted task with the same id. */
    @Test
    public void undoDeleteRestoresTask() throws Exception {
        Task t = service.addTask("Oops", "d");
        service.deleteTask(t.getId());
        assertTrue(service.canUndo());
        List<Task> back = service.undoDelete();
        assertEquals(1, back.size());
        assertEquals(t.getId(), back.get(0).getId());
        assertEquals("Oops", service.getTask(t.getId()).getTitle());
        assertFalse(service.canUndo());
        assertTrue(service.undoDelete().isEmpty());
    }

    /** Clear-completed removes only completed tasks and is undone in one step. */
    @Test
    public void clearCompletedAndUndo() throws Exception {
        Task a = service.addTask("a", "");
        Task b = service.addTask("b", "");
        service.addTask("c", "");
        service.setStatus(a.getId(), TaskStatus.COMPLETED);
        service.setStatus(b.getId(), TaskStatus.COMPLETED);
        assertEquals(2, service.clearCompleted());
        assertEquals(1, service.getTaskCount());
        assertEquals(0, service.clearCompleted());
        assertEquals(2, service.undoDelete().size());
        assertEquals(3, service.getTaskCount());
    }

    /** The undo history is capped. */
    @Test
    public void undoHistoryIsCapped() throws Exception {
        for (int i = 0; i < TaskService.UNDO_LIMIT + 5; i++) {
            Task t = service.addTask("t" + i, "");
            service.deleteTask(t.getId());
        }
        int restored = 0;
        while (service.canUndo()) {
            service.undoDelete();
            restored++;
        }
        assertEquals(TaskService.UNDO_LIMIT, restored);
    }

    /** Observers are notified after each change. */
    @Test
    public void listenersAreNotified() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        service.addChangeListener(calls::incrementAndGet);
        service.addChangeListener(null);
        Task t = service.addTask("x", "");
        service.setStatus(t.getId(), TaskStatus.COMPLETED);
        service.deleteTask(t.getId());
        assertEquals(3, calls.get());
    }

    /** Data saved by one service instance is loaded by the next one. */
    @Test
    public void dataSurvivesRestart() throws Exception {
        InMemoryTaskRepository repo = new InMemoryTaskRepository();
        TaskService first = new TaskService(repo);
        first.addTask("Persist me", "yes");
        TaskService second = new TaskService(repo);
        assertEquals(1, second.getTaskCount());
        assertEquals(2, second.addTask("next", "").getId());
    }
}
