package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;

/** Lowers the first-person fire overlay so it no longer covers the view. */
public final class LowFireModule extends Module {
    private final DoubleSetting height = register(DoubleSetting.extended("Height", "How far the flames are pushed down the screen", 0.3, 0, 1));
    public LowFireModule(EventBus bus) { super("LowFire", "Lowers the fire overlay while burning.", Category.RENDER, bus); }
    public double drop() { return height.getValue(); }
}
