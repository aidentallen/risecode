package dev.riserecode.module.modules;

import dev.riserecode.event.CrosshairEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;

/** Native centered reticle; deliberately not an editor-draggable text HUD element. */
public final class CrosshairModule extends Module {
    private final DoubleSetting scale = register(new DoubleSetting("Scale", "Native crosshair and attack indicator size", 1, 0.25, 4));
    public CrosshairModule(EventBus bus) {
        super("Crosshair", "Scale the original Minecraft crosshair around screen center.", Category.HUD, bus);
    }
    public DoubleSetting getScale() { return scale; }
    @Override protected void onEnable() { listen(CrosshairEvent.class, event -> event.scale(scale.getValue())); }
}
