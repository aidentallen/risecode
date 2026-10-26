package dev.riserecode.module.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MouseScrollEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.util.CameraEffectsManager;

public final class ZoomModule extends Module {
    private final CameraEffectsManager camera;
    private final DoubleSetting magnification = register(DoubleSetting.extended("Magnification", "Optical magnification", 3, 1, 10));
    private final BooleanSetting scroll = register(new BooleanSetting("ScrollAdjust", "Wheel changes zoom instead of the hotbar", true));
    private final BooleanSetting sensitivity = register(new BooleanSetting("ReduceSensitivity", "Scale mouse input with magnification", true));
    private boolean active;
    private double sessionMagnification;

    public ZoomModule(EventBus bus, CameraEffectsManager camera) {
        super("Zoom", "Smooth optical zoom with adjustable magnification.", Category.RENDER, bus);
        this.camera = camera; defaultBinding(InputConstants.KEY_C, BindMode.HOLD);
    }
    public DoubleSetting getMagnification() { return magnification; }
    public boolean isZoomActive() { return active; }
    public double activeMagnification() { return active ? sessionMagnification : 1; }
    @Override public boolean bindingActivatesEffect() { return true; }
    @Override public void activateBinding(boolean value) {
        if (!value || !isEnabled() || !camera.gameplay()) { active = false; camera.release(this); return; }
        if (!active) { sessionMagnification = magnification.getValue(); }
        active = true; camera.request(this, () -> sessionMagnification, sensitivity::getValue);
    }
    @Override protected void onEnable() {
        active = false;
        listen(UpdateEvent.class, event -> {
            if (active && !camera.gameplay()) { activateBinding(false); }
        });
        listen(MouseScrollEvent.class, 200, false, event -> {
            if (active && scroll.getValue() && camera.gameplay()) {
                sessionMagnification = Math.clamp(sessionMagnification * Math.pow(1.15, event.vertical()), magnification.getMin(), magnification.getMax());
                event.cancel();
            }
        });
    }
    @Override protected void onDisable() { activateBinding(false); }
}
