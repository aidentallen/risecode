package dev.riserecode.mixin;

import dev.riserecode.render.ShaderWorldOverlay;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldOverlayFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void riserecode$beginWorldOverlay(CallbackInfo callback) { ShaderWorldOverlay.beginFrame(); }

    @Inject(method = "render", at = @At("RETURN"))
    private void riserecode$finishWorldOverlay(CallbackInfo callback) { ShaderWorldOverlay.composite(); }
}
