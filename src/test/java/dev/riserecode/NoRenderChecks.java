package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.FogEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.NoRenderModule;
import java.nio.file.Files;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;
import org.slf4j.LoggerFactory;

public final class NoRenderChecks {
    private static int checks;
    private NoRenderChecks() { }
    private static void check(boolean value, String label) { checks++; if (!value) { throw new AssertionError(label); } }
    private static FogData fog() {
        var fog = new FogData(); fog.environmentalStart = 8; fog.environmentalEnd = 32;
        fog.renderDistanceStart = 240; fog.renderDistanceEnd = 256; fog.skyEnd = 256; fog.cloudEnd = 192;
        fog.color.set(0.2f, 0.3f, 0.4f, 1); return fog;
    }
    private static boolean suppressed(FogData fog) {
        return fog.environmentalStart == Float.MAX_VALUE && fog.environmentalEnd == Float.MAX_VALUE
                && fog.renderDistanceStart == Float.MAX_VALUE && fog.renderDistanceEnd == Float.MAX_VALUE
                && fog.skyEnd == Float.MAX_VALUE && fog.cloudEnd == Float.MAX_VALUE;
    }
    private static FogData post(EventBus bus, FogEvent.Type type) { var fog = fog(); bus.post(new FogEvent(type, fog)); return fog; }
    public static void main(String[] args) throws Exception {
        var bus = new EventBus(); var module = new NoRenderModule(bus);
        check(!module.isEnabled() && module.getCategory() == Category.RENDER && module.allFog.getValue(), "Disabled Render module with All Fog default");
        check(post(bus, FogEvent.Type.WATER).environmentalEnd == 32, "Disabled leaves native fog unchanged");
        module.setEnabled(true);
        for (var type : FogEvent.Type.values()) {
            var result = post(bus, type); check(suppressed(result), "All Fog covers " + type);
            check(result.color.equals(0.2f, 0.3f, 0.4f, 1), "Clear color preserved " + type);
            check(!module.fogSetting(type).isVisible(), "Individual options hidden under All Fog " + type);
        }
        module.allFog.setValue(false);
        for (var chosen : FogEvent.Type.values()) {
            module.fogSetting(chosen).setValue(true);
            for (var other : FogEvent.Type.values()) {
                check(suppressed(post(bus, other)) == (chosen == other), "Selected " + chosen + " isolates " + other);
            }
            check(module.fogSetting(chosen).isVisible(), "Individual option visible " + chosen);
            module.fogSetting(chosen).setValue(false);
        }
        check(FogEvent.classify(FogType.WATER, "minecraft:the_nether", false, false) == FogEvent.Type.WATER, "Fluid independent of dimension");
        check(FogEvent.classify(FogType.WATER, "minecraft:overworld", true, true) == FogEvent.Type.BLINDNESS, "Blindness precedes darkness/water");
        check(FogEvent.classify(FogType.WATER, "minecraft:overworld", false, true) == FogEvent.Type.DARKNESS, "Darkness precedes water");
        check(FogEvent.classify(FogType.LAVA, "minecraft:the_end", true, true) == FogEvent.Type.LAVA, "Lava precedes effects");
        check(FogEvent.classify(FogType.POWDER_SNOW, "minecraft:overworld", true, true) == FogEvent.Type.POWDER_SNOW, "Snow precedes effects");
        check(FogEvent.classify(FogType.NONE, "minecraft:overworld", false, false) == FogEvent.Type.OVERWORLD, "Overworld atmosphere");
        check(FogEvent.classify(FogType.ATMOSPHERIC, "minecraft:the_nether", false, false) == FogEvent.Type.NETHER, "Nether atmosphere");
        check(FogEvent.classify(FogType.NONE, "minecraft:the_end", false, false) == FogEvent.Type.END, "End atmosphere");
        check(FogEvent.classify(FogType.NONE, "custom:moon", false, false) == FogEvent.Type.OTHER_DIMENSIONS, "Custom dimension atmosphere");
        var manager = new ModuleManager(bus, LoggerFactory.getLogger("NoRenderChecks")); manager.register(module);
        var directory = Files.createTempDirectory("risecode-norender-check-");
        try (var config = new ConfigManager(directory, manager, () -> true, ignored -> { }, LoggerFactory.getLogger("NoRenderChecks"))) {
            module.fogSetting(FogEvent.Type.WATER).setValue(true); module.fogSetting(FogEvent.Type.NETHER).setValue(true);
            manager.setKeybind(module, 295); check(config.save(), "Config saves NoRender controls");
            module.allFog.setValue(true); module.fogSetting(FogEvent.Type.WATER).setValue(false); module.setEnabled(false);
            config.load(); check(module.isEnabled() && !module.allFog.getValue() && module.fogSetting(FogEvent.Type.WATER).getValue()
                    && module.fogSetting(FogEvent.Type.NETHER).getValue() && module.getKeybind() == 295, "Config restores toggles and binding");
            var profile = config.profileSnapshot(); module.allFog.setValue(true); module.setEnabled(false); config.applyProfile(profile);
            check(module.isEnabled() && !module.allFog.getValue() && suppressed(post(bus, FogEvent.Type.WATER)), "Profile restores selected fog and subscriptions");
        } finally {
            module.setEnabled(false);
            try (var files = Files.walk(directory)) { for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.delete(file); } }
        }
        check(bus.getListenerCount() == 0 && post(bus, FogEvent.Type.WATER).environmentalEnd == 32, "Disable releases subscriptions and restores fog");
        System.out.println("NoRender checks passed (" + checks + " assertions).");
    }
}
