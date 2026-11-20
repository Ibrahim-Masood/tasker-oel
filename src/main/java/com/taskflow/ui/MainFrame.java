package com.taskflow.ui;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.SortOption;
import com.taskflow.service.StorageException;
import com.taskflow.service.TaskException;
import com.taskflow.service.TaskNotFoundException;
import com.taskflow.service.TaskService;
import com.taskflow.service.TaskStats;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ColorUIResource;

/**
 * The main window of TaskFlow. It contains no business rules: every change goes through the
 * {@link TaskService}, and the window simply redraws itself when the service reports a change
 * (the Observer pattern).
 * <p>
 * Features: dashboard with progress ring, search, status filter, sorting, animated task cards,
 * add / edit dialog, undo for deletions, "clear completed", toast messages, light / dark mode
 * and keyboard shortcuts (Ctrl+N new task, Ctrl+Z undo, Ctrl+F search, Ctrl+T theme).
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** How cards should animate after the list is rebuilt. */
    private enum Mode {
        /** No animation. */ NONE,
        /** Only cards that were not visible before fade in. */ NEW_ONLY,
        /** All cards fade in one after another. */ STAGGER
    }

    private final transient TaskService service;
    private final transient Toasts toasts;
    private final StatsPanel stats = new StatsPanel();
    private final HintTextField searchField = new HintTextField("Search tasks by title or description...");
    private final Segmented filterBar = new Segmented(List.of("All", "Pending", "In Progress", "Completed"));
    private final FlatButton sortButton = new FlatButton("Sort: Newest first", Icons.Kind.CHEVRON_DOWN,
            FlatButton.Variant.SECONDARY);
    private final FlatButton undoButton = new FlatButton("Undo", Icons.Kind.UNDO, FlatButton.Variant.SECONDARY);
    private final FlatButton clearButton = new FlatButton("Clear completed", Icons.Kind.DOUBLE_CHECK,
            FlatButton.Variant.SECONDARY);
    private final FlatButton addButton = new FlatButton("New task", Icons.Kind.PLUS, FlatButton.Variant.PRIMARY);
    private final FlatButton themeButton = new FlatButton("", Icons.Kind.MOON, FlatButton.Variant.SECONDARY);
    private final ThemedLabel subtitle = new ThemedLabel(" ", () -> Theme.p().muted(), Theme.font(14f, Font.PLAIN));
    private final ThemedLabel resultCount = new ThemedLabel(" ", () -> Theme.p().muted(), Theme.font(13f, Font.PLAIN));
    private final ListPanel listPanel = new ListPanel();
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel center = new JPanel(centerLayout);
    private final EmptyState emptyState;

    private TaskStatus filter;
    private SortOption sort = SortOption.NEWEST;
    private final Map<Integer, Reveal> reveals = new HashMap<>();
    private Set<Integer> shown = new HashSet<>();
    private int pulseId = -1;
    private boolean deferRefresh;
    private boolean dirty;
    private Timer searchDebounce;

    /**
     * Builds the window and shows the saved tasks.
     *
     * @param service the business logic and data
     */
    public MainFrame(TaskService service) {
        super("Tasker – Task Management System");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(appIcon());
        setMinimumSize(new Dimension(940, 640));
        setSize(1160, 800);
        setLocationRelativeTo(null);

        emptyState = new EmptyState(this::addTask, this::loadSamples);
        Backdrop root = new Backdrop();
        root.setLayout(new BorderLayout());
        setContentPane(root);
        toasts = new Toasts(getLayeredPane());

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        applyUiDefaults();
        wireActions();
        service.addChangeListener(() -> {
            if (deferRefresh) {
                dirty = true;
            } else {
                refresh(Mode.NEW_ONLY);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                toasts.reposition(getLayeredPane());
            }
        });
        refresh(Mode.STAGGER);
    }

    // ------------------------------------------------------------------ building

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(26, 36, 14, 36));
        JPanel left = new JPanel(new BorderLayout(14, 0));
        left.setOpaque(false);
        left.add(new LogoMark(), BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        ThemedLabel name = new ThemedLabel("Tasker", () -> Theme.p().text(), Theme.font(27f, Font.BOLD));
        titles.add(name);
        titles.add(subtitle);
        left.add(titles, BorderLayout.CENTER);
        header.add(left, BorderLayout.WEST);
        themeButton.setToolTipText("Switch light / dark mode (Ctrl+T)");
        header.add(themeButton, BorderLayout.EAST);
        return header;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 36, 0, 36));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(stats);
        top.add(javax.swing.Box.createVerticalStrut(18));
        top.add(buildToolbar());
        body.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(listPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        center.setOpaque(false);
        center.add(scroll, "list");
        center.add(emptyState, "empty");
        body.add(center, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 12);
        FieldShell searchShell = new FieldShell(searchField, searchField, Icons.Kind.SEARCH);
        searchShell.setPreferredSize(new Dimension(200, 42));
        c.gridx = 0;
        bar.add(searchShell, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        bar.add(sortButton, c);
        c.gridx = 2;
        bar.add(undoButton, c);
        c.gridx = 3;
        bar.add(clearButton, c);
        c.gridx = 4;
        c.insets = new Insets(0, 0, 0, 0);
        bar.add(addButton, c);

        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(14, 0, 0, 12);
        bar.add(filterBar, c);
        c.gridx = 2;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(14, 0, 0, 0);
        bar.add(resultCount, c);
        return bar;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 36, 14, 36));
        footer.add(new ThemedLabel("Saved automatically · Ctrl+N new · Ctrl+Z undo · Ctrl+F search"
                + " · double-click a task to edit", () -> Theme.p().hint(), Theme.font(12f, Font.PLAIN)),
                BorderLayout.WEST);
        footer.add(new ThemedLabel("Built by Ibrahim Masood", () -> Theme.p().hint(), Theme.font(12f, Font.PLAIN)),
                BorderLayout.EAST);
        return footer;
    }

    private void wireActions() {
        addButton.setToolTipText("Add a new task (Ctrl+N)");
        addButton.addActionListener(e -> addTask());
        undoButton.setToolTipText("Restore the last deleted task (Ctrl+Z)");
        undoButton.addActionListener(e -> undo());
        clearButton.setToolTipText("Delete all completed tasks (can be undone)");
        clearButton.addActionListener(e -> clearCompleted());
        themeButton.addActionListener(e -> toggleTheme());
        sortButton.addActionListener(e -> showSortMenu());
        filterBar.setListener(i -> {
            filter = i == 0 ? null : TaskStatus.values()[i - 1];
            refresh(Mode.STAGGER);
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchChanged();
            }
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), this::addTask);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), this::undo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), () -> searchField.requestFocusInWindow());
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), this::toggleTheme);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), () -> {
            if (!searchField.getText().isEmpty()) {
                searchField.setText("");
            }
        });
    }

    private void bindKey(KeyStroke stroke, Runnable action) {
        Object key = "action-" + stroke;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
        getRootPane().getActionMap().put(key, new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------------ refresh

    private void searchChanged() {
        if (searchDebounce != null) {
            searchDebounce.stop();
        }
        searchDebounce = Anim.delay(150, () -> refresh(Mode.NONE));
    }

    /** Rebuilds the card list and the dashboard from the service. */
    private void refresh(Mode mode) {
        TaskStats s = service.getStats();
        stats.update(s);
        subtitle.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
                + "  ·  " + (s.pending() + s.inProgress()) + " open task"
                + (s.pending() + s.inProgress() == 1 ? "" : "s"));
        filterBar.setLabels(List.of("All  " + s.total(), "Pending  " + s.pending(),
                "In Progress  " + s.inProgress(), "Completed  " + s.completed()));
        undoButton.setEnabled(service.canUndo());
        clearButton.setEnabled(s.completed() > 0);

        List<Task> tasks = service.query(searchField.getText(), filter, sort);
        resultCount.setText(tasks.size() == s.total() ? s.total() + (s.total() == 1 ? " task" : " tasks")
                : "Showing " + tasks.size() + " of " + s.total());

        listPanel.removeAll();
        reveals.clear();
        Set<Integer> nowShown = new HashSet<>();
        int index = 0;
        for (Task t : tasks) {
            TaskCard card = new TaskCard(t, cardActions);
            Reveal r = new Reveal(card);
            reveals.put(t.getId(), r);
            listPanel.add(r);
            nowShown.add(t.getId());
            boolean animate = mode == Mode.STAGGER || (mode == Mode.NEW_ONLY && !shown.contains(t.getId()));
            if (animate) {
                r.animateIn(Math.min(index, 10) * 45);
            }
            if (t.getId() == pulseId) {
                card.pulse();
            }
            index++;
        }
        pulseId = -1;
        shown = nowShown;
        if (tasks.isEmpty()) {
            emptyState.setMode(s.total() == 0);
            centerLayout.show(center, "empty");
        } else {
            centerLayout.show(center, "list");
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    // ------------------------------------------------------------------ actions

    private final transient TaskCard.Actions cardActions = new TaskCard.Actions() {
        @Override
        public void toggleDone(Task task) {
            changeStatus(task, task.getStatus() == TaskStatus.COMPLETED ? TaskStatus.PENDING : TaskStatus.COMPLETED);
        }

        @Override
        public void cycleStatus(Task task) {
            changeStatus(task, task.getStatus().next());
        }

        @Override
        public void edit(Task task) {
            editTask(task);
        }

        @Override
        public void delete(Task task) {
            deleteTask(task);
        }
    };

    private void changeStatus(Task task, TaskStatus next) {
        pulseId = task.getId();
        try {
            service.setStatus(task.getId(), next);
            if (next == TaskStatus.COMPLETED) {
                toasts.show(Toasts.Kind.SUCCESS, "Completed “" + shorten(task.getTitle()) + "”", null, null);
            }
        } catch (TaskNotFoundException e) {
            reportError(e);
        } catch (StorageException e) {
            reportStorage(e);
        }
    }

    private void addTask() {
        int before = service.getTaskCount();
        deferRefresh = true;
        boolean ok = TaskDialog.open(this, null, d -> service.addTask(d.title(), d.description(), d.status(),
                d.priority(), d.dueDate()), this::reportError);
        deferRefresh = false;
        if (ok) {
            refresh(Mode.NEW_ONLY);
            toasts.show(Toasts.Kind.SUCCESS, "Task added", null, null);
        } else if (dirty || service.getTaskCount() != before) {
            refresh(Mode.NONE);
        }
        dirty = false;
    }

    private void editTask(Task task) {
        deferRefresh = true;
        boolean ok = TaskDialog.open(this, task, d -> service.updateTask(task.getId(), d.title(), d.description(),
                d.status(), d.priority(), d.dueDate()), this::reportError);
        deferRefresh = false;
        if (ok) {
            pulseId = task.getId();
            refresh(Mode.NONE);
            toasts.show(Toasts.Kind.SUCCESS, "Task updated", null, null);
        } else if (dirty) {
            refresh(Mode.NONE);
        }
        dirty = false;
    }

    private void deleteTask(Task task) {
        Reveal r = reveals.get(task.getId());
        Runnable doDelete = () -> {
            try {
                service.deleteTask(task.getId());
                toasts.show(Toasts.Kind.INFO, "Deleted “" + shorten(task.getTitle()) + "”", "Undo", this::undo);
            } catch (TaskNotFoundException e) {
                reportError(e);
                refresh(Mode.NONE);
            } catch (StorageException e) {
                reportStorage(e);
            }
        };
        if (r == null) {
            doDelete.run();
        } else {
            r.animateOut(doDelete);
        }
    }

    private void undo() {
        if (!service.canUndo()) {
            toasts.show(Toasts.Kind.INFO, "Nothing to undo", null, null);
            return;
        }
        try {
            List<Task> back = service.undoDelete();
            if (!back.isEmpty()) {
                pulseId = back.get(0).getId();
                toasts.show(Toasts.Kind.SUCCESS, back.size() == 1
                        ? "Restored “" + shorten(back.get(0).getTitle()) + "”"
                        : "Restored " + back.size() + " tasks", null, null);
            }
        } catch (StorageException e) {
            reportStorage(e);
        }
    }

    private void clearCompleted() {
        try {
            int n = service.clearCompleted();
            if (n == 0) {
                toasts.show(Toasts.Kind.INFO, "There are no completed tasks to clear", null, null);
            } else {
                toasts.show(Toasts.Kind.INFO, "Cleared " + n + (n == 1 ? " completed task" : " completed tasks"),
                        "Undo", this::undo);
            }
        } catch (StorageException e) {
            reportStorage(e);
        }
    }

    private void loadSamples() {
        try {
            SampleData.load(service);
            toasts.show(Toasts.Kind.SUCCESS, "Sample tasks loaded", null, null);
        } catch (TaskException e) {
            reportError(e);
        } catch (StorageException e) {
            reportStorage(e);
        }
    }

    private void showSortMenu() {
        Theme.Palette p = Theme.p();
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new LineBorder(p.border(), 1, true));
        menu.setBackground(p.surface());
        for (SortOption option : SortOption.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(option.getLabel(), option == sort);
            item.setFont(Theme.font(13.5f, Font.PLAIN));
            item.setBackground(p.surface());
            item.setForeground(p.text());
            item.setBorder(new EmptyBorder(8, 10, 8, 18));
            item.addActionListener(e -> {
                sort = option;
                sortButton.setText("Sort: " + option.getLabel());
                refresh(Mode.STAGGER);
            });
            menu.add(item);
        }
        menu.show(sortButton, 0, sortButton.getHeight() + 4);
    }

    private void toggleTheme() {
        BufferedImage old = new BufferedImage(getContentPane().getWidth(), getContentPane().getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = old.createGraphics();
        getContentPane().paint(g);
        g.dispose();
        Theme.toggle();
        applyUiDefaults();
        FadeGlass glass = new FadeGlass(old);
        setGlassPane(glass);
        glass.setVisible(true);
        Anim.run(420, Anim.EASE_IN_OUT, t -> {
            glass.alpha = (float) (1 - t);
            glass.repaint();
        }, () -> glass.setVisible(false));
        getContentPane().repaint();
    }

    // ------------------------------------------------------------------ helpers

    private void applyUiDefaults() {
        Theme.Palette p = Theme.p();
        themeButton.setIconKind(Theme.isDark() ? Icons.Kind.SUN : Icons.Kind.MOON);
        UIManager.put("ToolTip.background", new ColorUIResource(Theme.isDark() ? new Color(0x374151) : p.text()));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Theme.isDark() ? p.text() : Color.WHITE));
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
        UIManager.put("ToolTip.font", Theme.font(12.5f, Font.PLAIN));
        UIManager.put("RadioButtonMenuItem.selectionBackground", new ColorUIResource(p.surfaceAlt()));
        UIManager.put("RadioButtonMenuItem.selectionForeground", new ColorUIResource(p.text()));
        UIManager.put("RadioButtonMenuItem.acceleratorForeground", new ColorUIResource(p.muted()));
        UIManager.put("RadioButtonMenuItem.background", new ColorUIResource(p.surface()));
        UIManager.put("RadioButtonMenuItem.foreground", new ColorUIResource(p.text()));
        UIManager.put("PopupMenu.background", new ColorUIResource(p.surface()));
    }

    private void reportError(TaskException e) {
        toasts.show(Toasts.Kind.ERROR, e.getMessage(), null, null);
        refresh(Mode.NONE);
    }

    private void reportStorage(StorageException e) {
        toasts.show(Toasts.Kind.ERROR, "Could not save: " + e.getMessage(), null, null);
    }

    private static String shorten(String s) {
        return s.length() > 34 ? s.substring(0, 33) + "…" : s;
    }

    private static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Gfx.aa(img.getGraphics());
        LogoMark.paintMark(g, 0, 0, 64);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------ inner classes

    /** The window background: a flat colour with a soft blue glow at the top. */
    private static final class Backdrop extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.Palette p = Theme.p();
            g2.setColor(p.bg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            Color glow = Theme.mix(p.bg(), p.accent(), Theme.isDark() ? 0.16 : 0.09);
            g2.setPaint(new GradientPaint(0, 0, glow, 0, 300, p.bg()));
            g2.fillRect(0, 0, getWidth(), 300);
            g2.dispose();
        }
    }

    /** The blue rounded square with a tick that serves as the logo. */
    private static final class LogoMark extends JComponent {
        private static final long serialVersionUID = 1L;

        static void paintMark(Graphics2D g, double x, double y, double size) {
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(0x5A8BE0), (float) x, (float) (y + size),
                    new Color(0x2B5CB3)));
            g.fill(Gfx.rr(x, y, size, size, size * 0.28));
            Icons.paint(g, Icons.Kind.CHECK, x + size * 0.2, y + size * 0.2, size * 0.6, Color.WHITE);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(48, 48);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.aa(g);
            paintMark(g2, 0, 0, 48);
            g2.dispose();
        }
    }

    /** Vertical list of task cards that always matches the width of the scroll pane. */
    private static final class ListPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ListPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(2, 0, 10, 0));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(60, visibleRect.height - 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Glass pane that fades the old look away when the theme changes. */
    private static final class FadeGlass extends JComponent {
        private static final long serialVersionUID = 1L;
        private final transient BufferedImage snapshot;
        private float alpha = 1f;

        FadeGlass(BufferedImage snapshot) {
            this.snapshot = snapshot;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            Component content = SwingUtilities.getRootPane(this).getContentPane();
            Rectangle r = SwingUtilities.convertRectangle(content, new Rectangle(0, 0, content.getWidth(),
                    content.getHeight()), this);
            g2.drawImage(snapshot, r.x, r.y, null);
            g2.dispose();
        }
    }
}
