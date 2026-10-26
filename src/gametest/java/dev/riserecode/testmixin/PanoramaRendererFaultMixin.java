package dev.riserecode.testmixin;

import dev.riserecode.PanoramaIntegrationTest;
import dev.riserecode.ui.panorama.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public abstract class PanoramaRendererFaultMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void risecode$failure(PanoramaRenderer.Frame frame,CallbackInfo ci){if(PanoramaIntegrationTest.failNextRender){PanoramaIntegrationTest.failNextRender=false;throw new IllegalStateException("injected panorama failure");}}
}
