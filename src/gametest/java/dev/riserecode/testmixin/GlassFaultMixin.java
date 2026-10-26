package dev.riserecode.testmixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.GpuDevice;
import dev.riserecode.GlassIntegrationTest;
import dev.riserecode.render.glass.GlassRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GlassRenderer.class)
public abstract class GlassFaultMixin {
    @WrapOperation(method="type",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/GpuDevice;precompilePipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/pipeline/CompiledRenderPipeline;"))
    private static CompiledRenderPipeline fault(GpuDevice device,RenderPipeline pipeline,Operation<CompiledRenderPipeline> original){
        if(GlassIntegrationTest.failNextPipeline){GlassIntegrationTest.failNextPipeline=false;throw new IllegalStateException("Injected Glass readiness fault");}
        return original.call(device,pipeline);
    }
}
