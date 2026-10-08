package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class WatermarkModule extends HudModule {
    public final BooleanSetting logo = register(new BooleanSetting("Logo", "Show the imported branding logo", true));
    public WatermarkModule(EventBus bus, Minecraft mc) { super("Watermark", "Independent branding overlay; edit text in Appearance", bus, mc); setEnabled(true); }
    @Override protected Object contentKey() { return "watermark"; }
    @Override protected List<HudContent.Line> buildContent() { return List.of(); }
}
