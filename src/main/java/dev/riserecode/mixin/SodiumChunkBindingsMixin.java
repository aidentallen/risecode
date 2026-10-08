package dev.riserecode.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.render.ChunkTerrainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Select before DrawContext caches uniform locations, keeping program and draw data aligned. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class SodiumChunkBindingsMixin {
    @Shadow protected RenderPipeline activeProgram;
    @Inject(method = "begin", at = @At("TAIL"), require = 0)
    private void riserecode$program(CallbackInfo ci) {
        activeProgram = ChunkTerrainRenderer.adapt(AmbienceRenderer.adapt(activeProgram));
    }
}
