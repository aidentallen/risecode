package dev.riserecode.module.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.Client;
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
    private final DoubleSetting magnification = register(new DoubleSetting("Magnification", "Optical magnification", 3, 1, 10));
    private final BooleanSetting scroll = register(new BooleanSetting("ScrollAdjust", "Wheel changes zoom instead of the hotbar", true));
    private final BooleanSetting sensitivity = register(new BooleanSetting("ReduceSensitivity", "Scale mouse input with magnification", true));

    public ZoomModule(EventBus bus, CameraEffectsManager camera) {
        super("Zoom", "Smooth optical zoom with adjustable magnification.", Category.RENDER, bus);
        this.camera = camera; defaultBinding(InputConstants.KEY_C, BindMode.HOLD);
    }
    public DoubleSetting getMagnification() { return magnification; }
    @Override protected void onEnable() {
        camera.request(this, magnification::getValue, sensitivity::getValue);
        listen(UpdateEvent.class, event -> {
            if (camera.gameplay()) { camera.request(this, magnification::getValue, sensitivity::getValue); }
            else { camera.release(this); }
        });
        listen(MouseScrollEvent.class, 200, false, event -> {
            if (scroll.getValue() && camera.gameplay()) {
                magnification.setValue(Math.clamp(magnification.getValue() * Math.pow(1.15, event.vertical()), 1, 10));
                Client.INSTANCE.getConfigManager().requestSave(); event.cancel();
            }
        });
    }
    @Override protected void onDisable() { camera.release(this); }
}
