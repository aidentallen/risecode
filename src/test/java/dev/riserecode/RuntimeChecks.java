package dev.riserecode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.FovEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Checks persistence and registry behavior without launching Minecraft. */
public final class RuntimeChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeChecks.class);
    private static int assertions;

    private RuntimeChecks() { }

    public static void main(String[] args) throws Exception {
        for (float nativeFov : new float[]{0, -0.0F, .25F, 70, 180, Float.NaN, Float.POSITIVE_INFINITY}) {
            var event = new FovEvent(nativeFov);
            event.applyMagnification(1);
            check(Float.floatToIntBits(event.value()) == Float.floatToIntBits(nativeFov), "inactive zoom preserves native FOV exactly");
        }
        for (float nativeFov : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            var event = new FovEvent(nativeFov);
            event.applyMagnification(3);
            check(Float.floatToIntBits(event.value()) == Float.floatToIntBits(nativeFov), "zoom tolerates unready native camera FOV");
        }
        var zoomed = new FovEvent(90);
        zoomed.applyMagnification(3);
        check(zoomed.value() == 30, "active zoom divides valid FOV");
        var tiny = new FovEvent(Float.MIN_VALUE);
        tiny.applyMagnification(Double.MAX_VALUE);
        check(tiny.value() == 1, "extreme zoom clamps before float underflow");
        Path directory = Files.createTempDirectory("riserecode-config-check-");
        try {
            EventBus bus = new EventBus();
            ModuleManager manager = new ModuleManager(bus, LOGGER);
            CheckModule module = manager.register(new CheckModule(bus));
            AtomicBoolean hud = new AtomicBoolean(true);
            ConfigManager config = new ConfigManager(directory, manager, hud::get, hud::set, LOGGER);
            config.load();
            check(Files.exists(config.getFile()), "missing config creates defaults");
            check(manager.getByName("  CONFIGCHECK ").orElseThrow() == module, "name lookup");
            check(manager.getByClass(CheckModule.class).orElseThrow() == module, "class lookup");
            check(manager.getByCategory(Category.UTILITY).equals(manager.getModules()), "category lookup");
            module.flag.setValue(false);
            module.scale.setValue(1.75);
            module.curve.setValue(MathUtil.Easing.OUT_ELASTIC);
            module.color.setValue(0x803B82F6);
            manager.setKeybind(module, 65);
            check(manager.setEnabled(module, true), "manager enables module");
            bus.post(new CheckEvent());
            check(module.calls == 1 && bus.getListenerCount() == 1, "enable owns subscription");
            JsonObject ui = new JsonObject();
            ui.addProperty("accent", 0xFF59D5CB); ui.addProperty("offsetX", 12.5);
            config.setUiPreferences(ui); ui.addProperty("offsetX", 999);
            check(config.getUiPreferences().get("offsetX").getAsDouble() == 12.5, "UI write uses a snapshot");
            hud.set(false);
            check(config.save(), "atomic save succeeds");

            EventBus restoredBus = new EventBus();
            ModuleManager restoredManager = new ModuleManager(restoredBus, LOGGER);
            CheckModule restored = restoredManager.register(new CheckModule(restoredBus));
            AtomicBoolean restoredHud = new AtomicBoolean(true);
            ConfigManager loaded = new ConfigManager(directory, restoredManager, restoredHud::get, restoredHud::set, LOGGER);
            loaded.load();
            check(restored.isEnabled() && restored.getKeybind() == 65, "toggle and keybind round trip");
            check(!restored.flag.getValue() && restored.scale.getValue() == 1.75, "boolean and number round trip");
            check(restored.curve.getValue() == MathUtil.Easing.OUT_ELASTIC, "enum round trip");
            check(restored.color.getValue() == 0x803B82F6, "transparent ARGB round trip");
            check(restored.scaleAtEnable == 1.75, "settings restored before module enable");
            check(!restoredHud.get(), "HUD preference round trip");
            check(loaded.getUiPreferences().get("accent").getAsInt() == 0xFF59D5CB
                    && loaded.getUiPreferences().get("offsetX").getAsDouble() == 12.5, "UI preferences persist independently");
            loaded.getUiPreferences().addProperty("offsetX", -999);
            check(loaded.getUiPreferences().get("offsetX").getAsDouble() == 12.5, "UI reads cannot mutate config");
            restoredManager.setEnabled(restored, false);
            check(restoredBus.getListenerCount() == 0, "manager disable unsubscribes");

            JsonObject root = JsonParser.parseString(Files.readString(config.getFile())).getAsJsonObject();
            root.addProperty("futureField", "preserve me");
            JsonObject entry = root.getAsJsonObject("modules").getAsJsonObject("ConfigCheck");
            entry.addProperty("keybind", 1.5);
            entry.getAsJsonObject("settings").addProperty("scale", 99.0);
            Files.writeString(config.getFile(), root.toString());
            loaded.load();
            check(restored.scale.getValue() == 1.75 && restored.getKeybind() == 65, "invalid values preserve current state");
            loaded.save();
            check(JsonParser.parseString(Files.readString(config.getFile())).getAsJsonObject()
                    .get("futureField").getAsString().equals("preserve me"), "unknown fields survive saves");

            Files.writeString(config.getFile(), "{broken json");
            ConfigManager recovery = new ConfigManager(directory, new ModuleManager(new EventBus(), LOGGER),
                    hud::get, hud::set, LOGGER);
            recovery.load();
            check(JsonParser.parseString(Files.readString(config.getFile())).isJsonObject(), "malformed JSON recovers");
            try (var files = Files.list(directory)) {
                check(files.anyMatch(path -> path.getFileName().toString().startsWith("config.invalid-")),
                        "malformed input is backed up");
            }
            manager.disableAll();
            restoredManager.disableAll();
            check(bus.getListenerCount() == 0 && restoredBus.getListenerCount() == 0, "shutdown releases listeners");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
        System.out.println("Runtime checks passed (" + assertions + " assertions).");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) { throw new AssertionError(message); }
    }

    private static final class CheckEvent extends Event { }

    private static final class CheckModule extends Module {
        private final BooleanSetting flag = register(new BooleanSetting("flag", "Test boolean", true));
        private final DoubleSetting scale = register(new DoubleSetting("scale", "Test range", 1.0, 0.5, 2.0));
        private final EnumSetting<MathUtil.Easing> curve = register(
                new EnumSetting<>("curve", "Test enum", MathUtil.Easing.OUT_QUART));
        private final ColorSetting color = register(new ColorSetting("color", "Test ARGB", 0xFFFFFFFF));
        private int calls;
        private double scaleAtEnable;

        private CheckModule(EventBus bus) { super("ConfigCheck", "Config test", Category.UTILITY, bus); }

        @Override
        protected void onEnable() {
            scaleAtEnable = scale.getValue();
            listen(CheckEvent.class, event -> calls++);
        }
    }
}
