package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.FogEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after Sodium Extra's environment/distance adjustments, on either backend. */
@Mixin(value = FogRenderer.class, priority = 900)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void riserecode$fog(Camera camera, int distance, DeltaTracker delta, float darken,
            ClientLevel level, CallbackInfoReturnable<FogData> callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        var entity = camera.entity();
        boolean blindness = entity instanceof LivingEntity living && living.hasEffect(MobEffects.BLINDNESS);
        boolean darkness = entity instanceof LivingEntity living && living.hasEffect(MobEffects.DARKNESS);
        var type = FogEvent.classify(camera.getFluidInCamera(), level.dimension().identifier().toString(), blindness, darkness);
        Client.INSTANCE.getEventBus().post(new FogEvent(type, callback.getReturnValue()));
    }
}
