package dev.riserecode.testmixin;
import dev.riserecode.Release025Timings;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class Release025TimingMixin {
    @Inject(method="render",at=@At("HEAD")) private void start(CallbackInfo ci){Release025Timings.begin();}
    @Inject(method="render",at=@At("RETURN")) private void end(CallbackInfo ci){Release025Timings.end();}
}
