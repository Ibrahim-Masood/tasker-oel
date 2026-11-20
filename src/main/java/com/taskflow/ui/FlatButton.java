package com.taskflow.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

/**
 * A modern flat button with rounded corners, an optional vector icon and smooth hover / press
 * animations. It paints itself completely, so it looks the same on every operating system.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class FlatButton extends JButton {

    /** The visual style of a button. */
    public enum Variant {
        /** Solid blue, for the main action. */ PRIMARY,
        /** Outlined, for normal actions. */ SECONDARY,
        /** Red text, for destructive actions. */ DANGER,
        /** No background until hovered. */ GHOST,
        /** Like ghost, but turns red on hover. */ GHOST_DANGER
    }

    private final Variant variant;
    private Icons.Kind icon;
    private boolean compact;
    private final Anim.Value hover = new Anim.Value(this::repaint);
    private final Anim.Value press = new Anim.Value(this::repaint);

    /**
     * Creates a button.
     *
     * @param text    the label; may be empty for an icon-only button
     * @param icon    the icon shown before the label; may be {@code null}
     * @param variant the visual style
     */
    public FlatButton(String text, Icons.Kind icon, Variant variant) {
        super(text == null ? "" : text);
        this.icon = icon;
        this.variant = variant;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRequestFocusEnabled(false);
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.font(13.5f, Font.BOLD));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    hover.to(1, 140);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover.to(0, 220);
                press.to(0, 120);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    press.to(1, 70);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                press.to(0, 140);
            }
        });
    }

    /**
     * Changes the icon.
     *
     * @param kind the new icon, or {@code null} for none
     */
    public void setIconKind(Icons.Kind kind) {
        this.icon = kind;
        repaint();
    }

    /**
     * Makes an icon-only button slightly smaller (used inside task cards).
     *
     * @param compact {@code true} for the small size
     */
    public void setCompact(boolean compact) {
        this.compact = compact;
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        boolean hasText = !getText().isEmpty();
        if (!hasText) {
            int side = compact ? 34 : 38;
            return new Dimension(side, side);
        }
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText());
        if (icon != null) {
            w += 18 + 8;
        }
        return new Dimension(w + 36, 40);
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
        Theme.Palette p = Theme.p();
        double h = hover.get();
        Color fill;
        Color border = null;
        Color fg;
        switch (variant) {
            case PRIMARY -> {
                fill = Theme.mix(p.accent(), Color.WHITE, 0.14 * h);
                fg = p.onAccent();
            }
            case SECONDARY -> {
                fill = Theme.mix(p.surface(), p.surfaceAlt(), h);
                border = Theme.mix(p.border(), p.hint(), h * 0.7);
                fg = p.text();
            }
            case DANGER -> {
                fill = Theme.mix(p.surface(), p.danger(), 0.04 + 0.16 * h);
                border = Theme.mix(p.border(), p.danger(), 0.25 + 0.6 * h);
                fg = p.danger();
            }
            case GHOST_DANGER -> {
                fill = Theme.alpha(p.danger(), (int) (38 * h));
                fg = Theme.mix(p.muted(), p.danger(), h);
            }
            default -> {
                fill = Theme.alpha(p.surfaceAlt(), (int) (255 * h));
                fg = Theme.mix(p.muted(), p.text(), h);
            }
        }
        Graphics2D g2 = Gfx.aa(g);
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        }
        double dy = press.get();
        int w = getWidth();
        int ht = getHeight();
        double arc = compact || getText().isEmpty() ? 10 : 12;
        if (variant == Variant.PRIMARY) {
            g2.setPaint(new GradientPaint(0, 0, Theme.mix(fill, Color.WHITE, 0.10), 0, ht, fill));
        } else {
            g2.setColor(fill);
        }
        g2.fill(Gfx.rr(0, dy, w, ht - 1, arc));
        if (border != null) {
            g2.setColor(border);
            g2.draw(Gfx.rr(0.5, dy + 0.5, w - 1, ht - 2, arc));
        }
        FontMetrics fm = g2.getFontMetrics(getFont());
        boolean hasText = !getText().isEmpty();
        int textW = hasText ? fm.stringWidth(getText()) : 0;
        int iconSize = hasText ? 18 : (compact ? 18 : 20);
        int contentW = textW + (icon != null ? iconSize + (hasText ? 8 : 0) : 0);
        double x = (w - contentW) / 2.0;
        if (icon != null) {
            Icons.paint(g2, icon, x, dy + (ht - 1 - iconSize) / 2.0, iconSize, fg);
            x += iconSize + (hasText ? 8 : 0);
        }
        if (hasText) {
            g2.setFont(getFont());
            g2.setColor(fg);
            g2.drawString(getText(), (float) x, (float) (dy + (ht - 1 - fm.getHeight()) / 2.0 + fm.getAscent()));
        }
        g2.dispose();
    }
}
