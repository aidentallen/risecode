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
        dev.riserecode.performance.PerformancePolicy.beginExtraction();
        dev.riserecode.ui.component.ResourcesPreviews.beginFrame();
        if (dev.riserecode.Client.INSTANCE.isInitialized()) { dev.riserecode.Client.INSTANCE.getMaps().textures().beginFrame(); dev.riserecode.Client.INSTANCE.getCosmetics().beginFrame(); }
        if(!net.minecraft.client.Minecraft.getInstance().isWindowActive()){dev.riserecode.ui.animation.ButtonFeedback.cancelAll();}
        dev.riserecode.ui.menu.MenuTheme.beginFrame();
        long now = System.nanoTime(); AnimationManager.ui().frame(now); Theme.advance(now);
        dev.riserecode.render.ChunkTerrainRenderer.frame(now);
        // Transfer ownership before native block-entity extraction decides which models to keep.
        dev.riserecode.render.placement.PlacementAnimations.frame(dev.riserecode.render.placement.PlacementAnimations.time());
    }
}
