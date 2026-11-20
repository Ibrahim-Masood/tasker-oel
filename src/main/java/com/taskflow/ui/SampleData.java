package com.taskflow.ui;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.TaskService;
import com.taskflow.service.ValidationException;

import java.time.LocalDate;

/**
 * Creates a handful of realistic tasks so the application can be demonstrated quickly. Due dates
 * are relative to today, so the examples always include an overdue, a "today" and a future task.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class SampleData {

    private SampleData() {
    }

    /**
     * Adds the sample tasks to the service.
     *
     * @param service where to add the tasks
     * @throws ValidationException never in practice; the sample text is always valid
     */
    public static void load(TaskService service) throws ValidationException {
        LocalDate today = LocalDate.now();
        service.addTask("Finish the Software Construction OEL report",
                "Objective, design, data structures, screenshots and the test case table.",
                TaskStatus.IN_PROGRESS, Priority.HIGH, today.plusDays(2));
        service.addTask("Push the project to GitHub",
                "Initialise the repository, make meaningful commits and push the final version.",
                TaskStatus.PENDING, Priority.HIGH, today);
        service.addTask("Generate Javadoc HTML",
                "Run the Javadoc tool and review the pages in a browser.",
                TaskStatus.PENDING, Priority.MEDIUM, today.plusDays(1));
        service.addTask("Prepare for the viva",
                "Be ready to explain the classes, the Observer pattern and how validation works.",
                TaskStatus.PENDING, Priority.HIGH, today.plusDays(4));
        service.addTask("Write JUnit test cases",
                "Cover add, view, update, delete and validation, including the edge cases.",
                TaskStatus.COMPLETED, Priority.MEDIUM, today.minusDays(1));
        service.addTask("Design the user interface",
                "Colour palette, light and dark mode, cards and animations.",
                TaskStatus.COMPLETED, Priority.LOW, null);
        service.addTask("Submit the printed report",
                "Hand over the hard copy of the report in class.",
                TaskStatus.PENDING, Priority.MEDIUM, today.minusDays(2));
        service.addTask("Buy groceries", "Milk, eggs, bread and fruit.",
                TaskStatus.PENDING, Priority.LOW, today.plusDays(6));
    }
}
