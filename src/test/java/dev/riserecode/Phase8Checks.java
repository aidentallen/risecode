package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Behavioral checks for overlapping animations and atomic visual/layout profile restoration. */
public final class Phase8Checks {
    private static int assertions;
    private Phase8Checks() { }
    public static void main(String[] args) throws Exception {
        AnimationManager manager = AnimationManager.ui(); manager.clear();
        Animation.configureUi(() -> 1, () -> MathUtil.Easing.LINEAR);
        ArrayList<Animation> animations = new ArrayList<>();
        for (int i = 0; i < 80; i++) { animations.add(Animation.ui(0, 1, MathUtil.Easing.OUT_QUART).animateTo(1)); }
        manager.frame(1_000_000_000L); manager.frame(1_200_000_000L);
        check(animations.stream().allMatch(a -> Math.abs(a.getValue() - 0.2) < 1e-10), "80 animations advance together");
        Animation a = animations.getFirst(); a.animateTo(0);
        check(a.getValue() == 0.2 && a.getTarget() == 0, "reversal retains current rendered value");
        Animation b = Animation.ui(0, 1, MathUtil.Easing.LINEAR).animateTo(1);
        manager.frame(1_300_000_000L);
        check(Math.abs(a.getValue() - 0.18) < 1e-10 && Math.abs(b.getValue() - 0.1) < 1e-10, "new entry progresses while earlier one exits");
        double before = b.getValue(); b.update(0.2); b.update(0.2);
        check(b.getValue() == before, "legacy component reads cannot double-advance a scheduled animation");
        expectInvalid(() -> b.update(-1));
        a.pause(); manager.update(0.1); check(Math.abs(a.getValue() - 0.18) < 1e-10, "paused state is independent of siblings");
        a.resume(); manager.update(0.1); check(a.getValue() < 0.18, "resume preserves the reversed segment");
        manager.update(2);
        check(manager.activeCount() == 0 && a.getValue() == 0 && b.getValue() == 1, "finished states leave the active set and land exactly");
        Animation backend = new Animation(0, 1, MathUtil.Easing.LINEAR).animateTo(1); backend.update(0.3);
        check(backend.getValue() == 0.3, "backend timing remains explicit and unthemed"); manager.clear();

        check(ColorUtil.hsv(0, 1, 1) == 0xFFFF0000 && ColorUtil.hsv(1, 1, 1) == 0xFFFF0000, "rainbow wraps without a seam");
        for (ColorUtil.GradientMode mode : ColorUtil.GradientMode.values()) {
            int first = ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 0, 0, 1, 100);
            int repeated = ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 100, 0, 1, 100);
            check(first == repeated, "spatial gradient cycle " + mode);
            check((first >>> 24) == 255, "opaque gradient " + mode);
            if (mode != ColorUtil.GradientMode.STATIC) {
                check(first != ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 0, 0.15, 1, 100), "time changes " + mode);
            }
        }
        HudLayout layout = new HudLayout(0.2, 0.3); layout.scale.setValue(1.4); layout.background.setValue(HudLayout.Background.BLUR);
        JsonObject saved = layout.snapshot(); layout.position(0.8, 0.9); layout.apply(saved);
        check(layout.x() == 0.2 && layout.y() == 0.3 && layout.scale.getValue() == 1.4 && layout.background.getValue() == HudLayout.Background.BLUR, "layout round trip");
        saved.addProperty("x", 0.5); check(layout.x() == 0.2, "layout snapshot is detached");
        JsonObject bad = layout.snapshot(); bad.addProperty("x", Double.NaN); expectInvalid(() -> layout.apply(bad));
        check(layout.x() == 0.2, "invalid layout makes no partial changes");
        layout.position(-1, 9); check(layout.x() == 0 && layout.y() == 1, "normalized anchors clamp to the screen");
        expectInvalid(() -> new HudLayout(Double.POSITIVE_INFINITY, 0));
        expectInvalid(() -> new Render3DEvent.Quad(Vec3.ZERO, Vec3.ZERO, new Vec3(Double.NaN, 0, 0), Vec3.ZERO, 0));

        Path directory = Files.createTempDirectory("risecode-phase8-check-");
        try {
            var logger = LoggerFactory.getLogger(Phase8Checks.class);
            ThemeManager themes = new ThemeManager(directory, logger); themes.load();
            EventBus bus = new EventBus(); ModuleManager modules = new ModuleManager(bus, logger);
            Module test = modules.register(new Module("Profile fixture", "", Category.HUD, bus) { });
            Module movement = modules.register(new Module("Movement fixture", "", Category.MOVEMENT, bus) { });
            ConfigManager config = new ConfigManager(directory, modules, () -> true, ignored -> { }, logger);
            config.useExternalVisualPreferences();
            config.bindHudStore(layout::snapshot, layout::apply, HudLayout::validate);
            config.bindVisualPolicyStore(themes::policySnapshot, themes::applyPolicy, themes::validatePolicy);
            ProfileManager profiles = new ProfileManager(config, () -> { }, logger);
            layout.position(0.25, 0.45); modules.setEnabled(test, true);
            setChoice(themes, "Gradient Mode", ColorUtil.GradientMode.ASTOLFO);
            ((DoubleSetting) themes.getSetting("Animation Speed")).setValue(2.5);
            ((DoubleSetting) themes.getSetting("Font Scale")).setValue(1.3);
            check(profiles.saveProfile("Visual"), "combined profile saved");
            layout.position(0.8, 0.8); modules.setEnabled(test, false);
            setChoice(themes, "Gradient Mode", ColorUtil.GradientMode.STATIC);
            ((DoubleSetting) themes.getSetting("Animation Speed")).setValue(1.0);
            ((DoubleSetting) themes.getSetting("Font Scale")).setValue(1.1);
            check(profiles.loadProfile("Visual"), "combined profile loaded");
            check(layout.x() == 0.25 && layout.y() == 0.45 && test.isEnabled(), "profile restores HUD anchors and modules");
            check(themes.choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.ASTOLFO && themes.number("Animation Speed") == 2.5, "profile restores animation and gradient policy");
            check(themes.number("Font Scale") == 1.1, "profile preserves independent typography preferences");
            JsonObject invalid = config.profileSnapshot(); invalid.getAsJsonObject("visualPolicy").addProperty("Animation Speed", 99);
            invalid.getAsJsonObject("hudLayout").addProperty("x", 0.9);
            expectInvalid(() -> config.applyProfile(invalid));
            check(layout.x() == 0.25 && test.isEnabled() && themes.number("Animation Speed") == 2.5, "invalid visual policy cannot partially move HUD or restart modules");
            check(config.save(), "layout saves to main config"); layout.position(0, 0); config.load();
            check(layout.x() == 0.25 && layout.y() == 0.45, "main config reload restores layout");
            layout.position(0.35, 0.55); modules.setEnabled(test, false); modules.setEnabled(movement, true);
            check(profiles.saveActiveHudLayout(), "editor persists its selected profile");
            layout.position(0, 0); modules.setEnabled(test, true); check(profiles.loadProfile("Visual"), "edited profile reloads");
            check(layout.x() == 0.35 && layout.y() == 0.55 && !test.isEnabled(), "profile retains edited HUD layout and toggle");
            check(!movement.isEnabled(), "editor save preserves the profile's movement/combat snapshot");
            config.save();
            check(config.getActiveProfile().equals("Visual"), "selected profile persists in main config");
            modules.disableAll();
        } finally {
            try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); } }
            manager.clear(); Animation.configureUi(() -> 1, () -> null);
        }
        System.out.println("Phase 8 checks passed (" + assertions + " assertions).");
    }
    private static <E extends Enum<E>> void setChoice(ThemeManager manager, String name, E value) {
        @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) manager.getSetting(name); setting.setValue(value);
    }
    private static void expectInvalid(Runnable action) { try { action.run(); throw new AssertionError("Expected invalid input"); } catch (IllegalArgumentException expected) { assertions++; } }
    private static void check(boolean result, String message) { assertions++; if (!result) { throw new AssertionError(message); } }
}
