package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class FpsModule extends HudModule {
    public enum ColorMode { STATIC, THRESHOLD, GRADIENT }
    private final EnumSetting<ColorMode> colorMode = register(new EnumSetting<>("ColorMode", "Text color policy", ColorMode.GRADIENT));
    private final ColorSetting color = register(new ColorSetting("Color", "Static text color", 0xFFF0F2F3));
    private final BooleanSetting suffix = register(new BooleanSetting("Suffix", "Show FPS suffix", true));
    private final ColorSetting good = register(new ColorSetting("GoodColor", "High FPS color", 0xFF59D5A2));
    private final ColorSetting medium = register(new ColorSetting("MediumColor", "Medium FPS color", 0xFFFFC857));
    private final ColorSetting bad = register(new ColorSetting("BadColor", "Low FPS color", 0xFFFF647C));
    public FpsModule(EventBus bus, Minecraft mc) { super("FPS", "Current rendered frames per second.", bus, mc); }
    @Override protected Object contentKey() { return minecraft.getFps(); }
    @Override protected List<HudContent.Line> buildContent() {
        int fps = minecraft.getFps();
        ColorSetting selected = colorMode.getValue() == ColorMode.THRESHOLD ? fps >= 120 ? good : fps >= 60 ? medium : bad : color;
        String label = fps + (suffix.getValue() ? " FPS" : "");
        return List.of(colorMode.getValue() == ColorMode.GRADIENT ? HudContent.Line.text(label, Theme.accent(), true) : HudContent.Line.text(label, selected.paint()));
    }
}
