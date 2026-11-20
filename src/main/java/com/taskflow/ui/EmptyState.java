package com.taskflow.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The friendly message shown instead of the task list when there is nothing to display. It covers
 * two situations: there are no tasks at all, or the current search / filter matches nothing.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class EmptyState extends JPanel {

    private final ThemedLabel title;
    private final ThemedLabel subtitle;
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    private final IconDisc disc = new IconDisc();

    /**
     * Creates the empty-state panel.
     *
     * @param onAdd    runs when "Add your first task" is clicked
     * @param onSample runs when "Load sample tasks" is clicked
     */
    public EmptyState(Runnable onAdd, Runnable onSample) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        title = new ThemedLabel("", () -> Theme.p().text(), Theme.font(21f, Font.BOLD));
        subtitle = new ThemedLabel("", () -> Theme.p().muted(), Theme.font(14f, Font.PLAIN));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        for (JComponent c : new JComponent[] {disc, title, subtitle, buttons}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        buttons.setOpaque(false);
        FlatButton add = new FlatButton("Add your first task", Icons.Kind.PLUS, FlatButton.Variant.PRIMARY);
        add.addActionListener(e -> onAdd.run());
        FlatButton sample = new FlatButton("Load sample tasks", Icons.Kind.LIST, FlatButton.Variant.SECONDARY);
        sample.addActionListener(e -> onSample.run());
        buttons.add(add);
        buttons.add(sample);
        buttons.setMaximumSize(new Dimension(520, 44));
        setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        add(Box.createVerticalGlue());
        add(disc);
        add(Box.createVerticalStrut(18));
        add(title);
        add(Box.createVerticalStrut(6));
        add(subtitle);
        add(Box.createVerticalStrut(22));
        add(buttons);
        add(Box.createVerticalGlue());
    }

    /**
     * Chooses the message.
     *
     * @param noTasksAtAll {@code true} if the list is empty; {@code false} if only the filter matches nothing
     */
    public void setMode(boolean noTasksAtAll) {
        if (noTasksAtAll) {
            title.setText("No tasks yet");
            subtitle.setText("Create your first task to get started, or load a few samples to explore.");
        } else {
            title.setText("No matching tasks");
            subtitle.setText("Try a different search word or choose another status filter.");
        }
        buttons.setVisible(noTasksAtAll);
        revalidate();
        repaint();
    }

    /** The big circle with a clipboard icon. */
    private static final class IconDisc extends JComponent {
        IconDisc() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(96, 96);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.aa(g);
            Theme.Palette p = Theme.p();
            g2.setColor(Theme.alpha(p.accent(), 30));
            g2.fill(new Ellipse2D.Double(0, 0, 96, 96));
            g2.setColor(Theme.alpha(p.accent(), 40));
            g2.fill(new Ellipse2D.Double(14, 14, 68, 68));
            Icons.paint(g2, Icons.Kind.CLIPBOARD, 28, 28, 40, p.accentText());
            g2.dispose();
        }
    }
}
