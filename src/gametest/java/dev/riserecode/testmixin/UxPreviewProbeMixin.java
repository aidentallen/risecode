package dev.riserecode.testmixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.riserecode.cosmetics.*;
import dev.riserecode.event.CosmeticSubmitEvent;
@Mixin(CosmeticsRenderer.class)
public abstract class UxPreviewProbeMixin {
    @Inject(method="submit",at=@At("HEAD"))
    private static void riserecode$preview(CosmeticsManager manager,CosmeticSubmitEvent event,CallbackInfo ci){if(dev.riserecode.UxRefinementIntegrationTest.active&&((CosmeticRenderState)event.state).risecode$preview()){dev.riserecode.UxRefinementIntegrationTest.renderedAge=event.state.ageInTicks;dev.riserecode.UxRefinementIntegrationTest.renderedWalking=event.state.walkAnimationSpeed;dev.riserecode.UxRefinementIntegrationTest.renderedFlying=event.state.isFallFlying;dev.riserecode.UxRefinementIntegrationTest.renderedCrouching=event.state.isCrouching;}}
    @Inject(method="wings",at=@At("HEAD"))
    private static void riserecode$wingPhase(CosmeticSubmitEvent event,CosmeticLoadout.Item item,float time,int primary,int secondary,CallbackInfo ci){if(dev.riserecode.UxRefinementIntegrationTest.active){dev.riserecode.UxRefinementIntegrationTest.wingPhase=time;}}
}
