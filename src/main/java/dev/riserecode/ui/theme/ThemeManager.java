package dev.riserecode.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.ColorUtil;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;

/** Visual settings are independent of module configs and module profiles. Client-thread only. */
public final class ThemeManager implements AutoCloseable {
    public enum Preset { CLASSIC_RISECODE, OCEAN, AURORA, AMETHYST, ROSE, EMBER, MONOCHROME }
    public enum LogoPlacement { LEFT, RIGHT, ABOVE }
    public enum Channel { OPENING, NAVIGATION, HOVER, PRESS, TOGGLES, EXPANSION, SCROLLING, DRAGGING, HUD_ENTRANCE, HUD_EXIT, HUD_REORDERING }
    private JsonObject presetUndo;
    public enum Typeface { GOOGLE_SANS, QRAFTYS, MINECRAFTIA, MONTSERRAT, INTER }
    public enum HudBackground { NONE, SOLID_BOX, BLUR_BOX }
    public record Control(String group, Setting<?> setting, double step) { }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    private final Logger logger;
    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
    private final List<Control> controls = new ArrayList<>();
    private EventBus.Subscription subscription;
    private long dirtySince;
    private long retryAfter;
    private long revision;
    private String activeProfile = "default";
    private boolean loaded;

    public ThemeManager(Path directory, Logger logger) {
        this.directory = directory; this.logger = logger;
        add("Presets", new EnumSetting<>("Appearance Preset", "Apply a palette without changing your layout", Preset.CLASSIC_RISECODE));
        add("Branding", new dev.riserecode.setting.StringSetting("Display Name", "Interface name", "risecode", 64));
        add("Branding", new dev.riserecode.setting.StringSetting("Interface Tagline", "Title screen tagline", "make it yours", 96));
        add("Branding", new dev.riserecode.setting.StringSetting("Watermark Text", "Independent HUD text", "risecode", 96));
        add("Branding", new dev.riserecode.setting.StringSetting("Logo File", "PNG path; use Import Logo", "", 512));
        bool("Branding", "Logo Visible", true);
        number("Branding", "Logo Size", 16, 4, 96, 1);
        add("Branding", new EnumSetting<>("Logo Placement", "Logo beside or above text", LogoPlacement.LEFT));
        add("Branding", new ColorSetting("Logo Tint", "Logo color and opacity", 0xFFFFFFFF));
        add("Typography", new EnumSetting<>("Font Selection", "Typeface shared by menu and HUD", Typeface.GOOGLE_SANS));
        number("Typography", "Font Scale", 1, 0.75, 2, 0.01);
        bool("Typography", "Text Shadow", true);
        number("Typography", "Shadow Offset X", 0.5, -4, 4, 0.05);
        number("Typography", "Shadow Offset Y", 0.7, -4, 4, 0.05);
        number("Typography", "Shadow Blur", 0.65, 0, 8, 0.05);
        number("Typography", "Shadow Opacity", 0.6, 0, 1, 0.01);
        bool("Colors & Glow", "Global Glow", true);
        number("Colors & Glow", "Glow Radius", 5, 0, 20, 0.1);
        number("Colors & Glow", "Glow Intensity", 0.2, 0, 1, 0.01);
        bool("Colors & Glow", "Accent Gradient", true);
        add("Colors & Glow", new EnumSetting<>("Gradient Mode", "Animated accent style", ColorUtil.GradientMode.STATIC));
        number("Colors & Glow", "Gradient Speed", 0.18, 0, 3, 0.01);
        add("Colors & Glow", new EnumSetting<>("Gradient Direction", "Spatial gradient axis", ColorUtil.GradientDirection.HORIZONTAL));
        number("Colors & Glow", "Gradient Spacing", 100, 10, 400, 1);
        add("Colors & Glow", new ColorSetting("Primary Accent", "Main active color", Theme.TEAL));
        add("Colors & Glow", new ColorSetting("Secondary Accent", "Gradient endpoint", 0xFF7C9FF5));
        add("Interface Colors", new ColorSetting("Background Color", "Main menu background", Theme.BACKGROUND));
        add("Interface Colors", new ColorSetting("Sidebar Color", "Navigation background", Theme.PANEL));
        add("Interface Colors", new ColorSetting("Surface Color", "Cards and control backgrounds", Theme.SURFACE));
        add("Interface Colors", new ColorSetting("Hover Color", "Hovered cards and controls", Theme.HOVER));
        add("Interface Colors", new ColorSetting("Selected Tab Color", "Selected navigation label and icon", 0xFFFFFFFF));
        add("Interface Colors", new ColorSetting("Text Color", "Normal interface text", Theme.TEXT));
        add("Interface Colors", new ColorSetting("Secondary Text Color", "Descriptions and secondary labels", Theme.SECONDARY));
        add("Interface Colors", new ColorSetting("Muted Text Color", "Placeholder labels", Theme.MUTED));
        add("Interface Colors", new ColorSetting("Border Color", "Panel inner borders", 0xFFFFFFFF));
        add("Interface Colors", new ColorSetting("Shadow Color", "Panel drop shadows", Theme.SHADOW));
        add("Typography", new ColorSetting("Text Shadow Color", "Text shadow tint", 0xFF000000));
        add("HUD", new ColorSetting("Watermark Color", "Logo color or inherited accent", Theme.TEAL, ColorSetting.Mode.THEME));
        add("HUD", new ColorSetting("ArrayList Color", "Module names or inherited accent", Theme.TEAL, ColorSetting.Mode.THEME));
        add("HUD", new ColorSetting("HUD Background Color", "HUD panel tint", Theme.SURFACE));
        add("HUD", new ColorSetting("HUD Bar Track Color", "HUD bar backing", 0xFF34434F));
        number("Geometry & Layout", "Corner Radius", 10, 0, 30, 1);
        number("Geometry & Layout", "Panel Opacity", 0.87, 0, 1, 0.01);
        number("Geometry & Layout", "Border Thickness", 0.5, 0, 3, 0.05);
        number("Geometry & Layout", "Border Opacity", 0.08, 0, 1, 0.01);
        number("Geometry & Layout", "Content Padding", 8, 4, 20, 1);
        number("Geometry & Layout", "Interface Scale", 1, 0.75, 1.3, 0.01);
        number("Animations", "Animation Speed", 1, 0.5, 3, 0.05);
        add("Animations", new EnumSetting<>("Easing Curve", "Fallback curve for transitions without an independent channel", MathUtil.Easing.OUT_QUART));
        bool("Animations", "Reduced Motion", false);
        for (Channel channel : Channel.values()) {
            String label = channelLabel(channel);
            number("Animation: " + label, label + " Duration", channel == Channel.HUD_ENTRANCE ? 0.32 : 0.2, 0, 2, 0.01);
            add("Animation: " + label, new EnumSetting<>(label + " Easing", "Independent animation curve", MathUtil.Easing.OUT_QUART));
            if (channel == Channel.HOVER || channel == Channel.NAVIGATION || channel == Channel.HUD_ENTRANCE || channel == Channel.HUD_EXIT) {
                number("Animation: " + label, label + " Movement", 1, 0, 2, 0.01);
            }
            if (channel == Channel.OPENING || channel == Channel.PRESS) { number("Animation: " + label, label + " Scale", 1, 0, 2, 0.01); }
        }
        number("Background Effects", "GUI Blur Radius", 12, 0, 30, 1);
        number("Background Effects", "GUI Blur Opacity", 0.8, 0, 1, 0.01);
        bool("Background Effects", "World Blur", false);
        number("Background Effects", "World Blur Radius", 8, 0, 30, 1);
        bool("HUD", "HUD Visible", true);
        bool("HUD", "HUD Border", false);
        for (String family : List.of("Start Screen", "Multiplayer", "Options", "Loading")) {
            bool("Menu Screens", family + " Theme", true);
            bool("Menu Screens", family + " Background", true);
        }
        bool("Menu Screens", "Uncapped Themed Menus", true);
        number("Menu Screens", "Background Darkness", 0.42, 0, 0.85, 0.01);
        add("HUD", new EnumSetting<>("HUD Background", "Backdrop behind watermark and enabled modules", HudBackground.SOLID_BOX));
        number("HUD", "ArrayList Spacing", 0, 0, 12, 0.25);
        number("HUD", "ArrayList Padding", 2, 0, 12, 0.25);
    }
    private void add(String group, Setting<?> setting) { add(group, setting, 0); }
    private void add(String group, Setting<?> setting, double step) {
        settings.put(setting.getName(), setting); controls.add(new Control(group, setting, step));
    }
    private void number(String group, String name, double value, double min, double max, double step) {
        add(group, new DoubleSetting(name, name, value, min, max), step);
    }
    private void bool(String group, String name, boolean value) { add(group, new BooleanSetting(name, name, value)); }
    public String text(String name) { return ((dev.riserecode.setting.StringSetting) getSetting(name)).getValue(); }
    public static String channelLabel(Channel channel) { return channel.name().startsWith("HUD_") ? "HUD " + Theme.displayName(channel.name().substring(4)) : Theme.displayName(channel.name()); }
    public double channelNumber(Channel channel, String suffix) { return number(channelLabel(channel) + " " + suffix); }
    public MathUtil.Easing channelCurve(Channel channel) { return choice(channelLabel(channel) + " Easing", MathUtil.Easing.class); }
    public static int[] palette(Preset preset) { return switch (preset) {
        case CLASSIC_RISECODE -> new int[] {Theme.TEAL, 0xFF7C9FF5, Theme.BACKGROUND};
        case OCEAN -> new int[] {0xFF38C9EA, 0xFF3864D9, 0xFF111D2B};
        case AURORA -> new int[] {0xFF65E7B4, 0xFFA77BEF, 0xFF171B2D};
        case AMETHYST -> new int[] {0xFFB895ED, 0xFF7D68D6, 0xFF211B2D};
        case ROSE -> new int[] {0xFFF49FBA, 0xFFD96499, 0xFF2A1B25};
        case EMBER -> new int[] {0xFFF3AB57, 0xFFED6861, 0xFF2A211C};
        case MONOCHROME -> new int[] {0xFFE4E7EC, 0xFF9BA4B3, 0xFF1E2126};
    }; }
    public void applyPreset(Preset preset) {
        JsonObject values = new JsonObject(); int[] colors = palette(preset);
        for (String key : List.of("Primary Accent", "Secondary Accent", "Background Color", "Sidebar Color", "Surface Color", "Hover Color")) {
            int color = switch (key) { case "Primary Accent" -> colors[0]; case "Secondary Accent" -> colors[1]; case "Background Color" -> colors[2]; case "Sidebar Color" -> Theme.lerp(colors[2], 0xFF000000, 0.18); case "Surface Color" -> Theme.lerp(colors[2], 0xFF000000, 0.32); default -> Theme.lerp(colors[2], colors[0], 0.12); };
            values.add(key, ColorSetting.toJson(new ColorSetting.Paint(color, colors[1], ColorSetting.Mode.SOLID, ColorUtil.GradientDirection.HORIZONTAL, 0.18, 100)));
        }
        values.addProperty("Accent Gradient", true); values.addProperty("Global Glow", true);
        values.addProperty("Gradient Mode", preset == Preset.AURORA ? "SINE" : "STATIC");
        values.addProperty("Glow Radius", preset == Preset.MONOCHROME ? 3 : 5);
        values.addProperty("Glow Intensity", preset == Preset.MONOCHROME ? 0.12 : 0.2);
        values.addProperty("Background Darkness", preset == Preset.EMBER ? 0.48 : 0.42);
        values.addProperty("Appearance Preset", preset.name());
        presetUndo = new JsonObject(); JsonObject before = snapshot().settings();
        values.keySet().forEach(key -> presetUndo.add(key, before.get(key).deepCopy()));
        values.entrySet().forEach(entry -> assign(getSetting(entry.getKey()), decode(getSetting(entry.getKey()), entry.getValue()))); changed();
    }
    public boolean undoPreset() {
        if (presetUndo == null) { return false; }
        presetUndo.entrySet().forEach(entry -> assign(getSetting(entry.getKey()), decode(getSetting(entry.getKey()), entry.getValue())));
        presetUndo = null; changed(); return true;
    }
    public void importLogo() throws IOException {
        String name = dev.riserecode.ui.theme.BrandingLogo.importPng(directory, Path.of(text("Logo File")));
        ((dev.riserecode.setting.StringSetting) getSetting("Logo File")).setValue(name); changed();
    }
    public Path getDirectory() { return directory; }
    public Path getFontDirectory() { return directory.resolve("fonts"); }
    public List<Control> getControls() { return List.copyOf(controls); }
    public Setting<?> getSetting(String name) {
        Setting<?> result = settings.get(name);
        if (result == null) { throw new IllegalArgumentException("Unknown visual setting " + name); }
        return result;
    }
    public double number(String name) { return ((DoubleSetting) getSetting(name)).getValue(); }
    public boolean bool(String name) { return ((BooleanSetting) getSetting(name)).getValue(); }
    public ColorSetting colorSetting(String name) { return (ColorSetting) getSetting(name); }
    public int color(String name) { return ((ColorSetting) getSetting(name)).getValue(); }
    public <E extends Enum<E>> E choice(String name, Class<E> type) { return type.cast(getSetting(name).getValue()); }
    public long getRevision() { return revision; }
    public String getActiveProfile() { return activeProfile; }
    public boolean wasLoaded() { return loaded; }
    public void changed() { revision++; dirtySince = System.nanoTime(); retryAfter = 0; }
    public void bind(EventBus bus) {
        if (subscription != null) { return; }
        Theme.bind(this);
        ColorSetting.configureThemeSampler((x, y, time) -> getGradientColor(gradientDirection() == ColorUtil.GradientDirection.HORIZONTAL ? x : y, time));
        Animation.configureChannels(this);
        Animation.configureUi(() -> number("Animation Speed"), () -> choice("Easing Curve", MathUtil.Easing.class));
        subscription = bus.subscribe(UpdateEvent.class, ignored -> {
            long now = System.nanoTime();
            if (dirtySince != 0 && now >= retryAfter && now - dirtySince >= 400_000_000L) { save(); }
        });
    }
    public int getGradientColor(double offset, double currentTime) {
        ColorSetting primary = colorSetting("Primary Accent");
        if (choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.STATIC && primary.getMode() != ColorSetting.Mode.SOLID && primary.getMode() != ColorSetting.Mode.THEME) {
            return primary.paint().sample(offset, offset, currentTime);
        }
        return ColorUtil.gradient(choice("Gradient Mode", ColorUtil.GradientMode.class), color("Primary Accent"),
                color("Secondary Accent"), offset, currentTime, number("Gradient Speed"), number("Gradient Spacing"));
    }
    public ColorUtil.GradientDirection gradientDirection() {
        var primary = colorSetting("Primary Accent");
        return choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.STATIC
                && primary.getMode() != ColorSetting.Mode.SOLID && primary.getMode() != ColorSetting.Mode.THEME
                ? primary.getDirection() : choice("Gradient Direction", ColorUtil.GradientDirection.class);
    }
    private static final List<String> POLICY = List.of("Gradient Mode", "Gradient Speed", "Gradient Direction", "Gradient Spacing", "Animation Speed", "Easing Curve");
    private List<String> policyKeys() {
        return settings.keySet().stream().filter(key -> POLICY.contains(key) || key.equals("Reduced Motion") || java.util.Arrays.stream(Channel.values()).anyMatch(c -> key.startsWith(channelLabel(c) + " "))).toList();
    }
    public JsonObject policySnapshot() {
        JsonObject result = new JsonObject(); policyKeys().forEach(name -> result.add(name, GSON.toJsonTree(getSetting(name).getValue()))); return result;
    }
    public void validatePolicy(JsonObject values) { policyKeys().forEach(name -> { if (values.has(name)) { decode(getSetting(name), values.get(name)); } }); }
    public void applyPolicy(JsonObject values) {
        validatePolicy(values); policyKeys().forEach(name -> {
            JsonElement value=values.get(name);
            if (value==null && name.endsWith(" Easing") && values.has("Easing Curve")) { value=values.get("Easing Curve"); }
            if (value!=null) { assign(getSetting(name),decode(getSetting(name),value)); }
        }); changed();
    }
    public ThemeProfile snapshot() {
        JsonObject values = new JsonObject();
        settings.forEach((name, setting) -> values.add(name, setting instanceof ColorSetting color ? color.toJson() : GSON.toJsonTree(setting.getValue())));
        return new ThemeProfile(ThemeProfile.VERSION, values);
    }
    /** Validate the complete document before applying it, so a bad file cannot half-load a theme. */
    public void apply(ThemeProfile profile) {
        JsonObject values = profile.settings();
        Map<Setting<?>, Object> parsed = new LinkedHashMap<>();
        for (Setting<?> setting : settings.values()) {
            JsonElement value = values.get(setting.getName());
            if (value == null && setting.getName().endsWith(" Easing") && values.has("Easing Curve")) { value = values.get("Easing Curve"); }
            parsed.put(setting, value == null ? setting instanceof ColorSetting color ? color.defaultPaint() : setting.getDefaultValue() : decode(setting, value));
        }
        parsed.forEach(ThemeManager::assign);
        changed();
    }
    private static Object decode(Setting<?> setting, JsonElement element) {
        if (setting instanceof ColorSetting color) { return color.decode(element); }
        if (!element.isJsonPrimitive()) { throw new IllegalArgumentException("Invalid " + setting.getName()); }
        var value = element.getAsJsonPrimitive();
        if (setting instanceof dev.riserecode.setting.StringSetting text) {
            if (!value.isString() || value.getAsString().length() > text.maximum() || value.getAsString().codePoints().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Invalid text " + setting.getName()); }
            return value.getAsString();
        }
        if (setting instanceof BooleanSetting) {
            if (!value.isBoolean()) { throw new IllegalArgumentException("Expected boolean " + setting.getName()); }
            return value.getAsBoolean();
        }
        if (setting instanceof DoubleSetting number) {
            if (!value.isNumber()) { throw new IllegalArgumentException("Expected number " + setting.getName()); }
            double result = value.getAsDouble();
            if (!Double.isFinite(result) || result < number.getMin() || result > number.getMax()) {
                throw new IllegalArgumentException("Out-of-range " + setting.getName());
            }
            return result;
        }
        if (setting instanceof EnumSetting<?> options) {
            if (!value.isString()) { throw new IllegalArgumentException("Expected option " + setting.getName()); }
            return options.getOptions().stream().filter(option -> option.name().equals(value.getAsString())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown option " + value.getAsString()));
        }
        throw new IllegalArgumentException("Unsupported setting type");
    }
    private static <T> void assign(Setting<T> setting, Object value) {
        if (setting instanceof ColorSetting color) { color.apply((ColorSetting.Paint) value); return; }
        @SuppressWarnings("unchecked") T typed = (T) value;
        setting.setValue(typed);
    }
    public void load() {
        Path file = directory.resolve("theme.json");
        try {
            Files.createDirectories(directory.resolve("theme-profiles")); Files.createDirectories(getFontDirectory());
            if (Files.isRegularFile(file)) { apply(read(file)); loaded = true; dirtySince = 0; }
            else { save(); }
        } catch (IOException | RuntimeException failure) {
            logger.warn("Could not load theme; keeping defaults", failure);
            if (Files.isRegularFile(file)) {
                try {
                    Path backup = directory.resolve("theme-invalid-" + System.currentTimeMillis() + ".json");
                    Files.move(file, backup); loaded = true;
                    logger.warn("Preserved unreadable theme as {}", backup); save();
                } catch (IOException backupFailure) { logger.warn("Could not preserve unreadable theme", backupFailure); }
            }
        }
    }
    public boolean save() {
        try { write(directory.resolve("theme.json"), snapshot()); dirtySince = 0; retryAfter = 0; return true; }
        catch (IOException failure) { retryAfter = System.nanoTime() + 5_000_000_000L; logger.warn("Could not save theme", failure); return false; }
    }
    private Path profilePath(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,40}")) {
            throw new IllegalArgumentException("Use 1–40 letters, digits, _ or - for theme names");
        }
        return directory.resolve("theme-profiles").resolve(name.toLowerCase(Locale.ROOT) + ".json");
    }
    public boolean saveProfile(String name) {
        Path file = profilePath(name);
        try { write(file, snapshot()); activeProfile = name.toLowerCase(Locale.ROOT); return true; }
        catch (IOException failure) { logger.warn("Could not save theme profile {}", name, failure); return false; }
    }
    public boolean loadProfile(String name) {
        Path file = profilePath(name);
        try { apply(read(file)); activeProfile = name.toLowerCase(Locale.ROOT); return true; }
        catch (IOException | RuntimeException failure) { logger.warn("Could not load theme profile {}", name, failure); return false; }
    }
    public List<String> getProfiles() {
        Path folder = directory.resolve("theme-profiles");
        if (!Files.isDirectory(folder)) { return List.of(); }
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).map(file -> file.getFileName().toString())
                    .filter(name -> name.matches("[a-z0-9_-]{1,40}\\.json"))
                    .map(name -> name.substring(0, name.length() - 5)).sorted().toList();
        } catch (IOException failure) { logger.warn("Could not list theme profiles", failure); return List.of(); }
    }
    public void reset() { settings.values().forEach(Setting::reset); activeProfile = "default"; changed(); }
    private static ThemeProfile read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("schemaVersion") || !root.has("settings")) { throw new IllegalArgumentException("Invalid theme profile"); }
            return new ThemeProfile(root.get("schemaVersion").getAsBigDecimal().intValueExact(), root.getAsJsonObject("settings"));
        }
    }
    private static void write(Path file, ThemeProfile profile) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "theme-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(profile, writer); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    @Override public void close() { save(); if (subscription != null) { subscription.close(); subscription = null; } }
}
