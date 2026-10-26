package dev.riserecode;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.ColorUtil;
import java.nio.file.Files;
import java.util.Comparator;
import org.slf4j.LoggerFactory;

/** Independent paints and strict, backwards-compatible module/theme/profile persistence. */
public final class ColorPaintChecks {
    private static int checks;
    private static final class Fixture extends Module {
        private final ColorSetting color = register(new ColorSetting("Color", "", 0x80112233));
        Fixture(EventBus bus) { super("Paint fixture", "", Category.RENDER, bus); }
    }
    private ColorPaintChecks() { }
    public static void main(String[] args) throws Exception {
        ColorSetting color = new ColorSetting("Test", "", 0x80112233);
        check(ColorSetting.parseHex("#123456") == 0xFF123456 && ColorSetting.parseHex("80123456") == 0x80123456, "RGB/ARGB exact hex input");
        invalid(() -> ColorSetting.parseHex("#GG3322")); invalid(() -> ColorSetting.parseHex("12345"));
        color.setSecondary(0x40667788); color.setMode(ColorSetting.Mode.GRADIENT); color.setSpeed(0); color.setSpacing(100);
        check(color.paint().sample(0, 0, 0) == 0x80112233 && color.paint().sample(50, 0, 0) == 0x40667788, "gradient endpoints include independent alpha");
        color.setDirection(ColorUtil.GradientDirection.VERTICAL);
        check(color.paint().sample(50, 0, 0) == 0x80112233 && color.paint().sample(0, 50, 0) == 0x40667788, "independent gradient direction");
        var snapshot = color.paint(); color.setMode(ColorSetting.Mode.SOLID);
        check(snapshot.mode() == ColorSetting.Mode.GRADIENT, "extraction paint is detached from live setting");
        for (var mode : new ColorSetting.Mode[] {ColorSetting.Mode.RAINBOW, ColorSetting.Mode.ASTOLFO, ColorSetting.Mode.SINE}) {
            color.setMode(mode); color.setSpeed(1);
            check(color.paint().sample(0, 0, 0) != color.paint().sample(0, 0, 0.17), "time progresses independently " + mode);
            if (mode != ColorSetting.Mode.SINE) { check(color.paint().sample(0, 0, 0) >>> 24 == 128, "HSV modes preserve alpha " + mode); }
        }
        var saved = color.toJson(); var copy = new ColorSetting("Copy", "", 0); copy.apply(copy.decode(saved));
        check(copy.paint().equals(color.paint()), "complete color policy round trip");
        JsonObject malformed = saved.deepCopy(); malformed.addProperty("speed", Double.NaN); invalid(() -> copy.decode(malformed));
        check(copy.paint().equals(color.paint()), "invalid color cannot partially change policy");
        invalid(() -> color.setSpeed(13)); invalid(() -> color.setSpacing(0));
        check(color.decode(new JsonPrimitive("803B82F6")).primary() == 0x803B82F6 && color.decode(new JsonPrimitive(0x803B82F6)).primary() == 0x803B82F6, "legacy string and numeric colors");
        color.reset(); check(color.paint().equals(color.defaultPaint()), "reset restores endpoints and policy too");
        var layout = new HudLayout(0.2, 0.3); layout.backgroundColor.apply(copy.paint()); var layoutSave = layout.snapshot();
        layout.backgroundColor.reset(); layout.apply(layoutSave);
        check(layout.backgroundColor.paint().equals(copy.paint()), "HUD per-element background policy persists");
        layoutSave.remove("backgroundColor"); layout.apply(layoutSave);
        check(layout.backgroundColor.getMode() == ColorSetting.Mode.THEME, "legacy HUD layout inherits theme background");
        var directory = Files.createTempDirectory("risecode-paint-check-");
        try {
            var logger = LoggerFactory.getLogger(ColorPaintChecks.class); var bus = new EventBus(); var modules = new ModuleManager(bus, logger);
            var fixture = modules.register(new Fixture(bus)); fixture.color.apply(copy.paint());
            var config = new ConfigManager(directory, modules, () -> true, ignored -> { }, logger); config.save();
            fixture.color.reset(); config.load(); check(fixture.color.paint().equals(copy.paint()), "module config stores full paint");
            var profile = config.profileSnapshot(); fixture.color.reset(); config.applyProfile(profile);
            check(fixture.color.paint().equals(copy.paint()), "module profile restores full paint");
            JsonObject bad = profile.deepCopy(); bad.getAsJsonObject("modules").getAsJsonObject(fixture.getName()).getAsJsonObject("settings").getAsJsonObject("Color").addProperty("spacing", -1);
            invalid(() -> config.applyProfile(bad)); check(fixture.color.paint().equals(copy.paint()), "bad profile cannot half-apply a paint");
            profile.getAsJsonObject("modules").getAsJsonObject(fixture.getName()).getAsJsonObject("settings").remove("Color"); config.applyProfile(profile);
            check(fixture.color.paint().equals(fixture.color.defaultPaint()), "missing profile paint resets to declared default");
            var theme = new ThemeManager(directory, logger); theme.colorSetting("Surface Color").apply(copy.paint()); theme.save();
            var restored = new ThemeManager(directory, logger); restored.load();
            check(restored.colorSetting("Surface Color").paint().equals(copy.paint()), "interface paint stores in theme JSON");
            var legacy = restored.snapshot().settings(); legacy.addProperty("Primary Accent", 0xFF123456);
            restored.apply(new dev.riserecode.ui.theme.ThemeProfile(dev.riserecode.ui.theme.ThemeProfile.VERSION, legacy));
            check(restored.color("Primary Accent") == 0xFF123456, "legacy theme numeric color migrates");
            restored.bind(bus); dev.riserecode.ui.theme.Theme.setAccent(0x80123456);
            check(restored.color("Primary Accent") == 0x80123456, "live accent editing preserves selected alpha");
            var accent = restored.colorSetting("Primary Accent"); accent.setMode(ColorSetting.Mode.GRADIENT);
            accent.setSecondary(0x40667788); accent.setDirection(ColorUtil.GradientDirection.VERTICAL); accent.setSpeed(0);
            check(restored.gradientDirection() == ColorUtil.GradientDirection.VERTICAL, "inherited theme paint uses the accent picker axis");
            var inherited = new ColorSetting("Inherited", "", 0xFFFFFFFF, ColorSetting.Mode.THEME).paint();
            check(inherited.sample(0, 50, 0) == 0x40667788 && inherited.sample(50, 0, 0) == 0x80123456, "theme inheritance keeps both gradient endpoints and alpha");
            restored.close();
        } finally {
            try (var paths = Files.walk(directory)) { for (var path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); } }
        }
        System.out.println("Color paint checks passed (" + checks + " assertions).");
    }
    private static void invalid(Runnable action) { try { action.run(); throw new AssertionError("Expected invalid input"); } catch (IllegalArgumentException expected) { checks++; } }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
}
