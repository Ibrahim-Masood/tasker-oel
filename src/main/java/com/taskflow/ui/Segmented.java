package com.taskflow.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.JComponent;

/**
 * A segmented control (a row of options where exactly one is selected). The blue highlight
 * slides smoothly from one option to the next. It is used for the status filter and for the
 * status and priority pickers in the task dialog.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class Segmented extends JComponent {

    private static final int PAD = 4;
    private static final int SEG_PAD = 18;

    private List<String> labels;
    private int selected;
    private IntConsumer listener;
    private final Anim.Value pos = new Anim.Value(this::repaint);

    /**
     * Creates a control.
     *
     * @param labels the option texts, in order
     */
    public Segmented(List<String> labels) {
        this.labels = new ArrayList<>(labels);
        setOpaque(false);
        setFocusable(true);
        setFont(Theme.font(13f, Font.BOLD));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0) {
                    select(i, true);
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    select(Math.min(Segmented.this.labels.size() - 1, selected + 1), true);
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    select(Math.max(0, selected - 1), true);
                }
            }
        });
    }

    /**
     * Sets the code that runs when the user picks an option.
     *
     * @param listener receives the index of the new selection
     */
    public void setListener(IntConsumer listener) {
        this.listener = listener;
    }

    /**
     * Replaces the option texts (for example to show updated counts) without changing the selection.
     *
     * @param newLabels the new texts, same number as before
     */
    public void setLabels(List<String> newLabels) {
        this.labels = new ArrayList<>(newLabels);
        revalidate();
        repaint();
    }

    /**
     * Returns the selected index.
     *
     * @return the index of the selected option
     */
    public int getSelectedIndex() {
        return selected;
    }

    /**
     * Selects an option.
     *
     * @param index  which option
     * @param notify whether to call the listener
     */
    public void select(int index, boolean notify) {
        if (index < 0 || index >= labels.size()) {
            return;
        }
        boolean changed = index != selected;
        selected = index;
        pos.to(index, 260);
        if (changed && notify && listener != null) {
            listener.accept(index);
        }
    }

    /**
     * Selects an option without animation (used when a dialog opens).
     *
     * @param index which option
     */
    public void setSelectedIndexNow(int index) {
        selected = Math.max(0, Math.min(labels.size() - 1, index));
        pos.jump(selected);
    }

    private int[] widths() {
        FontMetrics fm = getFontMetrics(getFont());
        int[] w = new int[labels.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = fm.stringWidth(labels.get(i)) + SEG_PAD * 2;
        }
        return w;
    }

    private int indexAt(int x) {
        int cx = PAD;
        int[] w = widths();
        for (int i = 0; i < w.length; i++) {
            if (x >= cx && x < cx + w[i]) {
                return i;
            }
            cx += w[i];
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        int total = PAD * 2;
        for (int w : widths()) {
            total += w;
        }
        return new Dimension(total, 40);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Gfx.aa(g);
        int h = getHeight();
        g2.setColor(Theme.isDark() ? p.surface() : p.surfaceAlt());
        g2.fill(Gfx.rr(0, 0, getWidth(), h, 12));
        g2.setColor(p.border());
        g2.draw(Gfx.rr(0.5, 0.5, getWidth() - 1, h - 1, 12));

        int[] w = widths();
        double[] x = new double[w.length];
        double cx = PAD;
        for (int i = 0; i < w.length; i++) {
            x[i] = cx;
            cx += w[i];
        }
        double fp = Math.max(0, Math.min(w.length - 1, pos.get()));
        int lo = (int) Math.floor(fp);
        int hi = Math.min(w.length - 1, lo + 1);
        double frac = fp - lo;
        double hx = x[lo] + (x[hi] - x[lo]) * frac;
        double hw = w[lo] + (w[hi] - w[lo]) * frac;
        g2.setColor(p.accent());
        g2.fill(Gfx.rr(hx, PAD, hw, h - PAD * 2, 9));

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < w.length; i++) {
            double closeness = Math.max(0, 1 - Math.abs(fp - i));
            Color c = Theme.mix(p.muted(), p.onAccent(), closeness);
            g2.setColor(c);
            String s = labels.get(i);
            g2.drawString(s, (float) (x[i] + (w[i] - fm.stringWidth(s)) / 2.0),
                    (h - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (hasFocus()) {
            g2.setColor(Theme.alpha(p.accent(), 110));
            g2.draw(Gfx.rr(1.5, 1.5, getWidth() - 3, h - 3, 11));
        }
        g2.dispose();
    }
}
