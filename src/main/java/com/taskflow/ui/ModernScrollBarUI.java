package com.taskflow.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A thin, rounded scroll bar without arrow buttons that follows the current theme.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    /** Creates the scroll bar look; install it with {@code scrollBar.setUI(new ModernScrollBarUI())}. */
    public ModernScrollBarUI() {
        super();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(12, 12);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // transparent track
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = Gfx.aa(g);
        Theme.Palette p = Theme.p();
        int alpha = isDragging ? 200 : (isThumbRollover() ? 150 : 95);
        g2.setColor(Theme.alpha(p.hint(), alpha));
        boolean vertical = scrollbar.getOrientation() == java.awt.Adjustable.VERTICAL;
        if (vertical) {
            g2.fill(Gfx.rr(r.x + 3, r.y + 2, r.width - 6, r.height - 4, (r.width - 6) / 2.0));
        } else {
            g2.fill(Gfx.rr(r.x + 2, r.y + 3, r.width - 4, r.height - 6, (r.height - 6) / 2.0));
        }
        g2.dispose();
    }
}
