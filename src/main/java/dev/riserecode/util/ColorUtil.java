package dev.riserecode.util;

import dev.riserecode.ui.theme.Theme;

/** Original time-continuous gradients; offsets are logical UI pixels. */
public final class ColorUtil {
    public enum GradientMode { STATIC, RAINBOW, ASTOLFO, SINE }
    public enum GradientDirection { HORIZONTAL, VERTICAL }
    private ColorUtil() { }
    public static double time() { return Theme.gradientTime(); }
    public static int gradient(GradientMode mode, int primary, int secondary, double offset, double seconds, double speed, double spacing) {
        double phase = offset / Math.max(1, spacing) + seconds * speed;
        phase -= Math.floor(phase);
        return switch (mode) {
            case STATIC -> primary;
            case RAINBOW -> hsv(phase - Math.floor(phase), 0.65, 1);
            case ASTOLFO -> hsv(0.5 + 0.35 * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2)), 0.45, 1);
            case SINE -> Theme.lerp(primary, secondary, 0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
        };
    }
    public static int hsv(double hue, double saturation, double value) {
        double h = (hue - Math.floor(hue)) * 6, c = value * saturation, x = c * (1 - Math.abs(h % 2 - 1)), m = value - c;
        double r = 0, g = 0, b = 0;
        switch ((int) h) {
            case 0 -> { r = c; g = x; } case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; } case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; } default -> { r = c; b = x; }
        }
        return 0xFF000000 | (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }
}
