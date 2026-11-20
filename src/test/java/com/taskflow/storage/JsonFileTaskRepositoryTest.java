package com.taskflow.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the JSON file storage: round trips, special characters and damaged files.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class JsonFileTaskRepositoryTest {

    /** A temporary folder that JUnit deletes after each test. */
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path file;
    private JsonFileTaskRepository repo;

    /** Prepares a repository that points at a file inside the temporary folder. */
    @Before
    public void setUp() {
        file = tmp.getRoot().toPath().resolve("data").resolve("tasks.json");
        repo = new JsonFileTaskRepository(file);
    }

    /** A missing file means "no tasks yet", not an error. */
    @Test
    public void missingFileLoadsEmptyList() throws IOException {
        assertTrue(repo.load().isEmpty());
    }

    /** Every field survives save then load, including a due date and unicode text. */
    @Test
    public void roundTripKeepsAllFields() throws IOException {
        LocalDateTime created = LocalDateTime.of(2026, 11, 1, 10, 30, 15);
        Task t = new Task(7, "Café ☕ \"quoted\" \\ back\\slash", "line1\nline2\ttab",
                TaskStatus.IN_PROGRESS, Priority.HIGH, LocalDate.of(2026, 12, 25), created, created);
        List<Task> in = new ArrayList<>();
        in.add(t);
        repo.save(in);
        List<Task> out = repo.load();
        assertEquals(1, out.size());
        Task r = out.get(0);
        assertEquals(7, r.getId());
        assertEquals(t.getTitle(), r.getTitle());
        assertEquals("line1\nline2\ttab", r.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, r.getStatus());
        assertEquals(Priority.HIGH, r.getPriority());
        assertEquals(LocalDate.of(2026, 12, 25), r.getDueDate());
        assertEquals(created, r.getCreatedAt());
    }

    /** A task without a due date stays without one. */
    @Test
    public void nullDueDateRoundTrip() throws IOException {
        List<Task> in = new ArrayList<>();
        in.add(new Task(1, "No date", "d"));
        repo.save(in);
        assertNull(repo.load().get(0).getDueDate());
    }

    /** Saving creates missing folders and leaves no temporary file behind. */
    @Test
    public void saveCreatesFoldersAndCleansTemp() throws IOException {
        repo.save(new ArrayList<>());
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(file.resolveSibling("tasks.json.tmp")));
    }

    /** Records with bad values are skipped while good ones are still loaded. */
    @Test
    public void damagedRecordsAreSkipped() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "[{\"id\":\"1\",\"title\":\"Good\",\"status\":\"PENDING\"},"
                + "{\"id\":\"x\",\"title\":\"Bad id\"},"
                + "{\"id\":\"3\",\"title\":\"   \"},"
                + "{\"id\":\"4\",\"title\":\"Bad status\",\"status\":\"WRONG\"}]");
        List<Task> out = repo.load();
        assertEquals(1, out.size());
        assertEquals("Good", out.get(0).getTitle());
    }

    /** Completely corrupted JSON is reported as an IOException (the service wraps it). */
    @Test(expected = IOException.class)
    public void corruptFileThrows() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "this is not json {{{");
        repo.load();
    }
}
