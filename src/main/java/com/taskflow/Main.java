package com.taskflow;

import com.taskflow.service.StorageException;
import com.taskflow.service.TaskService;
import com.taskflow.storage.InMemoryTaskRepository;
import com.taskflow.storage.JsonFileTaskRepository;
import com.taskflow.ui.MainFrame;

import java.nio.file.Path;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Entry point of the TaskFlow application.
 * <p>
 * Tasks are saved to {@code ~/.taskflow/tasks.json}. A different file can be chosen with the
 * command line argument {@code --data=path/to/file.json}, which is handy for demos and testing.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts the application.
     *
     * @param args optional {@code --data=<file>} to choose where tasks are stored
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Path dataFile = JsonFileTaskRepository.defaultPath();
        for (String arg : args) {
            if (arg.startsWith("--data=")) {
                dataFile = Path.of(arg.substring("--data=".length()));
            }
        }
        final Path file = dataFile;
        SwingUtilities.invokeLater(() -> {
            TaskService service;
            try {
                service = new TaskService(new JsonFileTaskRepository(file));
            } catch (StorageException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\n\nTasker will start with an empty list and will not overwrite the file.",
                        "Tasker – could not load saved tasks", JOptionPane.WARNING_MESSAGE);
                service = new TaskService(new InMemoryTaskRepository());
            }
            new MainFrame(service).setVisible(true);
        });
    }
}
