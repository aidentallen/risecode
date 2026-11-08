package dev.riserecode.testmixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.irisshaders.iris.shadows.ShadowRenderer",remap=false)
public abstract class ReviewShadowMixin {
    @Inject(method="renderShadows",at=@At("HEAD"),require=0)
    private void riserecode$shadowInvocation(CallbackInfo callback){if(Boolean.getBoolean("riserecode.reviewOverlayOnly")){dev.riserecode.ReviewOverlayIntegrationTest.shadowPasses++;}}
}
