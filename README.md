# Tasker – Java Task Management System

A modern Java Swing task management (TODO) application built for the **Software Construction
Open-Ended Lab (OEL)**.

**Author:** Ibrahim Masood

## Features

Required by the lab:

* Add a task (title and description) with validation: **empty titles are rejected** with a message.
* View all tasks; a friendly message is shown when the list is empty.
* Update a task; updating a task that no longer exists is handled with an error message.
* Delete a task.
* Set the status: **Pending / In Progress / Completed**.

Extra features:

* Dashboard with an animated progress ring and counters (all, pending, in progress, completed, overdue).
* Priority (Low / Medium / High) and optional due date with overdue highlighting.
* Live search, status filter and six sort orders.
* **Undo** for deletions (single task or "clear completed").
* Tasks are saved automatically to `~/.taskflow/tasks.json` and loaded on start.
* **Light and dark mode** with a cross-fade (colours from the project palette), remembered between runs.
* Animations: cards fade and slide in, deleted cards slide out, sliding segmented control, hover lift,
  animated counters, toast messages, shake on invalid input.
* Keyboard shortcuts: `Ctrl+N` new task, `Ctrl+Z` undo, `Ctrl+F` search, `Ctrl+T` theme, `Esc` clear search;
  double-click a task to edit it.

## Requirements

* JDK 17 or newer
* NetBeans / IntelliJ IDEA / VS Code (Maven projects are supported by all of them)
* Git, GitHub, Javadoc (included in the JDK)

## Run

**NetBeans:** *File > Open Project* > select the `TaskFlow` folder > *Run Project* (F6).

**Command line (Maven):**
```
mvn compile exec:java -Dexec.mainClass=com.taskflow.Main
mvn test
mvn javadoc:javadoc
```

**Without Maven:**
```
javac -d out $(find src/main -name "*.java")        (Windows PowerShell: see docs/STEPS_AFTER_CODE.md)
java -cp out com.taskflow.Main
```
Use `--data=somefile.json` to store tasks in a different file.

## Project structure

```
src/main/java/com/taskflow
  Main.java                     entry point
  model/    Task, TaskStatus, Priority          data classes and enums
  service/  TaskService, SortOption, TaskStats  business logic, validation, custom exceptions
  storage/  TaskRepository, InMemory..., JsonFile..., MiniJson   persistence
  ui/       MainFrame, TaskCard, TaskDialog, StatsPanel, widgets, Theme, Anim, Icons
src/test/java                   JUnit 4 tests (service and storage)
docs/                           Javadoc, screenshots, test results, git evidence, report
```

## Design

Layered architecture: **UI -> Service -> Repository**. The UI contains no business rules; every change
goes through `TaskService`, which validates input, throws `ValidationException` / `TaskNotFoundException`,
saves through the `TaskRepository` interface and notifies the UI (Observer pattern).

Data structures: `LinkedHashMap<Integer, Task>` (O(1) lookup by id, keeps insertion order),
`ArrayDeque` as an undo stack, `ArrayList` of listeners, `Comparator` chains for sorting.

## Palette

`#ffffff` `#e5e7eb` `#1f2937` `#9ca3af` `#346ec9`: light mode uses white / light grey surfaces with navy text,
dark mode uses navy surfaces; `#346ec9` is the accent in both.
