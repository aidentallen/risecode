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
