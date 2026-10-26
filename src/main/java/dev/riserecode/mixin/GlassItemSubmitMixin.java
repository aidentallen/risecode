package dev.riserecode.mixin;
import dev.riserecode.render.glass.GlassCapture;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemFeatureRenderer.Submit.class)
public abstract class GlassItemSubmitMixin implements GlassCapture.ItemState {
    @Unique private GlassCapture.Surface risecode$surface;
    @Inject(method="<init>",at=@At("TAIL"))
    private void risecode$capture(CallbackInfo ci){risecode$surface=GlassCapture.CURRENT.get();}
    @Override public GlassCapture.Surface risecode$glassSurface(){return risecode$surface;}
}
