package com.taskflow.storage;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores tasks in a human-readable JSON file so they survive closing the application.
 * <p>
 * Saving is <em>atomic</em>: the data is first written to a temporary file next to the real one
 * and then moved over it, so a crash in the middle of a save can never leave a half-written,
 * corrupted data file. If one saved entry is damaged it is skipped instead of making the whole
 * file unreadable.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class JsonFileTaskRepository implements TaskRepository {

    private final Path file;

    /**
     * Creates a repository that uses the given file. The file and its folders are created on the
     * first save.
     *
     * @param file the path of the JSON data file
     */
    public JsonFileTaskRepository(Path file) {
        this.file = file;
    }

    /**
     * Returns the default data file: {@code .taskflow/tasks.json} inside the user's home folder.
     *
     * @return the default storage path
     */
    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".taskflow", "tasks.json");
    }

    /**
     * Returns the path of the data file.
     *
     * @return the file this repository reads and writes
     */
    public Path getFile() {
        return file;
    }

    /**
     * Reads the tasks from the JSON file.
     *
     * @return the stored tasks, or an empty list if the file does not exist yet
     * @throws IOException if the file cannot be read or is not valid JSON
     */
    @Override
    public List<Task> load() throws IOException {
        List<Task> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        for (Map<String, String> m : MiniJson.read(text)) {
            Task t = toTask(m);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Writes all tasks to the JSON file atomically.
     *
     * @param tasks the complete list of tasks to store
     * @throws IOException if the file cannot be written
     */
    @Override
    public void save(Collection<Task> tasks) throws IOException {
        List<Map<String, String>> objects = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(t.getId()));
            m.put("title", t.getTitle());
            m.put("description", t.getDescription());
            m.put("status", t.getStatus().name());
            m.put("priority", t.getPriority().name());
            m.put("dueDate", t.getDueDate() == null ? null : t.getDueDate().toString());
            m.put("createdAt", t.getCreatedAt().toString());
            m.put("updatedAt", t.getUpdatedAt().toString());
            objects.add(m);
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, MiniJson.write(objects), StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicNotSupported) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Converts one parsed JSON object back to a task.
     *
     * @param m the key/value pairs of one saved task
     * @return the task, or {@code null} if the entry is damaged (missing id or title)
     */
    private static Task toTask(Map<String, String> m) {
        try {
            String idText = m.get("id");
            String title = m.get("title");
            if (idText == null || title == null || title.isBlank()) {
                return null;
            }
            TaskStatus status = m.get("status") == null ? TaskStatus.PENDING : TaskStatus.parse(m.get("status"));
            Priority priority = m.get("priority") == null ? Priority.MEDIUM : Priority.parse(m.get("priority"));
            LocalDate due = m.get("dueDate") == null ? null : LocalDate.parse(m.get("dueDate"));
            LocalDateTime created = m.get("createdAt") == null ? null : LocalDateTime.parse(m.get("createdAt"));
            LocalDateTime updated = m.get("updatedAt") == null ? null : LocalDateTime.parse(m.get("updatedAt"));
            return new Task(Integer.parseInt(idText), title, m.get("description"), status, priority, due,
                    created, updated);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return null;
        }
    }
}
