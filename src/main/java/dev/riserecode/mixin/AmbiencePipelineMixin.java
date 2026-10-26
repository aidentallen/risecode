package dev.riserecode.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import dev.riserecode.render.AmbienceRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared native RenderPass boundary used by vanilla, Sodium, OpenGL and Vulkan. */
@Mixin(RenderPass.class)
public abstract class AmbiencePipelineMixin {
    @ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
    private RenderPipeline riserecode$water(RenderPipeline pipeline) { return dev.riserecode.render.ChunkTerrainRenderer.adapt(AmbienceRenderer.adapt(pipeline)); }
    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void riserecode$parameters(RenderPipeline pipeline, CallbackInfo ci) {
        AmbienceRenderer.bindParameters((RenderPass)(Object)this, pipeline);
        dev.riserecode.render.ChunkTerrainRenderer.bindParameters((RenderPass)(Object)this, pipeline);
    }
}
