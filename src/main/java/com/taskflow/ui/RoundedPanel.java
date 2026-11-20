package com.taskflow.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * A panel with rounded corners, a thin border and an optional soft shadow. The colours always
 * follow the current {@link Theme}.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class RoundedPanel extends JPanel {

    /** Space reserved on the left and right for the shadow. */
    public static final int SHADOW_X = 4;
    /** Space reserved above the body for the shadow. */
    public static final int SHADOW_TOP = 2;
    /** Space reserved below the body for the shadow. */
    public static final int SHADOW_BOTTOM = 7;

    private final int radius;
    private final boolean shadow;

    /**
     * Creates a rounded panel.
     *
     * @param layout the layout manager of the content
     * @param radius the corner radius in pixels
     * @param shadow whether to draw a soft shadow (the panel then needs the shadow insets as border)
     */
    public RoundedPanel(LayoutManager layout, int radius, boolean shadow) {
        super(layout);
        this.radius = radius;
        this.shadow = shadow;
        setOpaque(false);
    }

    /**
     * How strongly the panel is highlighted, from 0 to 1. Subclasses override this to animate the
     * shadow and border on hover.
     *
     * @return the highlight amount
     */
    protected double highlight() {
        return 0;
    }

    /**
     * The colour of the panel body.
     *
     * @return the fill colour
     */
    protected Color fillColor() {
        return Theme.p().surface();
    }

    /**
     * The colour of the thin outline.
     *
     * @return the border colour
     */
    protected Color borderColor() {
        return Theme.mix(Theme.p().border(), Theme.p().accent(), highlight() * 0.8);
    }

    /**
     * Returns the rectangle of the visible body (without shadow space).
     *
     * @return the body bounds
     */
    protected RoundRectangle2D body() {
        if (shadow) {
            return Gfx.rr(SHADOW_X, SHADOW_TOP, getWidth() - SHADOW_X * 2, getHeight() - SHADOW_TOP - SHADOW_BOTTOM,
                    radius);
        }
        return Gfx.rr(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.aa(g);
        RoundRectangle2D b = body();
        if (shadow) {
            double k = highlight();
            int layers = 7;
            int base = Theme.isDark() ? 0 : 7;
            for (int i = 1; i <= layers; i++) {
                int a = (int) (base * (1 - i / (double) (layers + 1)) * (0.7 + 0.9 * k));
                if (a <= 0) {
                    continue;
                }
                g2.setColor(new Color(15, 23, 42, a));
                g2.fill(new RoundRectangle2D.Double(b.getX() - i * 0.6, b.getY() + 1 + i * 0.5 - i * 0.6,
                        b.getWidth() + i * 1.2, b.getHeight() + i * 1.2, b.getArcWidth() + i, b.getArcHeight() + i));
            }
        }
        g2.setColor(fillColor());
        g2.fill(b);
        g2.setColor(borderColor());
        g2.draw(new RoundRectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getArcWidth(),
                b.getArcHeight()));
        g2.dispose();
        super.paintComponent(g);
    }
}
