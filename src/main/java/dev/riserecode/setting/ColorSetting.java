package dev.riserecode.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.riserecode.util.ColorUtil;
import java.util.Locale;
import java.util.Objects;

/** An ARGB color and independent paint policy; the scalar value remains backwards compatible. */
public final class ColorSetting extends Setting<Integer> {
    public enum Mode { SOLID, GRADIENT, RAINBOW, ASTOLFO, SINE, THEME }
    @FunctionalInterface public interface ThemeSampler { int sample(double x, double y, double seconds); }
    private static ThemeSampler themeSampler = (x, y, seconds) -> 0xFF59D5CB;
    private final Mode defaultMode;
    private final int defaultSecondary;
    private Mode mode;
    private int secondary;
    private ColorUtil.GradientDirection direction = ColorUtil.GradientDirection.HORIZONTAL;
    private double speed = 0.18;
    private double spacing = 100;

    /** Immutable paint can cross the extraction boundary without retaining a mutable setting. */
    public record Paint(int primary, int secondary, Mode mode, ColorUtil.GradientDirection direction,
                        double speed, double spacing) {
        public Paint {
            Objects.requireNonNull(mode); Objects.requireNonNull(direction);
            if (!Double.isFinite(speed) || speed < 0 || speed > 3 || !Double.isFinite(spacing) || spacing < 1 || spacing > 1000) {
                throw new IllegalArgumentException("Invalid color gradient speed/spacing");
            }
        }
        public int sample(double x, double y, double seconds) {
            if (mode == Mode.SOLID) { return primary; }
            if (mode == Mode.THEME) { return multiplyAlpha(themeSampler.sample(x, y, seconds), primary >>> 24); }
            double offset = direction == ColorUtil.GradientDirection.HORIZONTAL ? x : y;
            double phase = offset / spacing + seconds * speed;
            phase -= Math.floor(phase);
            return switch (mode) {
                case GRADIENT -> mix(primary, secondary, 1 - Math.abs(phase * 2 - 1));
                case SINE -> mix(primary, secondary, 0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
                case RAINBOW -> (ColorUtil.hsv(phase, 0.65, 1) & 0xFFFFFF) | (primary & 0xFF000000);
                case ASTOLFO -> (ColorUtil.hsv(0.5 + 0.35 * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2)), 0.45, 1) & 0xFFFFFF) | (primary & 0xFF000000);
                default -> primary;
            };
        }
        public Paint opacity(double multiplier) {
            double clamped = Math.clamp(multiplier, 0, 1);
            int p = (primary & 0xFFFFFF) | ((int) Math.round((primary >>> 24) * clamped) << 24);
            int s = (secondary & 0xFFFFFF) | ((int) Math.round((secondary >>> 24) * clamped) << 24);
            return new Paint(p, s, mode, direction, speed, spacing);
        }
        public boolean animated() { return mode != Mode.SOLID; }
    }
    public ColorSetting(String name, String description, int defaultArgb) { this(name, description, defaultArgb, Mode.SOLID); }
    public ColorSetting(String name, String description, int defaultArgb, Mode defaultMode) {
        super(name, description, defaultArgb); this.defaultMode = Objects.requireNonNull(defaultMode);
        defaultSecondary = (defaultArgb & 0xFF000000) | 0xD946EF;
        mode = defaultMode; secondary = defaultSecondary;
    }
    public static void configureThemeSampler(ThemeSampler sampler) { themeSampler = Objects.requireNonNull(sampler); }
    public Mode getMode() { return mode; }
    public void setMode(Mode value) { mode = Objects.requireNonNull(value); }
    public int getSecondary() { return secondary; }
    public void setSecondary(int value) { secondary = value; }
    public ColorUtil.GradientDirection getDirection() { return direction; }
    public void setDirection(ColorUtil.GradientDirection value) { direction = Objects.requireNonNull(value); }
    public double getSpeed() { return speed; }
    public double getSpacing() { return spacing; }
    public void setSpeed(double value) { new Paint(getValue(), secondary, mode, direction, value, spacing); speed = value; }
    public void setSpacing(double value) { new Paint(getValue(), secondary, mode, direction, speed, value); spacing = value; }
    public Paint paint() { return new Paint(getValue(), secondary, mode, direction, speed, spacing); }
    public Paint defaultPaint() { return new Paint(getDefaultValue(), defaultSecondary, defaultMode, ColorUtil.GradientDirection.HORIZONTAL, 0.18, 100); }
    public int sample(double x, double y) { return paint().sample(x, y, ColorUtil.time()); }
    public void apply(Paint paint) {
        setValue(paint.primary()); secondary = paint.secondary(); mode = paint.mode(); direction = paint.direction(); speed = paint.speed(); spacing = paint.spacing();
    }
    @Override protected void onReset() { apply(defaultPaint()); }
    public JsonObject toJson() { return toJson(paint()); }
    public static JsonObject toJson(Paint paint) {
        JsonObject object = new JsonObject();
        object.addProperty("color", hex(paint.primary())); object.addProperty("secondary", hex(paint.secondary()));
        object.addProperty("mode", paint.mode().name()); object.addProperty("direction", paint.direction().name());
        object.addProperty("speed", paint.speed()); object.addProperty("spacing", paint.spacing()); return object;
    }
    /** Legacy signed ARGB integers and six/eight digit hex strings remain readable. */
    public Paint decode(JsonElement value) {
        if (value == null) { return defaultPaint(); }
        if (value.isJsonPrimitive()) { Paint defaults = defaultPaint(); return new Paint(decodeArgb(value), defaults.secondary(), defaults.mode(), defaults.direction(), defaults.speed(), defaults.spacing()); }
        if (!value.isJsonObject()) { throw new IllegalArgumentException("Expected color or color policy"); }
        JsonObject object = value.getAsJsonObject();
        return new Paint(decodeArgb(object.get("color")), decodeArgb(object.get("secondary")),
                Mode.valueOf(string(object, "mode")), ColorUtil.GradientDirection.valueOf(string(object, "direction")),
                number(object, "speed"), number(object, "spacing"));
    }
    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) { throw new IllegalArgumentException("Invalid color " + name); }
        return value.getAsString();
    }
    private static double number(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Invalid color " + name); }
        return value.getAsDouble();
    }
    private static int decodeArgb(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) { throw new IllegalArgumentException("Invalid ARGB color"); }
        if (value.getAsJsonPrimitive().isString()) { return parseHex(value.getAsString()); }
        if (!value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Invalid ARGB color"); }
        return value.getAsBigDecimal().intValueExact();
    }
    public static int parseHex(String text) {
        String value = text.strip().replaceFirst("^#", "");
        if (!value.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) { throw new IllegalArgumentException("Use RRGGBB or AARRGGBB"); }
        int result = Integer.parseUnsignedInt(value, 16); return value.length() == 6 ? result | 0xFF000000 : result;
    }
    public static String hex(int color) { return String.format(Locale.ROOT, "%08X", color); }
    private static int multiplyAlpha(int color, int alpha) { return (color & 0xFFFFFF) | (((color >>> 24) * alpha / 255) << 24); }
    private static int mix(int from, int to, double progress) {
        int result = 0; for (int shift = 0; shift <= 24; shift += 8) {
            result |= (int) Math.round(((from >>> shift) & 255) * (1 - progress) + ((to >>> shift) & 255) * progress) << shift;
        } return result;
    }
    public int getAlpha() { return getValue() >>> 24; }
    public int getRed() { return (getValue() >>> 16) & 255; }
    public int getGreen() { return (getValue() >>> 8) & 255; }
    public int getBlue() { return getValue() & 255; }
    public static int argb(int alpha, int red, int green, int blue) {
        for (int component : new int[] {alpha, red, green, blue}) {
            if (component < 0 || component > 255) { throw new IllegalArgumentException("Color components must be within [0, 255]"); }
        } return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
