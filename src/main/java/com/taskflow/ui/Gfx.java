package com.taskflow.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Small drawing helpers shared by the custom components.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
final class Gfx {

    private Gfx() {
    }

    /**
     * Creates a copy of the graphics context with smooth shapes and text.
     *
     * @param g the original context
     * @return a new context that must be disposed by the caller
     */
    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    /**
     * Builds a rounded rectangle from a corner radius.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param r corner radius
     * @return the shape
     */
    static RoundRectangle2D rr(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }
}
