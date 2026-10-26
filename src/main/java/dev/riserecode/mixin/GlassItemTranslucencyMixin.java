package dev.riserecode.mixin;
import dev.riserecode.render.glass.GlassCapture;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemFeatureRenderer.Submit.class)
public abstract class GlassItemTranslucencyMixin {
    @Inject(method="hasTranslucency",at=@At("HEAD"),cancellable=true)
    private void risecode$translucent(CallbackInfoReturnable<Boolean> ci){if(((GlassCapture.ItemState)this).risecode$glassSurface()!=null)ci.setReturnValue(true);}
}
