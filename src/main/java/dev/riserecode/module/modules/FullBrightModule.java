package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;

/** Uses the supported vanilla gamma range, without modifying world lighting. */
public final class FullBrightModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting brightness = register(new DoubleSetting(
            "Brightness", "Vanilla brightness from 0% to 100%", 1.0, 0.0, 1.0));
    private Double previousGamma;

    public FullBrightModule(EventBus eventBus, Minecraft minecraft) {
        super("FullBright", "Raises client brightness and restores it on disable.", Category.RENDER, eventBus);
        this.minecraft = minecraft;
    }

    public DoubleSetting getBrightness() { return brightness; }

    @Override
    protected void onEnable() {
        previousGamma = minecraft.options.gamma().get();
        applyBrightness();
        listen(UpdateEvent.class, event -> applyBrightness());
    }

    private void applyBrightness() { minecraft.options.gamma().set(brightness.getValue()); }

    @Override
    protected void onDisable() {
        if (previousGamma != null) {
            minecraft.options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }
}
