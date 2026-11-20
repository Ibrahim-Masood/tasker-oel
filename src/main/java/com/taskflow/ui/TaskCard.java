package com.taskflow.ui;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The visual representation of one task: a status circle, title, description, status / priority /
 * due-date badges and edit / delete buttons. The left edge is coloured by priority. The card lifts
 * with a soft shadow when the mouse is over it and can flash briefly to confirm a change.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class TaskCard extends RoundedPanel {

    /** What the card asks the window to do. */
    public interface Actions {
        /**
         * The user clicked the status circle: complete the task or reopen it.
         *
         * @param task the task shown on the card
         */
        void toggleDone(Task task);

        /**
         * The user clicked the status badge: move to the next status.
         *
         * @param task the task shown on the card
         */
        void cycleStatus(Task task);

        /**
         * The user wants to edit the task.
         *
         * @param task the task shown on the card
         */
        void edit(Task task);

        /**
         * The user wants to delete the task.
         *
         * @param task the task shown on the card
         */
        void delete(Task task);
    }

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter CREATED = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);

    private final Task task;
    private final Anim.Value hover = new Anim.Value(this::repaint);
    private final Anim.Value pulse = new Anim.Value(this::repaint);

    /**
     * Builds the card for a task.
     *
     * @param task    the task to show (a copy; the card never changes it)
     * @param actions receives the user's clicks
     */
    public TaskCard(Task task, Actions actions) {
        super(new GridBagLayout(), 16, true);
        this.task = task;
        setBorder(BorderFactory.createEmptyBorder(SHADOW_TOP + 12, SHADOW_X + 20, SHADOW_BOTTOM + 12, SHADOW_X + 10));

        boolean done = task.getStatus() == TaskStatus.COMPLETED;
        GridBagConstraints c = new GridBagConstraints();

        StatusCircle circle = new StatusCircle(task.getStatus(), () -> actions.toggleDone(task));
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 14);
        add(circle, c);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        ThemedLabel title = new ThemedLabel(task.getTitle(),
                () -> task.getStatus() == TaskStatus.COMPLETED ? Theme.p().muted() : Theme.p().text(),
                Theme.font(15.5f, Font.BOLD));
        title.setStrike(done);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean noDesc = task.getDescription().isEmpty();
        ThemedLabel desc = new ThemedLabel(noDesc ? "No description" : task.getDescription(),
                () -> noDesc ? Theme.p().hint() : Theme.p().muted(), Theme.font(13f, noDesc ? Font.ITALIC : Font.PLAIN));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!noDesc) {
            desc.setToolTipText("<html><body style='width:340px'>" + escape(task.getDescription()) + "</body></html>");
        }
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        TaskStatus st = task.getStatus();
        Badge statusBadge = new Badge(st.getLabel(), () -> Theme.statusColor(st)).withDot()
                .clickable(() -> actions.cycleStatus(task), "Click to change status");
        Badge prio = new Badge(task.getPriority().getLabel(), () -> Theme.priorityColor(task.getPriority()))
                .withIcon(Icons.Kind.ALERT);
        meta.add(statusBadge);
        meta.add(gap());
        meta.add(prio);
        String due = dueText(task);
        if (due != null) {
            boolean overdue = task.isOverdue();
            LocalDate d = task.getDueDate();
            meta.add(gap());
            meta.add(new Badge(due, () -> overdue ? Theme.p().danger()
                    : (d.equals(LocalDate.now()) ? Theme.p().warning() : Theme.p().muted()))
                    .withIcon(overdue ? Icons.Kind.CLOCK : Icons.Kind.CALENDAR));
        }
        meta.add(gap());
        meta.add(new ThemedLabel("Created " + task.getCreatedAt().format(CREATED), () -> Theme.p().hint(),
                Theme.font(12f, Font.PLAIN)));
        center.add(title);
        center.add(javax.swing.Box.createVerticalStrut(3));
        center.add(desc);
        center.add(javax.swing.Box.createVerticalStrut(9));
        center.add(meta);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 8);
        add(center, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        FlatButton edit = new FlatButton("", Icons.Kind.EDIT, FlatButton.Variant.GHOST);
        edit.setCompact(true);
        edit.setToolTipText("Edit task");
        edit.addActionListener(e -> actions.edit(task));
        FlatButton delete = new FlatButton("", Icons.Kind.TRASH, FlatButton.Variant.GHOST_DANGER);
        delete.setCompact(true);
        delete.setToolTipText("Delete task");
        delete.addActionListener(e -> actions.delete(task));
        buttons.add(edit);
        buttons.add(delete);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        add(buttons, c);

        MouseAdapter hoverTracker = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover.to(1, 160);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), TaskCard.this);
                if (!TaskCard.this.contains(p)) {
                    hover.to(0, 240);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    actions.edit(task);
                }
            }
        };
        attach(this, hoverTracker);
    }

    private static Component gap() {
        return javax.swing.Box.createHorizontalStrut(8);
    }

    private static void attach(Container root, MouseAdapter l) {
        root.addMouseListener(l);
        for (Component child : root.getComponents()) {
            if (child instanceof Container sub && !(child instanceof javax.swing.JButton)) {
                attach(sub, l);
            } else if (child instanceof JComponent jc && !(child instanceof javax.swing.JButton)) {
                jc.addMouseListener(l);
            }
        }
    }

    /**
     * Returns the task shown on this card.
     *
     * @return the task snapshot
     */
    public Task getTask() {
        return task;
    }

    /** Flashes the card's border once, to confirm that something just changed. */
    public void pulse() {
        pulse.jump(1);
        Anim.delay(80, () -> pulse.to(0, 1100));
    }

    @Override
    protected double highlight() {
        return Math.max(hover.get() * 0.8, pulse.get());
    }

    @Override
    protected Color fillColor() {
        Theme.Palette p = Theme.p();
        return Theme.mix(p.surface(), p.accent(), pulse.get() * 0.10 + hover.get() * (Theme.isDark() ? 0.04 : 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Gfx.aa(g);
        RoundRectangle2D b = body();
        g2.clip(b);
        g2.setColor(Theme.priorityColor(task.getPriority()));
        g2.fillRect((int) b.getX(), (int) b.getY(), 5, (int) b.getHeight());
        g2.dispose();
    }

    private static String dueText(Task t) {
        LocalDate d = t.getDueDate();
        if (d == null) {
            return null;
        }
        if (t.getStatus() == TaskStatus.COMPLETED) {
            return "Due " + d.format(SHORT_DATE);
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days < 0) {
            return days == -1 ? "Overdue · yesterday" : "Overdue · " + d.format(SHORT_DATE);
        }
        if (days == 0) {
            return "Due today";
        }
        if (days == 1) {
            return "Due tomorrow";
        }
        return "Due " + d.format(SHORT_DATE);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** The round status indicator on the left; clicking it completes or reopens the task. */
    private static final class StatusCircle extends JComponent {
        private final TaskStatus status;
        private final Anim.Value hover = new Anim.Value(this::repaint);

        StatusCircle(TaskStatus status, Runnable onClick) {
            this.status = status;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(status == TaskStatus.COMPLETED ? "Mark as pending" : "Mark as completed");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1, 140);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0, 200);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(30, 30);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.aa(g);
            Theme.Palette p = Theme.p();
            double h = hover.get();
            double d = 26;
            double x = (getWidth() - d) / 2.0;
            double y = (getHeight() - d) / 2.0;
            switch (status) {
                case COMPLETED -> {
                    g2.setColor(Theme.mix(p.success(), Color.WHITE, 0.10 * h));
                    g2.fill(new Ellipse2D.Double(x, y, d, d));
                    Icons.paint(g2, Icons.Kind.CHECK, x + 4, y + 4, 18, Color.WHITE);
                }
                case IN_PROGRESS -> {
                    Color a = Theme.mix(p.accentText(), Color.WHITE, 0.1 * h);
                    g2.setColor(Theme.alpha(a, 50 + (int) (50 * h)));
                    g2.fill(new Arc2D.Double(x, y, d, d, 90, -180, Arc2D.PIE));
                    g2.setStroke(new java.awt.BasicStroke(2.2f));
                    g2.setColor(a);
                    g2.draw(new Ellipse2D.Double(x + 1, y + 1, d - 2, d - 2));
                }
                default -> {
                    g2.setStroke(new java.awt.BasicStroke(2.2f));
                    g2.setColor(Theme.mix(p.hint(), p.success(), h));
                    g2.draw(new Ellipse2D.Double(x + 1, y + 1, d - 2, d - 2));
                    if (h > 0.02) {
                        Icons.paint(g2, Icons.Kind.CHECK, x + 4, y + 4, 18, Theme.alpha(p.success(), (int) (200 * h)));
                    }
                }
            }
            g2.dispose();
        }
    }
}
