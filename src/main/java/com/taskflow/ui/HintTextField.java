package com.taskflow.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JTextField;

/**
 * A text field that shows grey placeholder text while it is empty.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class HintTextField extends JTextField {

    private final String hint;

    /**
     * Creates a field.
     *
     * @param hint the placeholder text shown while the field is empty
     */
    public HintTextField(String hint) {
        this.hint = hint;
        setFont(Theme.font(14f, Font.PLAIN));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && hint != null) {
            Graphics2D g2 = Gfx.aa(g);
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            g2.setColor(Theme.p().hint());
            g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
