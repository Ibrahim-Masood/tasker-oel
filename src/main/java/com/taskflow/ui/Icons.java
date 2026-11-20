package com.taskflow.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Vector icons drawn with Java2D. Drawing them in code means they stay sharp at any size and
 * any colour, and the application needs no image files or icon fonts.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class Icons {

    /** The available icons. */
    public enum Kind {
        /** A plus sign. */ PLUS,
        /** A pencil. */ EDIT,
        /** A bin. */ TRASH,
        /** A tick. */ CHECK,
        /** Two ticks. */ DOUBLE_CHECK,
        /** A curved arrow. */ UNDO,
        /** A magnifying glass. */ SEARCH,
        /** A sun. */ SUN,
        /** A crescent moon. */ MOON,
        /** A calendar page. */ CALENDAR,
        /** A clock face. */ CLOCK,
        /** A downward chevron. */ CHEVRON_DOWN,
        /** A cross. */ CLOSE,
        /** A clipboard. */ CLIPBOARD,
        /** A bullet list. */ LIST,
        /** A play triangle. */ PLAY,
        /** A warning triangle. */ ALERT
    }

    private Icons() {
    }

    /**
     * Draws an icon.
     *
     * @param g0    the graphics context
     * @param kind  which icon
     * @param x     left edge
     * @param y     top edge
     * @param size  width and height in pixels
     * @param color the colour
     */
    public static void paint(Graphics2D g0, Kind kind, double x, double y, double size, Color color) {
        Graphics2D g = Gfx.aa(g0);
        g.translate(x, y);
        double s = size / 24.0;
        g.scale(s, s);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case PLUS -> {
                line(g, 12, 5, 12, 19);
                line(g, 5, 12, 19, 12);
            }
            case CHECK -> poly(g, false, 5, 12.5, 10, 17, 19, 7);
            case DOUBLE_CHECK -> {
                poly(g, false, 2.5, 12.5, 7.5, 17, 16.5, 7);
                poly(g, false, 12, 15.5, 13.5, 17, 21.5, 7);
            }
            case EDIT -> {
                poly(g, true, 5, 19, 5.8, 15, 16.3, 4.6, 19.4, 7.7, 9, 18.2);
                line(g, 14, 7, 17, 10);
            }
            case TRASH -> {
                line(g, 4.5, 7, 19.5, 7);
                poly(g, false, 9, 7, 9, 4.5, 15, 4.5, 15, 7);
                poly(g, false, 6.5, 7, 7.5, 19.5, 16.5, 19.5, 17.5, 7);
                line(g, 10.5, 11, 10.5, 16);
                line(g, 13.5, 11, 13.5, 16);
            }
            case UNDO -> {
                poly(g, false, 9, 6.5, 4.5, 11, 9, 15.5);
                Path2D.Double path = new Path2D.Double();
                path.moveTo(4.5, 11);
                path.lineTo(14, 11);
                path.curveTo(18.5, 11, 20, 14, 20, 15.5);
                path.curveTo(20, 18, 18, 19.5, 15, 19.5);
                path.lineTo(10, 19.5);
                g.draw(path);
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(4.5, 4.5, 12, 12));
                line(g, 14.8, 14.8, 19.5, 19.5);
            }
            case SUN -> {
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    line(g, 12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7, 12 + Math.cos(a) * 9.6, 12 + Math.sin(a) * 9.6);
                }
            }
            case MOON -> {
                Area moon = new Area(new Ellipse2D.Double(4, 4, 16, 16));
                moon.subtract(new Area(new Ellipse2D.Double(9.5, 0.5, 14, 14)));
                g.fill(moon);
            }
            case CALENDAR -> {
                g.draw(Gfx.rr(4, 5.5, 16, 14.5, 3));
                line(g, 4, 10.5, 20, 10.5);
                line(g, 8, 3.5, 8, 7.5);
                line(g, 16, 3.5, 16, 7.5);
            }
            case CLOCK -> {
                g.draw(new Ellipse2D.Double(4, 4, 16, 16));
                poly(g, false, 12, 8, 12, 12, 15, 14);
            }
            case CHEVRON_DOWN -> poly(g, false, 7, 10, 12, 15, 17, 10);
            case CLOSE -> {
                line(g, 6.5, 6.5, 17.5, 17.5);
                line(g, 17.5, 6.5, 6.5, 17.5);
            }
            case CLIPBOARD -> {
                g.draw(Gfx.rr(5, 4.5, 14, 16.5, 3));
                g.draw(Gfx.rr(9, 2.5, 6, 4, 1.5));
                line(g, 8.5, 11.5, 15.5, 11.5);
                line(g, 8.5, 15.5, 13, 15.5);
            }
            case LIST -> {
                line(g, 9.5, 7, 20, 7);
                line(g, 9.5, 12, 20, 12);
                line(g, 9.5, 17, 20, 17);
                line(g, 4.5, 7, 5, 7);
                line(g, 4.5, 12, 5, 12);
                line(g, 4.5, 17, 5, 17);
            }
            case PLAY -> poly(g, true, 8, 5.5, 18.5, 12, 8, 18.5);
            case ALERT -> {
                poly(g, true, 12, 4, 21, 19.5, 3, 19.5);
                line(g, 12, 10, 12, 14);
                line(g, 12, 16.8, 12, 16.9);
            }
        }
        g.dispose();
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D g, boolean close, double... pts) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) {
            path.lineTo(pts[i], pts[i + 1]);
        }
        if (close) {
            path.closePath();
        }
        g.draw(path);
    }
}
