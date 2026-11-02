package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.HotbarPrivacy;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;

/** Local stream privacy for the hotbar, selected item name and held-item rendering. */
public final class HotbarModule extends Module {
    public final BooleanSetting blur = register(new BooleanSetting("Blur", "Frost the hotbar and selected item name", true));
    public final DoubleSetting blurStrength = register(DoubleSetting.extended("Blur Strength", "Frost radius", 90, 0, 140));
    public final BooleanSetting hideHeldItems = register(new BooleanSetting("Hide Held Items", "Empty hands in first person, third person and menus", false));

    public HotbarModule(EventBus bus) {
        super("Hotbar", "Hotbar privacy: blur contents or hide held items", Category.RENDER, bus);
        blurStrength.setVisibleWhen(blur::getValue);
        blur.onChange(this::push);
        blurStrength.onChange(this::push);
        hideHeldItems.onChange(this::push);
    }

    private void push() {
        HotbarPrivacy.configure(blur.getValue(), (int) Math.round(blurStrength.getValue()), hideHeldItems.getValue());
    }

    @Override
    protected void onEnable() {
        push();
    }

    @Override
    protected void onDisable() {
        HotbarPrivacy.configure(false, (int) Math.round(blurStrength.getValue()), false);
    }
}
