package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class MemoryUsageModule extends HudModule {
    public enum BarColorMode { STATIC, THRESHOLD, GRADIENT }
    private final BooleanSetting showBar = register(new BooleanSetting("ShowBar", "Display memory usage bar", true));
    private final EnumSetting<BarColorMode> colorMode = register(new EnumSetting<>("BarColorMode", "Bar color policy", BarColorMode.GRADIENT));
    private final ColorSetting color = register(new ColorSetting("BarColor", "Static bar color", 0xFF59D5CB));
    private final ColorSetting textColor = register(new ColorSetting("TextColor", "Memory readout color", Theme.TEXT));
    private final ColorSetting good = register(new ColorSetting("GoodColor", "Low memory use", 0xFF59D5A2));
    private final ColorSetting medium = register(new ColorSetting("MediumColor", "Medium memory use", 0xFFFFC857));
    private final ColorSetting bad = register(new ColorSetting("BadColor", "High memory use", 0xFFFF647C));
    public MemoryUsageModule(EventBus bus, Minecraft mc) { super("MemoryUsage", "Used and maximum Java heap memory.", bus, mc); }
    @Override protected Object contentKey() { Runtime runtime = Runtime.getRuntime(); return java.util.List.of(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory(), Theme.paint("ArrayList Color")); }
    @Override protected List<HudContent.Line> buildContent() {
        Runtime runtime = Runtime.getRuntime(); long used = runtime.totalMemory() - runtime.freeMemory(), max = runtime.maxMemory();
        double fraction = used / (double) max;
        int tint = switch (colorMode.getValue()) { case STATIC -> color.sample(0, 0); case GRADIENT -> Theme.accent();
            case THRESHOLD -> fraction < 0.6 ? good.sample(0, 0) : fraction < 0.85 ? medium.sample(0, 0) : bad.sample(0, 0); };
        return List.of(new HudContent.Line(List.of(HudContent.Span.paint(used / 1048576 + " / " + max / 1048576 + " MB", textColor.paint())), null,
                showBar.getValue() ? fraction : -1, tint, switch (colorMode.getValue()) {
                    case STATIC -> color.paint();
                    case THRESHOLD -> (fraction < 0.6 ? good : fraction < 0.85 ? medium : bad).paint();
                    case GRADIENT -> Theme.paint("ArrayList Color");
                }));
    }
}
