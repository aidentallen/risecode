package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class CoordinatesModule extends HudModule {
    private final ColorSetting x = register(new ColorSetting("XColor", "X axis", 0xFFFF879C));
    private final ColorSetting y = register(new ColorSetting("YColor", "Y axis", 0xFF8CE8B5));
    private final ColorSetting z = register(new ColorSetting("ZColor", "Z axis", 0xFF83BEFF));
    private final DoubleSetting precision = register(DoubleSetting.extended("Precision", "Decimal places", 1, 0, 3));
    private final BooleanSetting direction = register(new BooleanSetting("Direction", "Show direction letter", true));
    public CoordinatesModule(EventBus bus, Minecraft mc) { super("Coordinates", "Live position and cardinal direction.", bus, mc); }
    @Override protected Object contentKey() { var p = minecraft.player; return p == null ? "disconnected" : java.util.List.of(p.getX(), p.getY(), p.getZ(), p.getDirection()); }
    @Override protected List<HudContent.Line> buildContent() {
        var p = minecraft.player; String format = "% ." + (int) Math.round(precision.getValue()) + "f";
        String dx = String.format(Locale.ROOT, format, p == null ? 0 : p.getX());
        String dy = String.format(Locale.ROOT, format, p == null ? 0 : p.getY());
        String dz = String.format(Locale.ROOT, format, p == null ? 0 : p.getZ());
        String facing = direction.getValue() && p != null ? "  " + p.getDirection().getName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        return List.of(new HudContent.Line(List.of(HudContent.Span.paint("X" + dx + "  ", x.paint()),
                HudContent.Span.paint("Y" + dy + "  ", y.paint()), HudContent.Span.paint("Z" + dz + facing, z.paint())), null, -1, 0));
    }
}
