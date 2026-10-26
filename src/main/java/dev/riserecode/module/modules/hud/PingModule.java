package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class PingModule extends HudModule {
    private final DoubleSetting good = register(DoubleSetting.extended("GoodThreshold", "Good latency upper bound", 70, 0, 1000));
    private final DoubleSetting medium = register(DoubleSetting.extended("MediumThreshold", "Medium latency upper bound", 150, 0, 2000));
    private final ColorSetting goodColor = register(new ColorSetting("GoodColor", "Good latency", 0xFF59D5A2));
    private final ColorSetting mediumColor = register(new ColorSetting("MediumColor", "Medium latency", 0xFFFFC857));
    private final ColorSetting badColor = register(new ColorSetting("BadColor", "High latency", 0xFFFF647C));
    public PingModule(EventBus bus, Minecraft mc) { super("Ping", "Server latency in milliseconds.", bus, mc); }
    @Override protected Object contentKey() { var connection = minecraft.getConnection(); var info = connection == null || minecraft.player == null ? null : connection.getPlayerInfo(minecraft.player.getUUID()); return info == null ? -1 : info.getLatency(); }
    @Override protected List<HudContent.Line> buildContent() {
        var connection = minecraft.getConnection();
        var info = connection == null || minecraft.player == null ? null : connection.getPlayerInfo(minecraft.player.getUUID());
        int ping = info == null ? 0 : info.getLatency();
        return List.of(HudContent.Line.text(info == null ? "— ms" : ping + " ms", ping <= good.getValue() ? goodColor.paint()
                : ping <= Math.max(good.getValue(), medium.getValue()) ? mediumColor.paint() : badColor.paint()));
    }
}
