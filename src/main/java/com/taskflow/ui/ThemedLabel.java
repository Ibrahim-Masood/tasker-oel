package com.taskflow.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;

import javax.swing.JLabel;

/**
 * A label whose colour is looked up from the theme every time it paints, so it changes instantly
 * when the user switches between light and dark mode. Long text is shortened with "..." instead
 * of widening the layout, and the text can optionally be struck through.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class ThemedLabel extends JLabel {

    private final Supplier<Color> color;
    private boolean strike;

    /**
     * Creates a label.
     *
     * @param text  the text
     * @param color supplies the current text colour
     * @param font  the font
     */
    public ThemedLabel(String text, Supplier<Color> color, Font font) {
        super(text);
        this.color = color;
        setFont(font);
        setOpaque(false);
    }

    /**
     * Turns the strike-through line on or off (used for completed tasks).
     *
     * @param strike {@code true} to draw a line through the text
     */
    public void setStrike(boolean strike) {
        this.strike = strike;
        repaint();
    }

    @Override
    public Color getForeground() {
        return color == null ? super.getForeground() : color.get();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, super.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.aa(g);
        super.paintComponent(g2);
        if (strike && getText() != null && !getText().isEmpty()) {
            FontMetrics fm = g2.getFontMetrics(getFont());
            int w = Math.min(fm.stringWidth(getText()), getWidth());
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - fm.getAscent() / 3;
            g2.setColor(getForeground());
            g2.fillRect(0, y, w, 2);
        }
        g2.dispose();
    }
}
