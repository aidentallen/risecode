package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.keybind.KeybindController;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import java.nio.file.Files;
import java.util.Comparator;
import org.slf4j.LoggerFactory;

/** Activation ownership, repeat suppression, cleanup and complete profile binding persistence. */
public final class KeybindChecks {
    private static int checks;
    private static final class Fixture extends Module {
        private int enables, disables;
        Fixture(String name, EventBus bus) { super(name, "", Category.UTILITY, bus); }
        void defaults(int key, BindMode mode) { defaultBinding(key, mode); }
        @Override protected void onEnable() { enables++; }
        @Override protected void onDisable() { disables++; }
    }
    private KeybindChecks() { }
    public static void main(String[] args) throws Exception {
        var bus = new EventBus(); var logger = LoggerFactory.getLogger(KeybindChecks.class); var modules = new ModuleManager(bus, logger);
        var toggle = modules.register(new Fixture("Toggle", bus)); var hold = modules.register(new Fixture("Hold", bus));
        modules.setKeybind(toggle, 65); modules.setKeybind(hold, 66); modules.setBindMode(hold, Module.BindMode.HOLD);
        var keys = new KeybindController(modules);
        keys.press(65, true); check(toggle.isEnabled(), "toggle press enables");
        keys.press(65, true); check(toggle.isEnabled() && toggle.enables == 1, "duplicate/repeat press cannot toggle twice");
        keys.releaseKey(65); check(toggle.isEnabled(), "toggle release keeps its state");
        keys.press(65, true); check(!toggle.isEnabled(), "next press toggles off"); keys.releaseKey(65);
        keys.press(66, true); check(hold.isEnabled() && keys.isTemporarilyEnabled(hold), "hold enables with temporary ownership");
        check(!keys.persistentEnabled(hold), "held state is excluded from saved enabled state");
        keys.press(65, true); check(toggle.isEnabled() && hold.isEnabled(), "independent simultaneous keys");
        keys.releaseKey(66); check(!hold.isEnabled() && toggle.isEnabled(), "release only affects its own held module"); keys.releaseKey(65);
        keys.press(66, false); check(!hold.isEnabled(), "screens/disconnects cannot enable a module");
        keys.press(66, true); keys.update(false, key -> true); check(!hold.isEnabled(), "focus or screen loss releases holds");
        keys.press(66, true); keys.update(true, key -> false); check(!hold.isEnabled(), "poll recovers missed release callbacks"); keys.releaseKey(66);
        keys.press(66, true); modules.setKeybind(hold, 67); check(!hold.isEnabled(), "rebinding releases old activation immediately"); keys.releaseKey(66);
        keys.press(67, true); modules.setBindMode(hold, Module.BindMode.TOGGLE); check(!hold.isEnabled(), "mode change releases temporary lifecycle"); keys.releaseKey(67);
        modules.setBindMode(hold, Module.BindMode.HOLD); modules.setEnabled(hold, true);
        keys.press(67, true); keys.releaseKey(67); check(hold.isEnabled() && keys.persistentEnabled(hold), "hold does not discard an existing manual activation");
        modules.setEnabled(hold, false); keys.press(67, true); modules.setKeybind(hold, Module.UNBOUND);
        check(!hold.isEnabled() && hold.getKeybind() == Module.UNBOUND, "clearing bind restores held resources"); keys.releaseKey(67);
        var directory = Files.createTempDirectory("risecode-keybind-check-");
        try {
            var config = new ConfigManager(directory, modules, () -> true, ignored -> { }, logger); config.bindEnabledSnapshot(keys::persistentEnabled);
            modules.setKeybind(hold, 66); keys.press(66, true); config.save(); keys.releaseKey(66);
            modules.setBindMode(hold, Module.BindMode.TOGGLE); modules.setKeybind(hold, 67); config.load();
            check(hold.getKeybind() == 66 && hold.getBindMode() == Module.BindMode.HOLD && !hold.isEnabled(), "config restores key/mode without held activation");
            JsonObject profile = config.profileSnapshot(); modules.setBindMode(hold, Module.BindMode.TOGGLE); config.applyProfile(profile);
            check(hold.getBindMode() == Module.BindMode.HOLD && hold.getKeybind() == 66, "profile round-trips activation policy");
            keys.press(66, true); config.applyProfile(profile); check(!hold.isEnabled() && !keys.isTemporarilyEnabled(hold), "profile load clears old key ownership"); keys.releaseKey(66);
            var invalid = profile.deepCopy(); invalid.getAsJsonObject("modules").getAsJsonObject("Hold").addProperty("bindMode", "BAD");
            try { config.applyProfile(invalid); throw new AssertionError("Bad mode accepted"); } catch (IllegalArgumentException expected) { checks++; }
            check(hold.getBindMode() == Module.BindMode.HOLD && hold.getKeybind() == 66, "invalid profile cannot partially change bindings");
            var legacy = modules.register(new Fixture("FreeLook", bus)); legacy.defaults(342, Module.BindMode.HOLD);
            JsonObject old = config.profileSnapshot(); var entry = old.getAsJsonObject("modules").getAsJsonObject("FreeLook");
            entry.remove("bindMode"); entry.addProperty("keybind", -1); entry.getAsJsonObject("settings").addProperty("HoldKey", 295);
            config.applyProfile(old); check(legacy.getKeybind() == 295 && legacy.getBindMode() == Module.BindMode.HOLD, "legacy FreeLook fallback migrates to visible bind");
            modules.setKeybind(legacy, Module.UNBOUND); var cleared = config.profileSnapshot(); config.applyProfile(cleared);
            check(legacy.getKeybind() == Module.UNBOUND, "explicit reset never restores legacy fallback");
            var missing = cleared.deepCopy(); missing.getAsJsonObject("modules").remove("FreeLook"); config.applyProfile(missing);
            check(legacy.getKeybind() == 342 && legacy.getBindMode() == Module.BindMode.HOLD, "missing profile entry uses module binding defaults");
        } finally {
            keys.close();
            try (var paths = Files.walk(directory)) { for (var path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); } }
        }
        check(hold.enables == hold.disables, "all temporary lifecycles clean up exactly once");
        System.out.println("Keybind checks passed (" + checks + " assertions).");
    }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
}
