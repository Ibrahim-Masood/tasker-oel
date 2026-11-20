package com.taskflow.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.function.Supplier;

import javax.swing.JComponent;

/**
 * A small pill-shaped label such as "In Progress" or "High". It can show a coloured dot or an icon
 * and can optionally react to clicks.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class Badge extends JComponent {

    private final String text;
    private final Supplier<Color> color;
    private Icons.Kind icon;
    private boolean dot;
    private Runnable onClick;
    private final Anim.Value hover = new Anim.Value(this::repaint);

    /**
     * Creates a badge.
     *
     * @param text  the text
     * @param color supplies the current colour of the text and tinted background
     */
    public Badge(String text, Supplier<Color> color) {
        this.text = text;
        this.color = color;
        setOpaque(false);
        setFont(Theme.font(12f, Font.BOLD));
    }

    /**
     * Shows a small filled dot before the text.
     *
     * @return this badge, for chaining
     */
    public Badge withDot() {
        this.dot = true;
        return this;
    }

    /**
     * Shows an icon before the text.
     *
     * @param kind the icon
     * @return this badge, for chaining
     */
    public Badge withIcon(Icons.Kind kind) {
        this.icon = kind;
        return this;
    }

    /**
     * Makes the badge clickable.
     *
     * @param action the code to run on click
     * @param tip    the tooltip text
     * @return this badge, for chaining
     */
    public Badge clickable(Runnable action, String tip) {
        this.onClick = action;
        setToolTipText(tip);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover.to(1, 120);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover.to(0, 180);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
        });
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(text) + 20;
        if (dot) {
            w += 12;
        }
        if (icon != null) {
            w += 18;
        }
        return new Dimension(w, 26);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.aa(g);
        Color c = color.get();
        int h = getHeight();
        g2.setColor(Theme.alpha(c, (int) (34 + 34 * hover.get())));
        g2.fill(Gfx.rr(0, 0, getWidth(), h, h / 2.0));
        double x = 10;
        if (dot) {
            g2.setColor(c);
            g2.fill(new Ellipse2D.Double(x, h / 2.0 - 3, 6, 6));
            x += 12;
        }
        if (icon != null) {
            Icons.paint(g2, icon, x - 1, h / 2.0 - 7, 14, c);
            x += 17;
        }
        g2.setFont(getFont());
        g2.setColor(c);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (float) x, (h - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }
}
