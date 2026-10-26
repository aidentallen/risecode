package dev.riserecode;

import com.google.gson.JsonParser;
import dev.riserecode.event.CrosshairEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.modules.CrosshairModule;
import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.theme.ThemeManager;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Persistence and event contracts that do not need a GPU or a game world. */
public final class PolishChecks {
    private static int checks;
    private PolishChecks() { }
    public static void main(String[] args) throws Exception {
        var blocks = new BlockListSetting("Blocks", "", List.of("diamond_ore", "minecraft:diamond_ore", "other:missing"));
        check(blocks.getValue().equals(List.of("minecraft:diamond_ore", "other:missing")), "canonical immutable block IDs");
        check(blocks.decode(JsonParser.parseString("[\"missing_mod:future\",\"minecraft:chest\"]")).contains("missing_mod:future"), "preserve unavailable IDs");
        rejects(() -> blocks.decode(JsonParser.parseString("[1]")), "reject numeric block IDs");
        rejects(() -> blocks.decode(JsonParser.parseString("[\"Uppercase:Block\"]")), "reject invalid identifiers");
        rejects(() -> blocks.decode(JsonParser.parseString("{}")), "reject non-array block setting");
        blocks.toggle("minecraft:diamond_ore"); check(!blocks.getValue().contains("minecraft:diamond_ore"), "toggle removes selection");
        blocks.toggle("minecraft:diamond_ore"); check(blocks.getValue().contains("minecraft:diamond_ore"), "toggle adds selection");
        var layout = new HudLayout(0.2, 0.4); layout.border.setValue(HudLayout.Border.ON);
        var saved = layout.snapshot(); var copy = new HudLayout(0, 0); copy.apply(saved);
        check(copy.border.getValue() == HudLayout.Border.ON, "per-element border round trip");
        saved.remove("border"); copy.apply(saved); check(copy.border.getValue() == HudLayout.Border.INHERIT, "legacy layouts inherit HUD border");
        var picker = new ColorPicker(new ColorSetting("Color", "", -1), () -> { }, false);
        check(!picker.isExpanded(), "all color pickers start collapsed"); picker.setBounds(0, 0, 150, 22);
        check(picker.mouseClicked(10, 10, 1) && picker.isExpanded(), "right click expands picker");
        check(picker.mouseClicked(10, 10, 1) && !picker.isExpanded(), "right click collapses picker");
        var bus = new EventBus(); var crosshair = new CrosshairModule(bus);
        check(crosshair.getCategory() == Category.HUD && !crosshair.isEnabled(), "centered crosshair defaults disabled in HUD category");
        crosshair.setEnabled(true); check(bus.post(new CrosshairEvent()).scale() == 1, "enabled scale 1 preserves native size");
        crosshair.setEnabled(false); check(bus.post(new CrosshairEvent()).scale() == 1, "disable removes crosshair event override");
        var directory = Files.createTempDirectory("risecode-polish-check-");
        try {
            var theme = new ThemeManager(directory, LoggerFactory.getLogger("PolishChecks"));
            check(theme.color("Selected Tab Color") == -1, "selected tab defaults white");
            check(!theme.bool("HUD Border"), "global HUD borders opt in");
            for (String family : List.of("Start Screen", "Multiplayer", "Options")) {
                check(theme.bool(family + " Theme") && theme.bool(family + " Background"), "menu family defaults " + family);
                ((BooleanSetting) theme.getSetting(family + " Background")).setValue(false);
            }
            check(theme.save(), "theme settings save");
            var restored = new ThemeManager(directory, LoggerFactory.getLogger("PolishChecks")); restored.load();
            check(!restored.bool("Options Background") && restored.bool("Options Theme"), "independent menu toggles persist");
            theme.close(); restored.close();
        } finally { try (var files = Files.walk(directory)) { for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.delete(file); } } }
        System.out.println("Polish checks passed (" + checks + " assertions).");
    }
    private static void check(boolean value, String label) { checks++; if (!value) { throw new AssertionError(label); } }
    private static void rejects(Runnable action, String label) {
        try { action.run(); } catch (RuntimeException expected) { checks++; return; } throw new AssertionError(label);
    }
}
