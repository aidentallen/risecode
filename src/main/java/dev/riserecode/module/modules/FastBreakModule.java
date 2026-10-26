package dev.riserecode.module.modules;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MiningEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
public final class FastBreakModule extends Module {
    public final BooleanSetting noDelay = register(new BooleanSetting("No Delay", "Remove the delay between blocks", true));
    public final DoubleSetting multiplier = register(DoubleSetting.extended("Progress Multiplier", "Native mining progress multiplier", 1, 1, 5));
    public FastBreakModule(EventBus bus) { super("FastBreak", "Adjust mining delay and progress using normal interactions.", Category.PLAYER, bus); }
    @Override protected void onEnable() { listen(MiningEvent.class, e -> { e.noDelay(noDelay.getValue()); e.multiplier(multiplier.getValue()); }); }
}
