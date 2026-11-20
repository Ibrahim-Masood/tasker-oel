package com.taskflow.ui;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import javax.swing.Timer;

/**
 * A tiny animation engine built on {@link javax.swing.Timer}, so every animation runs safely on the
 * Swing event thread. Animations are time based (not frame based) and therefore take the same time
 * on slow and fast computers.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class Anim {

    /** Constant speed. */
    public static final DoubleUnaryOperator LINEAR = t -> t;

    /** Starts fast and slows down smoothly. */
    public static final DoubleUnaryOperator EASE_OUT = t -> 1 - Math.pow(1 - t, 3);

    /** Slow start, fast middle, slow end. */
    public static final DoubleUnaryOperator EASE_IN_OUT =
            t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;

    /** Overshoots the target slightly and settles back, which feels springy. */
    public static final DoubleUnaryOperator EASE_OUT_BACK = t -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    };

    private static final int FRAME_MS = 14;

    private Anim() {
    }

    /**
     * Runs an animation.
     *
     * @param durationMs how long it lasts, in milliseconds
     * @param easing     shapes the speed curve; maps 0..1 to 0..1
     * @param step       called on every frame with the eased progress
     * @param done       called once after the last frame; may be {@code null}
     * @return the running timer, which can be stopped to cancel the animation
     */
    public static Timer run(int durationMs, DoubleUnaryOperator easing, DoubleConsumer step, Runnable done) {
        long start = System.nanoTime();
        Timer timer = new Timer(FRAME_MS, null);
        timer.setCoalesce(true);
        timer.addActionListener(e -> {
            double raw = Math.min(1.0, (System.nanoTime() - start) / 1_000_000.0 / Math.max(1, durationMs));
            step.accept(easing.applyAsDouble(raw));
            if (raw >= 1.0) {
                timer.stop();
                if (done != null) {
                    done.run();
                }
            }
        });
        timer.start();
        return timer;
    }

    /**
     * Runs code once after a pause.
     *
     * @param delayMs the pause in milliseconds
     * @param action  the code to run
     * @return the timer, which can be stopped to cancel
     */
    public static Timer delay(int delayMs, Runnable action) {
        Timer timer = new Timer(Math.max(1, delayMs), e -> action.run());
        timer.setRepeats(false);
        timer.start();
        return timer;
    }

    /**
     * A number that glides smoothly to a new value, used for hover effects, counters and so on.
     */
    public static final class Value {
        private double value;
        private Timer timer;
        private final Runnable onChange;

        /**
         * Creates a value starting at 0.
         *
         * @param onChange called after every change, normally a {@code repaint}
         */
        public Value(Runnable onChange) {
            this(0, onChange);
        }

        /**
         * Creates a value.
         *
         * @param initial  the starting value
         * @param onChange called after every change
         */
        public Value(double initial, Runnable onChange) {
            this.value = initial;
            this.onChange = onChange;
        }

        /**
         * Returns the current (possibly mid-animation) value.
         *
         * @return the value
         */
        public double get() {
            return value;
        }

        /**
         * Sets the value immediately without animating.
         *
         * @param v the new value
         */
        public void jump(double v) {
            if (timer != null) {
                timer.stop();
            }
            value = v;
            onChange.run();
        }

        /**
         * Animates to a new value.
         *
         * @param target     where to go
         * @param durationMs how long to take, in milliseconds
         */
        public void to(double target, int durationMs) {
            if (timer != null) {
                timer.stop();
            }
            final double from = value;
            if (from == target) {
                return;
            }
            timer = run(durationMs, EASE_OUT, p -> {
                value = from + (target - from) * p;
                onChange.run();
            }, null);
        }
    }
}
