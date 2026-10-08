package dev.riserecode.ui.theme;

import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.animation.Animation;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Reference-derived palette and presentation preferences, independent of feature settings. */
public final class Theme {
    public static final int BACKGROUND = 0xFF191F27;
    public static final int PANEL = 0xFF151D20;
    public static final int SURFACE = 0xFF111820;
    public static final int HOVER = 0xFF18242C;
    public static final int TEXT = 0xFFF0F2F3;
    public static final int SECONDARY = 0xFF7F868E;
    public static final int MUTED = 0xFF525C65;
    public static final int TEAL = 0xFF59D5CB;
    public static final int BLUE = 0xFF3B82F6;
    public static final int FUCHSIA = 0xFFD946EF;
    public static final int BORDER = 0x08FFFFFF;
    public static final int SHADOW = 0x60000000;
    public static final int PANEL_RADIUS = 10;
    public static final int BUTTON_RADIUS = 5;
    public static final int PADDING = 8;
    private static int accent = TEAL;
    private static int previousAccent = TEAL;
    private static final Animation accentBlend = Animation.ui(1, 0.24, MathUtil.Easing.IN_OUT_CUBIC);
    private static long previousFrame;
    private static double gradientSeconds;
    private static boolean german;
    private static ThemeManager manager;
    public static void bind(ThemeManager value) { manager = value; }
    public static ThemeManager manager() { return manager; }
    public static double number(String name, double fallback) { return manager == null ? fallback : manager.number(name); }
    public static boolean bool(String name, boolean fallback) { return manager == null ? fallback : manager.bool(name); }
    public static double radius(double original) { return original * number("Corner Radius", 10) / 10; }
    public static int secondaryAccent() { return manager == null ? BLUE : manager.color("Secondary Accent"); }
    public static boolean hasGradient() { return manager != null && (manager.choice("Gradient Mode", dev.riserecode.util.ColorUtil.GradientMode.class) != dev.riserecode.util.ColorUtil.GradientMode.STATIC || manager.colorSetting("Primary Accent").getMode() != dev.riserecode.setting.ColorSetting.Mode.SOLID); }
    public static int accentAt(double amount) {
        if (hasGradient()) {
            return manager.getGradientColor(amount * manager.number("Gradient Spacing"), gradientSeconds);
        }
        return bool("Accent Gradient", true) ? lerp(accent(), secondaryAccent(), amount) : accent();
    }
    public static int gradientAt(double x, double y, double localAmount) {
        if (!hasGradient()) { return accentAt(localAmount); }
        double offset = manager.gradientDirection() == dev.riserecode.util.ColorUtil.GradientDirection.HORIZONTAL ? x : y;
        return manager.getGradientColor(offset, gradientSeconds);
    }
    public static dev.riserecode.setting.ColorSetting.Paint paint(String name) {
        return manager.colorSetting(name).paint();
    }
    /** Maps reference palette tokens without confusing user-selected ARGB settings. */
    public static dev.riserecode.setting.ColorSetting.Paint tokenPaint(int color) {
        if (manager == null) { return null; }
        String name = switch (color & 0xFFFFFF) {
            case BACKGROUND & 0xFFFFFF -> "Background Color";
            case PANEL & 0xFFFFFF -> "Sidebar Color";
            case SURFACE & 0xFFFFFF -> "Surface Color";
            case HOVER & 0xFFFFFF -> "Hover Color";
            case TEXT & 0xFFFFFF -> "Text Color";
            case SECONDARY & 0xFFFFFF -> "Secondary Text Color";
            case MUTED & 0xFFFFFF -> "Muted Text Color";
            case 0x34434F -> "HUD Bar Track Color";
            default -> null;
        };
        return name == null ? null : manager.colorSetting(name).paint().opacity((color >>> 24) / 255.0);
    }
    public static double gradientTime() { return gradientSeconds; }
    /** Resolve theme tokens before hover interpolation; never substitute user color endpoints. */
    public static int blendPalette(int from, int to, double progress) {
        var first = tokenPaint(from); var second = tokenPaint(to);
        return lerp(first == null ? from : first.sample(0, 0, gradientSeconds),
                second == null ? to : second.sample(0, 0, gradientSeconds), progress);
    }
    private static final Map<String, String> GERMAN = Map.ofEntries(
            Map.entry("Search", "Suche"), Map.entry("Combat", "Kampf"), Map.entry("Movement", "Bewegung"),
            Map.entry("Player", "Spieler"), Map.entry("Render", "Anzeige"), Map.entry("World", "Welt"),
            Map.entry("Utility", "Extras"), Map.entry("Profiles", "Profile"), Map.entry("Friends", "Freunde"),
            Map.entry("Appearance", "Design"), Map.entry("Language", "Sprache"), Map.entry("Save", "Speichern"),
            Map.entry("Load", "Laden"), Map.entry("Add", "Hinzufügen"), Map.entry("Remove", "Entfernen"),
            Map.entry("Start typing to search...", "Zum Suchen tippen..."), Map.entry("No matching modules", "Keine passenden Module"),
            Map.entry("No modules in this category", "Keine Module in dieser Kategorie"));
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_regular")));
    private static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_medium")));

    private Theme() { }
    public static int accent() {
        return hasGradient()
                ? manager.getGradientColor(0, gradientSeconds) : lerp(previousAccent, accent, accentBlend.getValue());
    }
    public static int accentTarget() { return accent; }
    public static void setAccent(int color) {
        int next = color;
        if (manager != null && manager.color("Primary Accent") != next) {
            ((dev.riserecode.setting.ColorSetting) manager.getSetting("Primary Accent")).setValue(next); manager.changed();
        }
        if (next == accent) { return; }
        previousAccent = accent(); accent = next; accentBlend.reset(0); accentBlend.animateTo(1);
    }
    /** HUD and screen share this clock, so two rendering callbacks never advance a transition twice. */
    public static void advance(long now) {
        double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9);
        previousFrame = now;
        gradientSeconds += Math.min(0.25, seconds) * number("Animation Speed", 1);
        if (manager != null && manager.color("Primary Accent") != accent) { setAccent(manager.color("Primary Accent")); }
        accentBlend.update(seconds);
    }
    public static boolean isGerman() { return german; }
    public static void setGerman(boolean value) { german = value; }
    public static String label(String value) { return german ? GERMAN.getOrDefault(value, value) : value; }

    /** Adds word spacing to identifiers without changing their persisted setting/module keys. */
    private static final Map<String,String> DISPLAY_NAMES = new java.util.LinkedHashMap<>() {
        private static final long serialVersionUID = 1L;
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> entry) { return size() > 512; }
    };
    public static String displayName(String value) { return DISPLAY_NAMES.computeIfAbsent(value, Theme::formatDisplayName); }
    private static String formatDisplayName(String value) {
        if (value.equals("FPS") || value.equals("HUD")) { return value; }
        String spaced = value.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        if (value.equals(value.toUpperCase(Locale.ROOT))) {
            StringBuilder title = new StringBuilder();
            for (String word : spaced.toLowerCase(Locale.ROOT).split(" ")) {
                if (!word.isEmpty()) {
                    if (!title.isEmpty()) { title.append(' '); }
                    title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
            }
            return title.toString();
        }
        return spaced;
    }
    public static Component text(String text, boolean title) {
        return Component.literal(text).withStyle(title ? TITLE : BODY);
    }
    public static int lerp(int from, int to, double progress) {
        double t = MathUtil.clamp01(progress);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            result |= (int) Math.round(MathUtil.lerp((from >>> shift) & 255, (to >>> shift) & 255, t)) << shift;
        }
        return result;
    }
    public static int opacity(int color, double opacity) {
        return (color & 0x00FFFFFF) | ((int) Math.round((color >>> 24) * MathUtil.clamp01(opacity)) << 24);
    }
}
