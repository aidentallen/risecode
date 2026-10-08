package dev.riserecode.mixin;

import dev.riserecode.render.AmbienceRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class AmbienceSkyMixin {
    @Shadow @Final private com.mojang.blaze3d.pipeline.RenderTarget renderTarget;
    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void riserecode$sky(CallbackInfo ci) { if (AmbienceRenderer.renderSky(renderTarget)) { ci.cancel(); } }
    @Inject(method = {"renderSunMoonAndStars", "renderSunriseAndSunset", "renderDarkDisc"}, at = @At("HEAD"), cancellable = true)
    private void riserecode$celestials(CallbackInfo ci) { if (AmbienceRenderer.skyActive()) { ci.cancel(); } }
}
