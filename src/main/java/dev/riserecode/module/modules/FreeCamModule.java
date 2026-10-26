package dev.riserecode.module.modules;

import dev.riserecode.event.CameraEvent;
import dev.riserecode.event.CameraInputEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Camera-only flight: no dummy entity, player teleport, packet replacement or server camera change. */
public final class FreeCamModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting speed = register(DoubleSetting.extended("Speed", "Camera blocks per tick", 0.5, 0.05, 5));
    private final BooleanSetting vertical = register(new BooleanSetting("VerticalMovement", "Jump/sneak moves vertically", true));
    private final BooleanSetting rotate = register(new BooleanSetting("Rotate", "Mouse rotates the detached camera", true));
    private LocalPlayer player;
    private ClientLevel level;
    private Vec3 position, previous;
    private float yaw, pitch;
    public FreeCamModule(EventBus bus, Minecraft mc) { super("FreeCam", "Client-side detached fly camera.", Category.RENDER, bus);
        minecraft = mc; }
    @Override protected void onEnable() {
        capture();
        listen(UpdateEvent.class, ignored -> { if (minecraft.player == null || minecraft.player != player || minecraft.level != level) { capture(); } });
        listen(CameraInputEvent.class, event -> {
            if (player == null || minecraft.gui.screen() != null) { return; }
            previous = position;
            double radians = Math.toRadians(yaw), forward = event.movement().y, left = event.movement().x;
            double dy = vertical.getValue() ? (event.keys().jump() ? 1 : 0) - (event.keys().shift() ? 1 : 0) : 0;
            Vec3 direction = new Vec3(-Math.sin(radians) * forward + Math.cos(radians) * left, dy,
                    Math.cos(radians) * forward + Math.sin(radians) * left);
            if (direction.lengthSqr() > 1) { direction = direction.normalize(); }
            position = position.add(direction.scale(speed.getValue())); event.cancel();
        });
        listen(MouseRotationEvent.class, 200, false, event -> {
            if (player == null) { return; }
            if (rotate.getValue()) { yaw += (float) event.yawDelta(); pitch = Math.clamp(pitch + (float) event.pitchDelta(), -90, 90); }
            event.cancel();
        });
        listen(CameraEvent.class, 200, false, event -> {
            if (player != null && position != null && minecraft.player == player) {
                float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                event.set(previous.lerp(position, partial), yaw, pitch);
            }
        });
    }
    private void capture() {
        player = minecraft.player; level = minecraft.level;
        if (player != null) { position = previous = player.getEyePosition(); yaw = player.getYRot(); pitch = player.getXRot(); }
        else { position = previous = null; }
    }
    public Vec3 cameraPosition() { return position; }
    @Override protected void onDisable() { player = null; level = null; position = previous = null; }
}
