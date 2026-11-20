package com.taskflow.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.Timer;

/**
 * Shows short "toast" messages at the bottom of the window. A toast slides up, fades in, waits and
 * fades out again. It can carry an action such as "UNDO". Only one toast is visible at a time.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class Toasts {

    /** The kind of message, which decides the icon colour. */
    public enum Kind {
        /** Something worked. */ SUCCESS,
        /** Something went wrong. */ ERROR,
        /** Neutral information. */ INFO
    }

    private final JLayeredPane layer;
    private Toast current;
    private Timer hold;

    /**
     * Creates a toast host for a window.
     *
     * @param layer the layered pane of the window; toasts float above everything in it
     */
    public Toasts(JLayeredPane layer) {
        this.layer = layer;
    }

    /**
     * Shows a message.
     *
     * @param kind        the kind of message
     * @param message     the text
     * @param actionLabel the label of an optional action button; {@code null} for none
     * @param action      the code the action button runs; may be {@code null}
     */
    public void show(Kind kind, String message, String actionLabel, Runnable action) {
        dismissNow();
        Toast toast = new Toast(kind, message, actionLabel, action);
        current = toast;
        Dimension size = toast.getPreferredSize();
        int x = (layer.getWidth() - size.width) / 2;
        int restY = layer.getHeight() - size.height - 30;
        toast.setBounds(x, restY + 26, size.width, size.height);
        layer.add(toast, JLayeredPane.POPUP_LAYER);
        Anim.run(260, Anim.EASE_OUT, t -> {
            toast.alpha = (float) t;
            toast.setLocation(x, (int) (restY + 26 * (1 - t)));
            toast.repaint();
        }, null);
        hold = Anim.delay(actionLabel == null ? 3200 : 5500, () -> fadeOut(toast));
    }

    private void fadeOut(Toast toast) {
        if (toast != current) {
            return;
        }
        Anim.run(240, Anim.EASE_OUT, t -> {
            toast.alpha = (float) (1 - t);
            toast.repaint();
        }, () -> {
            if (toast == current) {
                dismissNow();
            }
        });
    }

    private void dismissNow() {
        if (hold != null) {
            hold.stop();
        }
        if (current != null) {
            layer.remove(current);
            layer.repaint();
            current = null;
        }
    }

    /** The visual toast component. */
    private final class Toast extends JPanel {
        private final Kind kind;
        private float alpha;

        Toast(Kind kind, String message, String actionLabel, Runnable action) {
            super(new BorderLayout(12, 0));
            this.kind = kind;
            setOpaque(false);
            setBorder(new EmptyBorder(13, 46, 13, 18));
            JLabel text = new JLabel(message);
            text.setFont(Theme.font(14f, Font.PLAIN));
            text.setForeground(textColor());
            add(text, BorderLayout.CENTER);
            if (actionLabel != null) {
                JLabel act = new JLabel(actionLabel.toUpperCase());
                act.setFont(Theme.font(13f, Font.BOLD));
                act.setForeground(new Color(0x8DB3F5));
                act.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                act.setBorder(new EmptyBorder(0, 14, 0, 0));
                act.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        dismissNow();
                        if (action != null) {
                            action.run();
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        act.setForeground(Color.WHITE);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        act.setForeground(new Color(0x8DB3F5));
                    }
                });
                add(act, BorderLayout.EAST);
            }
        }

        private Color textColor() {
            return Theme.isDark() ? new Color(0xF3F4F6) : Color.WHITE;
        }

        private Color background() {
            return Theme.isDark() ? new Color(0x374151) : new Color(0x1F2937);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.min(d.width, Math.max(260, layer.getWidth() - 60)), d.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.aa(g);
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            Theme.Palette p = Theme.p();
            int h = getHeight();
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(Gfx.rr(2, 5, getWidth() - 4, h - 4, 14));
            g2.setColor(background());
            g2.fill(Gfx.rr(0, 0, getWidth(), h - 3, 14));
            Color c = switch (kind) {
                case SUCCESS -> new Color(0x4ADE80);
                case ERROR -> new Color(0xF87171);
                case INFO -> new Color(0x8DB3F5);
            };
            g2.setColor(Theme.alpha(c, 50));
            g2.fill(new java.awt.geom.Ellipse2D.Double(14, (h - 3 - 24) / 2.0, 24, 24));
            Icons.Kind icon = switch (kind) {
                case SUCCESS -> Icons.Kind.CHECK;
                case ERROR -> Icons.Kind.ALERT;
                case INFO -> Icons.Kind.CLIPBOARD;
            };
            Icons.paint(g2, icon, 18, (h - 3 - 16) / 2.0, 16, c);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }

        @Override
        public boolean contains(int x, int y) {
            return super.contains(x, y);
        }
    }

    /**
     * Tells whether a toast is currently on screen (used by tests).
     *
     * @return {@code true} if a toast is visible
     */
    public boolean isShowing() {
        return current != null;
    }

    /**
     * Keeps toasts inside the window after a resize.
     *
     * @param host the component whose size changed
     */
    public void reposition(JComponent host) {
        if (current != null) {
            Dimension size = current.getPreferredSize();
            current.setBounds((host.getWidth() - size.width) / 2, host.getHeight() - size.height - 30,
                    size.width, size.height);
        }
    }
}
