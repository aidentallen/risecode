# Phase 7 — Themes, typography, GPU effects and live customization

Implemented in `/home/toast/Work/riserecode`, version **0.6.0**, Minecraft **26.2**,
Fabric Loader **0.19.5**, Fabric API **0.161.0+26.2**, Loom **1.17.21**, Gradle **9.6.0**,
Java **25**. No preview features, Meteor imports or copied client implementations.
The existing 6.130 sidebar layout and lowercase **risecode** branding are retained.

The live Themes page contains 29 typed controls across six groups. This delivery
includes every new or changed production file in full, all seven shaders, build/
mixin integration and the verification files. Existing modules/settings/event bus
remain the underlying architecture. The former unused AWT `SmoothFont.java` was
removed; STB and original bitmap distance fields now handle client text.

> This is the historical 0.6.0 source snapshot. The 0.6.1 text renderer and shader
> supersede its normal-text SDF path; see [FONT_FIX.md](FONT_FIX.md) and current source.

## 1. Updated project file tree

Source, resources, build wrapper and documentation are shown below. Generated
build output, dependency caches and runtime data are excluded from this tree.

```text
riserecode/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/
│   ├── install-qraftys.py
│   └── verify.sh
├── src/
│   ├── gametest/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── testmixin/
│   │   │           │   └── ChatStateAccessor.java
│   │   │           ├── ClientIntegrationTest.java
│   │   │           ├── CombatIntegrationTest.java
│   │   │           ├── EngineIntegrationTest.java
│   │   │           └── ThemeIntegrationTest.java
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       └── riserecode-test.mixins.json
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── command/
│   │   │           │   ├── commands/
│   │   │           │   │   ├── BindCommand.java
│   │   │           │   │   ├── FriendCommand.java
│   │   │           │   │   ├── HelpCommand.java
│   │   │           │   │   ├── ProfileCommand.java
│   │   │           │   │   └── ToggleCommand.java
│   │   │           │   ├── Command.java
│   │   │           │   └── CommandManager.java
│   │   │           ├── config/
│   │   │           │   ├── ConfigManager.java
│   │   │           │   ├── JsonStorage.java
│   │   │           │   └── ProfileManager.java
│   │   │           ├── event/
│   │   │           │   ├── AttackEvent.java
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── PacketEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── friend/
│   │   │           │   └── FriendManager.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   ├── ChatInputMixin.java
│   │   │           │   ├── ConnectionMixin.java
│   │   │           │   ├── GuiRendererMixin.java
│   │   │           │   └── KeyboardHandlerMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   ├── FullBrightModule.java
│   │   │           │   │   ├── KillAuraModule.java
│   │   │           │   │   ├── NoFallModule.java
│   │   │           │   │   ├── SprintModule.java
│   │   │           │   │   └── TracersModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
│   │   │           ├── render/
│   │   │           │   └── WorldRenderer.java
│   │   │           ├── setting/
│   │   │           │   ├── BooleanSetting.java
│   │   │           │   ├── ColorSetting.java
│   │   │           │   ├── DoubleSetting.java
│   │   │           │   ├── EnumSetting.java
│   │   │           │   └── Setting.java
│   │   │           ├── ui/
│   │   │           │   ├── animation/
│   │   │           │   │   └── Animation.java
│   │   │           │   ├── clickgui/
│   │   │           │   │   └── ClickGui.java
│   │   │           │   ├── component/
│   │   │           │   │   ├── setting/
│   │   │           │   │   │   ├── ColorPicker.java
│   │   │           │   │   │   ├── ColorSwatch.java
│   │   │           │   │   │   ├── EnumSelector.java
│   │   │           │   │   │   ├── Slider.java
│   │   │           │   │   │   └── ToggleSwitch.java
│   │   │           │   │   ├── ActionButton.java
│   │   │           │   │   ├── Icon.java
│   │   │           │   │   ├── ModuleButton.java
│   │   │           │   │   ├── Panel.java
│   │   │           │   │   ├── Sidebar.java
│   │   │           │   │   ├── TextField.java
│   │   │           │   │   ├── ThemeEditor.java
│   │   │           │   │   └── UIComponent.java
│   │   │           │   ├── font/
│   │   │           │   │   ├── BitmapPackFont.java
│   │   │           │   │   └── CustomTextRenderer.java
│   │   │           │   ├── hud/
│   │   │           │   │   └── HudRenderer.java
│   │   │           │   ├── render/
│   │   │           │   │   └── UiMesh.java
│   │   │           │   └── theme/
│   │   │           │       ├── Theme.java
│   │   │           │       ├── ThemeManager.java
│   │   │           │       └── ThemeProfile.java
│   │   │           ├── util/
│   │   │           │   ├── AttackManager.java
│   │   │           │   ├── BlurUtil.java
│   │   │           │   ├── ItemUtil.java
│   │   │           │   ├── MathUtil.java
│   │   │           │   ├── RenderUtil.java
│   │   │           │   ├── RotationManager.java
│   │   │           │   └── TargetUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       ├── font/
│   │       │       │   ├── google_sans_medium.ttf
│   │       │       │   ├── google_sans_regular.ttf
│   │       │       │   ├── inter_medium.json
│   │       │       │   ├── inter_medium.ttf
│   │       │       │   ├── inter_regular.json
│   │       │       │   ├── inter_regular.ttf
│   │       │       │   ├── montserrat_medium.ttf
│   │       │       │   ├── montserrat_regular.ttf
│   │       │       │   ├── roboto_medium.json
│   │       │       │   ├── roboto_medium.ttf
│   │       │       │   ├── roboto_regular.json
│   │       │       │   └── roboto_regular.ttf
│   │       │       ├── lang/
│   │       │       │   └── en_us.json
│   │       │       └── shaders/
│   │       │           └── core/
│   │       │               ├── blur.fsh
│   │       │               ├── blur_box.fsh
│   │       │               ├── font.fsh
│   │       │               ├── hsv.fsh
│   │       │               ├── shape.fsh
│   │       │               ├── ui.vsh
│   │       │               └── window.fsh
│   │       ├── licenses/
│   │       │   ├── google-sans-NOTICE.txt
│   │       │   ├── google-sans-OFL.txt
│   │       │   ├── inter-OFL.txt
│   │       │   ├── montserrat-OFL.txt
│   │       │   └── roboto-LICENSE.txt
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── CombatChecks.java
│                   ├── EngineChecks.java
│                   ├── FoundationChecks.java
│                   ├── RuntimeChecks.java
│                   ├── ThemeChecks.java
│                   └── UiChecks.java
├── .gitignore
├── FONT_SOURCES.md
├── PHASE3.md
├── PHASE4.md
├── PHASE5.md
├── PHASE6.md
├── PHASE7.md
├── README.md
├── UI_REDESIGN.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

Local development assets and retained outputs:

```text
run/
  resourcepacks/qraftys-capitalized-font.zip
  config/riserecode/fonts/minecraftia.ttf
  config/riserecode/fonts/minecraftia-author-readme.txt
  config/riserecode/fonts/originals/qraftys-d5de138c0be9.zip
build/
  libs/riserecode-0.6.0.jar
  libs/riserecode-0.6.0-sources.jar
  verification/phase7/
    build.log
    client-gametest.log
    minecraft.log
    verification.json
    screenshots/
```

## 2. Code files

Each following listing is the complete current file. Shaders and build/resource
changes follow the Java implementation; tests are included at the end.

### src/main/java/dev/riserecode/ui/theme/ThemeProfile.java

```java
package dev.riserecode.ui.theme;

import com.google.gson.JsonObject;
import java.util.Objects;

/** Versioned visual snapshot. Neither callers nor Gson can mutate a live profile's settings. */
public record ThemeProfile(int schemaVersion, JsonObject settings) {
    public static final int VERSION = 1;
    public ThemeProfile {
        if (schemaVersion != VERSION) { throw new IllegalArgumentException("Unsupported theme schema " + schemaVersion); }
        settings = Objects.requireNonNull(settings, "settings").deepCopy();
    }
    @Override public JsonObject settings() { return settings.deepCopy(); }
}
```

### src/main/java/dev/riserecode/ui/theme/ThemeManager.java

```java
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
        add("Colors & Glow", new ColorSetting("Primary Accent", "Main active color", Theme.TEAL));
        add("Colors & Glow", new ColorSetting("Secondary Accent", "Gradient endpoint", 0xFF7C9FF5));
        number("Geometry & Layout", "Corner Radius", 10, 0, 30, 1);
        number("Geometry & Layout", "Panel Opacity", 0.87, 0, 1, 0.01);
        number("Geometry & Layout", "Border Thickness", 0.5, 0, 3, 0.05);
        number("Geometry & Layout", "Border Opacity", 0.08, 0, 1, 0.01);
        number("Geometry & Layout", "Content Padding", 8, 4, 20, 1);
        number("Geometry & Layout", "Interface Scale", 1, 0.75, 1.3, 0.01);
        number("Animations", "Animation Speed", 1, 0.5, 3, 0.05);
        add("Animations", new EnumSetting<>("Easing Curve", "Curve applied to every UI transition", MathUtil.Easing.OUT_QUART));
        number("Background Effects", "GUI Blur Radius", 12, 0, 30, 1);
        number("Background Effects", "GUI Blur Opacity", 0.8, 0, 1, 0.01);
        bool("Background Effects", "World Blur", false);
        number("Background Effects", "World Blur Radius", 8, 0, 30, 1);
        bool("HUD", "HUD Visible", true);
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
    public int color(String name) { return ((ColorSetting) getSetting(name)).getValue(); }
    public <E extends Enum<E>> E choice(String name, Class<E> type) { return type.cast(getSetting(name).getValue()); }
    public long getRevision() { return revision; }
    public String getActiveProfile() { return activeProfile; }
    public boolean wasLoaded() { return loaded; }
    public void changed() { revision++; dirtySince = System.nanoTime(); retryAfter = 0; }
    public void bind(EventBus bus) {
        if (subscription != null) { return; }
        Theme.bind(this);
        Animation.configureUi(() -> number("Animation Speed"), () -> choice("Easing Curve", MathUtil.Easing.class));
        subscription = bus.subscribe(UpdateEvent.class, ignored -> {
            long now = System.nanoTime();
            if (dirtySince != 0 && now >= retryAfter && now - dirtySince >= 400_000_000L) { save(); }
        });
    }
    public ThemeProfile snapshot() {
        JsonObject values = new JsonObject();
        settings.forEach((name, setting) -> values.add(name, GSON.toJsonTree(setting.getValue())));
        return new ThemeProfile(ThemeProfile.VERSION, values);
    }
    /** Validate the complete document before applying it, so a bad file cannot half-load a theme. */
    public void apply(ThemeProfile profile) {
        JsonObject values = profile.settings();
        Map<Setting<?>, Object> parsed = new LinkedHashMap<>();
        for (Setting<?> setting : settings.values()) {
            JsonElement value = values.get(setting.getName());
            parsed.put(setting, value == null ? setting.getDefaultValue() : decode(setting, value));
        }
        parsed.forEach(ThemeManager::assign);
        changed();
    }
    private static Object decode(Setting<?> setting, JsonElement element) {
        if (!element.isJsonPrimitive()) { throw new IllegalArgumentException("Invalid " + setting.getName()); }
        var value = element.getAsJsonPrimitive();
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
        if (setting instanceof ColorSetting) {
            if (!value.isNumber()) { throw new IllegalArgumentException("Expected ARGB color"); }
            return value.getAsBigDecimal().intValueExact();
        }
        if (setting instanceof EnumSetting<?> options) {
            if (!value.isString()) { throw new IllegalArgumentException("Expected option " + setting.getName()); }
            return options.getOptions().stream().filter(option -> option.name().equals(value.getAsString())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown option " + value.getAsString()));
        }
        throw new IllegalArgumentException("Unsupported setting type");
    }
    private static <T> void assign(Setting<T> setting, Object value) {
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
```

### src/main/java/dev/riserecode/ui/theme/Theme.java

```java
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
    private static boolean german;
    private static ThemeManager manager;
    public static void bind(ThemeManager value) { manager = value; }
    public static ThemeManager manager() { return manager; }
    public static double number(String name, double fallback) { return manager == null ? fallback : manager.number(name); }
    public static boolean bool(String name, boolean fallback) { return manager == null ? fallback : manager.bool(name); }
    public static double radius(double original) { return original * number("Corner Radius", 10) / 10; }
    public static int secondaryAccent() { return manager == null ? BLUE : manager.color("Secondary Accent"); }
    public static int accentAt(double amount) { return bool("Accent Gradient", true) ? lerp(accent(), secondaryAccent(), amount) : accent(); }
    private static final Map<String, String> GERMAN = Map.ofEntries(
            Map.entry("Search", "Suche"), Map.entry("Combat", "Kampf"), Map.entry("Movement", "Bewegung"),
            Map.entry("Player", "Spieler"), Map.entry("Render", "Anzeige"), Map.entry("World", "Welt"),
            Map.entry("Utility", "Extras"), Map.entry("Profiles", "Profile"), Map.entry("Friends", "Freunde"),
            Map.entry("Themes", "Design"), Map.entry("Language", "Sprache"), Map.entry("Save", "Speichern"),
            Map.entry("Load", "Laden"), Map.entry("Add", "Hinzufügen"), Map.entry("Remove", "Entfernen"),
            Map.entry("Start typing to search...", "Zum Suchen tippen..."), Map.entry("No matching modules", "Keine passenden Module"),
            Map.entry("No modules in this category", "Keine Module in dieser Kategorie"));
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_regular")));
    private static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_medium")));

    private Theme() { }
    public static int accent() { return lerp(previousAccent, accent, accentBlend.getValue()); }
    public static int accentTarget() { return accent; }
    public static void setAccent(int color) {
        int next = color | 0xFF000000;
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
        if (manager != null && manager.color("Primary Accent") != accent) { setAccent(manager.color("Primary Accent")); }
        accentBlend.update(seconds);
    }
    public static boolean isGerman() { return german; }
    public static void setGerman(boolean value) { german = value; }
    public static String label(String value) { return german ? GERMAN.getOrDefault(value, value) : value; }

    /** Adds word spacing to identifiers without changing their persisted setting/module keys. */
    public static String displayName(String value) {
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
```

### src/main/java/dev/riserecode/ui/font/CustomTextRenderer.java

```java
package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Original STB TrueType renderer: kerning, fractional advances and shader-smoothed distance fields.
 * Fonts upload once to single-channel atlases. Live shadows/glows use the same glyphs and no uploads.
 * Optional fonts are loaded from the resource directory or the user's config/fonts directory.
 */
public final class CustomTextRenderer {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PADDING = 48;
    private static final float DISTANCE_SCALE = 2.5f;
    private static final String SYMBOLS = "…−›•×✓";
    private static final Map<String, Face> FACES = new HashMap<>();
    private static final Map<ThemeManager.Typeface, Boolean> AVAILABLE = new HashMap<>();
    private static BitmapPackFont bitmap;
    private static ThemeManager manager;
    private static final Map<String, String> FAILURES = new HashMap<>();
    private static final class Pipelines {
        private static final RenderPipeline FONT = UiMesh.pipeline("font", "font");
    }
    static RenderPipeline fontPipeline() { return Pipelines.FONT; }
    private record Glyph(int cell, int width, int height, int xOffset, int yOffset, double advance) { }
    private static final class Face implements AutoCloseable {
        private final ByteBuffer data;
        private final STBTTFontinfo info = STBTTFontinfo.malloc();
        private final float scale;
        private final double baseline;
        private final boolean pixel;
        private final Map<Integer, Glyph> glyphs = new HashMap<>();
        private final Map<Long, Double> kerning = new HashMap<>();
        private GpuTexture texture;
        private GpuTextureView view;
        private Face(byte[] bytes, boolean pixel) {
            validate(bytes);
            this.pixel = pixel;
            data = MemoryUtil.memAlloc(bytes.length); data.put(bytes).flip();
            if (!STBTruetype.stbtt_InitFont(info, data)) { info.free(); MemoryUtil.memFree(data); throw new IllegalArgumentException("Invalid TrueType font"); }
            scale = pixel ? STBTruetype.stbtt_ScaleForMappingEmToPixels(info, 12 * DENSITY)
                    : STBTruetype.stbtt_ScaleForPixelHeight(info, 11 * DENSITY);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var x0 = stack.mallocInt(1); var y0 = stack.mallocInt(1); var x1 = stack.mallocInt(1); var y1 = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointBitmapBox(info, 'H', scale, scale, x0, y0, x1, y1);
                baseline = -y0.get(0) / (double) DENSITY;
            }
        }
        private static void validate(byte[] bytes) {
            if (bytes.length < 12) { throw new IllegalArgumentException("Truncated font header"); }
            ByteBuffer header = ByteBuffer.wrap(bytes);
            int signature = header.getInt(0), tables = Short.toUnsignedInt(header.getShort(4));
            if (signature != 0x00010000 && signature != 0x4F54544F || tables == 0 || 12L + 16L * tables > bytes.length) {
                throw new IllegalArgumentException("Expected a TrueType/OpenType SFNT file");
            }
            for (int index = 0; index < tables; index++) {
                int entry = 12 + index * 16;
                long offset = Integer.toUnsignedLong(header.getInt(entry + 8)), length = Integer.toUnsignedLong(header.getInt(entry + 12));
                if (offset + length > bytes.length) { throw new IllegalArgumentException("Truncated font table"); }
            }
        }
        private double advance(int code) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var width = stack.mallocInt(1); var bearing = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointHMetrics(info, code, width, bearing);
                return width.get(0) * scale / DENSITY;
            }
        }
        private double kern(int previous, int next) {
            if (previous < 0) { return 0; }
            long key = ((long) previous << 32) | next;
            return kerning.computeIfAbsent(key, ignored -> STBTruetype.stbtt_GetCodepointKernAdvance(info, previous, next) * (double) scale / DENSITY);
        }
        private void upload() {
            if (view != null) { return; }
            ByteBuffer pixels = MemoryUtil.memCalloc(SIDE * SIDE);
            try {
                int cell = 0;
                for (int code = 32; code < 256; code++) { bake(pixels, code, cell++); }
                for (int code : SYMBOLS.codePoints().toArray()) { bake(pixels, code, cell++); }
                texture = RenderSystem.getDevice().createTexture(() -> "risecode STB font", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                        GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
                view = RenderSystem.getDevice().createTextureView(texture);
                try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> "risecode font upload", GpuBuffer.USAGE_COPY_SRC, pixels)) {
                    RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, texture, 0, 0, SIDE, SIDE, 0, 0);
                }
            } finally { MemoryUtil.memFree(pixels); }
        }
        private void bake(ByteBuffer pixels, int code, int cell) {
            if (STBTruetype.stbtt_FindGlyphIndex(info, code) == 0) { return; }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1); var h = stack.mallocInt(1); var xo = stack.mallocInt(1); var yo = stack.mallocInt(1);
                ByteBuffer sdf = STBTruetype.stbtt_GetCodepointSDF(info, scale, code, PADDING, (byte) 128, DISTANCE_SCALE, w, h, xo, yo);
                if (sdf == null) { glyphs.put(code, new Glyph(cell, 0, 0, 0, 0, advance(code))); return; }
                try {
                    if (w.get(0) > CELL || h.get(0) > CELL) { throw new IllegalArgumentException("Font glyph exceeds atlas cell"); }
                    int startX = cell % 16 * CELL, startY = cell / 16 * CELL;
                    for (int y = 0; y < h.get(0); y++) {
                        for (int x = 0; x < w.get(0); x++) { pixels.put((startY + y) * SIDE + startX + x, sdf.get(y * w.get(0) + x)); }
                    }
                    glyphs.put(code, new Glyph(cell, w.get(0), h.get(0), xo.get(0), yo.get(0), advance(code)));
                } finally { STBTruetype.stbtt_FreeSDF(sdf); }
            }
        }
        @Override public void close() {
            if (view != null) { view.close(); }
            if (texture != null) { texture.close(); }
            info.free(); MemoryUtil.memFree(data);
        }
    }
    private CustomTextRenderer() { }
    public static void configure(ThemeManager value) { manager = value; }
    private static String base(ThemeManager.Typeface font) { return font.name().toLowerCase(Locale.ROOT); }
    public static boolean available(ThemeManager.Typeface font) {
        return AVAILABLE.computeIfAbsent(font, CustomTextRenderer::findAvailable);
    }
    private static boolean findAvailable(ThemeManager.Typeface font) {
        if (font == ThemeManager.Typeface.QRAFTYS) { return manager != null && BitmapPackFont.find(manager.getDirectory()) != null; }
        String file = base(font) + "_regular.ttf";
        return CustomTextRenderer.class.getResource("/assets/riserecode/font/" + file) != null
                || manager != null && (Files.isRegularFile(manager.getFontDirectory().resolve(file))
                || Files.isRegularFile(manager.getFontDirectory().resolve(base(font) + ".ttf")));
    }
    public static String status() {
        ThemeManager.Typeface chosen = selected();
        if (chosen == ThemeManager.Typeface.QRAFTYS) {
            if (FAILURES.containsKey("qraftys_pack")) { return "Invalid Qraftys pack · using Inter"; }
            return available(chosen) ? "Qraftys resource-pack glyphs · nearest sampling"
                    : "Add Qraftys ZIP to resourcepacks · using Inter";
        }
        if (FAILURES.containsKey(base(chosen) + "_regular")) { return "Invalid " + base(chosen) + " font · using Inter"; }
        return available(chosen) ? "STB distance-field rendering · live preview"
                : "Install " + base(chosen) + ".ttf in config/riserecode/fonts · using Inter";
    }
    private static ThemeManager.Typeface selected() {
        return manager == null ? ThemeManager.Typeface.INTER : manager.choice("Font Selection", ThemeManager.Typeface.class);
    }
    private static BitmapPackFont bitmap() {
        if (selected() == ThemeManager.Typeface.QRAFTYS && bitmap != null) { return bitmap; }
        if (selected() != ThemeManager.Typeface.QRAFTYS || !available(selected()) || FAILURES.containsKey("qraftys_pack")) { return null; }
        if (bitmap == null) {
            try { bitmap = new BitmapPackFont(BitmapPackFont.find(manager.getDirectory())); }
            catch (IOException | RuntimeException failure) {
                FAILURES.put("qraftys_pack", failure.getMessage());
                org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load Qraftys pack; using Inter", failure);
            }
        }
        return bitmap;
    }
    private static Face face(boolean title) {
        ThemeManager.Typeface typeface = selected();
        if (typeface == ThemeManager.Typeface.QRAFTYS || !available(typeface)) { typeface = ThemeManager.Typeface.INTER; }
        String name = base(typeface) + (title ? "_medium" : "_regular");
        if (FAILURES.containsKey(name)) { return fallbackFace(title); }
        Face cached = FACES.get(name); if (cached != null) { return cached; }
        try {
            Face loaded = new Face(read(name + ".ttf", name.replace("_medium", "_regular") + ".ttf"), typeface == ThemeManager.Typeface.MINECRAFTIA);
            FACES.put(name, loaded); return loaded;
        } catch (IOException | RuntimeException failure) {
            FAILURES.put(name, failure.getMessage());
            org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load {}; using Inter", name, failure);
            return fallbackFace(title);
        }
    }
    private static Face fallbackFace(boolean title) {
        String key = title ? "inter_medium" : "inter_regular";
        return FACES.computeIfAbsent(key, ignored -> {
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + key + ".ttf")) {
                if (stream == null) { throw new IOException("Bundled Inter font missing"); }
                return new Face(stream.readAllBytes(), false);
            } catch (IOException failure) { throw new IllegalStateException("Missing bundled fallback font", failure); }
        });
    }
    private static byte[] read(String preferred, String fallback) throws IOException {
        for (String file : new String[] {preferred, fallback, fallback.replace("_regular", "")}) {
            if (manager != null) {
                Path local = manager.getFontDirectory().resolve(file);
                if (Files.isRegularFile(local)) {
                    if (Files.size(local) > 16 * 1024 * 1024) { throw new IOException("Font file exceeds 16 MiB"); }
                    return Files.readAllBytes(local);
                }
            }
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + file)) {
                if (stream != null) { return stream.readAllBytes(); }
            }
        }
        throw new IOException("Missing " + preferred);
    }
    private static boolean supports(Face face, int code) {
        return (code >= 32 && code < 256 || SYMBOLS.indexOf(code) >= 0)
                && (code == ' ' || STBTruetype.stbtt_FindGlyphIndex(face.info, code) != 0);
    }
    private static double advance(Face face, Font fallback, int code, boolean title) {
        return supports(face, code) ? face.advance(code) : fallback.width(Theme.text(new String(Character.toChars(code)), title));
    }
    private static double kern(Face face, int previous, int next) { return supports(face, previous) && supports(face, next) ? face.kern(previous, next) : 0; }
    public static int width(Font fallback, String original, boolean title) {
        BitmapPackFont pack = bitmap();
        if (pack != null) { return (int) Math.ceil(pack.width(fallback, original) * Theme.number("Font Scale", 1)); }
        String value = original; Face face = face(title);
        double amount = 0; int previous = -1;
        for (int code : value.codePoints().toArray()) { amount += kern(face, previous, code) + advance(face, fallback, code, title); previous = code; }
        return (int) Math.ceil(amount * Theme.number("Font Scale", 1));
    }
    @FunctionalInterface private interface Layer { void draw(double x, double y, int color, double blur, int mode, boolean gradient); }
    public static void draw(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent) {
        if ((color >>> 24) == 0 || original.isEmpty()) { return; }
        BitmapPackFont pack = bitmap();
        Layer layer;
        if (pack != null) { layer = (x, y, tint, blur, mode, gradient) -> pack.draw(graphics, fallback, original, x, y, tint, blur, mode, gradient); }
        else {
            Face face = face(title); face.upload();
            layer = (x, y, tint, blur, mode, gradient) -> drawFace(graphics, fallback, face, original, x, y, tint, blur, mode, gradient, title);
        }
        graphics.pose().pushMatrix();
        float fontScale = (float) Theme.number("Font Scale", 1); graphics.pose().scale(fontScale, fontScale);
        try {
            if (shadow && Theme.bool("Text Shadow", true)) {
                layer.draw(Theme.number("Shadow Offset X", 0.5) / fontScale,
                        Theme.number("Shadow Offset Y", 0.7) / fontScale,
                        Theme.opacity(color & 0xFF000000, Theme.number("Shadow Opacity", 0.6)),
                        Theme.number("Shadow Blur", 0.65) / fontScale, 1, false);
            }
            if (accent && Theme.bool("Global Glow", true) && Theme.number("Glow Radius", 5) > 0) {
                layer.draw(0, 0, Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                        Theme.number("Glow Radius", 5) / 3 / fontScale, 1, false);
            }
            layer.draw(0, 0, color, 0, pack != null || selected() == ThemeManager.Typeface.MINECRAFTIA ? 2 : 0, accent);
        } finally { graphics.pose().popMatrix(); }
    }
    private static void drawFace(GuiGraphicsExtractor graphics, Font fallback, Face face, String value, double x, double y,
            int color, double blur, int mode, boolean gradient, boolean title) {
        int[] codes = value.codePoints().toArray(); double total = 0; int previous = -1;
        for (int code : codes) { total += kern(face, previous, code) + advance(face, fallback, code, title); previous = code; }
        previous = -1; double advance = 0;
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(mode == 2 ? FilterMode.NEAREST : FilterMode.LINEAR);
        TextureSetup setup = TextureSetup.singleTexture(face.view, sampler);
        for (int code : codes) {
            advance += kern(face, previous, code); Glyph glyph = face.glyphs.get(code);
            if (!supports(face, code)) {
                drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + advance, y, color, blur);
            }
            if (glyph != null && glyph.width > 0 && code != ' ') {
                double gx = x + advance + glyph.xOffset / (double) DENSITY;
                double gy = y + face.baseline + glyph.yOffset / (double) DENSITY;
                if (mode == 2) { gx = Math.rint(gx); gy = Math.rint(gy); }
                int tint = gradient ? Theme.opacity(Theme.accentAt(advance / Math.max(1, total)), (color >>> 24) / 255.0) : color;
                float u = glyph.cell % 16 * CELL / (float) SIDE, v = glyph.cell / 16 * CELL / (float) SIDE;
                UiMesh.quad(graphics, Pipelines.FONT, setup, gx, gy, glyph.width / (double) DENSITY,
                        glyph.height / (double) DENSITY, u, v, u + glyph.width / (float) SIDE, v + glyph.height / (float) SIDE,
                        tint, tint, (int) Math.round(blur * 256), mode, 0, 0);
            }
            advance += advance(face, fallback, code, title); previous = code;
        }
    }
    /** Unicode outside the cached Latin atlas retains the selected font for its neighbouring glyphs.
     * Native fallback shadows use the same offsets and a normalized Gaussian stencil.
     */
    static void drawFallbackGlyph(GuiGraphicsExtractor graphics, Font font, String glyph, double x, double y, int color, double blur) {
        int radius = blur > 0.05 ? 3 : 0; double sum = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) { sum += Math.exp(-(dx * dx + dy * dy) / 2.0); }
        }
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tint = Theme.opacity(color, Math.exp(-(dx * dx + dy * dy) / 2.0) / sum);
                if ((tint >>> 24) == 0) { continue; }
                graphics.pose().pushMatrix(); graphics.pose().translate((float) (x + dx * blur), (float) (y + dy * blur));
                graphics.text(font, Theme.text(glyph, false), 0, 0, tint, false); graphics.pose().popMatrix();
            }
        }
    }
    public static void close() {
        FACES.values().forEach(Face::close); FACES.clear(); FAILURES.clear(); AVAILABLE.clear();
        if (bitmap != null) { bitmap.close(); bitmap = null; }
    }
}
```

### src/main/java/dev/riserecode/ui/font/BitmapPackFont.java

```java
package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.system.MemoryUtil;

/** Reads the user's Qraftys resource pack without bundling its copyrighted artwork in the mod.
 * Original bitmap advances and small-cap lowercase glyphs are preserved. A linear-time Euclidean
 * distance transform creates a separate GPU atlas for live shadows/glow; text uses nearest sampling.
 */
final class BitmapPackFont implements AutoCloseable {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PAD = 48;
    private static final String[] ROWS = {
            "", "", " !\"#$%&'()*+,-./", "0123456789:;<=>?", "@ABCDEFGHIJKLMNO", "PQRSTUVWXYZ[\\]^_",
            "`abcdefghijklmno", "pqrstuvwxyz{|}~\0", "", "\0\0\0\0\0\0\0\0\0\0\0\0£\0\0ƒ",
            "\0\0\0\0\0\0ªº\0\0¬\0\0\0«»", "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐", "└┴┬├─┼╞╟╚╔╩╦╠═╬╧",
            "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀", "\0\0\0\0\0\0\0\0\0\0\0\0\0∅∈\0", "≡±≥≤⌠⌡÷≈°∙\0√ⁿ²■\0" };
    private record Glyph(int cell, int advance) { }
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final GpuTexture texture;
    private final GpuTextureView view;

    static Path find(Path configDirectory) {
        Path override = configDirectory.resolve("fonts/qraftys.zip");
        if (Files.isRegularFile(override)) { return override; }
        Path packs = configDirectory.toAbsolutePath().getParent().getParent().resolve("resourcepacks");
        Path canonical = packs.resolve("qraftys-capitalized-font.zip");
        if (Files.isRegularFile(canonical)) { return canonical; }
        if (!Files.isDirectory(packs)) { return null; }
        try (var paths = Files.list(packs)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.contains("qraft") && name.endsWith(".zip");
            }).sorted().findFirst().orElse(null);
        } catch (IOException ignored) { return null; }
    }

    BitmapPackFont(Path pack) throws IOException {
        ByteBuffer atlas = MemoryUtil.memCalloc(SIDE * SIDE);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry("assets/minecraft/textures/font/ascii.png");
            if (entry == null || entry.getSize() > 1024 * 1024) { throw new IOException("Missing/oversized Qraftys ascii.png"); }
            try (var stream = zip.getInputStream(entry); NativeImage image = NativeImage.read(stream)) {
                if (image.getWidth() != 128 || image.getHeight() != 128) { throw new IOException("Expected the Qraftys 16×16 bitmap grid"); }
                for (int row = 0; row < ROWS.length; row++) {
                    int[] codes = ROWS[row].codePoints().toArray();
                    for (int column = 0; column < codes.length; column++) {
                        int code = codes[column]; if (code == 0) { continue; }
                        int cell = row * 16 + column, extent = 0;
                        boolean[] mask = new boolean[CELL * CELL];
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                if ((image.getPixel(column * 8 + x, row * 8 + y) >>> 24) == 0) { continue; }
                                extent = Math.max(extent, x + 1);
                                for (int dy = 0; dy < DENSITY; dy++) {
                                    for (int dx = 0; dx < DENSITY; dx++) { mask[(PAD + y * DENSITY + dy) * CELL + PAD + x * DENSITY + dx] = true; }
                                }
                            }
                        }
                        glyphs.put(code, new Glyph(cell, code == ' ' ? 4 : extent + 1));
                        if (extent == 0) { continue; }
                        double[] inside = distances(mask, true), outside = distances(mask, false);
                        for (int y = 0; y < CELL; y++) {
                            for (int x = 0; x < CELL; x++) {
                                int index = y * CELL + x;
                                double distance = mask[index] ? Math.sqrt(outside[index]) - 0.5 : 0.5 - Math.sqrt(inside[index]);
                                int value = (int) Math.round(Math.max(0, Math.min(255, 128 + 2.5 * distance)));
                                atlas.put((cell / 16 * CELL + y) * SIDE + cell % 16 * CELL + x, (byte) value);
                            }
                        }
                    }
                }
            }
            texture = RenderSystem.getDevice().createTexture(() -> "risecode Qraftys bitmap SDF", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
            view = RenderSystem.getDevice().createTextureView(texture);
            try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> "risecode bitmap font upload", GpuBuffer.USAGE_COPY_SRC, atlas)) {
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, texture, 0, 0, SIDE, SIDE, 0, 0);
            }
        } finally { MemoryUtil.memFree(atlas); }
    }

    /** Squared distance to the nearest selected pixel: separable lower envelopes of parabolas. */
    private static double[] distances(boolean[] mask, boolean selected) {
        double[] result = new double[mask.length], input = new double[CELL], output = new double[CELL];
        int[] vertices = new int[CELL]; double[] boundaries = new double[CELL + 1];
        for (int x = 0; x < CELL; x++) {
            for (int y = 0; y < CELL; y++) { input[y] = mask[y * CELL + x] == selected ? 0 : 1e9; }
            distanceLine(input, output, vertices, boundaries);
            for (int y = 0; y < CELL; y++) { result[y * CELL + x] = output[y]; }
        }
        for (int y = 0; y < CELL; y++) {
            System.arraycopy(result, y * CELL, input, 0, CELL); distanceLine(input, output, vertices, boundaries);
            System.arraycopy(output, 0, result, y * CELL, CELL);
        }
        return result;
    }
    private static void distanceLine(double[] input, double[] output, int[] vertices, double[] boundaries) {
        int k = 0; vertices[0] = 0; boundaries[0] = Double.NEGATIVE_INFINITY; boundaries[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < CELL; q++) {
            double intersection;
            do {
                int p = vertices[k]; intersection = ((input[q] + q * q) - (input[p] + p * p)) / (2.0 * (q - p));
                if (intersection > boundaries[k]) { break; }
                k--;
            } while (k >= 0);
            k++; vertices[k] = q; boundaries[k] = intersection; boundaries[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < CELL; q++) {
            while (boundaries[k + 1] < q) { k++; }
            double delta = q - vertices[k]; output[q] = delta * delta + input[vertices[k]];
        }
    }
    private double advance(Font fallback, int code) {
        Glyph glyph = glyphs.get(code);
        return glyph == null ? fallback.width(Theme.text(new String(Character.toChars(code)), false)) : glyph.advance;
    }
    double width(Font fallback, String value) { return value.codePoints().mapToDouble(code -> advance(fallback, code)).sum(); }
    void draw(GuiGraphicsExtractor graphics, Font fallback, String value, double x, double y, int color, double blur, int mode, boolean gradient) {
        double cursor = 0, total = width(fallback, value);
        TextureSetup setup = TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(mode == 2 ? FilterMode.NEAREST : FilterMode.LINEAR));
        for (int code : value.codePoints().toArray()) {
            int tint = gradient ? Theme.opacity(Theme.accentAt(cursor / Math.max(1, total)), (color >>> 24) / 255.0) : color;
            Glyph glyph = glyphs.get(code);
            if (glyph == null) {
                CustomTextRenderer.drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + cursor, y, tint, blur);
            } else if (code != ' ') {
                float u = glyph.cell % 16 / 16f, v = glyph.cell / 16 / 16f;
                UiMesh.quad(graphics, CustomTextRenderer.fontPipeline(), setup, x + cursor - PAD / (double) DENSITY, y - PAD / (double) DENSITY,
                        CELL / (double) DENSITY, CELL / (double) DENSITY, u, v, u + 1 / 16f, v + 1 / 16f,
                        tint, tint, (int) Math.round(blur * 256), mode, 0, 0);
            }
            cursor += advance(fallback, code);
        }
    }
    @Override public void close() { view.close(); texture.close(); }
}
```

### src/main/java/dev/riserecode/ui/render/UiMesh.java

```java
package dev.riserecode.ui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Four-vertex extracted quads. Effects travel in vertex attributes, avoiding mutable shared UBOs. */
public final class UiMesh {
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UV1", GpuFormat.RG16_SINT)
            .addAttribute("UV2", GpuFormat.RG16_SINT).build();
    private UiMesh() { }
    public static RenderPipeline pipeline(String name, String fragment) {
        return RenderPipelines.register(RenderPipeline.builder(fragment.equals("shape") || fragment.equals("window") || fragment.equals("hsv") ? RenderPipelines.GUI_SNIPPET : RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name)).withVertexShader(id("core/ui"))
                .withFragmentShader(id("core/" + fragment)).withVertexBinding(0, FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("riserecode", path); }
    public static void quad(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double width, double height, float u0, float v0, float u1, float v1,
            int topColor, int bottomColor, int a, int b, int c, int d) {
        if (width <= 0 || height <= 0) { return; }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(width + 1), (int) Math.ceil(height + 1)).transformMaxBounds(pose);
        if (scissor != null) { bounds = bounds.intersection(scissor); }
        if (bounds == null) { return; }
        graphics.guiRenderState.addGuiElement(new Quad(pipeline, texture, pose, (float) x, (float) y,
                (float) width, (float) height, u0, v0, u1, v1, topColor, bottomColor, a, b, c, d, scissor, bounds));
    }
    private record Quad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x, float y,
            float width, float height, float u0, float v0, float u1, float v1, int top, int bottom,
            int a, int b, int c, int d, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, x, y, u0, v0, top); vertex(vertices, x, y + height, u0, v1, bottom);
            vertex(vertices, x + width, y + height, u1, v1, bottom); vertex(vertices, x + width, y, u1, v0, top);
        }
        private void vertex(VertexConsumer vertices, float px, float py, float u, float v, int color) {
            vertices.addVertexWith2DPose(pose, px, py).setUv(u, v).setColor(color).setUv1(a, b).setUv2(c, d);
        }
    }
}
```

### src/main/java/dev/riserecode/util/RenderUtil.java

```java
package dev.riserecode.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Antialiased signed-distance geometry extracted into the game's modern GUI batching pipeline. */
public final class RenderUtil {
    private static final RenderPipeline SHAPE = UiMesh.pipeline("shape", "shape");
    private static final RenderPipeline WINDOW = UiMesh.pipeline("window", "window");
    private RenderUtil() { }
    public static void drawWindow(GuiGraphicsExtractor graphics, double x, double y, double opacity) {
        shape(graphics, WINDOW, x, y, 428, 320, Theme.number("Corner Radius", 10),
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Panel Opacity", 1)),
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Panel Opacity", 1)), 0);
        drawRoundedBorder(graphics, x, y, 428, 320, 10,
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Border Opacity", 0.08)));
    }
    public static void drawRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        int rgb = color & 0xFFFFFF;
        int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
        boolean surface = (r >= 17 && r <= 25 && g >= 24 && g <= 36 && b >= 30 && b <= 44) || rgb == (Theme.SURFACE & 0xFFFFFF) || rgb == (Theme.PANEL & 0xFFFFFF)
                || rgb == (Theme.BACKGROUND & 0xFFFFFF) || rgb == (Theme.HOVER & 0xFFFFFF);
        if (surface) { color = Theme.opacity(color, Theme.number("Panel Opacity", 1)); }
        shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, 0);
        if (surface) {
            drawRoundedBorder(graphics, x, y, width, height, radius,
                    Theme.opacity(0xFFFFFFFF, (color >>> 24) / 255.0 * Theme.number("Border Opacity", 0.08)));
        }
    }
    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        double thickness = Theme.number("Border Thickness", 1);
        if (thickness > 0) { shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, thickness); }
    }
    public static void drawDropShadow(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        shape(graphics, SHAPE, x, y + 3, width, height, Theme.radius(radius), color, color, -12);
    }
    public static void drawGlow(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, int color) {
        double size = Theme.number("Glow Radius", 5);
        if (Theme.bool("Global Glow", true) && size > 0) {
            shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)), -size);
        }
    }
    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int top, int bottom) {
        shape(graphics, SHAPE, x, y, width, height, 0, top, bottom, 0);
    }
    private static void shape(GuiGraphicsExtractor graphics, RenderPipeline pipeline, double x, double y,
            double width, double height, double radius, int top, int bottom, double effect) {
        if (width <= 0 || height <= 0 || (top >>> 24) == 0) { return; }
        double pad = effect < 0 ? -effect + 1 : 1;
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int code = effect < 0 ? -(int) Math.round(-effect * 256) - 1 : (int) Math.round(effect * 256);
        UiMesh.quad(graphics, pipeline, TextureSetup.noTexture(), x - pad, y - pad, width + pad * 2,
                height + pad * 2, (float) (-width / 2 - pad), (float) (-height / 2 - pad),
                (float) (width / 2 + pad), (float) (height / 2 + pad), top, bottom,
                (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(radius * 256), code);
    }
    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color) { drawText(graphics, font, text, x, y, color, false, true); }
    public static void drawText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            int color, boolean title, boolean shadow) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y);
        CustomTextRenderer.draw(graphics, font, text, color, title, shadow, false);
        graphics.pose().popMatrix();
    }
    public static void accentText(GuiGraphicsExtractor graphics, Font font, String text, double x,
            double y, double scale, double opacity) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, Theme.opacity(Theme.accent(), opacity), false, true, true);
        graphics.pose().popMatrix();
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            double scale, int color) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, color, false, true, false); graphics.pose().popMatrix();
    }
    public static int textWidth(Font font, String text, boolean title) { return CustomTextRenderer.width(font, text, title); }
    public static void line(GuiGraphicsExtractor graphics, double x1, double y1, double x2, double y2, double thickness, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) { return; }
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x1, (float) y1);
        graphics.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
        drawRoundedRect(graphics, 0, -thickness / 2, length, thickness, thickness / 2, color); graphics.pose().popMatrix();
    }
    public static String ellipsize(Font font, String text, double width) {
        if (textWidth(font, text, false) <= width) { return text; }
        String shortened = text;
        while (!shortened.isEmpty() && textWidth(font, shortened + "…", false) > width) {
            shortened = shortened.substring(0, shortened.offsetByCodePoints(shortened.length(), -1));
        }
        return shortened + "…";
    }
    public static void scissorPush(GuiGraphicsExtractor graphics, double x, double y, double width, double height) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }
    public static void scissorPop(GuiGraphicsExtractor graphics) { graphics.disableScissor(); }
}
```

### src/main/java/dev/riserecode/util/BlurUtil.java

```java
package dev.riserecode.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.buffers.Std140Builder;
import org.lwjgl.system.MemoryStack;

/** Scene-only half-resolution blur. Extraction requests effects; the render hook executes them
 * after the world render and before all GUI/HUD draws, avoiding recursive menu blur and readbacks.
 */
public final class BlurUtil {
    private static final class Pipelines {
        private static final RenderPipeline BLUR = RenderPipelines.register(RenderPipeline.builder()
                .withLocation(UiMesh.id("pipeline/blur")).withVertexShader("core/screenquad")
                .withFragmentShader(UiMesh.id("core/blur")).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(Optional.empty()).withCull(false)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
        private static final RenderPipeline BOX = UiMesh.pipeline("blur_box", "blur_box");
    }
    private static TextureTarget snapshot, ping, gui, world;
    private static final GpuBuffer[] PARAMETERS = new GpuBuffer[6];
    private static boolean guiRequested, worldRequested;
    private static double requestedGuiRadius, requestedWorldRadius, worldOpacity;
    private static int parameterIndex, allocations;
    private static long frames;
    private static double lastSubmitMilliseconds;
    private BlurUtil() { }
    public static int getAllocationCount() { return allocations; }
    public static long getRenderedFrames() { return frames; }
    public static double getLastSubmitMilliseconds() { return lastSubmitMilliseconds; }
    public static int getTargetWidth() { return snapshot == null ? 0 : snapshot.width; }
    public static int getTargetHeight() { return snapshot == null ? 0 : snapshot.height; }
    private static void ensureTargets() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, (minecraft.getWindow().getScreenWidth() + 1) / 2);
        int height = Math.max(1, (minecraft.getWindow().getScreenHeight() + 1) / 2);
        if (snapshot != null && snapshot.width == width && snapshot.height == height) { return; }
        destroyTargets();
        snapshot = new TextureTarget("risecode scene / half", width, height, false, GpuFormat.RGBA8_UNORM);
        ping = new TextureTarget("risecode blur / ping", width, height, false, GpuFormat.RGBA8_UNORM);
        gui = new TextureTarget("risecode blur / GUI", width, height, false, GpuFormat.RGBA8_UNORM);
        world = new TextureTarget("risecode blur / world", width, height, false, GpuFormat.RGBA8_UNORM);
        allocations++;
    }
    public static void drawGui(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, double openingOpacity) {
        if (Theme.bool("World Blur", false) && Theme.number("World Blur Radius", 8) > 0) {
            ensureTargets(); worldRequested = true;
            requestedWorldRadius = Theme.number("World Blur Radius", 8); worldOpacity = MathUtil.clamp01(openingOpacity);
        }
        drawBox(graphics, x, y, width, height, radius,
                openingOpacity * Theme.number("GUI Blur Opacity", 0.8));
    }
    public static void drawBox(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, double opacity) {
        double blur = Theme.number("GUI Blur Radius", 12);
        if (blur <= 0 || opacity <= 0 || width <= 0 || height <= 0) { return; }
        ensureTargets(); guiRequested = true; requestedGuiRadius = blur;
        double pad = 1;
        int color = Theme.opacity(0xFFFFFFFF, opacity);
        UiMesh.quad(graphics, Pipelines.BOX, TextureSetup.singleTexture(gui.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)), x - pad, y - pad, width + pad * 2,
                height + pad * 2, (float) (-width / 2 - pad), (float) (-height / 2 - pad),
                (float) (width / 2 + pad), (float) (height / 2 + pad), color, color,
                (int) Math.round(width * 16), (int) Math.round(height * 16),
                (int) Math.round(Math.min(Theme.radius(radius), Math.min(width, height) / 2) * 256), 0);
    }
    /** Invoked on the render thread by GuiRendererMixin after panorama and before GUI preparation. */
    public static void renderBeforeGui(RenderTarget main) {
        if ((!guiRequested && !worldRequested) || snapshot == null) { return; }
        long start = System.nanoTime(); parameterIndex = 0;
        try {
            pass(main.getColorTextureView(), snapshot, 0, 0, 0, 1);
            if (guiRequested) { gaussian(snapshot, gui, requestedGuiRadius); }
            if (worldRequested) {
                if (guiRequested && requestedWorldRadius == requestedGuiRadius) {
                    pass(gui.getColorTextureView(), main, 0, 0, 2, worldOpacity);
                } else {
                    gaussian(snapshot, world, requestedWorldRadius);
                    pass(world.getColorTextureView(), main, 0, 0, 2, worldOpacity);
                }
            }
            frames++;
        } finally {
            guiRequested = worldRequested = false; lastSubmitMilliseconds = (System.nanoTime() - start) / 1e6;
        }
    }
    private static void gaussian(RenderTarget source, RenderTarget destination, double radius) {
        // The farthest bilinear tap lies at 3.230769 source texels; radius is in full framebuffer pixels.
        float spread = (float) (radius / (2 * 3.230769));
        pass(source.getColorTextureView(), ping, spread / snapshot.width, 0, 1, 1);
        pass(ping.getColorTextureView(), destination, 0, spread / snapshot.height, 1, 1);
    }
    private static void pass(GpuTextureView source, RenderTarget destination, float dx, float dy, int mode, double opacity) {
        int slot = parameterIndex++;
        if (PARAMETERS[slot] == null) {
            PARAMETERS[slot] = RenderSystem.getDevice().createBuffer(() -> "risecode blur parameters",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bytes = Std140Builder.onStack(stack, 16).putVec4(dx, dy, mode, (float) opacity).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMETERS[slot].slice(), bytes);
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "risecode half-resolution blur", destination.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(Pipelines.BLUR); pass.setUniform("BlurParams", PARAMETERS[slot]);
            pass.bindTexture("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }
    private static void destroyTargets() {
        if (snapshot != null) {
            snapshot.destroyBuffers(); ping.destroyBuffers(); gui.destroyBuffers(); world.destroyBuffers();
            snapshot = ping = gui = world = null;
        }
    }
    public static void close() {
        destroyTargets();
        for (int i = 0; i < PARAMETERS.length; i++) { if (PARAMETERS[i] != null) { PARAMETERS[i].close(); PARAMETERS[i] = null; } }
        guiRequested = worldRequested = false;
    }
}
```

### src/main/java/dev/riserecode/util/MathUtil.java

```java
package dev.riserecode.util;

/** Stateless easing functions for normalized animation time. */
public final class MathUtil {
    private MathUtil() {
    }

    public enum Easing {
        OUT_QUART,
        IN_OUT_CUBIC,
        OUT_ELASTIC,
        LINEAR,
        OUT_BOUNCE;

        public double apply(double progress) {
            return switch (this) {
                case OUT_QUART -> easeOutQuart(progress);
                case IN_OUT_CUBIC -> easeInOutCubic(progress);
                case OUT_ELASTIC -> easeOutElastic(progress);
                case LINEAR -> clamp01(progress);
                case OUT_BOUNCE -> easeOutBounce(progress);
            };
        }
    }

    /** Clamps finite input to [0, 1]; invalid time values fail early. */
    public static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Progress must be finite");
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static double easeOutQuart(double progress) {
        double inverse = 1.0 - clamp01(progress);
        double squared = inverse * inverse;
        return 1.0 - squared * squared;
    }

    public static double easeInOutCubic(double progress) {
        double t = clamp01(progress);
        if (t < 0.5) {
            return 4.0 * t * t * t;
        }
        double inverse = 1.0 - t;
        return 1.0 - 4.0 * inverse * inverse * inverse;
    }

    /** Output deliberately overshoots 1 to provide the requested bounce. */
    public static double easeOutElastic(double progress) {
        double t = clamp01(progress);
        if (t == 0.0 || t == 1.0) {
            return t;
        }
        double period = (2.0 * Math.PI) / 3.0;
        return Math.pow(2.0, -10.0 * t) * Math.sin((10.0 * t - 0.75) * period) + 1.0;
    }

    public static double easeOutBounce(double progress) {
        double t = clamp01(progress);
        double n = 7.5625, d = 2.75;
        if (t < 1 / d) { return n * t * t; }
        if (t < 2 / d) { t -= 1.5 / d; return n * t * t + 0.75; }
        if (t < 2.5 / d) { t -= 2.25 / d; return n * t * t + 0.9375; }
        t -= 2.625 / d; return n * t * t + 0.984375;
    }

    /** Unclamped interpolation: elastic easing needs values beyond [0, 1]. */
    public static double lerp(double start, double end, double amount) {
        return Math.fma(amount, end - start, start);
    }
}
```

### src/main/java/dev/riserecode/ui/animation/Animation.java

```java
package dev.riserecode.ui.animation;

import dev.riserecode.util.MathUtil;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A render-thread animation with explicit delta time, measured in seconds.
 * Retargeting starts at the current rendered value, preserving position.
 * Call update once per frame and read getValue freely between updates.
 */
public final class Animation {
    public enum State {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private static DoubleSupplier uiSpeed = () -> 1;
    private static Supplier<MathUtil.Easing> uiCurve = () -> null;
    private boolean themed;
    private MathUtil.Easing activeCurve;
    private double segmentDuration;
    public static void configureUi(DoubleSupplier speed, Supplier<MathUtil.Easing> curve) {
        uiSpeed = Objects.requireNonNull(speed); uiCurve = Objects.requireNonNull(curve);
    }
    /** Ordinary animations stay deterministic; only UI instances adopt theme preferences. */
    public static Animation ui(double initialValue, double duration, MathUtil.Easing easing) {
        Animation result = new Animation(initialValue, duration, easing); result.themed = true; return result;
    }
    private final double durationSeconds;
    private final MathUtil.Easing easing;
    private double value;
    private double startValue;
    private double targetValue;
    private double elapsedSeconds;
    private State state = State.IDLE;

    public Animation(double initialValue, double durationSeconds, MathUtil.Easing easing) {
        requireFinite(initialValue, "Initial value");
        requireFinite(durationSeconds, "Duration");
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        this.durationSeconds = durationSeconds;
        this.easing = Objects.requireNonNull(easing, "easing");
        activeCurve = easing; segmentDuration = durationSeconds;
        value = startValue = targetValue = initialValue;
    }

    /** Repeated requests for the same target do not restart the animation. */
    public Animation animateTo(double target) {
        requireFinite(target, "Target");
        if (!Double.isFinite(target - value)) {
            throw new IllegalArgumentException("Animation range must be finite");
        }
        if (target == targetValue) {
            return this;
        }
        startValue = value;
        segmentDuration = durationSeconds; activeCurve = selectedCurve();
        targetValue = target;
        elapsedSeconds = 0.0;
        state = State.RUNNING;
        if (durationSeconds == 0.0 || startValue == targetValue) {
            finish();
        }
        return this;
    }

    public double update(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Delta time cannot be negative");
        }
        if (state != State.RUNNING) {
            return value;
        }
        MathUtil.Easing selected = selectedCurve();
        if (activeCurve != selected) {
            segmentDuration = Math.max(0, segmentDuration - elapsedSeconds);
            startValue = value; elapsedSeconds = 0; activeCurve = selected;
        }
        double speed = themed ? Math.max(0.5, Math.min(3, uiSpeed.getAsDouble())) : 1;
        elapsedSeconds = Math.min(segmentDuration, elapsedSeconds + deltaSeconds * speed);
        // Avoid an extra frame caused only by accumulated floating-point error.
        double tolerance = Math.min(4.0 * Math.ulp(segmentDuration), segmentDuration * 1.0e-12);
        if (segmentDuration - elapsedSeconds <= tolerance) {
            finish();
        } else {
            value = MathUtil.lerp(startValue, targetValue, activeCurve.apply(getProgress()));
        }
        return value;
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    public void finish() {
        value = targetValue;
        elapsedSeconds = segmentDuration;
        state = State.FINISHED;
    }

    /** Immediately establishes a new idle value and discards the old target. */
    public void reset(double initialValue) {
        requireFinite(initialValue, "Initial value");
        value = startValue = targetValue = initialValue;
        elapsedSeconds = 0.0;
        state = State.IDLE;
    }

    public double getValue() {
        return value;
    }

    public double getTarget() {
        return targetValue;
    }

    public double getProgress() {
        if (segmentDuration == 0.0) {
            return state == State.FINISHED ? 1.0 : 0.0;
        }
        return elapsedSeconds / segmentDuration;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    private MathUtil.Easing selectedCurve() {
        MathUtil.Easing selected = themed ? uiCurve.get() : null;
        return selected == null ? easing : selected;
    }
    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
```

### src/main/java/dev/riserecode/ui/component/ThemeEditor.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Scrollable two-column visual controls, with independent named theme profiles and live preview. */
public final class ThemeEditor extends UIComponent {
    private record Control(ThemeManager.Control model, UIComponent view) { }
    private final ThemeManager manager;
    private final Map<String, List<Control>> groups = new LinkedHashMap<>();
    private final List<ActionButton> actions = new ArrayList<>();
    private final TextField name = new TextField(() -> "Theme name");
    private final Animation scroll = Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART);
    private UIComponent captured;
    private double maxScroll;
    public ThemeEditor(ThemeManager manager, Consumer<String> notice) {
        this.manager = manager; name.setValue("default");
        for (ThemeManager.Control model : manager.getControls()) {
            var setting = model.setting(); UIComponent view;
            if (setting instanceof BooleanSetting bool) { view = new ToggleSwitch(bool, manager::changed); }
            else if (setting instanceof DoubleSetting number) { view = new Slider(number, manager::changed, model.step()); }
            else if (setting instanceof EnumSetting<?> option) { view = enumView(option, manager::changed); }
            else if (setting instanceof ColorSetting color) { view = new ColorPicker(color, manager::changed); }
            else { throw new IllegalArgumentException("Missing theme control " + setting.getName()); }
            groups.computeIfAbsent(model.group(), ignored -> new ArrayList<>()).add(new Control(model, view));
        }
        actions.add(new ActionButton("Save", () -> {
            try { notice.accept(manager.saveProfile(name.getValue()) ? "Theme saved" : "Could not save theme"); }
            catch (IllegalArgumentException invalid) { notice.accept(invalid.getMessage()); }
        }));
        actions.add(new ActionButton("Load", () -> {
            try { notice.accept(manager.loadProfile(name.getValue()) ? "Theme loaded" : "Theme not found or invalid"); }
            catch (IllegalArgumentException invalid) { notice.accept(invalid.getMessage()); }
        }));
        actions.add(new ActionButton("Reset", () -> { manager.reset(); notice.accept("Default theme restored"); }));
        actions.add(new ActionButton("Fonts", () -> { CustomTextRenderer.close(); notice.accept("Fonts reloaded from resources and local files"); }));
    }
    private static <E extends Enum<E>> UIComponent enumView(EnumSetting<E> option, Runnable changed) { return new EnumSelector<>(option, changed); }
    public UIComponent getControl(String label) {
        return groups.values().stream().flatMap(List::stream).filter(control -> control.model.setting().getName().equals(label))
                .map(Control::view).findFirst().orElseThrow();
    }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public TextField getProfileField() { return name; }
    public List<ActionButton> getActions() { return List.copyOf(actions); }
    public void reveal(String label) {
        double desired = getControl(label).getBounds().y() - y - 79 + scroll.getValue();
        scroll.animateTo(Math.max(0, Math.min(maxScroll, desired)));
    }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double pad = manager.number("Content Padding");
        double actionWidth = 43, actionGap = 5, toolbarWidth = actions.size() * (actionWidth + actionGap);
        name.setBounds(x, y, Math.max(60, width - toolbarWidth - 8), 22); name.render(graphics, font, mouseX, mouseY, seconds, opacity);
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).setBounds(x + width - toolbarWidth + i * (actionWidth + actionGap), y, actionWidth, 22);
            actions.get(i).render(graphics, font, mouseX, mouseY, seconds, opacity);
        }
        double viewportY = y + 31, viewportHeight = height - 31;
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget())));
        double offset = Math.max(0, scroll.update(seconds));
        double top = viewportY - offset;
        RenderUtil.scissorPush(graphics, x, viewportY, width, viewportHeight);
        try {
            for (var entry : groups.entrySet()) {
                String group = entry.getKey(); List<Control> values = entry.getValue();
                double row = Math.max(28, 18 * manager.number("Font Scale") + 12);
                double titleHeight = group.equals("Typography") ? 43 : 29;
                double innerWidth = width - pad * 2, gap = Math.max(6, pad), columnWidth = (innerWidth - gap) / 2;
                double content = 0;
                for (int i = 0; i < values.size(); i += 2) {
                    double rowHeight = values.get(i).view instanceof ColorPicker ? 82 : row;
                    if (i + 1 < values.size() && values.get(i + 1).view instanceof ColorPicker) { rowHeight = 82; }
                    content += rowHeight + 3;
                }
                double cardHeight = titleHeight + content + pad;
                RenderUtil.drawRoundedRect(graphics, x, top, width - 4, cardHeight, 5, Theme.opacity(Theme.SURFACE, opacity));
                RenderUtil.accentText(graphics, font, group, x + pad, top + 8, 0.95, opacity);
                if (group.equals("Typography")) {
                    RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, CustomTextRenderer.status(), (innerWidth - 4) / 0.68),
                            x + pad, top + 26, 0.68, Theme.opacity(Theme.SECONDARY, opacity));
                }
                double controlY = top + titleHeight;
                for (int i = 0; i < values.size(); i += 2) {
                    double rowHeight = values.get(i).view instanceof ColorPicker ? 82 : row;
                    if (i + 1 < values.size() && values.get(i + 1).view instanceof ColorPicker) { rowHeight = 82; }
                    for (int column = 0; column < 2 && i + column < values.size(); column++) {
                        UIComponent control = values.get(i + column).view;
                        control.setBounds(x + pad + column * (columnWidth + gap), controlY, columnWidth - 4, rowHeight);
                        control.render(graphics, font, mouseY < viewportY || mouseY >= y + height ? -1e6 : mouseX,
                                mouseY < viewportY || mouseY >= y + height ? -1e6 : mouseY, seconds, opacity);
                    }
                    controlY += rowHeight + 3;
                }
                top += cardHeight + pad;
            }
            maxScroll = Math.max(0, top + offset - viewportY - viewportHeight);
        } finally { RenderUtil.scissorPop(graphics); }
        if (maxScroll > 0) {
            double thumbHeight = Math.max(18, viewportHeight * viewportHeight / (viewportHeight + maxScroll));
            double thumbY = viewportY + MathUtil.clamp01(offset / maxScroll) * (viewportHeight - thumbHeight);
            RenderUtil.drawRoundedRect(graphics, x + width - 2, thumbY, 1.5, thumbHeight, 0.75,
                    Theme.opacity(Theme.accent(), opacity * 0.65));
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) { name.setFocused(false); return false; }
        if (name.mouseClicked(mouseX, mouseY, button)) { return true; }
        for (ActionButton action : actions) { if (action.mouseClicked(mouseX, mouseY, button)) { return true; } }
        if (mouseY < y + 31) { return true; }
        for (var group : groups.values()) {
            for (Control control : group) {
                if (control.view.mouseClicked(mouseX, mouseY, button)) { captured = control.view; return true; }
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; } boolean result = captured.mouseReleased(mouseX, mouseY, button); captured = null; return result;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!contains(mouseX, mouseY)) { return false; }
        mouseReleased(0, 0, 0); scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - vertical * 32))); return true;
    }
    public void unfocus() { name.setFocused(false); mouseReleased(0, 0, 0); }
    public boolean charTyped(CharacterEvent event) { return name.charTyped(event); }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) { return name.keyPressed(event, clipboard, copy); }
}
```

### src/main/java/dev/riserecode/ui/component/setting/ColorPicker.java

```java
package dev.riserecode.ui.component.setting;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Continuous HSV picker with mouse capture and no per-frame texture uploads. */
public final class ColorPicker extends UIComponent {
    private static final class Pipelines { private static final RenderPipeline HSV = UiMesh.pipeline("hsv", "hsv"); }
    private final ColorSetting setting;
    private final Runnable changed;
    private double hue, saturation, value;
    private int displayed;
    private int captured;
    public ColorPicker(ColorSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; readColor(); }
    private void readColor() {
        displayed = setting.getValue();
        double r = ((displayed >>> 16) & 255) / 255.0, g = ((displayed >>> 8) & 255) / 255.0, b = (displayed & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        value = max; saturation = max == 0 ? 0 : delta / max;
        if (delta > 0) {
            double h = max == r ? (g - b) / delta : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
            hue = (h / 6 + 1) % 1;
        }
    }
    public static int hsv(double hue, double saturation, double value) {
        hue = hue - Math.floor(hue); saturation = MathUtil.clamp01(saturation); value = MathUtil.clamp01(value);
        double h = hue * 6, c = value * saturation, second = c * (1 - Math.abs(h % 2 - 1)), m = value - c;
        double r, g, b;
        switch ((int) h) {
            case 0 -> { r = c; g = second; b = 0; }
            case 1 -> { r = second; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = second; }
            case 3 -> { r = 0; g = second; b = c; }
            case 4 -> { r = second; g = 0; b = c; }
            default -> { r = c; g = 0; b = second; }
        }
        return 0xFF000000 | (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        if (setting.getValue() != displayed) { readColor(); }
        String label = setting.getName();
        double scale = Math.min(0.85, (width - 3) / Math.max(1, RenderUtil.textWidth(font, label, false)));
        RenderUtil.text(graphics, font, label, x, y, scale, Theme.opacity(Theme.TEXT, opacity));
        double top = y + 17, boxHeight = height - 30, boxWidth = width - 20;
        int white = Theme.opacity(0xFFFFFFFF, opacity);
        UiMesh.quad(graphics, Pipelines.HSV, TextureSetup.noTexture(), x, top, boxWidth, boxHeight, 0, 0, 1, 1,
                white, white, (int) Math.round(hue * 30000), 0, 0, 0);
        UiMesh.quad(graphics, Pipelines.HSV, TextureSetup.noTexture(), x + width - 12, top, 12, boxHeight, 0, 0, 1, 1,
                white, white, 0, 1, 0, 0);
        double dotX = x + saturation * boxWidth, dotY = top + (1 - value) * boxHeight;
        RenderUtil.drawRoundedRect(graphics, dotX - 2.5, dotY - 2.5, 5, 5, 2.5, white);
        RenderUtil.drawRoundedRect(graphics, dotX - 1.5, dotY - 1.5, 3, 3, 1.5, Theme.opacity(setting.getValue(), opacity));
        RenderUtil.drawRoundedRect(graphics, x + width - 13, top + hue * boxHeight - 1, 14, 2, 0.5, white);
        RenderUtil.text(graphics, font, String.format(Locale.ROOT, "#%06X", setting.getValue() & 0xFFFFFF),
                x, y + height - 9, 0.72, Theme.opacity(Theme.SECONDARY, opacity));
    }
    private void update(double mouseX, double mouseY) {
        double top = y + 17, boxHeight = height - 30;
        if (captured == 1) { saturation = MathUtil.clamp01((mouseX - x) / (width - 20)); value = 1 - MathUtil.clamp01((mouseY - top) / boxHeight); }
        else if (captured == 2) { hue = MathUtil.clamp01((mouseY - top) / boxHeight); }
        displayed = hsv(hue, saturation, value); setting.setValue(displayed); changed.run();
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY) || mouseY < y + 17 || mouseY > y + height - 13) { return false; }
        captured = mouseX >= x + width - 12 ? 2 : mouseX < x + width - 20 ? 1 : 0;
        if (captured == 0) { return false; } update(mouseX, mouseY); return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button) { if (captured == 0 || button != 0) { return false; } update(mouseX, mouseY); return true; }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { boolean result = captured != 0; captured = 0; return result; }
}
```

### src/main/java/dev/riserecode/ui/clickgui/ClickGui.java

```java
package dev.riserecode.ui.clickgui;

import com.google.gson.JsonObject;
import dev.riserecode.Client;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.Icon;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.Sidebar;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.ThemeEditor;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native 26.2 screen hosting an original, fully extracted sidebar/card interface. */
public final class ClickGui extends Screen {
    public enum Page { SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, PROFILES, FRIENDS, THEMES, LANGUAGE }
    private static final double WINDOW_WIDTH = 428;
    private static final double WINDOW_HEIGHT = 320;
    private final List<Panel> panels = new ArrayList<>();
    private final Panel searchPanel;
    private final Sidebar sidebar;
    private final TextField field = new TextField(this::placeholder);
    private final ToggleSwitch hudToggle;
    private final Runnable saveChanges;
    private final List<ActionButton> actions = new ArrayList<>();
    private final Animation opening = Animation.ui(0, 0.26, MathUtil.Easing.OUT_QUART);
    private final Animation canvasZoom = Animation.ui(1, 0.24, MathUtil.Easing.OUT_QUART);
    private final Animation pageFade = Animation.ui(1, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation positionX = Animation.ui(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation positionY = Animation.ui(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation utilityScroll = Animation.ui(0, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation feedback = Animation.ui(0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
    private Screen parent;
    private Page page = Page.SEARCH;
    private UIComponent captured;
    private final ThemeManager themes;
    private final ThemeEditor themeEditor;
    private boolean closing;
    private boolean dragging;
    private double dragX;
    private double dragY;
    private double offsetX;
    private double offsetY;
    private double windowX;
    private double windowY;
    private double canvasScale = 1;
    private double preferredScale = 1;
    private double canvasWidth;
    private double canvasHeight;
    private double maxUtilityScroll;
    private long previousFrame;
    private double messageTime;
    private String message = "";

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal(Client.NAME));
        this.saveChanges = saveChanges;
        themes = Client.INSTANCE.getThemeManager();
        themeEditor = new ThemeEditor(themes, this::notice);
        searchPanel = new Panel(null, modules, saveChanges);
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
        sidebar = new Sidebar(List.of(
                new Sidebar.Item("Search", Icon.SEARCH), new Sidebar.Item("Combat", Icon.COMBAT),
                new Sidebar.Item("Movement", Icon.MOVEMENT), new Sidebar.Item("Player", Icon.PLAYER),
                new Sidebar.Item("Render", Icon.RENDER), new Sidebar.Item("World", Icon.WORLD),
                new Sidebar.Item("Utility", Icon.UTILITY), new Sidebar.Item("Profiles", Icon.PROFILES),
                new Sidebar.Item("Friends", Icon.FRIENDS), new Sidebar.Item("Themes", Icon.THEMES),
                new Sidebar.Item("Language", Icon.LANGUAGE)), index -> selectPage(Page.values()[index]));
        hudToggle = new ToggleSwitch("HUD", hud::isEnabled, next -> { hud.setEnabled(next); saveChanges.run(); });
        loadPreferences();
    }
    public List<Panel> getPanels() { return List.copyOf(panels); }
    public Panel getActivePanel() {
        if (page == Page.SEARCH) { return searchPanel; }
        if (page.ordinal() > Page.UTILITY.ordinal()) { return null; }
        Category category = Category.valueOf(page.name());
        return panels.stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow();
    }
    public ModuleButton getModuleButton(String name) {
        Panel panel = getActivePanel();
        if (panel == null) { throw new IllegalStateException("The selected page has no modules"); }
        return panel.getModuleButtons().stream().filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    public ThemeEditor getThemeEditor() { return themeEditor; }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public List<ActionButton> getActionButtons() { return List.copyOf(actions); }
    public Sidebar getSidebar() { return sidebar; }
    public TextField getSearchField() { return field; }
    public Page getPage() { return page; }
    public double getCanvasScale() { return canvasScale; }
    public boolean isOpen() { return !closing && !opening.isRunning() && opening.getValue() == 1; }
    public UIComponent.Bounds getWindowBounds() { return new UIComponent.Bounds(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT); }

    private void loadPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        try {
            if (ui.has("german")) { Theme.setGerman(ui.get("german").getAsBoolean()); }
            preferredScale = themes.number("Interface Scale");
            if (ui.has("offsetX")) { offsetX = finite(ui.get("offsetX").getAsDouble(), -2000, 2000); }
            if (ui.has("offsetY")) { offsetY = finite(ui.get("offsetY").getAsDouble(), -2000, 2000); }
        } catch (RuntimeException invalid) { Client.INSTANCE.getLogger().warn("Ignoring invalid UI preference: {}", invalid.getMessage()); }
    }
    private static double finite(double value, double min, double max) {
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("Non-finite UI value"); }
        return Math.max(min, Math.min(max, value));
    }
    private void persistPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        ui.remove("accent"); ui.remove("scale"); ui.addProperty("german", Theme.isGerman());
        ui.addProperty("offsetX", offsetX); ui.addProperty("offsetY", offsetY);
        Client.INSTANCE.getConfigManager().setUiPreferences(ui);
    }
    public void refresh() {
        releaseCapture(); searchPanel.refresh(); panels.forEach(Panel::refresh);
        if (page == Page.PROFILES || page == Page.FRIENDS) { rebuildActions(); }
    }
    public void selectCategory(Category category) { selectPage(Page.valueOf(category.name())); }
    public void selectPage(Page next) {
        if (closing || next == page) { return; }
        releaseCapture(); page = next; sidebar.setSelected(next.ordinal());
        themeEditor.unfocus();
        field.setValue(""); field.setFocused(next == Page.SEARCH || next == Page.PROFILES || next == Page.FRIENDS);
        searchPanel.setQuery(""); utilityScroll.reset(0); pageFade.reset(0); pageFade.animateTo(1);
        Panel panel = getActivePanel(); if (panel != null) { panel.open(); }
        rebuildActions();
    }
    public void open(Screen parent) {
        this.parent = parent; closing = false; previousFrame = System.nanoTime();
        opening.reset(0); opening.animateTo(1); searchPanel.open(); panels.forEach(Panel::open);
        field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        minecraft.gui.setScreen(this);
    }
    @Override
    protected void init() {
        canvasScale = fittedScale(); canvasZoom.reset(canvasScale);
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        clampOffsets();
        windowX = (canvasWidth - WINDOW_WIDTH) / 2 + offsetX; windowY = (canvasHeight - WINDOW_HEIGHT) / 2 + offsetY;
        positionX.reset(windowX); positionY.reset(windowY); layout(); rebuildActions();
    }
    private double fittedScale() {
        return Math.max(0.1, Math.min(preferredScale * height / 576.0, Math.min((width - 20.0) / WINDOW_WIDTH, (height - 20.0) / WINDOW_HEIGHT)));
    }
    private void clampOffsets() {
        offsetX = Math.max(-(canvasWidth - WINDOW_WIDTH) / 2 + 8, Math.min((canvasWidth - WINDOW_WIDTH) / 2 - 8, offsetX));
        offsetY = Math.max(-(canvasHeight - WINDOW_HEIGHT) / 2 + 8, Math.min((canvasHeight - WINDOW_HEIGHT) / 2 - 8, offsetY));
    }
    private void layout() {
        sidebar.setBounds(windowX, windowY, 107, 320);
        double pad = themes.number("Content Padding");
        double left = windowX + 107 + pad, contentWidth = WINDOW_WIDTH - 107 - pad * 2;
        field.setBounds(left, windowY + pad - 1, contentWidth, 27);
        searchPanel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        for (Panel panel : panels) { panel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2); }
        themeEditor.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        hudToggle.setBounds(windowX + 129, windowY + 234, 265, 17);
    }
    private String placeholder() {
        return switch (page) {
            case SEARCH -> Theme.label("Start typing to search...");
            case PROFILES -> Theme.isGerman() ? "Profilname eingeben..." : "Enter a profile name...";
            case FRIENDS -> Theme.isGerman() ? "Spielername eingeben..." : "Enter a player name...";
            default -> Theme.label(page.name().substring(0, 1) + page.name().substring(1).toLowerCase(java.util.Locale.ROOT));
        };
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) { extractPanorama(graphics, delta); }
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime(); Theme.advance(now); double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9); previousFrame = now;
        preferredScale = themes.number("Interface Scale"); canvasZoom.animateTo(fittedScale());
        canvasScale = Math.max(0.1, canvasZoom.update(seconds));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        double alpha = opening.update(seconds);
        if (closing && !opening.isRunning()) { minecraft.gui.setScreen(parent); return; }
        clampOffsets();
        positionX.animateTo((canvasWidth - WINDOW_WIDTH) / 2 + offsetX);
        positionY.animateTo((canvasHeight - WINDOW_HEIGHT) / 2 + offsetY);
        windowX = positionX.update(seconds); windowY = positionY.update(seconds); layout();
        double pointerX = pointerX(mouseX); double pointerY = pointerY(mouseY);
        double scale = windowScale();
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        graphics.pose().translate((float) (windowX + WINDOW_WIDTH / 2), (float) (windowY + WINDOW_HEIGHT / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-windowX - WINDOW_WIDTH / 2), (float) (-windowY - WINDOW_HEIGHT / 2));
        try {
            RenderUtil.drawDropShadow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10,
                    Theme.opacity(Theme.SHADOW, alpha * 0.7));
            BlurUtil.drawGui(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10, alpha);
            RenderUtil.drawWindow(graphics, windowX, windowY, alpha);
            sidebar.render(graphics, font, pointerX, pointerY, seconds, alpha);
            double contentAlpha = alpha * pageFade.update(seconds);
            if (page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) {
                field.render(graphics, font, pointerX, pointerY, seconds, contentAlpha);
            } else {
                String title = placeholder();
                RenderUtil.text(graphics, font, title, windowX + 267.5 - RenderUtil.textWidth(font, title, false) / 2,
                        windowY + 18, 1, Theme.opacity(Theme.TEXT, contentAlpha));
            }
            Panel panel = getActivePanel();
            if (panel != null) {
                if (page == Page.SEARCH) { panel.setQuery(field.getValue()); }
                panel.render(graphics, font, closing ? -1e6 : pointerX, closing ? -1e6 : pointerY, seconds, contentAlpha);
            } else { renderUtility(graphics, pointerX, pointerY, seconds, contentAlpha); }
            messageTime = Math.max(0, messageTime - seconds); feedback.animateTo(messageTime > 0 ? 1 : 0);
            double notice = feedback.update(seconds);
            if (notice > 0) {
                double noticeWidth = Math.min(292, RenderUtil.textWidth(font, message, false) * 0.8 + 18);
                RenderUtil.drawDropShadow(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SHADOW, alpha * notice));
                RenderUtil.drawRoundedRect(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SURFACE, alpha * notice));
                RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, message, (noticeWidth - 18) / 0.8), windowX + 128,
                        windowY + 296, 0.8, Theme.opacity(Theme.accent(), alpha * notice));
            }
        } finally { graphics.pose().popMatrix(); }
    }
    private void notice(String text) { message = text; messageTime = 3; }
    private void rebuildActions() {
        actions.clear();
        if (page == Page.PROFILES) {
            actions.add(new ActionButton("Save", () -> {
                try { notice(Client.INSTANCE.getProfileManager().saveProfile(field.getValue()) ? "Profile saved" : "Could not save profile"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (String profile : Client.INSTANCE.getProfileManager().getProfiles()) {
                actions.add(new ActionButton(profile, () -> notice(Client.INSTANCE.getProfileManager().loadProfile(profile)
                        ? "Loaded " + profile : "Could not load " + profile)));
            }
        } else if (page == Page.FRIENDS) {
            actions.add(new ActionButton("Add", () -> {
                try { Client.INSTANCE.getFriendManager().add(field.getValue()); field.setValue(""); notice("Friend added"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (var friend : Client.INSTANCE.getFriendManager().getFriends()) {
                actions.add(new ActionButton(friend.name() + "  ×", () -> { Client.INSTANCE.getFriendManager().remove(friend.name()); notice("Friend removed"); rebuildActions(); }));
            }
        } else if (page == Page.LANGUAGE) {
            actions.add(new ActionButton("English", () -> { Theme.setGerman(false); persistPreferences(); }));
            actions.add(new ActionButton("Deutsch", () -> { Theme.setGerman(true); persistPreferences(); }));
        }
    }
    private void renderUtility(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double seconds, double alpha) {
        double left = windowX + 115; double top = windowY + 38;
        if (page == Page.THEMES) {
            themeEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else if (page == Page.LANGUAGE) {
            card(graphics, left, top, 305, 84, "Interface language", "Module identifiers and commands keep their original names.", alpha);
            for (int i = 0; i < actions.size(); i++) {
                actions.get(i).setBounds(left + 8 + i * 145, top + 45, 137, 25);
                actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                if (Theme.isGerman() == (i == 1)) { RenderUtil.drawRoundedBorder(graphics, left + 8 + i * 145, top + 45, 137, 25, 5, Theme.opacity(Theme.accent(), alpha * 0.7)); }
            }
        } else {
            maxUtilityScroll = Math.max(0, actions.size() * 35.0 + 25 - 274);
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget())));
            double scroll = utilityScroll.update(seconds);
            RenderUtil.scissorPush(graphics, left, top, 305, 274);
            try {
                for (int i = 0; i < actions.size(); i++) {
                    actions.get(i).setBounds(left, top + i * 35 - scroll, 305, 27);
                    actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                }
                String hint = page == Page.PROFILES ? "Select a profile to load it." : "Select a friend to remove them.";
                RenderUtil.text(graphics, font, hint, left + 7, top + actions.size() * 35 - scroll + 5, 0.8, Theme.opacity(Theme.SECONDARY, alpha));
            } finally { RenderUtil.scissorPop(graphics); }
        }
    }
    private void card(GuiGraphicsExtractor graphics, double x, double y, double w, double h, String title, String description, double alpha) {
        RenderUtil.drawRoundedRect(graphics, x, y, w, h, 5, Theme.opacity(Theme.SURFACE, alpha));
        RenderUtil.text(graphics, font, title, x + 7, y + 7, 1, Theme.opacity(Theme.TEXT, alpha));
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, description, (w - 14) / 0.8), x + 7, y + 25, 0.8, Theme.opacity(Theme.SECONDARY, alpha * 0.8));
    }
    private double windowScale() { return MathUtil.lerp(0.94, 1, opening.getValue()); }
    private double pointerX(double mouseX) { double center = windowX + WINDOW_WIDTH / 2; return center + (mouseX / canvasScale - center) / windowScale(); }
    private double pointerY(double mouseY) { double center = windowY + WINDOW_HEIGHT / 2; return center + (mouseY / canvasScale - center) / windowScale(); }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || opening.getValue() < 0.5) { return true; }
        double x = pointerX(event.x()); double y = pointerY(event.y());
        if (sidebar.mouseClicked(x, y, event.button())) { return true; }
        if ((page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) && field.mouseClicked(x, y, event.button())) { return true; }
        if (x >= windowX && x < windowX + 107 && y >= windowY && y < windowY + 39 && event.button() == 0) {
            dragging = true; dragX = event.x() / canvasScale - windowX; dragY = event.y() / canvasScale - windowY; return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.mouseClicked(x, y, event.button())) { captured = panel; return true; }
        if (page == Page.THEMES && themeEditor.mouseClicked(x, y, event.button())) { captured = themeEditor; return true; }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            for (ActionButton action : List.copyOf(actions)) { if (action.mouseClicked(x, y, event.button())) { return true; } }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (closing) { return true; }
        if (dragging && event.button() == 0) {
            offsetX = event.x() / canvasScale - dragX - (canvasWidth - WINDOW_WIDTH) / 2;
            offsetY = event.y() / canvasScale - dragY - (canvasHeight - WINDOW_HEIGHT) / 2; clampOffsets(); return true;
        }
        return captured != null && captured.mouseDragged(pointerX(event.x()), pointerY(event.y()), event.button());
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean result = dragging || captured != null;
        if (captured != null) { captured.mouseReleased(pointerX(event.x()), pointerY(event.y()), event.button()); }
        if (dragging) { persistPreferences(); }
        dragging = false; captured = null; return result;
    }
    private void releaseCapture() { dragging = false; if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; } }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) { return true; }
        double x = pointerX(mouseX); double y = pointerY(mouseY);
        Panel panel = getActivePanel(); if (panel != null) { return panel.mouseScrolled(x, y, vertical); }
        if (page == Page.THEMES) { return themeEditor.mouseScrolled(x, y, vertical); }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget() - vertical * 24))); return true;
        }
        return false;
    }
    @Override
    public boolean charTyped(CharacterEvent event) { return closing || (page == Page.THEMES ? themeEditor.charTyped(event) : field.charTyped(event)); }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) { return true; }
        if (event.isEscape()) { onClose(); return true; }
        if (event.hasControlDown() && event.key() == 70) { selectPage(Page.SEARCH); field.setFocused(true); return true; }
        if (event.key() == 258) {
            int next = Math.floorMod(sidebar.getSelected() + (event.hasShiftDown() ? -1 : 1), Page.values().length);
            selectPage(Page.values()[next]); return true;
        }
        if (event.isConfirmation() && (page == Page.PROFILES || page == Page.FRIENDS) && !actions.isEmpty()) {
            var b = actions.getFirst().getBounds(); actions.getFirst().mouseClicked(b.centerX(), b.centerY(), 0); return true;
        }
        return page == Page.THEMES ? themeEditor.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)
                : field.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard);
    }
    /** Right Shift may reverse an exit before the screen is removed, preserving the rendered pose. */
    public void toggleVisibility() {
        if (closing) {
            closing = false; opening.animateTo(1);
            field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        } else { onClose(); }
    }
    @Override
    public void onClose() {
        if (closing) { return; }
        releaseCapture(); themeEditor.unfocus(); themes.save(); closing = true; field.setFocused(false); opening.animateTo(0); persistPreferences(); saveChanges.run();
    }
    @Override
    public void removed() { themeEditor.unfocus(); themes.save(); releaseCapture(); persistPreferences(); saveChanges.run(); }
    @Override
    public boolean isPauseScreen() { return false; }
}
```

### src/main/java/dev/riserecode/ui/hud/HudRenderer.java

```java
package dev.riserecode.ui.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer implements AutoCloseable {
    /** Read-only presentation state, useful to future HUD editors. */
    public record EntryState(Module module, double visibility, double y) { }
    public static final double ROW_HEIGHT = 11.5;
    private static final class Entry {
        private final Module module;
        private final Animation y;
        private Animation slide = Animation.ui(0, 0.32, MathUtil.Easing.OUT_QUART);
        private boolean active;
        private int width;

        private Entry(Module module) {
            this.module = module;
            y = Animation.ui(0, 0.24, MathUtil.Easing.OUT_QUART);
        }

        private void update(boolean enabled, double deltaSeconds) {
            if (enabled != active) {
                active = enabled;
                slide = Animation.ui(slide.getValue(), enabled ? 0.32 : 0.22,
                        MathUtil.Easing.OUT_QUART);
                slide.animateTo(enabled ? 1 : 0);
            }
            slide.update(deltaSeconds);
        }
    }

    private final ThemeManager themes;
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final String watermark;
    private final EventBus.Subscription subscription;
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private final Animation visibility = Animation.ui(1, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    private boolean enabled = true;

    public HudRenderer(EventBus eventBus, Minecraft minecraft, ModuleManager modules, String watermark) {
        this(eventBus, minecraft, modules, watermark, null);
    }
    public HudRenderer(EventBus eventBus, Minecraft minecraft, ModuleManager modules, String watermark, ThemeManager themes) {
        this.themes = themes; this.minecraft = minecraft; this.modules = modules; this.watermark = watermark;
        subscription = eventBus.subscribe(Render2DEvent.class, this::render);
    }

    public boolean isEnabled() { return themes == null ? enabled : themes.bool("HUD Visible"); }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (themes != null) { ((BooleanSetting) themes.getSetting("HUD Visible")).setValue(enabled); themes.changed(); }
    }
    public List<EntryState> getEntries() {
        return entries.values().stream().map(entry -> new EntryState(entry.module, entry.slide.getValue(), entry.y.getValue())).toList();
    }

    private void render(Render2DEvent event) {
        Theme.advance(System.nanoTime());
        double seconds = event.getDeltaSeconds();
        visibility.animateTo(isEnabled() ? 1 : 0);
        double opacity = MathUtil.clamp01(visibility.update(seconds));
        for (Module module : modules.getModules()) {
            if (module.isEnabled()) { entries.computeIfAbsent(module, Entry::new); }
        }
        for (Entry entry : entries.values()) { entry.update(entry.module.isEnabled(), seconds); }
        entries.values().removeIf(entry -> !entry.active && !entry.slide.isRunning() && entry.slide.getValue() == 0);
        if (minecraft.font == null) { return; }
        var sorted = new ArrayList<>(entries.values());
        for (Entry entry : sorted) { entry.width = RenderUtil.textWidth(minecraft.font, Theme.displayName(entry.module.getName()), false); }
        sorted.sort(Comparator.comparingInt((Entry entry) -> entry.width).reversed().thenComparing(entry -> entry.module.getName()));
        double padding = themes == null ? 2 : themes.number("ArrayList Padding");
        double spacing = themes == null ? 0 : themes.number("ArrayList Spacing");
        double rowHeight = 7.5 * Theme.number("Font Scale", 1) + padding * 2;
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).y.animateTo(i * (rowHeight + spacing));
            sorted.get(i).y.update(seconds);
        }
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || opacity <= 0) { return; }
        GuiGraphicsExtractor graphics = event.getGraphics();
        // Reference density follows the framebuffer height, independent of vanilla's GUI scale.
        double referenceScale = graphics.guiHeight() / 576.0;
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) referenceScale, (float) referenceScale);
        try {
            double markWidth = RenderUtil.textWidth(minecraft.font, watermark, false) + padding * 2 + 1;
            background(graphics, 3, 3, markWidth, rowHeight + 1, opacity);
            RenderUtil.drawRoundedRect(graphics, 3 + padding, rowHeight + 3, markWidth - padding * 2, 0.7, 0.35,
                    Theme.opacity(Theme.accent(), opacity * 0.7));
            RenderUtil.drawText(graphics, minecraft.font, watermark, 3 + padding, 3 + padding,
                    Theme.opacity(Theme.TEXT, opacity), false, true);
            for (Entry entry : sorted) {
                double amount = entry.slide.getValue();
                double alpha = opacity * MathUtil.clamp01(amount);
                double boxWidth = entry.width + padding * 2 + 1;
                double x = graphics.guiWidth() / referenceScale - boxWidth + (1 - amount) * (boxWidth + 8);
                double y = entry.y.getValue();
                background(graphics, x, y, boxWidth, rowHeight, alpha);
                RenderUtil.accentText(graphics, minecraft.font, Theme.displayName(entry.module.getName()),
                        x + padding, y + padding, 1, alpha);
            }
        } finally { graphics.pose().popMatrix(); }
    }

    private void background(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double opacity) {
        ThemeManager.HudBackground kind = themes == null ? ThemeManager.HudBackground.SOLID_BOX
                : themes.choice("HUD Background", ThemeManager.HudBackground.class);
        if (kind == ThemeManager.HudBackground.NONE) { return; }
        if (kind == ThemeManager.HudBackground.BLUR_BOX) { BlurUtil.drawBox(graphics, x, y, width, height, 3, opacity * 0.8); }
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, 3,
                Theme.opacity(Theme.SURFACE, opacity * (kind == ThemeManager.HudBackground.BLUR_BOX ? 0.3 : 0.8)));
    }
    @Override
    public void close() { subscription.close(); entries.clear(); }
}
```

### src/main/java/dev/riserecode/ui/component/UIComponent.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class UIComponent {
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected final Animation hover = Animation.ui(0, 0.14, MathUtil.Easing.IN_OUT_CUBIC);

    public record Bounds(double x, double y, double width, double height) {
        public double centerX() { return x + width / 2; }
        public double centerY() { return y + height / 2; }
    }

    public void setBounds(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public Bounds getBounds() { return new Bounds(x, y, width, height); }
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    protected double updateHover(double mouseX, double mouseY, double deltaSeconds) {
        hover.animateTo(contains(mouseX, mouseY) ? 1 : 0);
        return hover.update(deltaSeconds);
    }

    public abstract void render(GuiGraphicsExtractor graphics, Font font, double mouseX,
            double mouseY, double deltaSeconds, double opacity);
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) { return false; }
}
```

### src/main/java/dev/riserecode/ui/component/ModuleButton.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorSwatch;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A compact card; modules own behavior, this adapter owns all presentation. */
public final class ModuleButton extends UIComponent {
    public static final double HEADER_HEIGHT = 40;
    public record SettingControl(Setting<?> setting, UIComponent component, double height) { }
    private final Module module;
    private final ModuleManager manager;
    private final List<SettingControl> controls = new ArrayList<>();
    private final Map<UIComponent, Animation> positions = new IdentityHashMap<>();
    private final Animation enabled;
    private final Animation expansion = Animation.ui(0, 0.24, MathUtil.Easing.OUT_QUART);
    private final Animation bodyHeight = Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART);
    private boolean expanded;
    private boolean showCategory;
    private UIComponent captured;

    public ModuleButton(Module module, ModuleManager manager, Runnable changed) {
        this.module = module; this.manager = manager;
        enabled = Animation.ui(module.isEnabled() ? 1 : 0, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
        for (Setting<?> setting : module.getSettings()) {
            UIComponent component;
            double controlHeight;
            if (setting instanceof BooleanSetting bool) { component = new ToggleSwitch(bool, changed); controlHeight = 15; }
            else if (setting instanceof DoubleSetting number) { component = new Slider(number, changed); controlHeight = 24; }
            else if (setting instanceof EnumSetting<?> option) { component = enumControl(option, changed); controlHeight = 15; }
            else if (setting instanceof ColorSetting color) { component = new ColorSwatch(color, changed); controlHeight = 15; }
            else { throw new IllegalArgumentException("No UI adapter for " + setting.getClass().getName()); }
            controls.add(new SettingControl(setting, component, controlHeight));
            positions.put(component, Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART));
        }
    }
    private static <E extends Enum<E>> UIComponent enumControl(EnumSetting<E> setting, Runnable changed) { return new EnumSelector<>(setting, changed); }
    public Module getModule() { return module; }
    public boolean isExpanded() { return expanded; }
    public void setShowCategory(boolean value) { showCategory = value; }
    public List<SettingControl> getSettingControls() { return List.copyOf(controls); }
    public void update(double deltaSeconds) {
        expansion.animateTo(expanded ? 1 : 0); expansion.update(deltaSeconds);
        double result = 4;
        for (SettingControl control : controls) { if (control.setting().isVisible()) { result += control.height(); } }
        bodyHeight.animateTo(expanded ? result : 0); bodyHeight.update(deltaSeconds);
        // Visibility changes release a captured slider before it can edit a hidden setting.
        if (captured != null && controls.stream().anyMatch(c -> c.component() == captured && !c.setting().isVisible())) {
            captured.mouseReleased(0, 0, 0); captured = null;
        }
    }
    public double getPreferredHeight() { return HEADER_HEIGHT + bodyHeight.getValue(); }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        hover.animateTo(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT ? 1 : 0);
        double hovered = hover.update(seconds);
        enabled.animateTo(module.isEnabled() ? 1 : 0);
        double active = enabled.update(seconds);
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered * 0.65), opacity));
        String name = Theme.displayName(module.getName());
        double left = x + 7 + hovered * 0.7;
        if (active > 0.01) {
            RenderUtil.accentText(graphics, font, RenderUtil.ellipsize(font, name, width - 20), left, y + 7, 1, opacity * MathUtil.clamp01(active));
        }
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, name, width - 20), left, y + 7, 1,
                Theme.opacity(Theme.TEXT, opacity * (1 - MathUtil.clamp01(active))));
        if (showCategory) {
            String category = "(" + Theme.label(module.getCategory().getDisplayName()) + ")";
            double categoryX = left + RenderUtil.textWidth(font, name, false) + 5;
            if (categoryX + RenderUtil.textWidth(font, category, false) * 0.75 < x + width - 6) {
                RenderUtil.text(graphics, font, category, categoryX, y + 9, 0.75, Theme.opacity(Theme.SECONDARY, opacity * 0.7));
            }
        }
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, module.getDescription(), (width - 14) / 0.8),
                x + 7, y + 25, 0.8, Theme.opacity(Theme.SECONDARY, opacity * 0.8));
        if (height - HEADER_HEIGHT <= 0.1) { return; }
        RenderUtil.scissorPush(graphics, x + 3, y + HEADER_HEIGHT, width - 6, height - HEADER_HEIGHT - 2);
        try {
            double top = 0;
            for (SettingControl control : controls) {
                if (!control.setting().isVisible()) { continue; }
                Animation position = positions.get(control.component()); position.animateTo(top);
                control.component().setBounds(x + 7, y + HEADER_HEIGHT + position.update(seconds), width - 14, control.height());
                control.component().render(graphics, font, mouseX, mouseY, seconds, opacity * expansion.getValue());
                top += control.height();
            }
        } finally { RenderUtil.scissorPop(graphics); }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) { return false; }
        if (mouseY < y + HEADER_HEIGHT) {
            if (button == 1 && !controls.isEmpty()) {
                expanded = !expanded;
                if (!expanded) { mouseReleased(mouseX, mouseY, 0); }
            } else if (button == 0) { manager.toggle(module); }
            return true;
        }
        if (!expanded) { return true; }
        for (SettingControl control : controls) {
            // Partially revealed rows must not accept input outside the animated card's clip.
            if (control.setting().isVisible() && control.component().getBounds().y() + control.height() <= y + height + 0.01
                    && control.component().mouseClicked(mouseX, mouseY, button)) {
                captured = control.component(); return true;
            }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button); captured = null; return consumed;
    }
}
```

### src/main/java/dev/riserecode/ui/component/Panel.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Clipped, smoothly scrolling module column. Window movement belongs to the screen host. */
public final class Panel extends UIComponent {
    private final Category category;
    private final ModuleManager manager;
    private final Runnable changed;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation opening = Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation scroll = Animation.ui(0, 0.18, MathUtil.Easing.OUT_QUART);
    private String query = "";
    private double maxScroll;
    private ModuleButton captured;

    /** A null category represents the global Search page. */
    public Panel(Category category, ModuleManager manager, Runnable changed) {
        this.category = category; this.manager = manager; this.changed = changed; syncModules();
    }
    public Category getCategory() { return category; }
    public List<ModuleButton> getModuleButtons() { syncModules(); return List.copyOf(buttons); }
    public List<ModuleButton> getVisibleButtons() { return buttons.stream().filter(this::matches).toList(); }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public boolean isOpen() { return !opening.isRunning() && opening.getValue() == 1; }
    public void open() { mouseReleased(0, 0, 0); opening.reset(0); opening.animateTo(1); }
    public void refresh() { mouseReleased(0, 0, 0); syncModules(); }
    public void setQuery(String value) {
        String next = value.strip().toLowerCase(Locale.ROOT);
        if (next.equals(query)) { return; }
        query = next; mouseReleased(0, 0, 0); scroll.reset(0); open();
    }
    private boolean matches(ModuleButton button) {
        String haystack = (button.getModule().getName() + " " + Theme.displayName(button.getModule().getName()) + " "
                + button.getModule().getDescription() + " " + button.getModule().getCategory().getDisplayName()).toLowerCase(Locale.ROOT);
        for (String word : query.split("\\s+")) { if (!haystack.contains(word)) { return false; } }
        return true;
    }
    private void syncModules() {
        List<Module> registered = category == null ? manager.getModules() : manager.getByCategory(category);
        buttons.removeIf(button -> !registered.contains(button.getModule()));
        for (Module module : registered) {
            if (buttons.stream().noneMatch(button -> button.getModule() == module)) {
                ModuleButton button = new ModuleButton(module, manager, changed); button.setShowCategory(category == null); buttons.add(button);
            }
        }
        buttons.sort(Comparator.comparing(button -> button.getModule().getName(), String.CASE_INSENSITIVE_ORDER));
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        syncModules();
        double amount = opening.update(seconds);
        List<ModuleButton> visible = getVisibleButtons();
        double content = 0;
        for (ModuleButton button : visible) { button.update(seconds); content += button.getPreferredHeight() + Theme.number("Content Padding", 8); }
        maxScroll = Math.max(0, content - Theme.number("Content Padding", 8) - height);
        if (scroll.getValue() > maxScroll) { scroll.reset(maxScroll); }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget()))); scroll.update(seconds);
        RenderUtil.scissorPush(graphics, x, y, width, height);
        try {
            double next = y - scroll.getValue() + (1 - amount) * 5;
            for (ModuleButton button : visible) {
                button.setBounds(x, next, width, button.getPreferredHeight());
                button.render(graphics, font, contains(mouseX, mouseY) ? mouseX : -1e6,
                        contains(mouseX, mouseY) ? mouseY : -1e6, seconds, opacity * amount);
                next += button.getPreferredHeight() + Theme.number("Content Padding", 8);
            }
            if (visible.isEmpty()) {
                RenderUtil.text(graphics, font, Theme.label(query.isEmpty() ? "No modules in this category" : "No matching modules"),
                        x + 7, y + 9, 0.85, Theme.opacity(Theme.SECONDARY, opacity * amount));
            }
        } finally { RenderUtil.scissorPop(graphics); }
        if (maxScroll > 0) {
            double thumb = Math.max(14, height * height / (height + maxScroll));
            RenderUtil.drawRoundedRect(graphics, x + width - 2, y + (height - thumb) * scroll.getValue() / maxScroll,
                    1.5, thumb, 0.75, Theme.opacity(Theme.accent(), opacity * 0.25));
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) { return false; }
        for (ModuleButton child : getVisibleButtons()) {
            if (child.mouseClicked(mouseX, mouseY, button)) { captured = child; return true; }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button); captured = null; return consumed;
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY)) { return false; }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - amount * 24))); return true;
    }
}
```

### src/main/java/dev/riserecode/ui/component/Sidebar.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed quarter-width navigation with a compact animated selection pill. */
public final class Sidebar extends UIComponent {
    public record Item(String label, Icon icon) { }
    private static final double ROW_HEIGHT = 20.75;
    private final List<Item> items;
    private final IntConsumer selectedAction;
    private final List<Animation> hovers;
    private final Animation selectedY = Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation selectedWidth = Animation.ui(52, 0.2, MathUtil.Easing.OUT_QUART);
    private int selected;
    public Sidebar(List<Item> items, IntConsumer selectedAction) {
        this.items = List.copyOf(items); this.selectedAction = selectedAction;
        hovers = items.stream().map(item -> Animation.ui(0, 0.16, MathUtil.Easing.IN_OUT_CUBIC)).toList();
    }
    public void setSelected(int index) { selected = index; selectedY.animateTo(index * ROW_HEIGHT); }
    public int getSelected() { return selected; }
    public Bounds getItemBounds(int index) { return new Bounds(x + 10, y + 43 + index * ROW_HEIGHT, width - 20, 17); }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double logoScale = Math.min(1.65, (width - 37) / Math.max(1, RenderUtil.textWidth(font, Client.NAME, false)));
        RenderUtil.text(graphics, font, Client.NAME, x + 14, y + 14, logoScale, Theme.opacity(Theme.TEXT, opacity));
        double versionX = x + 14 + RenderUtil.textWidth(font, Client.NAME, false) * logoScale + 1;
        RenderUtil.text(graphics, font, Client.INSTANCE.getVersion(), versionX, y + 12, 0.55,
                Theme.opacity(Theme.accent(), opacity * 0.85));
        String selectedLabel = Theme.label(items.get(selected).label());
        selectedWidth.animateTo(Math.min(width - 20, RenderUtil.textWidth(font, selectedLabel, false) * 0.88 + 28));
        double pillWidth = selectedWidth.update(seconds);
        double top = y + 43 + selectedY.update(seconds);
        RenderUtil.drawGlow(graphics, x + 10, top, pillWidth, 16, 4,
                Theme.opacity(Theme.accent(), opacity));
        RenderUtil.drawRoundedRect(graphics, x + 10, top, pillWidth, 16, 4,
                Theme.opacity(Theme.lerp(Theme.accent(), 0xFF1AA99E, 0.2), opacity));
        for (int i = 0; i < items.size(); i++) {
            Bounds row = getItemBounds(i);
            boolean inside = mouseX >= row.x() && mouseX < row.x() + row.width() && mouseY >= row.y() && mouseY < row.y() + row.height();
            hovers.get(i).animateTo(inside ? 1 : 0);
            double amount = hovers.get(i).update(seconds);
            if (i != selected) {
                RenderUtil.drawRoundedRect(graphics, row.x(), row.y(), Math.min(width - 20,
                        RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false) * 0.88 + 28), 16, 4,
                        Theme.opacity(0xFF314049, opacity * amount * 0.4));
            }
            int color = Theme.opacity(Theme.lerp(0xFFC9CFD1, Theme.TEXT, amount), opacity);
            items.get(i).icon().draw(graphics, x + 14 + amount * 0.65, row.y() + 3, color);
            double labelScale = Math.min(0.88, (width - 40) / Math.max(1, RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false)));
            RenderUtil.text(graphics, font, Theme.label(items.get(i).label()), x + 29 + amount * 0.65,
                    row.y() + 4, labelScale, color);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) { return false; }
        for (int i = 0; i < items.size(); i++) {
            Bounds b = getItemBounds(i);
            if (mouseX >= b.x() && mouseX < b.x() + b.width() && mouseY >= b.y() && mouseY < b.y() + b.height()) {
                selectedAction.accept(i); return true;
            }
        }
        return false;
    }
}
```

### src/main/java/dev/riserecode/ui/component/ActionButton.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ActionButton extends UIComponent {
    private final Animation press = Animation.ui(0, 0.18, MathUtil.Easing.OUT_QUART);
    private final String label;
    private final Runnable action;

    public ActionButton(String label, Runnable action) { this.label = label; this.action = action; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        double down = press.update(deltaSeconds);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + width / 2), (float) (y + height / 2));
        graphics.pose().scale((float) (1 - down * 0.025), (float) (1 - down * 0.025));
        graphics.pose().translate((float) (-x - width / 2), (float) (-y - height / 2));
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered), opacity));
        String text = Theme.label(label);
        double scale = Math.min(0.88, (width - 8) / Math.max(1, RenderUtil.textWidth(font, text, false)));
        RenderUtil.text(graphics, font, text, x + (width - RenderUtil.textWidth(font, text, false) * scale) / 2,
                y + (height - 9 * Theme.number("Font Scale", 1) * scale) / 2, scale,
                Theme.opacity(Theme.lerp(Theme.TEXT, Theme.accent(), hovered * 0.45), opacity));
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        press.reset(1); press.animateTo(0); action.run(); return true;
    }
}
```

### src/main/java/dev/riserecode/ui/component/setting/ColorSwatch.java

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Palette cycle control. Preserves the setting's existing alpha channel. */
public final class ColorSwatch extends UIComponent {
    private static final int[] PALETTE = {Theme.BLUE, Theme.FUCHSIA, 0xFF22D3EE, 0xFF34D399, 0xFFFBBF24, Theme.TEXT};
    private final ColorSetting setting;
    private final Runnable changed;
    private final Animation pulse = Animation.ui(1, 0.18, MathUtil.Easing.OUT_QUART);
    private int previous;

    public ColorSwatch(ColorSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; previous = setting.getValue(); }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        if (previous != setting.getValue()) { previous = setting.getValue(); pulse.reset(0); pulse.animateTo(1); }
        double progress = pulse.update(deltaSeconds);
        String label = Theme.displayName(setting.getName());
        RenderUtil.text(graphics, font, label, x + hovered * 0.7, y + (height - 9) / 2, 0.88,
                Theme.opacity(Theme.TEXT, opacity));
        double left = x + RenderUtil.textWidth(font, label, false) * 0.88 + 6;
        double size = 6 - (1 - progress) * 1.5;
        RenderUtil.drawRoundedRect(graphics, left, y + (height - size) / 2, size, size, 2,
                Theme.opacity(setting.getValue() | 0xFF000000, opacity));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if ((PALETTE[i] & 0xFFFFFF) == (setting.getValue() & 0xFFFFFF)) { index = i; break; }
        }
        setting.setValue((setting.getValue() & 0xFF000000) | (PALETTE[(index + 1) % PALETTE.length] & 0xFFFFFF));
        changed.run();
        return true;
    }
}
```

### src/main/java/dev/riserecode/ui/component/setting/Slider.java

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Slider extends UIComponent {
    private final DoubleSetting setting;
    private final Runnable changed;
    private final Animation position;
    private boolean dragging;
    private final double step;

    public Slider(DoubleSetting setting, Runnable changed) {
        this(setting, changed, 0);
    }
    public Slider(DoubleSetting setting, Runnable changed, double step) {
        this.setting = setting; this.changed = changed; this.step = step;
        position = Animation.ui(normalized(), 0.1, MathUtil.Easing.OUT_QUART);
    }

    private double normalized() {
        double range = setting.getMax() - setting.getMin();
        return range == 0 ? 0 : (setting.getValue() - setting.getMin()) / range;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        position.animateTo(normalized());
        double progress = MathUtil.clamp01(position.update(deltaSeconds));
        String value = String.format(Locale.ROOT, step == 1 ? "%.0f" : "%.2f", setting.getValue());
        String label = Theme.displayName(setting.getName()) + ": " + value;
        double labelScale = Math.min(0.88, (width - 4) / Math.max(1, RenderUtil.textWidth(font, label, false)));
        RenderUtil.text(graphics, font, label,
                x + hovered * 0.7, y + 1, labelScale, Theme.opacity(Theme.TEXT, opacity));
        double trackX = x + 5;
        double trackWidth = width - 10;
        double trackY = y + height - 6;
        RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth, 1.5, 0.75,
                Theme.opacity(Theme.MUTED, opacity * 0.45));
        if (progress > 0.001) {
            RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth * progress, 1.5, 0.75,
                    Theme.opacity(Theme.accent(), opacity * 0.8));
        }
        double thumb = trackX + trackWidth * progress;
        double size = 4 + hovered + (dragging ? 0.5 : 0);
        RenderUtil.drawGlow(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.accent(), opacity * (0.6 + hovered * 0.4)));
        RenderUtil.drawRoundedRect(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.TEXT, opacity));
    }

    private void setFromMouse(double mouseX) {
        double progress = MathUtil.clamp01((mouseX - x - 5) / Math.max(1, width - 10));
        double value = MathUtil.lerp(setting.getMin(), setting.getMax(), progress);
        if (step > 0) { value = setting.getMin() + Math.rint((value - setting.getMin()) / step) * step; }
        setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(), value)));
        changed.run();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) { return false; }
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        return consumed;
    }
}
```

### src/main/java/dev/riserecode/ui/component/setting/ToggleSwitch.java

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The reference's compact inline indicator, with interpolated color and a soft pulse. */
public final class ToggleSwitch extends UIComponent {
    private final String label;
    private final BooleanSupplier value;
    private final Consumer<Boolean> setter;
    private final Animation slide;
    public ToggleSwitch(BooleanSetting setting, Runnable changed) {
        this(setting.getName(), setting::getValue, next -> { setting.setValue(next); changed.run(); });
    }
    public ToggleSwitch(String label, BooleanSupplier value, Consumer<Boolean> setter) {
        this.label = label; this.value = value; this.setter = setter;
        slide = Animation.ui(value.getAsBoolean() ? 1 : 0, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        slide.animateTo(value.getAsBoolean() ? 1 : 0);
        double active = slide.update(deltaSeconds);
        String text = Theme.label(Theme.displayName(label));
        double textScale = Math.min(0.88, (width - 24) / Math.max(1, RenderUtil.textWidth(font, text, false)));
        RenderUtil.text(graphics, font, text, x + hovered * 0.7, y + (height - 9 * Theme.number("Font Scale", 1) * textScale) / 2, textScale,
                Theme.opacity(Theme.lerp(Theme.TEXT, 0xFFFFFFFF, hovered), opacity));
        double dotX = x + RenderUtil.textWidth(font, text, false) * textScale + 6 + hovered * 0.7;
        double dotY = y + height / 2 - 2;
        RenderUtil.drawGlow(graphics, dotX, dotY, 4, 4, 2, Theme.opacity(Theme.accent(), opacity * active));
        RenderUtil.drawRoundedRect(graphics, dotX, dotY, 4, 4, 2,
                Theme.opacity(Theme.lerp(Theme.MUTED, Theme.accent(), active), opacity));
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        setter.accept(!value.getAsBoolean()); return true;
    }
}
```

### src/main/java/dev/riserecode/ui/component/setting/EnumSelector.java

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Inline cycle selector: left advances, right reverses, with a short value transition. */
public final class EnumSelector<E extends Enum<E>> extends UIComponent {
    private final EnumSetting<E> setting;
    private final Runnable changed;
    private final Animation change = Animation.ui(1, 0.16, MathUtil.Easing.OUT_QUART);
    private E displayed;
    public EnumSelector(EnumSetting<E> setting, Runnable changed) {
        this.setting = setting; this.changed = changed; displayed = setting.getValue();
    }
    private String option() {
        if (displayed instanceof MathUtil.Easing easing) {
            return switch (easing) {
                case LINEAR -> "Linear";
                case OUT_QUART -> "Ease-Out-Quart";
                case IN_OUT_CUBIC -> "Ease-In-Out-Cubic";
                case OUT_ELASTIC -> "Elastic";
                case OUT_BOUNCE -> "Bounce";
            };
        }
        return Theme.displayName(displayed.name());
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        if (displayed != setting.getValue()) { displayed = setting.getValue(); change.reset(0); change.animateTo(1); }
        double progress = change.update(deltaSeconds);
        String label = Theme.displayName(setting.getName()) + ": ";
        String option = option();
        double scale = Math.min(0.88, (width - 3) / Math.max(1, RenderUtil.textWidth(font, label + option, false)));
        double top = y + (height - 9 * Theme.number("Font Scale", 1) * scale) / 2;
        RenderUtil.text(graphics, font, label, x + hovered * 0.7, top, scale, Theme.opacity(Theme.TEXT, opacity));
        double valueX = x + RenderUtil.textWidth(font, label, false) * scale + hovered * 0.7;
        RenderUtil.text(graphics, font, option, valueX + (1 - progress) * 3, top,
                scale, Theme.opacity(Theme.lerp(Theme.TEXT, Theme.accent(), hovered * 0.6), opacity * progress));
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) { return false; }
        var options = setting.getOptions();
        setting.setValue(options.get(Math.floorMod(options.indexOf(setting.getValue()) + (button == 0 ? 1 : -1), options.size())));
        changed.run(); return true;
    }
}
```

### src/main/java/dev/riserecode/Client.java

```java
package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.BindCommand;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.render.WorldRenderer;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "risecode";
    private final Logger logger = LoggerFactory.getLogger(NAME);
    private final EventBus eventBus = new EventBus((event, failure) ->
            logger.error("Listener failed for {}", event.getClass().getSimpleName(), failure));
    private String version = "development";
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private ProfileManager profileManager;
    private WorldRenderer worldRenderer;
    private RotationManager rotationManager;
    private AttackManager attackManager;
    private HudRenderer hudRenderer;
    private ThemeManager themeManager;
    private ClickGui clickGui;
    private volatile boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        friendManager = new FriendManager();
        worldRenderer = new WorldRenderer(eventBus);
        rotationManager = new RotationManager(eventBus, minecraft);
        attackManager = new AttackManager(eventBus, minecraft);
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft, friendManager, rotationManager, attackManager);
        themeManager = new ThemeManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), logger);
        themeManager.load(); themeManager.bind(eventBus); CustomTextRenderer.configure(themeManager);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME, themeManager);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, friendManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.useExternalVisualPreferences(); configManager.load();
        if (!themeManager.wasLoaded()) {
            var oldUi = configManager.getUiPreferences();
            hudRenderer.setEnabled(configManager.getLegacyHudEnabled());
            try {
                if (oldUi.has("accent")) { ((dev.riserecode.setting.ColorSetting) themeManager.getSetting("Primary Accent")).setValue(oldUi.get("accent").getAsInt()); }
                if (oldUi.has("scale")) { ((dev.riserecode.setting.DoubleSetting) themeManager.getSetting("Interface Scale")).setValue(oldUi.get("scale").getAsDouble()); }
                themeManager.changed(); themeManager.save();
            } catch (RuntimeException invalid) { logger.warn("Could not migrate legacy UI preferences", invalid); }
        }
        moduleManager.setChangeListener(configManager::requestSave);
        friendManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        profileManager = new ProfileManager(configManager, () -> {
            if (clickGui != null && minecraft.gui.screen() == clickGui) { clickGui.refresh(); }
        }, logger);
        commandManager = new CommandManager(configManager::getCommandPrefix, configManager::setCommandPrefix, message -> {
            if (minecraft.player != null) {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(NAME + " » ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
            } else { logger.info("[Command] {}", message); }
        }, logger);
        commandManager.register(new ToggleCommand(moduleManager, commandManager));
        commandManager.register(new BindCommand(moduleManager, keybindManager, commandManager));
        commandManager.register(new FriendCommand(friendManager, commandManager, name -> {
            var connection = minecraft.getConnection();
            var info = connection == null ? null : connection.getPlayerInfoIgnoreCase(name);
            return info == null ? null : info.getProfile().id();
        }));
        commandManager.register(new ProfileCommand(profileManager, commandManager));
        commandManager.register(new HelpCommand(commandManager));
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        themeManager.close(); BlurUtil.close(); CustomTextRenderer.close();
        worldRenderer.close();
        configManager.close();
        moduleManager.disableAll();
        rotationManager.close();
        attackManager.close();
        Minecraft.getInstance().options.save();
        initialized = false;
    }

    public boolean isInitialized() { return initialized; }
    public String getName() { return NAME; }
    public String getVersion() { return version; }
    public Logger getLogger() { return logger; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    public RotationManager getRotationManager() { return rotationManager; }
    public AttackManager getAttackManager() { return attackManager; }
    public ThemeManager getThemeManager() { return themeManager; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    /** Screens capture the font, so create this only after Minecraft initializes it. */
    public ClickGui getClickGui() {
        if (clickGui == null) {
            if (!initialized || Minecraft.getInstance().font == null) {
                throw new IllegalStateException("The Minecraft GUI is not ready");
            }
            clickGui = new ClickGui(moduleManager, hudRenderer, configManager::requestSave);
        }
        return clickGui;
    }
}
```

### src/main/java/dev/riserecode/config/ConfigManager.java

```java
package dev.riserecode.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.slf4j.Logger;

/** Global preferences plus reusable, validated module snapshots for profiles. */
public final class ConfigManager implements AutoCloseable {
    private record ModuleState(Module module, boolean enabled, int keybind, Map<Setting<?>, JsonElement> settings) { }
    private record ProfileState(boolean hud, List<ModuleState> modules) { }
    private final Gson gson = new Gson();
    private final Path directory;
    private final Path file;
    private final ModuleManager modules;
    private final FriendManager friends;
    private boolean externalVisualPreferences;
    private final BooleanSupplier hudEnabled;
    private final Consumer<Boolean> setHudEnabled;
    private final Logger logger;
    private JsonObject document = new JsonObject();
    private String commandPrefix = ".";
    private EventBus.Subscription subscription;
    private boolean dirty;
    private long saveAfterNanos;

    public ConfigManager(Path directory, ModuleManager modules, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this(directory, modules, new FriendManager(), hudEnabled, setHudEnabled, logger);
    }

    public ConfigManager(Path directory, ModuleManager modules, FriendManager friends, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this.directory = directory; file = directory.resolve("config.json"); this.modules = modules;
        this.friends = friends; this.hudEnabled = hudEnabled; this.setHudEnabled = setHudEnabled; this.logger = logger;
    }

    /** Phase 7 clients persist HUD/theme choices independently; legacy consumers retain compatibility. */
    public void useExternalVisualPreferences() { externalVisualPreferences = true; }
    public boolean getLegacyHudEnabled() {
        JsonElement value = document.get("hudEnabled");
        return value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean() || value.getAsBoolean();
    }
    public Path getDirectory() { return directory; }
    public Path getFile() { return file; }
    /** UI preferences are global; loading a combat profile never moves the window or changes its theme. */
    public JsonObject getUiPreferences() { return objectOrEmpty(document.get("ui")).deepCopy(); }
    public void setUiPreferences(JsonObject preferences) {
        document.add("ui", preferences.deepCopy()); requestSave();
    }
    public String getCommandPrefix() { return commandPrefix; }
    public void setCommandPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.length() > 8 || prefix.startsWith("/")
                || prefix.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Prefix must have 1–8 non-space characters and cannot start with /");
        }
        commandPrefix = prefix; requestSave();
    }

    public void init(EventBus eventBus) {
        if (subscription != null) { return; }
        subscription = eventBus.subscribe(UpdateEvent.class, -100, false, event -> {
            if (dirty && System.nanoTime() - saveAfterNanos >= 0 && !save()) {
                saveAfterNanos = System.nanoTime() + 5_000_000_000L;
            }
        });
    }
    public void requestSave() { dirty = true; saveAfterNanos = System.nanoTime() + 500_000_000L; }

    public void load() {
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) { save(); return; }
            document = JsonStorage.read(file);
        } catch (IOException | RuntimeException failure) {
            logger.warn("Could not read {}; preserving the bad file and using defaults", file, failure);
            try {
                if (Files.exists(file)) { Files.move(file, directory.resolve("config.invalid-" + System.nanoTime() + ".json")); }
                document = new JsonObject(); save();
            } catch (IOException backupFailure) { logger.error("Could not preserve invalid config", backupFailure); }
            return;
        }
        if (document.has("commandPrefix")) { apply("commandPrefix", () -> setCommandPrefix(stringValue(document.get("commandPrefix")))); }
        loadFriends(document.get("friends"));
        if (!externalVisualPreferences && document.has("hudEnabled")) { apply("hudEnabled", () -> setHudEnabled.accept(booleanValue(document.get("hudEnabled")))); }
        JsonObject entries = objectOrEmpty(document.get("modules"));
        // Disable before replacing values so lifecycle resources are restored first.
        for (Module module : modules.getModules()) {
            if (entries.has(module.getName())) { modules.setEnabled(module, false); }
        }
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            JsonObject values = objectOrEmpty(entry.get("settings"));
            for (Setting<?> setting : module.getSettings()) {
                if (values.has(setting.getName())) {
                    apply(module.getName() + "." + setting.getName(), () -> loadSetting(setting, values.get(setting.getName())));
                }
            }
            if (entry.has("keybind")) {
                apply(module.getName() + ".keybind", () -> modules.setKeybind(module, keyValue(entry.get("keybind"))));
            }
        }
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            if (entry.has("enabled")) {
                apply(module.getName() + ".enabled", () -> modules.setEnabled(module, booleanValue(entry.get("enabled"))));
            }
        }
    }

    private void loadFriends(JsonElement value) {
        if (value == null) { return; }
        if (!value.isJsonArray()) { logger.warn("Ignoring invalid friends list"); return; }
        List<FriendManager.Friend> next = new ArrayList<>();
        for (JsonElement entry : value.getAsJsonArray()) {
            apply("friend", () -> {
                JsonObject object = requiredObject(entry);
                String name = stringValue(object.get("name"));
                UUID uuid = object.has("uuid") ? UUID.fromString(stringValue(object.get("uuid"))) : null;
                next.add(new FriendManager.Friend(name, uuid));
            });
        }
        friends.replace(next);
    }

    public JsonObject profileSnapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", 2);
        if (!externalVisualPreferences) { result.addProperty("hudEnabled", hudEnabled.getAsBoolean()); }
        JsonObject entries = new JsonObject(); result.add("modules", entries);
        for (Module module : modules.getModules()) { entries.add(module.getName(), moduleSnapshot(module, new JsonObject())); }
        return result;
    }

    private JsonObject moduleSnapshot(Module module, JsonObject entry) {
        entry.addProperty("enabled", module.isEnabled()); entry.addProperty("keybind", module.getKeybind());
        JsonObject values = objectOrEmpty(entry.get("settings")); entry.add("settings", values);
        for (Setting<?> setting : module.getSettings()) { values.add(setting.getName(), encodeSetting(setting, setting.getValue())); }
        return entry;
    }

    private JsonElement encodeSetting(Setting<?> setting, Object value) {
        if (setting instanceof ColorSetting) { return new JsonPrimitive(String.format(java.util.Locale.ROOT, "%08X", (Integer) value)); }
        if (value instanceof Enum<?> option) { return new JsonPrimitive(option.name()); }
        return gson.toJsonTree(value);
    }

    public boolean save() {
        try {
            JsonObject next = document.deepCopy();
            next.addProperty("schemaVersion", 2);
            if (externalVisualPreferences) { next.remove("hudEnabled"); }
            else { next.addProperty("hudEnabled", hudEnabled.getAsBoolean()); }
            next.addProperty("commandPrefix", commandPrefix);
            JsonArray friendList = new JsonArray(); next.add("friends", friendList);
            for (FriendManager.Friend friend : friends.getFriends()) {
                JsonObject value = new JsonObject(); value.addProperty("name", friend.name());
                if (friend.uuid() != null) { value.addProperty("uuid", friend.uuid().toString()); }
                friendList.add(value);
            }
            JsonObject entries = objectOrEmpty(next.get("modules")); next.add("modules", entries);
            for (Module module : modules.getModules()) {
                entries.add(module.getName(), moduleSnapshot(module, objectOrEmpty(entries.get(module.getName()))));
            }
            JsonStorage.write(file, next); document = next; dirty = false; return true;
        } catch (IOException | RuntimeException failure) {
            logger.error("Could not save {}", file, failure); dirty = true; return false;
        }
    }

    /** Validate first, then restart lifecycles; rollback is attempted on activation failure. */
    public void applyProfile(JsonObject data) {
        ProfileState next = parseProfile(data);
        ProfileState previous = parseProfile(profileSnapshot());
        try { commit(next); }
        catch (RuntimeException failure) {
            try { commit(previous); }
            catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            requestSave(); throw failure;
        }
        requestSave();
    }

    private ProfileState parseProfile(JsonObject data) {
        if (data.has("schemaVersion")) {
            int version = integerValue(data.get("schemaVersion"));
            if (version < 1 || version > 2) { throw new IllegalArgumentException("Unsupported profile schema " + version); }
        }
        JsonObject entries = requiredObject(data.get("modules"));
        boolean hud = !externalVisualPreferences && data.has("hudEnabled") ? booleanValue(data.get("hudEnabled")) : true;
        List<ModuleState> states = new ArrayList<>();
        for (Module module : modules.getModules()) {
            JsonObject entry = entries.has(module.getName()) ? requiredObject(entries.get(module.getName())) : new JsonObject();
            boolean enabled = entry.has("enabled") && booleanValue(entry.get("enabled"));
            int key = entry.has("keybind") ? keyValue(entry.get("keybind")) : Module.UNBOUND;
            JsonObject values = entry.has("settings") ? requiredObject(entry.get("settings")) : new JsonObject();
            Map<Setting<?>, JsonElement> settings = new LinkedHashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                JsonElement value = values.has(setting.getName()) ? values.get(setting.getName()).deepCopy()
                        : encodeSetting(setting, setting.getDefaultValue());
                validateSetting(setting, value); settings.put(setting, value);
            }
            states.add(new ModuleState(module, enabled, key, settings));
        }
        return new ProfileState(hud, states);
    }

    private void commit(ProfileState state) {
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), false)) { throw new IllegalStateException("Could not disable " + entry.module().getName()); }
        }
        for (ModuleState entry : state.modules()) {
            entry.settings().forEach(ConfigManager::loadSetting); modules.setKeybind(entry.module(), entry.keybind());
        }
        if (!externalVisualPreferences) { setHudEnabled.accept(state.hud()); }
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), entry.enabled())) { throw new IllegalStateException("Could not enable " + entry.module().getName()); }
        }
    }

    private static void validateSetting(Setting<?> setting, JsonElement value) {
        if (setting instanceof BooleanSetting) { booleanValue(value); }
        else if (setting instanceof DoubleSetting number) {
            double decoded = numberValue(value).getAsDouble();
            if (!Double.isFinite(decoded) || decoded < number.getMin() || decoded > number.getMax()) {
                throw new IllegalArgumentException("Out-of-range value for " + setting.getName());
            }
        } else if (setting instanceof ColorSetting) { colorValue(value); }
        else if (setting instanceof EnumSetting<?> choice) { enumValue(choice, value); }
        else { throw new IllegalArgumentException("Unsupported setting type " + setting.getClass().getName()); }
    }
    private static void loadSetting(Setting<?> setting, JsonElement value) {
        validateSetting(setting, value);
        if (setting instanceof BooleanSetting flag) { flag.setValue(booleanValue(value)); }
        else if (setting instanceof DoubleSetting number) { number.setValue(numberValue(value).getAsDouble()); }
        else if (setting instanceof ColorSetting color) { color.setValue(colorValue(value)); }
        else if (setting instanceof EnumSetting<?> choice) { loadEnum(choice, value); }
    }
    private static <E extends Enum<E>> E enumValue(EnumSetting<E> setting, JsonElement value) {
        String name = stringValue(value);
        return setting.getOptions().stream().filter(option -> option.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown option " + name));
    }
    private static <E extends Enum<E>> void loadEnum(EnumSetting<E> setting, JsonElement value) { setting.setValue(enumValue(setting, value)); }
    private static int colorValue(JsonElement value) {
        String hex = stringValue(value);
        if (!hex.matches("[0-9a-fA-F]{8}")) { throw new IllegalArgumentException("Color must be eight ARGB hexadecimal digits"); }
        return (int) Long.parseLong(hex, 16);
    }
    private void apply(String label, Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException failure) { logger.warn("Ignoring invalid config value {}: {}", label, failure.getMessage()); }
    }
    private static String stringValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) { throw new IllegalArgumentException("Expected a string"); }
        return value.getAsString();
    }
    private static boolean booleanValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) { throw new IllegalArgumentException("Expected a boolean"); }
        return value.getAsBoolean();
    }
    private static JsonPrimitive numberValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Expected a number"); }
        return value.getAsJsonPrimitive();
    }
    private static int integerValue(JsonElement value) { return numberValue(value).getAsBigDecimal().intValueExact(); }
    private static int keyValue(JsonElement value) {
        int key = integerValue(value);
        if (key < Module.UNBOUND) { throw new IllegalArgumentException("Key must be unbound (-1) or non-negative"); }
        return key;
    }
    private static JsonObject requiredObject(JsonElement value) {
        if (value == null || !value.isJsonObject()) { throw new IllegalArgumentException("Expected an object"); }
        return value.getAsJsonObject();
    }
    private static JsonObject objectOrEmpty(JsonElement value) { return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject(); }
    @Override
    public void close() { if (subscription != null) { subscription.close(); } }
}
```

### src/main/java/dev/riserecode/command/commands/ProfileCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.config.ProfileManager;
import java.util.Locale;

public final class ProfileCommand extends Command {
    private final ProfileManager profiles;
    private final CommandManager commands;
    public ProfileCommand(ProfileManager profiles, CommandManager commands) {
        super("profile", "Save or load module configurations", "profile <save|load> <name> | profile list");
        this.profiles = profiles; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var names = profiles.getProfiles(); commands.reply(names.isEmpty() ? "No profiles saved" : "Profiles: " + String.join(", ", names));
            return;
        }
        if (args.length != 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> commands.reply(profiles.saveProfile(args[1]) ? "Saved profile " + args[1] : "Could not save profile " + args[1]);
            case "load" -> commands.reply(profiles.loadProfile(args[1]) ? "Loaded profile " + args[1] : "Profile missing or invalid: " + args[1]);
            default -> throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage());
        }
    }
}
```

### src/main/java/dev/riserecode/mixin/GuiRendererMixin.java

```java
package dev.riserecode.mixin;

import dev.riserecode.util.BlurUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture the finished world or panorama before any HUD/menu draw. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void riserecode$blurBeforeGui(CallbackInfo callback) {
        BlurUtil.renderBeforeGui(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }
}
```

### src/main/resources/assets/riserecode/shaders/core/blur.fsh

```glsl
#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color;
    if (Params.z == 0.0) {
        vec2 halfTexel = 0.5 / vec2(textureSize(Sampler0, 0));
        color = (texture(Sampler0, texCoord + halfTexel).rgb + texture(Sampler0, texCoord - halfTexel).rgb
               + texture(Sampler0, texCoord + vec2(halfTexel.x, -halfTexel.y)).rgb
               + texture(Sampler0, texCoord + vec2(-halfTexel.x, halfTexel.y)).rgb) * 0.25;
    } else if (Params.z == 1.0) {
        // Nine-tap separable Gaussian collapsed to five bilinear texture fetches.
        color = texture(Sampler0, texCoord).rgb * 0.227027027;
        color += (texture(Sampler0, texCoord + Params.xy * 1.384615385).rgb
                + texture(Sampler0, texCoord - Params.xy * 1.384615385).rgb) * 0.316216216;
        color += (texture(Sampler0, texCoord + Params.xy * 3.230769231).rgb
                + texture(Sampler0, texCoord - Params.xy * 3.230769231).rgb) * 0.070270270;
    } else { color = texture(Sampler0, texCoord).rgb; }
    fragColor = vec4(color, Params.w);
}
```

### src/main/resources/assets/riserecode/shaders/core/blur_box.fsh

```glsl
#version 330
uniform sampler2D Sampler0;
in vec2 local;
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    vec2 halfSize = data.xy / 32.0;
    float radius = min(data.z / 256.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(local) - halfSize + radius;
    float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(distance), 0.001);
    float coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
    fragColor = vec4(texture(Sampler0, screenUV).rgb, tint.a * coverage);
}
```

### src/main/resources/assets/riserecode/shaders/core/font.fsh

```glsl
#version 330
uniform sampler2D Sampler0;
in vec2 local;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
// STB stores 128 at the outline and 2.5 intensity units per baked pixel; density is two.
void main() {
    float distance = (texture(Sampler0, local).r * 255.0 - 128.0) / 5.0;
    float coverage;
    if (data.y == 1.0 && data.x > 0.0) {
        float sigma = max(data.x / 256.0, 0.01);
        // Abramowitz-Stegun erf approximation: analytical Gaussian silhouette shadow.
        float z = abs(distance) / (sigma * 1.41421356237);
        float t = 1.0 / (1.0 + 0.3275911 * z);
        float erf = 1.0 - (((((1.061405429*t - 1.453152027)*t) + 1.421413741)*t - 0.284496736)*t + 0.254829592)*t*exp(-z*z);
        coverage = 0.5 + 0.5 * sign(distance) * erf;
    } else if (data.y == 2.0) { coverage = step(0.0, distance); }
    else { float aa = max(fwidth(distance), 0.001); coverage = smoothstep(-aa * 0.5, aa * 0.5, distance); }
    fragColor = vec4(tint.rgb, tint.a * coverage);
}
```

### src/main/resources/assets/riserecode/shaders/core/hsv.fsh

```glsl
#version 330
in vec2 local;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
vec3 hsv(float h, float s, float v) {
    vec3 p = abs(fract(vec3(h) + vec3(0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
    return v * mix(vec3(1), clamp(p - 1.0, 0.0, 1.0), s);
}
void main() {
    vec3 color = data.y == 1.0 ? hsv(local.y, 1.0, 1.0) : hsv(data.x / 30000.0, local.x, 1.0 - local.y);
    fragColor = vec4(color, tint.a);
}
```

### src/main/resources/assets/riserecode/shaders/core/shape.fsh

```glsl
#version 330
in vec2 local;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    vec2 halfSize = data.xy / 32.0;
    float radius = min(data.z / 256.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(local) - halfSize + radius;
    float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(distance), 0.001);
    float coverage;
    if (data.w < 0.0) {
        float sigma = max((-data.w - 1.0) / 768.0, 0.001);
        coverage = exp(-pow(max(distance, 0.0), 2.0) / (2.0 * sigma * sigma));
    } else {
        coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
        if (data.w > 0.0) {
            float thickness = data.w / 256.0;
            coverage *= smoothstep(-thickness - aa * 0.5, -thickness + aa * 0.5, distance);
        }
    }
    fragColor = vec4(tint.rgb, tint.a * coverage);
}
```

### src/main/resources/assets/riserecode/shaders/core/ui.vsh

```glsl
#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
in ivec2 UV2;
out vec2 local;
out vec2 screenUV;
out vec4 tint;
flat out vec4 data;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
    screenUV = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
    tint = Color;
    data = vec4(UV1, UV2);
}
```

### src/main/resources/assets/riserecode/shaders/core/window.fsh

```glsl
#version 330
in vec2 local;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    vec2 halfSize = data.xy / 32.0;
    float radius = min(data.z / 256.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(local) - halfSize + radius;
    float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(distance), 0.001);
    vec2 uv = (local + halfSize) / (halfSize * 2.0);
    vec3 color = local.x + halfSize.x < 107.0
        ? mix(vec3(23,31,34), vec3(20,28,30), uv.y) / 255.0
        : mix(vec3(30,34,44), vec3(24,31,37), (uv.x + uv.y) * 0.5) / 255.0;
    fragColor = vec4(color, tint.a * (1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance)));
}
```

### src/main/resources/fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "riserecode",
  "version": "${version}",
  "name": "risecode",
  "description": "A modular client utility mod with a fluid sidebar interface and compact HUD.",
  "environment": "client",
  "entrypoints": {
    "client": ["dev.riserecode.RiseRecodeClient"]
  },
  "mixins": ["riserecode.mixins.json"],
  "depends": {
    "fabricloader": ">=0.19.5",
    "minecraft": "26.2",
    "java": ">=25",
    "fabric-api": ">=0.161.0+26.2"
  }
}
```

### src/main/resources/riserecode.mixins.json

```json
{
  "required": true,
  "package": "dev.riserecode.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": [
    "KeyboardHandlerMixin",
    "ChatInputMixin",
    "ConnectionMixin",
    "GuiRendererMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### build.gradle

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    compileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
    testCompileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
}

processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand version: project.version }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-Xlint:all', '-Werror']
}

tasks.register('foundationCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.FoundationChecks'
}

tasks.register('runtimeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.RuntimeChecks'
}

tasks.register('uiCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.UiChecks'
}

tasks.register('engineCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.EngineChecks'
}

tasks.register('combatCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.CombatChecks'
}

tasks.register('themeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.ThemeChecks'
}

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck', 'uiCheck', 'engineCheck', 'combatCheck', 'themeCheck' }

// The dependency-free checks above use JavaExec rather than a JUnit runner.
tasks.named('test') { enabled = false }

fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'riserecode-test'
        enableGameTests = false
        enableClientGameTests = true
    }
}

loom.runs.clientGameTest {
    runDir = 'build/run/clientGameTest'
    programArgs '--graphicsBackend', providers.gradleProperty('graphicsBackend').getOrElse('OPENGL')
}

// The test instance is disposable. Copy only optional, user-installed font assets after cleanup;
// they remain local to development and never enter either distributable JAR.
tasks.named('runClientGameTest') {
    doFirst {
        copy {
            from 'run/resourcepacks/qraftys-capitalized-font.zip'
            into 'build/run/clientGameTest/resourcepacks'
        }
        copy {
            from 'run/config/riserecode/fonts'
            include '*.ttf', 'qraftys.zip'
            into 'build/run/clientGameTest/config/riserecode/fonts'
        }
    }
}
```

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.2
mod_version=0.6.0
maven_group=dev.riserecode
archives_base_name=riserecode
```

### scripts/install-qraftys.py

```python
#!/usr/bin/env python3
"""Install a user-downloaded Qraftys pack locally, preserving glyphs and original ZIP.

Usage: python scripts/install-qraftys.py /path/to/qraftys.zip [game-directory]
Only pack.mcmeta compatibility metadata is adapted for the pinned Minecraft 26.2.
No third-party artwork is copied into src/main/resources or the distributable JAR.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("source", type=Path)
parser.add_argument("game_directory", type=Path, nargs="?", default=Path("run"))
args = parser.parse_args()
source = args.source.resolve()
game = args.game_directory.resolve()
packs = game / "resourcepacks"
originals = game / "config/riserecode/fonts/originals"
packs.mkdir(parents=True, exist_ok=True)
originals.mkdir(parents=True, exist_ok=True)
digest = hashlib.sha256(source.read_bytes()).hexdigest()
backup = originals / ("qraftys-" + digest[:12] + ".zip")
if not backup.exists():
    shutil.copy2(source, backup)
target = packs / "qraftys-capitalized-font.zip"
if target.exists():
    old_digest = hashlib.sha256(target.read_bytes()).hexdigest()
    old_backup = originals / ("installed-qraftys-" + old_digest[:12] + ".zip")
    if not old_backup.exists():
        shutil.copy2(target, old_backup)
temporary = None
try:
    with zipfile.ZipFile(source) as incoming:
        incoming.getinfo("assets/minecraft/textures/font/ascii.png")
        metadata = json.loads(incoming.read("pack.mcmeta"))
        metadata["pack"].pop("pack_format", None)
        metadata["pack"].pop("supported_formats", None)
        metadata["pack"].update(min_format=[88, 0], max_format=[88, 0])
        with tempfile.NamedTemporaryFile(dir=packs, suffix=".tmp", delete=False) as handle:
            temporary = Path(handle.name)
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as outgoing:
            for entry in incoming.infolist():
                outgoing.writestr(entry, json.dumps(metadata, indent=2).encode() if entry.filename == "pack.mcmeta" else incoming.read(entry))
        with zipfile.ZipFile(temporary) as adapted:
            assert adapted.read("assets/minecraft/textures/font/ascii.png") == incoming.read("assets/minecraft/textures/font/ascii.png")
    temporary.replace(target)
finally:
    if temporary is not None:
        temporary.unlink(missing_ok=True)
print("Installed:", target)
print("Original:", backup)
print("Glyph artwork unchanged; compatibility metadata targets Minecraft 26.2 (88.0).")
```

### src/main/resources/licenses/google-sans-NOTICE.txt

```text
Google Sans static Regular and Medium, official release v14.000.
https://github.com/googlefonts/googlesans/releases/tag/v14.000

Copyright notice embedded in these unmodified font files:
Copyright 2025 The Google Sans Project Authors (github.com/googlefonts/googlesans)

Embedded license notice:
This Font Software is licensed under the SIL Open Font License, Version 1.1.
This license is available with a FAQ at: https://openfontlicense.org

The upstream repository's OFL.txt is included as google-sans-OFL.txt.
```

### src/test/java/dev/riserecode/ThemeChecks.java

```java
package dev.riserecode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeProfile;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

/** Independent visual persistence, atomic validation, HSV math and live animation continuity. */
public final class ThemeChecks {
    private static int assertions;
    private ThemeChecks() { }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("risecode-theme-check-");
        try {
            ThemeManager manager = new ThemeManager(directory, LoggerFactory.getLogger(ThemeChecks.class));
            manager.load();
            check(Files.isRegularFile(directory.resolve("theme.json")) && Files.isDirectory(manager.getFontDirectory()), "default visual config and font directory created");
            ((DoubleSetting) manager.getSetting("Font Scale")).setValue(1.4);
            ((ColorSetting) manager.getSetting("Primary Accent")).setValue(0xFFDC517F);
            ((BooleanSetting) manager.getSetting("World Blur")).setValue(true);
            check(manager.saveProfile("Night"), "named theme saved atomically");
            manager.reset(); check(manager.loadProfile("night"), "theme name normalization and load");
            check(manager.number("Font Scale") == 1.4 && manager.bool("World Blur") && manager.color("Primary Accent") == 0xFFDC517F, "visual values restore together");
            check(manager.getProfiles().equals(java.util.List.of("night")), "only valid named theme profiles listed");
            check(!Files.exists(directory.resolve("config.json")) && !Files.exists(directory.resolve("profiles")), "visual writes never create module configs/profiles");
            ThemeProfile snapshot = manager.snapshot(); JsonObject detached = snapshot.settings(); detached.addProperty("Font Scale", 0.9);
            check(snapshot.settings().get("Font Scale").getAsDouble() == 1.4, "snapshot is isolated from caller mutation");
            JsonObject invalid = snapshot.settings(); invalid.addProperty("Font Scale", 1.2); invalid.addProperty("Glow Radius", 100);
            JsonObject invalidRange = invalid;
            expectInvalid(() -> manager.apply(new ThemeProfile(1, invalidRange)));
            check(manager.number("Font Scale") == 1.4 && manager.number("Glow Radius") == 5, "bad profile cannot partially apply earlier fields");
            invalid = snapshot.settings(); invalid.addProperty("Text Shadow", "false"); JsonObject wrongType = invalid;
            expectInvalid(() -> manager.apply(new ThemeProfile(1, wrongType)));
            check(!manager.loadProfile("missing") && manager.number("Font Scale") == 1.4, "missing profile preserves current state");
            expectInvalid(() -> manager.saveProfile("../outside")); expectInvalid(() -> manager.loadProfile("/absolute"));
            expectInvalid(() -> new ThemeProfile(2, new JsonObject()));
            manager.save(); ThemeManager loaded = new ThemeManager(directory, LoggerFactory.getLogger(ThemeChecks.class)); loaded.load();
            check(loaded.wasLoaded() && loaded.number("Font Scale") == 1.4, "main theme reload");
            JsonObject root = JsonParser.parseString(Files.readString(directory.resolve("theme.json"))).getAsJsonObject();
            check(!root.has("modules") && root.has("settings"), "versioned visual schema is separate");
            JsonObject partial = new JsonObject(); partial.addProperty("Font Scale", 1.1); partial.addProperty("Future Setting", true);
            loaded.apply(new ThemeProfile(1, partial));
            check(loaded.number("Font Scale") == 1.1 && loaded.number("Panel Opacity") == 0.87, "new/missing fields receive defaults; unknown fields ignored");
            check(ColorPicker.hsv(0, 1, 1) == 0xFFFF0000 && ColorPicker.hsv(1.0 / 3, 1, 1) == 0xFF00FF00, "HSV primaries");
            check(ColorPicker.hsv(1, 1, 1) == 0xFFFF0000 && ColorPicker.hsv(0.8, 0, 0.5) == 0xFF808080, "HSV wraps hue and handles gray");
            AtomicReference<MathUtil.Easing> curve = new AtomicReference<>(MathUtil.Easing.LINEAR);
            Animation.configureUi(() -> 2, curve::get);
            Animation raw = new Animation(0, 1, MathUtil.Easing.LINEAR).animateTo(1);
            Animation themed = Animation.ui(0, 1, MathUtil.Easing.OUT_QUART).animateTo(1);
            check(raw.update(0.25) == 0.25 && themed.update(0.25) == 0.5, "UI speed/easing do not change backend animations");
            Theme.advance(1_000_000_000L); Theme.setAccent(Theme.BLUE); Theme.advance(1_060_000_000L);
            check(Theme.accent() == Theme.lerp(Theme.TEAL, Theme.BLUE, 0.5), "accent transitions share the global UI speed and curve");
            curve.set(MathUtil.Easing.OUT_BOUNCE); double before = themed.getValue(); themed.update(0);
            check(themed.getValue() == before, "live curve change preserves rendered position"); themed.update(0.3);
            check(themed.getValue() == 1 && !themed.isRunning(), "retargeted curve settles exactly");
            for (MathUtil.Easing easing : MathUtil.Easing.values()) {
                check(easing.apply(0) == 0 && easing.apply(1) == 1, "curve endpoints " + easing);
            }
            check(((EnumSetting<?>) loaded.getSetting("Font Selection")).getOptions().size() == 5, "all five font slots available");
            Files.writeString(directory.resolve("theme.json"), "{broken-theme");
            ThemeManager recovered = new ThemeManager(directory, LoggerFactory.getLogger(ThemeChecks.class)); recovered.load();
            try (var files = Files.list(directory)) {
                Path backup = files.filter(path -> path.getFileName().toString().startsWith("theme-invalid-")).findFirst().orElseThrow();
                check(Files.readString(backup).equals("{broken-theme"), "unreadable main theme preserved before defaults are saved");
            }
            check(JsonParser.parseString(Files.readString(directory.resolve("theme.json"))).getAsJsonObject().has("settings"), "fresh valid visual defaults recover corrupt main config");
            Animation.configureUi(() -> 1, () -> null);
            System.out.println("Theme checks passed (" + assertions + " assertions).");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
    }
    private static void expectInvalid(Runnable action) {
        try { action.run(); throw new AssertionError("Expected invalid theme rejection"); }
        catch (IllegalArgumentException expected) { assertions++; }
    }
    private static void check(boolean result, String message) { assertions++; if (!result) { throw new AssertionError(message); } }
}
```

### src/test/java/dev/riserecode/FoundationChecks.java

```java
package dev.riserecode;

import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free behavioral checks; run with bash scripts/verify.sh. */
public final class FoundationChecks {
    private static int assertions;

    private FoundationChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkEasing();
        checkAnimation();
        checkEventDispatch();
        checkCancellationAndFailures();
        checkConcurrentRegistry();
        checkSettings();
        checkModuleLifecycle();
        System.out.println("Foundation checks passed (" + assertions + " assertions).");
    }

    private static void checkEasing() {
        for (MathUtil.Easing easing : MathUtil.Easing.values()) {
            near(0.0, easing.apply(0.0), "easing start");
            near(1.0, easing.apply(1.0), "easing end");
            near(0.0, easing.apply(-2.0), "easing lower clamp");
            near(1.0, easing.apply(2.0), "easing upper clamp");
        }
        near(0.9375, MathUtil.easeOutQuart(0.5), "quart midpoint");
        near(0.0625, MathUtil.easeInOutCubic(0.25), "cubic first half");
        near(0.9375, MathUtil.easeInOutCubic(0.75), "cubic second half");
        check(MathUtil.easeOutElastic(0.4) > 1.0, "elastic overshoot is retained");
        near(12.0, MathUtil.lerp(0.0, 10.0, 1.2), "interpolation permits overshoot");
        expect(IllegalArgumentException.class, () -> MathUtil.easeOutQuart(Double.NaN));
    }

    private static void checkAnimation() {
        Animation coarse = new Animation(0.0, 1.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        Animation fine = new Animation(0.0, 1.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        coarse.update(0.5);
        for (int frame = 0; frame < 5; frame++) {
            fine.animateTo(1.0);
            fine.update(0.1);
        }
        near(coarse.getValue(), fine.getValue(), "frame-rate independent midpoint");
        check(fine.getState() == Animation.State.RUNNING, "repeated target keeps running");
        fine.pause();
        double paused = fine.getValue();
        near(paused, fine.update(10.0), "pause preserves current value");
        fine.resume();
        for (int frame = 0; frame < 5; frame++) {
            fine.update(0.1);
        }
        near(1.0, fine.getValue(), "exact endpoint");
        check(fine.getState() == Animation.State.FINISHED, "rounding cannot delay completion");
        fine.animateTo(0.0);
        fine.update(0.25);
        double beforeRetarget = fine.getValue();
        fine.animateTo(2.0);
        near(beforeRetarget, fine.getValue(), "retarget has no position jump");
        fine.update(100.0);
        near(2.0, fine.getValue(), "large delta finishes at target");
        fine.reset(0.25);
        check(fine.getState() == Animation.State.IDLE, "reset returns to idle");
        near(0.0, fine.getProgress(), "reset clears progress");
        Animation instant = new Animation(0.0, 0.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        near(1.0, instant.getValue(), "zero duration completes immediately");
        check(instant.getState() == Animation.State.FINISHED, "zero duration state");
        Animation elastic = new Animation(0.0, 1.0, MathUtil.Easing.OUT_ELASTIC).animateTo(1.0);
        check(elastic.update(0.4) > 1.0, "animation preserves bounce");
        expect(IllegalArgumentException.class, () -> fine.update(-0.01));
        expect(IllegalArgumentException.class, () -> fine.update(Double.NaN));
        expect(IllegalArgumentException.class, () -> fine.animateTo(Double.POSITIVE_INFINITY));
        expect(IllegalArgumentException.class,
                () -> new Animation(0.0, -1.0, MathUtil.Easing.OUT_QUART));
    }

    private static void checkEventDispatch() {
        EventBus bus = strictBus();
        List<String> order = new ArrayList<>();
        EventBus.Subscription first = bus.subscribe(UpdateEvent.class, 0, false, e -> order.add("first"));
        bus.subscribe(Event.class, 10, false, e -> order.add("base"));
        bus.subscribe(UpdateEvent.class, 0, false, e -> order.add("second"));
        UpdateEvent event = new UpdateEvent();
        check(bus.post(event) == event, "post returns original event");
        check(order.equals(List.of("base", "first", "second")), "priority and stable order");
        first.close();
        first.close();
        check(!first.isActive(), "subscription close is idempotent");
        order.clear();
        bus.post(new UpdateEvent());
        check(order.equals(List.of("base", "second")), "closed listener leaves cached dispatch");
        check(bus.getListenerCount() == 2, "listener count reflects close");

        EventBus mutating = strictBus();
        AtomicInteger removedCalls = new AtomicInteger();
        EventBus.Subscription removed = mutating.subscribe(UpdateEvent.class, e -> removedCalls.incrementAndGet());
        mutating.subscribe(UpdateEvent.class, 20, false, e -> removed.close());
        mutating.post(new UpdateEvent());
        check(removedCalls.get() == 0, "close during dispatch skips later callback");

        EventBus growing = strictBus();
        AtomicInteger addedCalls = new AtomicInteger();
        EventBus.Subscription installer = growing.subscribe(UpdateEvent.class,
                e -> growing.subscribe(UpdateEvent.class, next -> addedCalls.incrementAndGet()));
        growing.post(new UpdateEvent());
        check(addedCalls.get() == 0, "new subscription waits for next dispatch");
        installer.close();
        growing.post(new UpdateEvent());
        check(addedCalls.get() == 1, "new registry invalidates dispatch cache");
    }

    private static void checkCancellationAndFailures() {
        EventBus bus = strictBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(PacketEvent.class, 10, false, e -> {
            calls.add("cancel");
            e.cancel();
        });
        bus.subscribe(PacketEvent.class, 0, false, e -> calls.add("skip"));
        bus.subscribe(Event.class, -10, true, e -> calls.add("observe"));
        PacketEvent packet = bus.post(new PacketEvent());
        check(packet.isCancelled(), "packet cancellation reaches hook");
        check(calls.equals(List.of("cancel", "observe")), "cancelled event filters listeners");
        expect(IllegalStateException.class, () -> new UpdateEvent().cancel());

        List<RuntimeException> errors = new ArrayList<>();
        EventBus resilient = new EventBus((event, failure) -> errors.add(failure));
        AtomicInteger laterCalls = new AtomicInteger();
        resilient.subscribe(UpdateEvent.class, e -> {
            throw new IllegalArgumentException("intentional listener failure");
        });
        resilient.subscribe(UpdateEvent.class, e -> laterCalls.incrementAndGet());
        resilient.post(new UpdateEvent());
        check(errors.size() == 1, "listener failure is reported");
        check(laterCalls.get() == 1, "listener failure does not stop later callbacks");
    }

    private static void checkConcurrentRegistry() throws Exception {
        EventBus bus = strictBus();
        AtomicInteger persistentCalls = new AtomicInteger();
        bus.subscribe(UpdateEvent.class, e -> persistentCalls.incrementAndGet());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var posting = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 2_000; i++) {
                    bus.post(new UpdateEvent());
                }
                return null;
            });
            var changing = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 500; i++) {
                    EventBus.Subscription temporary = bus.subscribe(Event.class, e -> { });
                    temporary.close();
                }
                return null;
            });
            start.countDown();
            posting.get();
            changing.get();
        }
        check(persistentCalls.get() == 2_000, "concurrent registry changes keep persistent listeners");
        check(bus.getListenerCount() == 1, "concurrent temporary subscriptions all removed");
        bus.post(new UpdateEvent());
        check(persistentCalls.get() == 2_001, "final registry cache remains valid");
    }

    private static void checkSettings() {
        BooleanSetting show = new BooleanSetting("show", "Show advanced settings", false);
        DoubleSetting scale = new DoubleSetting("scale", "HUD scale", 1.0, 0.5, 2.0);
        scale.setVisibleWhen(show::getValue);
        check(!scale.isVisible(), "visibility reads current predicate");
        show.toggle();
        check(scale.isVisible(), "visibility changes with controlling setting");
        scale.setValue(1.5);
        expect(IllegalArgumentException.class, () -> scale.setValue(3.0));
        near(1.5, scale.getValue(), "failed validation preserves previous value");
        expect(IllegalArgumentException.class, () -> scale.setValue(Double.NaN));
        expect(IllegalArgumentException.class,
                () -> new DoubleSetting("bad", "Invalid default", 3.0, 0.0, 1.0));
        expect(IllegalArgumentException.class,
                () -> new DoubleSetting("bad", "Invalid bounds", 0.0, 2.0, 1.0));
        scale.reset();
        near(1.0, scale.getValue(), "reset restores typed default");
        expect(NullPointerException.class, () -> show.setValue(null));
        EnumSetting<MathUtil.Easing> easing = new EnumSetting<>("easing", "Panel curve", MathUtil.Easing.OUT_QUART);
        check(easing.getOptions().size() == 5, "enum exposes all options");
        easing.setValue(MathUtil.Easing.IN_OUT_CUBIC);
        easing.reset();
        check(easing.getValue() == MathUtil.Easing.OUT_QUART, "enum reset");
        expect(UnsupportedOperationException.class, () -> easing.getOptions().clear());
        ColorSetting color = new ColorSetting("accent", "Active color", ColorSetting.argb(128, 59, 130, 246));
        check(color.getAlpha() == 128 && color.getRed() == 59
                && color.getGreen() == 130 && color.getBlue() == 246, "ARGB round trip");
        expect(IllegalArgumentException.class, () -> ColorSetting.argb(256, 0, 0, 0));
    }

    private static void checkModuleLifecycle() {
        EventBus bus = strictBus();
        TestModule module = new TestModule(bus);
        check(!module.isEnabled() && module.getKeybind() == Module.UNBOUND, "module defaults");
        check(module.getSettings().size() == 1, "module owns typed setting list");
        expect(UnsupportedOperationException.class, () -> module.getSettings().clear());
        expect(IllegalArgumentException.class,
                () -> module.addSetting(new BooleanSetting("visible", "Duplicate", false)));
        expect(IllegalArgumentException.class, () -> module.setKeybind(-2));
        module.setKeybind(344);
        check(module.getKeybind() == 344, "keybind assignment");
        module.setEnabled(true);
        module.setEnabled(true);
        check(module.enables == 1 && bus.getListenerCount() == 1, "enable is idempotent");
        check(module.updates == 0, "handlers wait for enable to finish");
        bus.post(new UpdateEvent());
        check(module.updates == 1, "enabled module receives event");
        module.setEnabled(false);
        bus.post(new UpdateEvent());
        check(module.updates == 1 && bus.getListenerCount() == 0, "disable releases listeners");
        module.toggle();
        check(bus.getListenerCount() == 1, "re-enable registers once");
        module.toggle();

        module.failEnable = true;
        module.failDisable = true;
        RuntimeException enableFailure = expect(IllegalArgumentException.class, () -> module.setEnabled(true));
        check(enableFailure.getSuppressed().length == 1, "rollback preserves enable failure");
        check(!module.isEnabled() && bus.getListenerCount() == 0, "failed enable releases listeners");
        module.failEnable = false;
        module.failDisable = false;
        module.setEnabled(true);
        module.failDisable = true;
        expect(IllegalStateException.class, () -> module.setEnabled(false));
        check(!module.isEnabled() && bus.getListenerCount() == 0, "failed disable still removes listeners");

        EventBus reentrantBus = strictBus();
        Module reentrant = new Module("recursive", "Reject lifecycle recursion", Category.UTILITY, reentrantBus) {
            @Override
            protected void onEnable() {
                toggle();
            }
        };
        expect(IllegalStateException.class, () -> reentrant.setEnabled(true));
        check(!reentrant.isEnabled(), "lifecycle recursion rolls back cleanly");
    }

    private static EventBus strictBus() {
        return new EventBus((event, failure) -> {
            throw new AssertionError("Unexpected listener failure", failure);
        });
    }

    private static void near(double expected, double actual, String message) {
        check(Double.isFinite(actual) && Math.abs(expected - actual) < 1.0e-10,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return type.cast(failure);
            }
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static final class UpdateEvent extends Event {
    }

    private static final class PacketEvent extends Event {
        private PacketEvent() {
            super(true);
        }
    }

    private static final class TestModule extends Module {
        private final EventBus bus;
        private int enables;
        private int updates;
        private boolean failEnable;
        private boolean failDisable;

        private TestModule(EventBus bus) {
            super("test", "Test lifecycle cleanup", Category.UTILITY, bus);
            this.bus = bus;
            register(new BooleanSetting("visible", "Test flag", true));
        }

        private void addSetting(Setting<?> setting) {
            register(setting);
        }

        @Override
        protected void onEnable() {
            enables++;
            listen(UpdateEvent.class, event -> updates++);
            bus.post(new UpdateEvent());
            if (failEnable) {
                throw new IllegalArgumentException("intentional enable failure");
            }
        }

        @Override
        protected void onDisable() {
            if (failDisable) {
                throw new IllegalStateException("intentional disable failure");
            }
        }
    }
}
```

### src/gametest/java/dev/riserecode/ClientIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.util.RenderUtil;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.hud.HudRenderer;
import net.minecraft.client.input.CharacterEvent;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 240;
    private enum Mode { FIRST, SECOND }
    private static final class ControlsModule extends Module {
        private final BooleanSetting advanced = register(new BooleanSetting("Advanced", "", false));
        private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "", Mode.FIRST));
        private final ColorSetting color = register(new ColorSetting("Accent", "", 0x803B82F6));
        private final DoubleSetting conditional = register(new DoubleSetting("Conditional", "", 0.5, 0, 1));
        private ControlsModule(String name) {
            super(name, "Client test fixture", Category.UTILITY, Client.INSTANCE.getEventBus());
            conditional.setVisibleWhen(advanced::getValue);
            for (int i = 0; i < 8; i++) { register(new DoubleSetting("Slider " + (i + 1), "", 0.5, 0, 1)); }
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> minecraft.options.guiScale().set(2));
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getThemeManager().reset();
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitTick();
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> minecraft.gui.screen() == gui()), "Right Shift reverses an unfinished exit");
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.THEMES));
        context.waitTicks(5);
        context.runOnClient(minecraft -> gui().getThemeEditor().reveal("HUD Visible"));
        context.waitTicks(8);
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
        // Search through the real character-input adapter; a space matches formatted module names.
        context.runOnClient(minecraft -> {
            gui().getSearchField().setFocused(true);
            "full bright".codePoints().forEach(cp -> gui().charTyped(new CharacterEvent(cp)));
        });
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().size() == 1);
        check(context.computeOnClient(minecraft -> gui().getActivePanel().getVisibleButtons().getFirst().getModule() == fullBright()), "search filters formatted names");
        context.runOnClient(minecraft -> gui().getSearchField().setValue("no matching module"));
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().isEmpty());
        context.runOnClient(minecraft -> gui().getSearchField().setValue(""));
        context.waitTicks(5);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 63, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().getFirst().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue()) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.runOnClient(minecraft -> gui().selectCategory(Category.UTILITY));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().size() == 2);
        context.waitTicks(5);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 40, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.color.getValue() == 0x80D946EF), "color palette preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-4);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 50, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Drag the single window through the actual native mouse handler.
        double startX = context.computeOnClient(minecraft -> gui().getWindowBounds().x());
        move(context, () -> gui().getWindowBounds(), 0.08, 0.04);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> gui().getWindowBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().y() >= 8), "window drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().x() >= 7.99
                && (gui().getWindowBounds().x() + gui().getWindowBounds().width()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledWidth()
                && (gui().getWindowBounds().y() + gui().getWindowBounds().height()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledHeight()), "small window fits and stays clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        AtomicInteger hudFrames = new AtomicInteger();
        AtomicInteger worldFrames = new AtomicInteger();
        var hudSubscription = Client.INSTANCE.getEventBus().subscribe(Render2DEvent.class, event -> hudFrames.incrementAndGet());
        var worldSubscription = Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class, event -> worldFrames.incrementAndGet());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(minecraft -> Client.INSTANCE.getModuleManager().setKeybind(fullBright(), InputConstants.KEY_B));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> fullBright().isEnabled()), "module key toggles once");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == fullBright() && entry.visibility() == 1), ANIMATION_TIMEOUT);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.runOnClient(minecraft -> { controls.setEnabled(true); longer.setEnabled(true); });
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 3);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % HudRenderer.ROW_HEIGHT == 0), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, Theme.displayName(entry.module().getName()), false))).orElseThrow();
                return widest.y() == 0;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 0 || entry.y() == HudRenderer.ROW_HEIGHT), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.THEMES));
            context.waitTicks(5);
            context.runOnClient(minecraft -> {
                Theme.setAccent(Theme.BLUE);
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.15);
                Client.INSTANCE.getThemeManager().changed();
            });
            context.waitFor(minecraft -> Theme.accent() == Theme.BLUE && Math.abs(gui().getCanvasScale() - 1.15) < 0.001, ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> Theme.accentTarget() == Theme.BLUE), "theme selection animates across GUI and HUD");
            context.runOnClient(minecraft -> {
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.0);
                Theme.setAccent(Theme.TEAL); gui().getThemeEditor().reveal("Font Selection");
            });
            context.waitFor(minecraft -> gui().getCanvasScale() == 1 && Theme.accent() == Theme.TEAL, ANIMATION_TIMEOUT);
            context.waitTicks(8);
            context.takeScreenshot("riserecode-themes");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.FRIENDS));
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiFriend"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI adds through the existing manager");
            context.takeScreenshot("riserecode-friends");
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> !Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI removes through the existing manager");
            context.runOnClient(minecraft -> { gui().selectPage(ClickGui.Page.PROFILES); fullBright().getBrightness().setValue(0.6); });
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiProfile"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(Files.exists(Client.INSTANCE.getProfileManager().getFile("GuiProfile")), "profile UI saves a real snapshot");
            context.runOnClient(minecraft -> fullBright().getBrightness().setValue(0.2));
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() == 0.6), "profile UI restores settings and refreshes");
            context.takeScreenshot("riserecode-profiles");
            context.runOnClient(minecraft -> {
                fullBright().getBrightness().setValue(1.0); gui().selectPage(ClickGui.Page.SEARCH);
                gui().getSearchField().setValue("tracer");
            });
            context.waitTicks(6);
            context.takeScreenshot("riserecode-search");
            context.getInput().resizeWindow(2560, 1440);
            context.waitTicks(5);
            double referenceWidth = context.computeOnClient(minecraft -> gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth());
            context.runOnClient(minecraft -> minecraft.options.guiScale().set(3));
            context.waitTicks(5);
            check(context.computeOnClient(minecraft -> Math.abs(gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth() - referenceWidth) < 1), "reference window size is independent of Minecraft GUI scale");
            context.runOnClient(minecraft -> {
                gui().getSearchField().setValue("render");
                var module = Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow();
                module.setEnabled(true);
            });
            context.waitTicks(6);
            context.runOnClient(minecraft -> {
                var button = gui().getModuleButton("Tracers");
                if (!button.isExpanded()) { button.mouseClicked(button.getBounds().x() + 10, button.getBounds().y() + 10, 1); }
            });
            context.waitTicks(8);
            context.takeScreenshot("risecode-reference-layout");
            context.runOnClient(minecraft -> {
                Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow().setEnabled(false);
                minecraft.options.guiScale().set(2);
            });
            context.getInput().resizeWindow(2048, 1152);
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue(""));
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
            EngineIntegrationTest.verify(context, world);
            CombatIntegrationTest.verify(context, world);
            ThemeIntegrationTest.verify(context);
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().setKeybind(fullBright(), -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static FullBrightModule fullBright() { return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static Panel panel(Category category) { return gui().getPanels().stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow(); }
    private static ModuleButton button(String name) {
        return gui().getModuleButton(name);
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().isOpen(), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.centerY() - control.centerY()) / 24;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 24));
        context.waitFor(minecraft -> Math.abs(panel.getScrollOffset() - target) < 0.01, ANIMATION_TIMEOUT);
    }
    private static void move(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> {
            var b = bounds.get();
            return new double[] {(b.x() + b.width() * x) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                    (b.y() + b.height() * y) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTick();
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int mouseButton) {
        // Headers stay fixed when a module expands; clicking the center of the full body would edit a setting.
        Supplier<UIComponent.Bounds> header = () -> {
            var b = bounds.get(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), Math.min(28, b.height()));
        };
        move(context, header, 0.4, 0.5);
        context.getInput().pressMouse(mouseButton);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
```

### src/gametest/java/dev/riserecode/EngineIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.TracersModule;
import dev.riserecode.testmixin.ChatStateAccessor;
import dev.riserecode.ui.clickgui.ClickGui;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;

/** Uses the already loaded Phase 4 test world; no connections to an external server. */
public final class EngineIntegrationTest {
    private EngineIntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var client = Client.INSTANCE;
        var modules = client.getModuleManager();
        var sprint = context.computeOnClient(minecraft -> modules.getByClass(SprintModule.class).orElseThrow());
        var aura = context.computeOnClient(minecraft -> modules.getByClass(KillAuraModule.class).orElseThrow());
        var tracers = context.computeOnClient(minecraft -> modules.getByClass(TracersModule.class).orElseThrow());
        var noFall = context.computeOnClient(minecraft -> modules.getByClass(NoFallModule.class).orElseThrow());
        AtomicInteger chatPackets = new AtomicInteger();
        AtomicInteger attacks = new AtomicInteger();
        AtomicInteger receivePackets = new AtomicInteger();
        AtomicInteger leakedServerSends = new AtomicInteger();
        AtomicReference<String> receiveThread = new AtomicReference<>();
        var sends = client.getEventBus().subscribe(PacketEvent.Send.class, -200, true, event -> {
            if (event.getPacket() instanceof ServerboundChatPacket) { chatPackets.incrementAndGet(); }
            if (event.getPacket() instanceof ServerboundAttackPacket) { attacks.incrementAndGet(); }
            if (event.getPacket() instanceof ClientboundSystemChatPacket) { leakedServerSends.incrementAndGet(); }
        });
        var receives = client.getEventBus().subscribe(PacketEvent.Receive.class, 100, false, event -> {
            receivePackets.incrementAndGet(); receiveThread.set(Thread.currentThread().getName());
            if (event.getPacket() instanceof ClientboundSystemChatPacket chat) {
                if (chat.content().getString().equals("engine-receive-cancel")) { event.cancel(); }
                if (chat.content().getString().equals("engine-receive-before")) {
                    event.setPacket(new ClientboundSystemChatPacket(Component.literal("engine-receive-after"), false));
                }
            }
        });
        RemotePlayer[] actors = new RemotePlayer[2];
        try {
            command(context, ".help"); command(context, ".unknown");
            check(chatPackets.get() == 0, "local commands never produce chat packets");
            command(context, ".bind Sprint F6");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == InputConstants.KEY_F6), "bind command reaches module manager");
            command(context, ".bind Sprint RIGHT_SHIFT");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == InputConstants.KEY_F6), "GUI key conflict leaves module bind intact");
            command(context, ".t Sprint");
            check(context.computeOnClient(minecraft -> sprint.isEnabled()), "toggle alias initializes module");
            context.getInput().holdKey(InputConstants.KEY_W);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.player.isSprinting()), "forward input requests sprint");
            context.getInput().releaseKey(InputConstants.KEY_W);
            context.runOnClient(minecraft -> sprint.getOmniSprint().setValue(true));
            context.getInput().holdKey(InputConstants.KEY_S);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.player.isSprinting()), "omni input requests sprint");
            context.getInput().releaseKey(InputConstants.KEY_S);
            context.getInput().pressKey(InputConstants.KEY_F6);
            check(context.computeOnClient(minecraft -> !sprint.isEnabled()), "bound real key toggles module off once");
            command(context, ".bind Sprint NONE");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == -1), "NONE unbinds module");

            context.runOnClient(minecraft -> {
                noFall.getMode().setValue(NoFallModule.Mode.VANILLA); modules.setEnabled(noFall, true);
                minecraft.player.fallDistance = 6;
                client.getEventBus().post(new UpdateEvent(minecraft, 0));
                check(minecraft.player.fallDistance == 0, "NoFall vanilla local reset");
                noFall.getMode().setValue(NoFallModule.Mode.PACKET);
            });
            AtomicBoolean cancelledCallback = new AtomicBoolean();
            AtomicReference<Packet<?>> originalPacket = new AtomicReference<>();
            AtomicReference<Packet<?>> replacementPacket = new AtomicReference<>();
            AtomicBoolean resetAtSend = new AtomicBoolean();
            var replace = client.getEventBus().subscribe(PacketEvent.Send.class, -10, false, event -> {
                if (event.getPacket() == originalPacket.get()) {
                    resetAtSend.set(net.minecraft.client.Minecraft.getInstance().player.fallDistance == 0);
                    event.setPacket(replacementPacket.get()); event.cancel();
                }
            });
            try {
                context.runOnClient(minecraft -> {
                    minecraft.player.fallDistance = 7;
                    var packet = new ServerboundMovePlayerPacket.StatusOnly(false, false);
                    var replacement = new ServerboundMovePlayerPacket.StatusOnly(false, true);
                    originalPacket.set(packet); replacementPacket.set(replacement);
                    minecraft.getConnection().getConnection().send(packet, future -> cancelledCallback.set(!future.isSuccess()));
                    check(minecraft.player.fallDistance == 0 && !packet.isOnGround(), "packet-mode reset does not spoof ground state");
                });
                context.waitFor(minecraft -> cancelledCallback.get());
                check(resetAtSend.get(), "NoFall observes real network send before cancellation");
            } finally { replace.close(); }
            context.runOnClient(minecraft -> {
                modules.setEnabled(noFall, false);
                minecraft.player.fallDistance = 4;
                client.getEventBus().post(new UpdateEvent(minecraft, 0));
                check(minecraft.player.fallDistance == 4, "disabled NoFall unsubscribes");
                minecraft.player.fallDistance = 0;
            });

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.send(new ClientboundSystemChatPacket(Component.literal("engine-receive-cancel"), false));
                player.connection.send(new ClientboundSystemChatPacket(Component.literal("engine-receive-before"), false));
            });
            context.waitFor(minecraft -> hasChat("engine-receive-after"));
            check(context.computeOnClient(minecraft -> !hasChat("engine-receive-cancel") && !hasChat("engine-receive-before")), "receive cancellation/replacement affect delivered chat");
            check(receivePackets.get() >= 2 && !"Render thread".equals(receiveThread.get()), "receive events run on network thread");
            check(leakedServerSends.get() == 0, "integrated server connection is excluded from client hooks");

            context.runOnClient(minecraft -> {
                double x = minecraft.player.getX(); double y = minecraft.player.getY(); double z = minecraft.player.getZ();
                actors[0] = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), "FriendNPC"));
                actors[1] = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), "EnemyNPC"));
                for (int i = 0; i < actors.length; i++) {
                    actors[i].setId(1_000_000 + i); actors[i].snapTo(x + 1 + i, y, z + 1);
                    actors[i].setOldPosAndRot(); minecraft.level.addEntity(actors[i]);
                }
                aura.getTargets().setValue(KillAuraModule.Targets.PLAYERS);
                aura.getRange().setValue(3.0);
            });
            command(context, ".friend add FriendNPC");
            check(context.computeOnClient(minecraft -> client.getFriendManager().isFriend("friendnpc")), "friend command updates whitelist");
            AtomicReference<UUID> selected = new AtomicReference<>();
            var auraObserver = client.getEventBus().subscribe(AttackEvent.class, 50, false, event -> {
                selected.set(event.getTarget().getUUID()); event.cancel();
            });
            try {
                command(context, ".t KillAura");
                context.waitFor(minecraft -> selected.get() != null);
                check(selected.get().equals(actors[1].getUUID()), "aura skips nearer friend and picks nearest valid player");
                check(attacks.get() == 0, "cancelled attack intent stops game-mode attack");
            } finally { auraObserver.close(); context.runOnClient(minecraft -> modules.setEnabled(aura, false)); }
            command(context, ".t Tracers");
            context.waitFor(minecraft -> client.getWorldRenderer().getLines().size() == 2);
            check(context.computeOnClient(minecraft -> client.getWorldRenderer().getLines().stream()
                    .anyMatch(line -> line.color() == tracers.getFriendColor().getValue())), "friends have a distinct tracer color");
            context.waitTicks(8);
            context.takeScreenshot("riserecode-tracers");

            // A real server-spawned mob verifies the ordinary attack packet and vanilla damage path.
            double[] position = context.computeOnClient(minecraft -> new double[] {
                    minecraft.player.getX() + 2, minecraft.player.getY(), minecraft.player.getZ()});
            world.getServer().runCommand(String.format(java.util.Locale.ROOT,
                    "summon minecraft:pig %.3f %.3f %.3f {Tags:[\"riserecode_engine\"],NoAI:1b}", position[0], position[1], position[2]));
            UUID pigUuid = world.getServer().computeOnServer(server -> java.util.stream.StreamSupport
                    .stream(server.overworld().getAllEntities().spliterator(), false)
                    .filter(entity -> entity.entityTags().contains("riserecode_engine")).map(Entity::getUUID).findFirst().orElseThrow());
            context.waitFor(minecraft -> minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class,
                    minecraft.player.getBoundingBox().inflate(4), entity -> entity.getUUID().equals(pigUuid)).size() == 1);
            context.runOnClient(minecraft -> { aura.getTargets().setValue(KillAuraModule.Targets.MOBS); modules.setEnabled(aura, true); });
            context.waitFor(minecraft -> attacks.get() > 0);
            world.getServer().waitFor(server -> java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                    .filter(entity -> entity.getUUID().equals(pigUuid))
                    .anyMatch(entity -> entity instanceof net.minecraft.world.entity.LivingEntity living && living.getHealth() < living.getMaxHealth()));
            context.runOnClient(minecraft -> modules.setEnabled(aura, false));

            context.runOnClient(minecraft -> {
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), true);
                modules.setKeybind(sprint, InputConstants.KEY_F7);
                client.getHudRenderer().setEnabled(true);
            });
            command(context, ".profile save Engine");
            check(Files.exists(client.getProfileManager().getFile("Engine")), "profile command writes a named snapshot");
            command(context, ".friend add AfterSave");
            context.runOnClient(minecraft -> {
                modules.setEnabled(tracers, false);
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), false);
                modules.setKeybind(sprint, -1); client.getHudRenderer().setEnabled(false);
                client.getCommandManager().setPrefix("-");
            });
            command(context, "-profile load Engine");
            check(context.computeOnClient(minecraft -> tracers.isEnabled() && modules.getByClass(FullBrightModule.class).orElseThrow().isEnabled()
                    && sprint.getKeybind() == InputConstants.KEY_F7 && !client.getHudRenderer().isEnabled()), "module profile restores modules and binds, preserving independent HUD preferences");
            check(context.computeOnClient(minecraft -> client.getFriendManager().isFriend("AfterSave") && client.getCommandManager().getPrefix().equals("-")), "profile preserves global friends and prefix");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            context.waitFor(minecraft -> client.getClickGui().isOpen(), 2000);
            context.runOnClient(minecraft -> check(client.getProfileManager().loadProfile("Engine"), "profile loads while GUI open"));
            check(context.computeOnClient(minecraft -> client.getClickGui().getPanels().stream()
                    .flatMap(panel -> panel.getModuleButtons().stream()).anyMatch(button -> button.getModule() == tracers && button.getModule().isEnabled())), "open GUI reflects profile state");
            context.takeScreenshot("riserecode-phase5-gui");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            command(context, "-friend remove FriendNPC");
            check(context.computeOnClient(minecraft -> !client.getFriendManager().isFriend("FriendNPC")), "friend removal command");
            check(chatPackets.get() == 0, "all commands remain local through chat mixin");
        } finally {
            sends.close(); receives.close();
            context.runOnClient(minecraft -> {
                modules.setEnabled(sprint, false); modules.setEnabled(aura, false); modules.setEnabled(tracers, false); modules.setEnabled(noFall, false);
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), false);
                modules.setKeybind(sprint, -1); client.getCommandManager().setPrefix(".");
                client.getFriendManager().remove("FriendNPC"); client.getFriendManager().remove("AfterSave");
                for (var actor : actors) { if (actor != null && minecraft.level != null) { minecraft.level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED); } }
                client.getConfigManager().save();
            });
        }
    }
    private static void command(ClientGameTestContext context, String message) {
        context.runOnClient(minecraft -> minecraft.getConnection().sendChat(message)); context.waitTick();
    }
    private static boolean hasChat(String text) {
        var state = net.minecraft.client.Minecraft.getInstance().gui.hud.getChat().storeState();
        return ((ChatStateAccessor) state).riserecode$getMessages().stream().anyMatch(message -> message.content().getString().contains(text));
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
```

### src/gametest/java/dev/riserecode/ThemeIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Real keyboard/mouse, native STB glyphs, live profiles and GPU blur on Minecraft 26.2/OpenGL. */
public final class ThemeIntegrationTest {
    private ThemeIntegrationTest() { }
    public static void verify(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getThemeManager().reset();
            Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(true);
        });
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitForScreen(ClickGui.class);
        context.waitFor(minecraft -> gui().isOpen(), 3000);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.THEMES)); context.waitTicks(6);
        int originalWidth = context.computeOnClient(minecraft -> RenderUtil.textWidth(minecraft.font, "AVATAR", false));
        reveal(context, "Font Scale"); click(context, "Font Scale", 0.65, 0.7);
        check(context.computeOnClient(minecraft -> themes().number("Font Scale") > 1.5
                && RenderUtil.textWidth(minecraft.font, "AVATAR", false) > originalWidth), "real font slider immediately changes glyph metrics");
        number(context, "Font Scale", 1); reveal(context, "Font Selection");
        for (ThemeManager.Typeface choice : new ThemeManager.Typeface[] {ThemeManager.Typeface.QRAFTYS, ThemeManager.Typeface.MINECRAFTIA,
                ThemeManager.Typeface.MONTSERRAT, ThemeManager.Typeface.INTER, ThemeManager.Typeface.GOOGLE_SANS}) {
            click(context, "Font Selection", 0.3, 0.5); context.waitTicks(4);
            check(context.computeOnClient(minecraft -> themes().choice("Font Selection", ThemeManager.Typeface.class) == choice), "font picker " + choice);
            if (choice == ThemeManager.Typeface.QRAFTYS && CustomTextRenderer.available(choice)) {
                context.runOnClient(minecraft -> {
                    check(CustomTextRenderer.status().contains("Qraftys resource-pack glyphs"), "real bitmap pack instead of uppercase fallback");
                    try (ZipFile zip = new ZipFile(minecraft.gameDirectory.toPath().resolve("resourcepacks/qraftys-capitalized-font.zip").toFile());
                            var stream = zip.getInputStream(zip.getEntry("assets/minecraft/textures/font/ascii.png"));
                            NativeImage image = NativeImage.read(stream)) {
                        int expected = 0;
                        for (int code : "risecode".codePoints().toArray()) {
                            int extent = 0;
                            for (int x = 0; x < 8; x++) {
                                for (int y = 0; y < 8; y++) { if ((image.getPixel(code % 16 * 8 + x, code / 16 * 8 + y) >>> 24) != 0) { extent = x + 1; } }
                            }
                            expected += extent + 1;
                        }
                        check(RenderUtil.textWidth(minecraft.font, "risecode", false) == expected, "original small-cap glyph advances");
                    } catch (java.io.IOException failure) { throw new AssertionError("Qraftys pack read", failure); }
                    minecraft.getResourcePackRepository().reload();
                    var pack = minecraft.getResourcePackRepository().getPack("file/qraftys-capitalized-font.zip");
                    check(pack != null && pack.getCompatibility().isCompatible(), "local resource pack accepted by Minecraft 26.2");
                });
                context.takeScreenshot("risecode-font-qraftys");
            } else if (choice == ThemeManager.Typeface.MONTSERRAT || choice == ThemeManager.Typeface.INTER
                    || choice == ThemeManager.Typeface.MINECRAFTIA && CustomTextRenderer.available(choice)) {
                check(CustomTextRenderer.available(choice), "bundled face " + choice);
                context.takeScreenshot("risecode-font-" + choice.name().toLowerCase(Locale.ROOT));
            } else if (!CustomTextRenderer.available(choice)) {
                check(CustomTextRenderer.status().contains("using Inter"), "missing optional face is labelled honestly");
            }
        }
        UIComponent.Bounds reloadButton = context.computeOnClient(minecraft -> gui().getThemeEditor().getActions().getLast().getBounds());
        move(context, reloadButton.x() + reloadButton.width() / 2, reloadButton.y() + reloadButton.height() / 2);
        context.getInput().pressMouse(0); context.waitTicks(4);
        check(CustomTextRenderer.status().contains("STB distance-field"), "font atlases reload without a client restart");
        reveal(context, "Primary Accent");
        UIComponent picker = context.computeOnClient(minecraft -> gui().getThemeEditor().getControl("Primary Accent"));
        int oldColor = themes().color("Primary Accent");
        var b = context.computeOnClient(minecraft -> picker.getBounds());
        move(context, b.x() + (b.width() - 20) * 0.8, b.y() + 17 + (b.height() - 30) * 0.2);
        context.getInput().pressMouse(0);
        check(context.computeOnClient(minecraft -> themes().color("Primary Accent") != oldColor), "HSV square edits the live accent");
        check(picker instanceof ColorPicker, "HSV control adapter"); context.takeScreenshot("risecode-theme-colors");
        reveal(context, "Border Thickness"); click(context, "Border Thickness", 0.01, 0.8);
        check(themes().number("Border Thickness") == 0, "zero border is supported");
        number(context, "Corner Radius", 0); context.takeScreenshot("risecode-theme-sharp");
        number(context, "Corner Radius", 16); number(context, "Border Thickness", 0.8);
        number(context, "Panel Opacity", 0.7); number(context, "GUI Blur Radius", 18); number(context, "World Blur Radius", 26);
        reveal(context, "World Blur"); click(context, "World Blur", 0.3, 0.5);
        check(themes().bool("World Blur"), "world blur toggle");
        context.waitTicks(8); context.takeScreenshot("risecode-theme-world-blur");
        context.runOnClient(minecraft -> check(themes().saveProfile("ThemeTest"), "theme profile save"));
        number(context, "Panel Opacity", 0.95);
        context.runOnClient(minecraft -> check(themes().loadProfile("themetest"), "theme profile load while screen is open"));
        check(themes().number("Panel Opacity") == 0.7 && gui().getPage() == ClickGui.Page.THEMES, "profile updates live controls without replacing screen");
        int allocations = BlurUtil.getAllocationCount(); context.waitTicks(10);
        check(BlurUtil.getAllocationCount() == allocations, "targets reused across frames");
        check(BlurUtil.getTargetWidth() == 1024 && BlurUtil.getTargetHeight() == 576, "half-resolution targets at 2048x1152");
        context.getInput().resizeWindow(1920, 1080); context.waitTicks(6);
        check(BlurUtil.getTargetWidth() == 960 && BlurUtil.getTargetHeight() == 540 && BlurUtil.getAllocationCount() > allocations, "targets resized once with framebuffer");
        long start = System.nanoTime(); long firstFrame = BlurUtil.getRenderedFrames();
        context.waitTicks(20);
        long elapsed = System.nanoTime() - start, rendered = BlurUtil.getRenderedFrames() - firstFrame;
        Client.INSTANCE.getLogger().info("Phase 7 observed blur frames/sec={}, last CPU submission={}ms (not a GPU timing)", rendered * 1e9 / elapsed, BlurUtil.getLastSubmitMilliseconds());
        AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
        context.runOnClient(minecraft -> reload.set(minecraft.reloadResourcePacks()));
        context.waitFor(minecraft -> reload.get().isDone(), 15_000);
        check(!reload.get().isCompletedExceptionally(), "resource reload with live font atlases and custom pipelines");
        context.waitFor(minecraft -> minecraft.gui.overlay() == null, 15_000);
        context.runOnClient(minecraft -> {
            themes().reset(); themes().changed();
        });
        context.getInput().resizeWindow(2048, 1152); context.waitTicks(6);
        reveal(context, "Animation Speed"); click(context, "Animation Speed", 0.7, 0.7);
        check(themes().number("Animation Speed") > 2, "animation speed slider");
        reveal(context, "Easing Curve"); click(context, "Easing Curve", 0.25, 0.5); context.waitTicks(5);
        reveal(context, "Font Selection"); context.waitTicks(6); context.takeScreenshot("risecode-phase7-themes");
        context.runOnClient(minecraft -> {
            themes().reset();
            ((BooleanSetting) themes().getSetting("World Blur")).setValue(true);
            ((DoubleSetting) themes().getSetting("World Blur Radius")).setValue(18.0);
            gui().selectPage(ClickGui.Page.SEARCH);
        });
        context.waitTicks(6); context.takeScreenshot("risecode-phase7-client");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(minecraft -> minecraft.gui.screen() == null, 3000);
        context.runOnClient(minecraft -> setHudBackground(ThemeManager.HudBackground.BLUR_BOX)); context.waitTicks(6);
        long hudFrames = BlurUtil.getRenderedFrames(); context.waitTicks(5);
        check(BlurUtil.getRenderedFrames() > hudFrames, "blur boxes capture the world while the menu is closed");
        context.takeScreenshot("risecode-hud-blur-box");
        context.runOnClient(minecraft -> setHudBackground(ThemeManager.HudBackground.NONE)); context.waitTicks(6);
        hudFrames = BlurUtil.getRenderedFrames(); context.waitTicks(5);
        check(BlurUtil.getRenderedFrames() == hudFrames, "no blur work when HUD and menu no longer request it");
        context.takeScreenshot("risecode-hud-no-background");
        context.runOnClient(minecraft -> {
            themes().reset(); Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(false); themes().save();
        });
    }
    private static ThemeManager themes() { return Client.INSTANCE.getThemeManager(); }
    private static void setHudBackground(ThemeManager.HudBackground value) {
        @SuppressWarnings("unchecked") EnumSetting<ThemeManager.HudBackground> setting = (EnumSetting<ThemeManager.HudBackground>) themes().getSetting("HUD Background");
        setting.setValue(value); themes().changed();
    }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static void number(ClientGameTestContext context, String name, double value) {
        context.runOnClient(minecraft -> { ((DoubleSetting) themes().getSetting(name)).setValue(value); themes().changed(); }); context.waitTicks(3);
    }
    private static void reveal(ClientGameTestContext context, String name) {
        context.runOnClient(minecraft -> gui().getThemeEditor().reveal(name)); context.waitTicks(7);
    }
    private static void click(ClientGameTestContext context, String name, double px, double py) {
        UIComponent.Bounds b = context.computeOnClient(minecraft -> gui().getThemeEditor().getControl(name).getBounds());
        move(context, b.x() + b.width() * px, b.y() + b.height() * py); context.getInput().pressMouse(0);
    }
    private static void move(ClientGameTestContext context, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> new double[] {
                x * gui().getCanvasScale() * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                y * gui().getCanvasScale() * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()});
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTick();
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
```

## 3. Resource setup

The TrueType directory is exactly `src/main/resources/assets/riserecode/font/`.
The following six files are already bundled with their original OFL notices:

```text
google_sans_regular.ttf       # Google Sans v14.000, default body
google_sans_medium.ttf        # matching title weight
montserrat_regular.ttf        # Montserrat v7.222
montserrat_medium.ttf
inter_regular.ttf            # existing Inter, embedded version 4.001
inter_medium.ttf
```

`CustomTextRenderer` loads these directly from classpath streams into LWJGL STB.
It computes real kerning/fractional advances, bakes a single-channel signed-distance
atlas once per face, and submits extracted quads through Minecraft's native GUI
pipeline. Font Scale, shadow offsets/blur/opacity and glow are shader parameters;
changing them does not rebuild or upload the atlas. Latin text retains its chosen
face even next to unsupported Unicode; native fallback handles those individual
glyphs with the same configurable shadow offsets and a Gaussian stencil.

User overrides belong to `<game>/config/riserecode/fonts/`:

```text
google_sans.ttf               # or google_sans_regular.ttf + google_sans_medium.ttf
montserrat.ttf               # same naming rules
inter.ttf                    # same naming rules
minecraftia.ttf              # renamed Minecraftia-Regular.ttf
qraftys.zip                  # optional local-pack override
```

A medium face falls back to the matching regular file. Use **Themes → Fonts** after
installing/replacing an asset. Missing or invalid files are labelled in Typography
and fall back to Inter. Minecraftia uses its recommended 12 px em scale and nearest
sampling; it is installed locally in this workspace for personal-use development,
with its author's readme. It is excluded from both distributable JARs. See the
[author listing](https://www.dafont.com/minecraftia.font) for commercial-app licensing.

**Qraftys is a resource pack, not a TTF.** The exact
[user-supplied CAPITALIZED FONT 2.1 pack](https://www.curseforge.com/minecraft/texture-packs/qraftys-capitalized-font/files/5605880)
is installed at `run/resourcepacks/qraftys-capitalized-font.zip`. Its original ZIP
is preserved under the local fonts/originals directory. Only pack compatibility
metadata was adapted from format 34 to the verified 26.2 resource format **88.0**;
all artwork is byte-identical. No pack assets enter the source resources or JAR.

`BitmapPackFont` reads `assets/minecraft/textures/font/ascii.png` from this local ZIP,
uses its real character cells/advances, and preserves the original small-cap lowercase
glyphs. A separable Euclidean distance transform supplies soft shadows/glows while
nearest sampling keeps normal bitmap text crisp. Input strings remain unchanged.
To install the downloaded pack for a different instance:

```bash
python scripts/install-qraftys.py /path/to/qraftys.zip /path/to/game-directory
```

Choose **Qraftys** in Themes to apply it immediately to risecode. Enable the pack in
Minecraft's Resource Packs menu separately if you also want it on vanilla interfaces.
[FONT_SOURCES.md](FONT_SOURCES.md) provides source links, exact font hashes, notices
and all local/bundled asset details.

All seven shader files listed above remain under
`assets/riserecode/shaders/core/`. The native shader manager loads them when the
registered render pipeline is used. No legacy shader JSON or raw OpenGL calls
are needed. The necessary `GuiRendererMixin` is registered in the existing mixin
JSON alongside the input/chat/network adapters; compatibility remains `JAVA_25`.

## 4. Setup and integration notes

### Startup and persistence

`Client.INSTANCE.init()` constructs and loads ThemeManager before creating the HUD
and loading module state. It binds typed settings into Theme and the UI animation
policy, configures CustomTextRenderer, and gives the same manager directly to the
HUD and ClickGUI editor. The editor adapts existing Boolean/Double/Enum/Color settings;
modules do not acquire UI dependencies.

Active visual preferences live at `config/riserecode/theme.json`. Named themes live
at `config/riserecode/theme-profiles/<name>.json`, with schema version 1. Save/Load
uses the toolbar's name field; names allow 1–40 letters, digits, `_` and `-`, normalized
to lowercase. Documents are validated completely before changing settings. Missing
fields receive defaults, unknown fields are ignored, and corrupt main JSON is preserved
as `theme-invalid-<timestamp>.json` before defaults are written. Saves use a temporary
file and atomic replacement where available, with a 400 ms edit debounce and a 5 s
retry delay on write failure. Closing the GUI and shutting down flush preferences.

Module profiles continue to store module states/settings/keybinds. HUD visibility,
fonts and other visual choices are now independent. Existing `ui.accent`, `ui.scale`
and `hudEnabled` migrate once when no active theme exists. Window offsets and navigation
language remain global UI preferences. Theme loads update existing control objects
in place, so an open GUI stays open and immediately reflects the loaded values.

### Rendering, blur and performance

Minecraft 26.2 separates GUI extraction from rendering. `UiMesh` snapshots each
quad's pose, bounds and native scissor; shader effects are vertex attributes rather
than mutable shared uniforms. Rounded geometry uses signed-distance fragment math,
antialiasing uses derivatives, HSV uses actual color-space math, and glow is an
expanded Gaussian silhouette. Native scissors intersect nested content viewports;
input uses the inverse of the same animated canvas transform.

During extraction, GUI/HUD blur boxes request a scene effect and reference the
appropriate reusable texture. `GuiRendererMixin` runs after world or title panorama
rendering and before `GuiRenderer.prepare()`, capturing the **finished scene before
all HUD/menu drawing**. This prevents recursive GUI blur and leaves text, hotbar and
controls crisp.

Four depthless GPU targets are allocated at `(framebufferSize + 1) / 2`: scene,
ping, GUI output and world output. At 1920×1080, each is 960×540. They are reused
across frames and rebuilt only on framebuffer resize. A four-fetch downsample is
followed by horizontal/vertical Gaussian passes, each compressing nine taps into
five bilinear fetches. GUI and world radii are independent; equal radii share the
GUI output. World blur composites across the screen behind the GUI with its opening
opacity; GUI blur is restricted by rounded panel geometry and its own opacity.

The world image is refreshed each requested frame so animation/world motion remain
live; caching applies to textures/buffers rather than freezing the scene. Six tiny
persistent parameter buffers keep per-pass values separate. There is no GPU-to-CPU
readback, no per-frame framebuffer creation and no effect work when neither GUI nor
HUD requests blur. Resources close at client shutdown. **World Blur** only runs while
the GUI is open; **HUD Background → Blur Box** independently blurs the scene behind
watermark/list boxes during gameplay.

Blur radii are framebuffer pixels. Layout/font/glow distances use the client's
reference UI units before its animated canvas transform. The reference canvas is
576 units high: the 428×320 window and 107-unit sidebar retain the chosen 6.130 layout.
Corner Radius scales all component radii proportionally and supports zero; panel
opacity, border thickness/opacity, padding, interface scale and HSV accents all update
live. The shared theme controls 0.5×–3× animation speed and Linear/Quart/Cubic/Elastic/
Bounce easing, including accent transitions. Ordinary backend animations are unaffected.

### Run and test

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

Install `build/libs/riserecode-0.6.0.jar` in a Minecraft 26.2 Fabric/Java 25 profile,
with Fabric API. Restart a running external game after replacing its mod JAR. Local
optional font files/packs must also be installed in that instance as described above.

1. Press Right Shift and open Themes. Wheel-scroll the six visual categories.
2. Cycle Font Selection, drag Font Scale, edit shadow offsets/blur and press Fonts
   after adding a local asset. Menu, watermark and enabled-module list share the choice.
3. Edit both HSV accents, toggle gradient/glow, and drag radius/intensity/geometry.
   Zero border and zero corner radius work without restarting.
4. Change GUI Blur Radius/Opacity, then enable World Blur and compare the two radii.
   Close the GUI and choose HUD None/Solid Box/Blur Box to inspect gameplay backgrounds.
5. Set the global speed/curve, hover navigation, drag the logo region, expand module
   settings, toggle modules and watch retained exit rows and sorted Y transitions.
6. Save a named theme, change several values, and Load it while the menu remains open.
   Loading a module profile does not overwrite the theme.

For automated verification:

```bash
./gradlew build runClientGameTest --offline --no-daemon
```

The disposable test instance is cleared before each run. The build copies only the
optional local Qraftys pack/TTFs from `run/` into that test instance after cleanup;
these remain absent from both distributable JARs. Other machines can run without
them and exercise the explicit fallback path. The tests require a display and driver.

The final build passed **273** checks: Foundation 90, Runtime 22, UI 25, Engine 55,
Combat 50 and Theme 31. The full real Minecraft/OpenGL run passed keyboard/mouse
input, all five font choices, actual Qraftys advances/26.2 pack compatibility, HSV
changes, live theme profiles, font/resource reload, blur target reuse/resize and HUD
blur/none modes, along with the existing engine/combat integration suite. Resource
reload tests wait for the native loading overlay to finish fading before input.

At 1920×1080 on this machine's **AMD Radeon RX 9070 / Mesa 26.2.2**, with GUI radius
18 and world radius 26, the test observed approximately **120 rendered blur frames/s**
over 20 ticks. This is a short hardware-specific observation, not a universal FPS
guarantee. The 0.011887 ms logged submission time measures CPU command submission,
not GPU execution. OpenGL was exercised; Vulkan was not verified in this phase.

Evidence is retained in `build/verification/phase7/`: build/client/Minecraft logs,
24 screenshots and verification.json. The package check verifies class major 69
(Java 25), client-only metadata, all seven shaders/notices, absence of test classes
and absence of Qraftys/Minecraftia artwork from both distributable JARs.

- [Themes screenshot](build/verification/phase7/screenshots/risecode-phase7-themes.png)
- [Client with world blur](build/verification/phase7/screenshots/risecode-phase7-client.png)
- [Actual Qraftys bitmap face](build/verification/phase7/screenshots/risecode-font-qraftys.png)
- [Minecraftia](build/verification/phase7/screenshots/risecode-font-minecraftia.png)
- [Montserrat](build/verification/phase7/screenshots/risecode-font-montserrat.png)
- [Inter](build/verification/phase7/screenshots/risecode-font-inter.png)
- [HSV colors and layout](build/verification/phase7/screenshots/risecode-theme-colors.png)
- [Sharp geometry](build/verification/phase7/screenshots/risecode-theme-sharp.png)
- [HUD blur boxes](build/verification/phase7/screenshots/risecode-hud-blur-box.png)

Final mod JAR SHA-256: `250b7a83142c20debbf8cb8d4358e624d16d0404e0588669b4ed89ae7d42fb2d`.
