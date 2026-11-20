package com.taskflow.ui;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.TaskException;
import com.taskflow.service.TaskService;
import com.taskflow.service.ValidationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The window used both to add a new task and to edit an existing one. Input is checked while the
 * user types (character counters, red hint when the title is empty) and again by the service when
 * the user presses Save; any message from the service is shown inside the dialog and the window
 * shakes to draw attention to it.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class TaskDialog extends JDialog {

    /**
     * The values typed by the user.
     *
     * @param title       the title text
     * @param description the description text
     * @param status      the chosen status
     * @param priority    the chosen priority
     * @param dueDate     the chosen due date, or {@code null}
     */
    public record Draft(String title, String description, TaskStatus status, Priority priority, LocalDate dueDate) { }

    /** Saves a draft; implemented by the main window using the {@link TaskService}. */
    public interface Saver {
        /**
         * Stores the draft.
         *
         * @param draft what the user entered
         * @throws TaskException if the service rejects the data or the task no longer exists
         */
        void save(Draft draft) throws TaskException;
    }

    private final Saver saver;
    private final HintTextField titleField = new HintTextField("What needs to be done?");
    private final JTextArea descArea = new JTextArea(4, 20);
    private final HintTextField dueField = new HintTextField("YYYY-MM-DD  (optional)");
    private final Segmented statusPick = new Segmented(List.of("Pending", "In Progress", "Completed"));
    private final Segmented priorityPick = new Segmented(List.of("Low", "Medium", "High"));
    private final FieldShell titleShell;
    private final FieldShell descShell;
    private final FieldShell dueShell;
    private final ThemedLabel titleError = errorLabel();
    private final ThemedLabel dueError = errorLabel();
    private final ThemedLabel titleCount = counterLabel();
    private final ThemedLabel descCount = counterLabel();
    private boolean titleTouched;
    private boolean saved;
    private TaskException failure;

    private TaskDialog(Frame owner, Task existing, Saver saver) {
        super(owner, true);
        this.saver = saver;
        setUndecorated(true);
        setTitle(existing == null ? "New task" : "Edit task");

        titleField.getDocument().addDocumentListener(simpleListener(this::onTitleChanged));
        descArea.getDocument().addDocumentListener(simpleListener(this::onDescChanged));
        dueField.getDocument().addDocumentListener(simpleListener(this::clearDueError));
        descArea.setOpaque(false);
        descArea.setBorder(null);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(Theme.font(14f, Font.PLAIN));
        JScrollPane descScroll = new JScrollPane(descArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        descScroll.getViewport().setOpaque(false);
        descScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        descScroll.getVerticalScrollBar().setOpaque(false);
        titleShell = new FieldShell(titleField, titleField, null);
        descShell = new FieldShell(descScroll, descArea, null);
        dueShell = new FieldShell(dueField, dueField, Icons.Kind.CALENDAR);

        RoundedPanel root = new RoundedPanel(new BorderLayout(), 20, false) {
            @Override
            protected Color borderColor() {
                return Theme.p().border();
            }
        };
        root.setOpaque(true);
        root.add(buildHeader(existing), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildFooter(existing), BorderLayout.SOUTH);
        setContentPane(new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.p().border());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        getContentPane().add(root);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        if (existing != null) {
            titleField.setText(existing.getTitle());
            descArea.setText(existing.getDescription());
            statusPick.setSelectedIndexNow(existing.getStatus().ordinal());
            priorityPick.setSelectedIndexNow(existing.getPriority().ordinal());
            if (existing.getDueDate() != null) {
                dueField.setText(existing.getDueDate().toString());
            }
            titleTouched = true;
        } else {
            statusPick.setSelectedIndexNow(0);
            priorityPick.setSelectedIndexNow(1);
        }
        onTitleChanged();
        onDescChanged();

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> submit(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        titleField.addActionListener(e -> submit());
        dueField.addActionListener(e -> submit());

        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the dialog and waits until it is closed.
     *
     * @param owner    the parent window
     * @param existing the task to edit, or {@code null} to create a new one
     * @param saver    stores the data when the user presses Save
     * @param onFailure receives errors other than validation errors (for example "task not found")
     * @return {@code true} if the data was saved, {@code false} if the user cancelled or saving failed
     */
    public static boolean open(Frame owner, Task existing, Saver saver, Consumer<TaskException> onFailure) {
        TaskDialog d = new TaskDialog(owner, existing, saver);
        d.fadeIn();
        d.titleField.requestFocusInWindow();
        d.setVisible(true);
        if (d.failure != null) {
            onFailure.accept(d.failure);
        }
        return d.saved;
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildHeader(Task existing) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Gfx.aa(g);
                g2.setColor(Theme.p().surface());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 6, 18));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        ThemedLabel h = new ThemedLabel(existing == null ? "New task" : "Edit task",
                () -> Theme.p().text(), Theme.font(21f, Font.BOLD));
        ThemedLabel sub = new ThemedLabel(existing == null ? "Add the details below and press Save."
                : "Task #" + existing.getId() + " – change anything and press Save.",
                () -> Theme.p().muted(), Theme.font(13f, Font.PLAIN));
        titles.add(h);
        titles.add(javax.swing.Box.createVerticalStrut(2));
        titles.add(sub);
        header.add(titles, BorderLayout.CENTER);
        FlatButton close = new FlatButton("", Icons.Kind.CLOSE, FlatButton.Variant.GHOST);
        close.setToolTipText("Close (Esc)");
        close.addActionListener(e -> dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(close);
        header.add(closeWrap, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            private Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - start.x, loc.y + e.getY() - start.y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        titles.addMouseListener(drag);
        titles.addMouseMotionListener(drag);
        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.p().surface());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        form.setBorder(BorderFactory.createEmptyBorder(12, 28, 8, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        int row = 0;

        c.gridy = row++;
        form.add(labelRow("Title", true, titleCount), c);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(titleShell, c);
        c.gridy = row++;
        c.insets = new Insets(4, 2, 8, 0);
        form.add(titleError, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = row++;
        form.add(labelRow("Description", false, descCount), c);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 14, 0);
        form.add(descShell, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = row++;
        form.add(labelRow("Status", false, null), c);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 14, 0);
        form.add(wrapLeft(statusPick), c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = row++;
        form.add(labelRow("Priority", false, null), c);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 14, 0);
        form.add(wrapLeft(priorityPick), c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = row++;
        form.add(labelRow("Due date", false, null), c);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(dueShell, c);
        c.gridy = row++;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(quickDates(), c);
        c.gridy = row;
        c.insets = new Insets(4, 2, 0, 0);
        form.add(dueError, c);
        return form;
    }

    private JComponent buildFooter(Task existing) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.p().surface());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        footer.setBorder(BorderFactory.createEmptyBorder(8, 18, 22, 18));
        FlatButton cancel = new FlatButton("Cancel", null, FlatButton.Variant.SECONDARY);
        cancel.addActionListener(e -> dispose());
        FlatButton save = new FlatButton(existing == null ? "Add task" : "Save changes", Icons.Kind.CHECK,
                FlatButton.Variant.PRIMARY);
        save.addActionListener(e -> submit());
        footer.add(cancel);
        footer.add(save);
        return footer;
    }

    private JComponent labelRow(String text, boolean required, ThemedLabel counter) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(new ThemedLabel(text, () -> Theme.p().text(), Theme.font(13.5f, Font.BOLD)));
        if (required) {
            left.add(new ThemedLabel(" *", () -> Theme.p().danger(), Theme.font(13.5f, Font.BOLD)));
        }
        row.add(left, BorderLayout.WEST);
        if (counter != null) {
            row.add(counter, BorderLayout.EAST);
        }
        return row;
    }

    private JComponent wrapLeft(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private JComponent quickDates() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(quickButton("Today", 0));
        p.add(quickButton("Tomorrow", 1));
        p.add(quickButton("In a week", 7));
        FlatButton none = new FlatButton("No date", null, FlatButton.Variant.GHOST);
        none.setFont(Theme.font(12.5f, Font.BOLD));
        none.addActionListener(e -> dueField.setText(""));
        p.add(none);
        return p;
    }

    private FlatButton quickButton(String label, int days) {
        FlatButton b = new FlatButton(label, null, FlatButton.Variant.SECONDARY);
        b.setFont(Theme.font(12.5f, Font.BOLD));
        b.addActionListener(e -> dueField.setText(LocalDate.now().plusDays(days).toString()));
        return b;
    }

    private static ThemedLabel errorLabel() {
        return new ThemedLabel(" ", () -> Theme.p().danger(), Theme.font(12.5f, Font.PLAIN));
    }

    private static ThemedLabel counterLabel() {
        return new ThemedLabel("", () -> Theme.p().hint(), Theme.font(12f, Font.PLAIN));
    }

    // ----------------------------------------------------------------- behaviour

    private void onTitleChanged() {
        int len = titleField.getText().length();
        titleCount.setText(len + " / " + TaskService.MAX_TITLE_LENGTH);
        if (len > 0) {
            titleTouched = true;
        }
        boolean blank = titleField.getText().trim().isEmpty();
        boolean tooLong = titleField.getText().trim().length() > TaskService.MAX_TITLE_LENGTH;
        if (titleTouched && blank) {
            showTitleError("Title cannot be empty.");
        } else if (tooLong) {
            showTitleError("Title is too long (maximum " + TaskService.MAX_TITLE_LENGTH + " characters).");
        } else {
            titleError.setText(" ");
            titleShell.setError(false);
        }
    }

    private void clearDueError() {
        dueError.setText(" ");
        dueShell.setError(false);
    }

    private void onDescChanged() {
        int len = descArea.getText().length();
        descCount.setText(len + " / " + TaskService.MAX_DESCRIPTION_LENGTH);
        descShell.setError(len > TaskService.MAX_DESCRIPTION_LENGTH);
    }

    private void showTitleError(String message) {
        titleError.setText(message);
        titleShell.setError(true);
    }

    private void submit() {
        LocalDate due = null;
        String dueText = dueField.getText().trim();
        if (!dueText.isEmpty()) {
            try {
                due = LocalDate.parse(dueText);
            } catch (DateTimeParseException e) {
                dueError.setText("Use the format YYYY-MM-DD, for example " + LocalDate.now().plusDays(3) + ".");
                dueShell.setError(true);
                shake();
                return;
            }
        }
        Draft draft = new Draft(titleField.getText(), descArea.getText(),
                TaskStatus.values()[statusPick.getSelectedIndex()],
                Priority.values()[priorityPick.getSelectedIndex()], due);
        try {
            saver.save(draft);
            saved = true;
            dispose();
        } catch (ValidationException e) {
            switch (e.getField()) {
                case ValidationException.FIELD_TITLE -> {
                    showTitleError(e.getMessage());
                    titleField.requestFocusInWindow();
                }
                case ValidationException.FIELD_DUE_DATE -> {
                    dueError.setText(e.getMessage());
                    dueShell.setError(true);
                }
                default -> descShell.setError(true);
            }
            shake();
        } catch (TaskException e) {
            // e.g. the task was removed meanwhile: close and let the main window report it
            failure = e;
            dispose();
        }
    }

    private void shake() {
        Point origin = getLocation();
        Anim.run(380, Anim.LINEAR, t -> setLocation(origin.x + (int) (Math.sin(t * Math.PI * 7) * 11 * (1 - t)),
                origin.y), () -> setLocation(origin));
    }

    private void fadeIn() {
        try {
            if (!GraphicsEnvironment.isHeadless() && getGraphicsConfiguration() != null
                    && getGraphicsConfiguration().getDevice()
                    .isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
                setOpacity(0f);
                Anim.run(200, Anim.EASE_OUT, t -> setOpacity((float) t), null);
            }
        } catch (RuntimeException ignored) {
            // no translucency support: the dialog simply appears
        }
    }

    private static DocumentListener simpleListener(Runnable r) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        };
    }
}
