package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CameraEvent;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow public abstract Vec3 position();
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(Vec3 position);
    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void riserecode$camera(float partialTick, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        CameraEvent event = new CameraEvent(position(), yRot(), xRot(), detached);
        Client.INSTANCE.getEventBus().post(event);
        setPosition(event.position()); setRotation(event.yaw(), event.pitch()); detached = event.detached();
    }
}
