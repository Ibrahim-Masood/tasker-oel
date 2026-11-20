package com.taskflow.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/**
 * A rounded, themed frame around a text component. The border glows blue while the field has
 * focus and turns red when {@link #setError(boolean)} is used. An optional icon is drawn at the left.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class FieldShell extends JPanel {

    private final JComponent focusTarget;
    private final Icons.Kind leading;
    private boolean error;
    private final Anim.Value focus = new Anim.Value(this::repaint);

    /**
     * Wraps a component.
     *
     * @param content     the component to show inside (a text field, or a scroll pane with a text area)
     * @param focusTarget the component whose focus drives the glow; also themed for text colours
     * @param leading     an icon for the left side, or {@code null}
     */
    public FieldShell(JComponent content, JComponent focusTarget, Icons.Kind leading) {
        super(new BorderLayout());
        this.focusTarget = focusTarget;
        this.leading = leading;
        setOpaque(false);
        content.setOpaque(false);
        content.setBorder(null);
        setBorder(new EmptyBorder(8, leading != null ? 42 : 14, 8, 12));
        add(content, BorderLayout.CENTER);
        focusTarget.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focus.to(1, 160);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focus.to(0, 220);
            }
        });
    }

    /**
     * Marks the field as invalid (red border) or valid.
     *
     * @param error {@code true} to show the error state
     */
    public void setError(boolean error) {
        this.error = error;
        repaint();
    }

    private void applyTextColors() {
        if (focusTarget instanceof JTextComponent tc) {
            Theme.Palette p = Theme.p();
            if (!p.text().equals(tc.getForeground())) {
                tc.setForeground(p.text());
            }
            if (!p.text().equals(tc.getCaretColor())) {
                tc.setCaretColor(p.text());
            }
            Color sel = Theme.alpha(p.accent(), 90);
            if (!sel.equals(tc.getSelectionColor())) {
                tc.setSelectionColor(sel);
            }
            if (!p.text().equals(tc.getSelectedTextColor())) {
                tc.setSelectedTextColor(p.text());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        applyTextColors();
        Theme.Palette p = Theme.p();
        double f = focus.get();
        Graphics2D g2 = Gfx.aa(g);
        int w = getWidth();
        int h = getHeight();
        Color accent = error ? p.danger() : p.accent();
        if (f > 0.01 || error) {
            g2.setColor(Theme.alpha(accent, (int) ((error ? 45 : 60) * Math.max(f, error ? 1 : 0))));
            g2.setStroke(new java.awt.BasicStroke(4f));
            g2.draw(Gfx.rr(1.5, 1.5, w - 3, h - 3, 11));
        }
        g2.setColor(Theme.mix(p.surfaceAlt(), p.surface(), f * 0.6));
        g2.fill(Gfx.rr(1, 1, w - 2, h - 2, 11));
        g2.setStroke(new java.awt.BasicStroke(1.2f));
        g2.setColor(error ? p.danger() : Theme.mix(p.border(), p.accent(), f));
        g2.draw(Gfx.rr(1, 1, w - 2, h - 2, 11));
        if (leading != null) {
            Icons.paint(g2, leading, 14, (h - 18) / 2.0, 18, Theme.mix(p.hint(), p.accentText(), f));
        }
        g2.dispose();
    }
}
