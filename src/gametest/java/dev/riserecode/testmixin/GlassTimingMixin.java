package dev.riserecode.testmixin;
import dev.riserecode.GlassTimings;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class GlassTimingMixin {
    @Inject(method="render",at=@At("HEAD")) private void start(CallbackInfo ci){GlassTimings.begin();}
    @Inject(method="render",at=@At("RETURN")) private void end(CallbackInfo ci){GlassTimings.end();}
}
