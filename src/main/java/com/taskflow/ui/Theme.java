package com.taskflow.ui;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Central place for colours and fonts, with a light and a dark mode.
 * <p>
 * Both palettes are built from the five colours chosen for the project:
 * white {@code #ffffff}, light grey {@code #e5e7eb}, navy {@code #1f2937}, grey {@code #9ca3af}
 * and blue {@code #346ec9}. Components never store colours; they ask {@link #p()} every time they
 * paint, so switching the mode only needs a repaint.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
public final class Theme {

    /** The two colour modes. */
    public enum Mode {
        /** Light mode. */
        LIGHT,
        /** Dark mode. */
        DARK
    }

    /**
     * One complete set of colours.
     *
     * @param bg         window background
     * @param surface    cards and dialogs
     * @param surfaceAlt inputs and hover fills
     * @param border     thin outlines
     * @param text       main text
     * @param muted      secondary text
     * @param hint       placeholders and idle icons
     * @param accent     brand blue used for fills
     * @param accentText brand blue used for text (lighter in dark mode)
     * @param onAccent   text drawn on top of the accent colour
     * @param danger     errors and high priority
     * @param success    completed tasks
     * @param warning    medium priority and "due today"
     * @param info       low priority
     */
    public record Palette(Color bg, Color surface, Color surfaceAlt, Color border, Color text, Color muted,
                          Color hint, Color accent, Color accentText, Color onAccent, Color danger,
                          Color success, Color warning, Color info) { }

    private static final Palette LIGHT = new Palette(
            rgb(0xF2F4F7), rgb(0xFFFFFF), rgb(0xF3F4F6), rgb(0xE5E7EB), rgb(0x1F2937), rgb(0x6B7280),
            rgb(0x9CA3AF), rgb(0x346EC9), rgb(0x346EC9), rgb(0xFFFFFF), rgb(0xDC2626), rgb(0x16A34A),
            rgb(0xD97706), rgb(0x0891B2));

    private static final Palette DARK = new Palette(
            rgb(0x111827), rgb(0x1F2937), rgb(0x293548), rgb(0x374151), rgb(0xE5E7EB), rgb(0x9CA3AF),
            rgb(0x6B7280), rgb(0x346EC9), rgb(0x6C9BEA), rgb(0xFFFFFF), rgb(0xF87171), rgb(0x4ADE80),
            rgb(0xFBBF24), rgb(0x22D3EE));

    private static final String FAMILY = pickFamily();
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Mode mode = loadMode();

    private Theme() {
    }

    /**
     * Returns the colours of the current mode.
     *
     * @return the active palette
     */
    public static Palette p() {
        return mode == Mode.DARK ? DARK : LIGHT;
    }

    /**
     * Tells whether dark mode is active.
     *
     * @return {@code true} in dark mode
     */
    public static boolean isDark() {
        return mode == Mode.DARK;
    }

    /**
     * Switches between light and dark mode, remembers the choice and notifies listeners.
     */
    public static void toggle() {
        setMode(mode == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    /**
     * Sets the colour mode.
     *
     * @param newMode the mode to use
     */
    public static void setMode(Mode newMode) {
        mode = newMode;
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean("dark", newMode == Mode.DARK);
        } catch (RuntimeException ignored) {
            // preferences are optional
        }
        for (Runnable r : new ArrayList<>(LISTENERS)) {
            r.run();
        }
    }

    /**
     * Registers code to run when the mode changes.
     *
     * @param listener the code to run
     */
    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    /**
     * Creates a font in the application's font family.
     *
     * @param size  the size in points
     * @param style {@link Font#PLAIN}, {@link Font#BOLD} ...
     * @return the font
     */
    public static Font font(float size, int style) {
        return new Font(FAMILY, style, 12).deriveFont(style, size);
    }

    /**
     * Blends two colours.
     *
     * @param a the colour for {@code t = 0}
     * @param b the colour for {@code t = 1}
     * @param t how far towards {@code b}, from 0 to 1
     * @return the mixed colour
     */
    public static Color mix(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    /**
     * Returns the same colour with a different opacity.
     *
     * @param c     the colour
     * @param alpha opacity from 0 (clear) to 255 (solid)
     * @return the translucent colour
     */
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /**
     * Colour that represents a status.
     *
     * @param status the status
     * @return grey for pending, blue for in progress, green for completed
     */
    public static Color statusColor(TaskStatus status) {
        Palette p = p();
        return switch (status) {
            case PENDING -> p.muted();
            case IN_PROGRESS -> p.accentText();
            case COMPLETED -> p.success();
        };
    }

    /**
     * Colour that represents a priority.
     *
     * @param priority the priority
     * @return red for high, amber for medium, teal for low
     */
    public static Color priorityColor(Priority priority) {
        Palette p = p();
        return switch (priority) {
            case HIGH -> p.danger();
            case MEDIUM -> p.warning();
            case LOW -> p.info();
        };
    }

    private static Color rgb(int hex) {
        return new Color(hex);
    }

    private static Mode loadMode() {
        try {
            return Preferences.userNodeForPackage(Theme.class).getBoolean("dark", false) ? Mode.DARK : Mode.LIGHT;
        } catch (RuntimeException e) {
            return Mode.LIGHT;
        }
    }

    private static String pickFamily() {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String name : new String[] {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans"}) {
            if (installed.contains(name)) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }
}
