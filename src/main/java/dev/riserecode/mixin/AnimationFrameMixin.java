package dev.riserecode.mixin;

import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class AnimationFrameMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void riserecode$animationFrame(CallbackInfo callback) {
        long now = System.nanoTime(); AnimationManager.ui().frame(now); Theme.advance(now);
        dev.riserecode.render.ChunkTerrainRenderer.frame(now);
    }
}
