package dev.riserecode.ui.theme;

import dev.riserecode.event.Event;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModulePresentation;

/** Adapts lifecycle-neutral modules to scoped appearance on the client thread. */
public final class ThemedModulePresentation implements ModulePresentation {
    @Override public double duration(Module module) {
        if ((module.popOverride.getValue() || !module.popIn.getValue()) && Theme.manager() != null) { Theme.manager().module(module).migrate(module); }
        return ModuleAppearance.call(module.capabilityId(), () -> Theme.bool("Reduced Motion", false) ? 0 : Theme.number("Opening Duration", .2));
    }
    @Override public double scale(Module module, double progress) {
        return ModuleAppearance.call(module.capabilityId(), () -> dev.riserecode.ui.animation.PopIn.scale(
                dev.riserecode.ui.animation.Animation.scale(ThemeManager.Channel.OPENING), progress));
    }
    @Override public void dispatch(Module module, Event event, Runnable listener) {
        if (event instanceof dev.riserecode.event.Render3DEvent || event instanceof dev.riserecode.event.EnchantmentRenderEvent) {
            ModuleAppearance.with(module.capabilityId(), listener);
        } else { listener.run(); }
    }
}
