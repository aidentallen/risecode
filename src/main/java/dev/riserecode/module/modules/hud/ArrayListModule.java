package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ArrayListModule extends HudModule {
    public final BooleanSetting containers = register(new BooleanSetting("Include Containers", "Include Watermark and ArrayList in the list", false));
    public final BooleanSetting hud = register(new BooleanSetting("Include HUD Modules", "List other HUD modules", false));
    public ArrayListModule(EventBus bus, Minecraft mc) { super("ArrayList", "Animated enabled-module list", bus, mc); setEnabled(true); }
    @Override protected Object contentKey() { return "arraylist"; }
    @Override protected List<HudContent.Line> buildContent() { return List.of(); }
}
