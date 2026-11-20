package com.taskflow.ui;

import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Wraps a task card and adds enter / exit animations. When a card appears it fades in while the
 * space it needs grows from nothing; when a card is deleted it slides to the right, fades out and
 * the gap closes. The wrapped card is always laid out at its full size, so its content never
 * jumbles while the animation runs. The wrapper also provides the vertical gap between cards.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public class Reveal extends JPanel {

    /** Vertical gap between two cards, in pixels. */
    public static final int GAP = 6;

    private final JComponent content;
    private double factor = 1;
    private double alpha = 1;
    private int slide;
    private Timer running;

    /**
     * Wraps a component.
     *
     * @param content the card to show
     */
    public Reveal(JComponent content) {
        super(null);
        this.content = content;
        setOpaque(false);
        add(content);
    }

    /**
     * Returns the wrapped card.
     *
     * @return the card
     */
    public JComponent getContent() {
        return content;
    }

    /**
     * Plays the entrance animation.
     *
     * @param delayMs how long to wait before starting, so several cards can appear one after another
     */
    public void animateIn(int delayMs) {
        factor = 0;
        alpha = 0;
        slide = -18;
        Anim.delay(delayMs, () -> running = Anim.run(380, Anim.EASE_OUT, t -> {
            factor = t;
            alpha = t;
            slide = (int) (-18 * (1 - t));
            relayout();
        }, null));
    }

    /**
     * Plays the exit animation and then runs the given code (normally the actual deletion).
     *
     * @param done runs after the card has disappeared
     */
    public void animateOut(Runnable done) {
        if (running != null) {
            running.stop();
        }
        running = Anim.run(300, Anim.EASE_IN_OUT, t -> {
            factor = 1 - t;
            alpha = 1 - t;
            slide = (int) (70 * t);
            relayout();
        }, done);
    }

    private void relayout() {
        Container parent = getParent();
        revalidate();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = content.getPreferredSize();
        return new Dimension(d.width, (int) Math.round((d.height + GAP) * factor));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void doLayout() {
        Dimension d = content.getPreferredSize();
        content.setBounds(slide, 0, getWidth(), d.height);
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 0.999) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, alpha))));
        super.paint(g2);
        g2.dispose();
    }
}
