package com.taskflow.ui;

import com.taskflow.service.TaskStats;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * The dashboard row at the top of the window: a progress ring plus counters for all tasks,
 * pending, in progress, completed and overdue tasks. Numbers count up smoothly and the ring
 * sweeps to its new angle whenever the data changes.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class StatsPanel extends JPanel {

    private final ProgressCard progress = new ProgressCard();
    private final StatCard total = new StatCard("All tasks", Icons.Kind.LIST, () -> Theme.p().accentText());
    private final StatCard pending = new StatCard("Pending", Icons.Kind.CLOCK, () -> Theme.p().muted());
    private final StatCard inProgress = new StatCard("In progress", Icons.Kind.PLAY, () -> Theme.p().accentText());
    private final StatCard completed = new StatCard("Completed", Icons.Kind.CHECK, () -> Theme.p().success());
    private final StatCard overdue = new StatCard("Overdue", Icons.Kind.ALERT, () -> Theme.p().danger());

    /** Creates the panel with all counters at zero. */
    public StatsPanel() {
        super(new GridBagLayout());
        setOpaque(false);
        StatCard[] cards = {total, pending, inProgress, completed, overdue};
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 1.55;
        c.insets = new Insets(0, 0, 0, 12);
        add(progress, c);
        for (int i = 0; i < cards.length; i++) {
            c.gridx = i + 1;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 0, i == cards.length - 1 ? 0 : 12);
            add(cards[i], c);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, 98);
    }

    /**
     * Shows new numbers, animating from the old ones.
     *
     * @param s the current statistics
     */
    public void update(TaskStats s) {
        progress.setProgress(s.completionRatio(), s.completed(), s.total());
        total.setValue(s.total());
        pending.setValue(s.pending());
        inProgress.setValue(s.inProgress());
        completed.setValue(s.completed());
        overdue.setValue(s.overdue());
    }

    /** One counter tile. */
    private static final class StatCard extends RoundedPanel {
        private final String label;
        private final Icons.Kind icon;
        private final Supplier<Color> color;
        private final Anim.Value number = new Anim.Value(this::repaint);
        private final Anim.Value hover = new Anim.Value(this::repaint);
        private int target;

        StatCard(String label, Icons.Kind icon, Supplier<Color> color) {
            super(null, 16, true);
            this.label = label;
            this.icon = icon;
            this.color = color;
            setBorder(BorderFactory.createEmptyBorder(SHADOW_TOP, SHADOW_X, SHADOW_BOTTOM, SHADOW_X));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover.to(1, 150);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover.to(0, 220);
                }
            });
        }

        void setValue(int v) {
            if (v != target) {
                target = v;
                number.to(v, 650);
            }
        }

        @Override
        protected double highlight() {
            return hover.get();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Gfx.aa(g);
            Theme.Palette p = Theme.p();
            Color c = color.get();
            int x = SHADOW_X + 18;
            int h = getHeight() - SHADOW_TOP - SHADOW_BOTTOM;
            int cy = SHADOW_TOP + h / 2;
            g2.setColor(Theme.alpha(c, 36));
            g2.fill(new Ellipse2D.Double(x, cy - 21, 42, 42));
            Icons.paint(g2, icon, x + 10, cy - 11, 22, c);

            g2.setFont(Theme.font(28f, Font.BOLD));
            g2.setColor(p.text());
            g2.drawString(String.valueOf(Math.round(number.get())), x + 56, cy + 2);
            g2.setFont(Theme.font(12.5f, Font.PLAIN));
            g2.setColor(p.muted());
            g2.drawString(label, x + 56, cy + 22);
            g2.dispose();
        }
    }

    /** The wide tile with the progress ring. */
    private static final class ProgressCard extends RoundedPanel {
        private final Anim.Value sweep = new Anim.Value(this::repaint);
        private final Anim.Value hover = new Anim.Value(this::repaint);
        private int done;
        private int all;

        ProgressCard() {
            super(null, 16, true);
            setBorder(BorderFactory.createEmptyBorder(SHADOW_TOP, SHADOW_X, SHADOW_BOTTOM, SHADOW_X));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover.to(1, 150);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover.to(0, 220);
                }
            });
        }

        void setProgress(double ratio, int completed, int total) {
            this.done = completed;
            this.all = total;
            sweep.to(ratio, 800);
        }

        @Override
        protected double highlight() {
            return hover.get();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Gfx.aa(g);
            Theme.Palette p = Theme.p();
            int h = getHeight() - SHADOW_TOP - SHADOW_BOTTOM;
            int d = 62;
            double rx = SHADOW_X + 18;
            double ry = SHADOW_TOP + (h - d) / 2.0;
            g2.setStroke(new java.awt.BasicStroke(7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.mix(p.border(), p.surfaceAlt(), 0.2));
            g2.draw(new Ellipse2D.Double(rx + 4, ry + 4, d - 8, d - 8));
            double ratio = sweep.get();
            if (ratio > 0.001) {
                g2.setColor(ratio >= 0.999 ? p.success() : p.accent());
                g2.draw(new Arc2D.Double(rx + 4, ry + 4, d - 8, d - 8, 90, -360 * ratio, Arc2D.OPEN));
            }
            String pct = Math.round(ratio * 100) + "%";
            g2.setFont(Theme.font(15f, Font.BOLD));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(p.text());
            g2.drawString(pct, (float) (rx + (d - fm.stringWidth(pct)) / 2.0), (float) (ry + d / 2.0 + fm.getAscent() / 2.0 - 2));

            double tx = rx + d + 16;
            g2.setFont(Theme.font(15f, Font.BOLD));
            g2.setColor(p.text());
            g2.drawString("Progress", (float) tx, (float) (ry + 24));
            g2.setFont(Theme.font(12.5f, Font.PLAIN));
            g2.setColor(p.muted());
            String sub = all == 0 ? "No tasks yet" : done + " of " + all + " completed";
            g2.drawString(sub, (float) tx, (float) (ry + 44));
            g2.dispose();
        }
    }
}
