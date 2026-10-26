package dev.riserecode.mixin;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.View;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class GlassFrameMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void risecode$begin(CallbackInfo ci){GlassRenderer.beginFrame();}
    @Inject(method="renderLevel",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/GameRenderer;hudProjection:Lnet/minecraft/client/renderer/Projection;",ordinal=0))
    private void risecode$world(CallbackInfo ci){GlassRenderer.flush(View.WORLD);}
    @Inject(method="renderItemInHand",at=@At("RETURN"))
    private void risecode$hand(CallbackInfo ci){GlassRenderer.flush(View.HAND);}
    @Inject(method={"resize","close"},at=@At("HEAD"))
    private void risecode$release(CallbackInfo ci){GlassRenderer.close();GlassPreview.close();}
}
