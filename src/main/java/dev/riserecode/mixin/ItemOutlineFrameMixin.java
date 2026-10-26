package dev.riserecode.mixin;

import dev.riserecode.render.ItemOutlineRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ItemOutlineFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void riserecode$beginOutlineFrame(CallbackInfo callback) { ItemOutlineRenderer.beginFrame(); dev.riserecode.render.SilhouetteOutlineRenderer.beginFrame(); }
}
