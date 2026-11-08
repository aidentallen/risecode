package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;

/** Raises how far you can target and strike entities. Servers still enforce their own limit. */
public final class ReachModule extends Module {
    private final DoubleSetting reach = register(DoubleSetting.extended("Reach", "Entity reach in blocks", 4, 3, 6));
    public ReachModule(EventBus bus) { super("Reach", "Extends entity attack and targeting range.", Category.COMBAT, bus); }
    public double reach() { return reach.getValue(); }
}
