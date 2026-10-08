package dev.riserecode.module.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.CameraEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Hold-to-look changes camera angles only; body rotation stays under vanilla control. */
public final class FreeLookModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting sensitivity = register(new DoubleSetting("Sensitivity", "Mouse multiplier", 1, 0.1, 3));
    private final BooleanSetting perspective = register(new BooleanSetting("Perspective", "Use third-person while held", true));
    private CameraType originalView;
    private boolean held;
    private float yaw, pitch;
    public FreeLookModule(EventBus bus, Minecraft mc) {
        super("FreeLook", "Look around without rotating your body.", Category.PLAYER, bus); minecraft = mc;
        defaultBinding(InputConstants.KEY_LALT, BindMode.HOLD);
    }
    @Override protected void onEnable() {
        listen(UpdateEvent.class, ignored -> {
            boolean pressed = minecraft.player != null && minecraft.player.isAlive() && minecraft.gui.screen() == null && minecraft.isWindowActive()
                    && (getBindMode() == BindMode.TOGGLE || getKeybind() != UNBOUND && InputConstants.isKeyDown(minecraft.getWindow(), getKeybind()));
            if (pressed && !held) {
                held = true; yaw = minecraft.player.getYRot(); pitch = minecraft.player.getXRot(); originalView = minecraft.options.getCameraType();
                if (perspective.getValue()) { minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
            } else if (!pressed && held) { release(); }
        });
        listen(MouseRotationEvent.class, 100, false, event -> {
            if (held && !event.isCancelled()) { yaw += (float) (event.yawDelta() * sensitivity.getValue());
                pitch = Math.clamp(pitch + (float) (event.pitchDelta() * sensitivity.getValue()), -90, 90); event.cancel(); }
        });
        listen(CameraEvent.class, 100, false, event -> {
            if (held && !dev.riserecode.Client.INSTANCE.getModuleManager().getByClass(FreeCamModule.class).map(Module::isEnabled).orElse(false)) {
                boolean mirrored = minecraft.options.getCameraType().isMirrored();
                float cameraYaw = mirrored ? yaw + 180 : yaw;
                float cameraPitch = mirrored ? -pitch : pitch;
                Vec3 position = event.position();
                if (!minecraft.options.getCameraType().isFirstPerson() && minecraft.level != null) {
                    float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    Vec3 eye = minecraft.player.getEyePosition(partial);
                    double yr = Math.toRadians(cameraYaw), pr = Math.toRadians(cameraPitch);
                    Vec3 backward = new Vec3(Math.sin(yr) * Math.cos(pr), Math.sin(pr), -Math.cos(yr) * Math.cos(pr)).scale(4);
                    Vec3 requested = eye.add(backward);
                    var hit = minecraft.level.clip(new ClipContext(eye, requested, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
                    double distance = Math.min(4, Math.max(0, eye.distanceTo(hit.getLocation()) - 0.1));
                    position = eye.add(backward.normalize().scale(distance));
                }
                event.set(position, cameraYaw, cameraPitch);
            }
        });
    }
    public boolean isHeld() { return held; }
    private void release() { if (originalView != null) { minecraft.options.setCameraType(originalView); } held = false; originalView = null; }
    @Override protected void onDisable() { release(); }
}
