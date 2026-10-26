package dev.riserecode.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Vector4f;

/** Keep custom world primitives out of Iris's pack shaders and final world overwrite. */
public final class ShaderWorldOverlay {
    public static final OutputTarget OUTPUT = new OutputTarget("risecode shader world overlay", ShaderWorldOverlay::target);
    private static TextureTarget overlay;
    private static boolean drawn;

    private static final class Pipelines {
        private static final RenderPipeline COMPOSITE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(dev.riserecode.ui.render.UiMesh.id("pipeline/world_overlay_composite"))
                .withVertexShader("core/screenquad").withFragmentShader("core/blit_screen")
                .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
                .withDepthStencilState(Optional.empty()).withCull(false)
                .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES).build());
    }

    private ShaderWorldOverlay() { }
    // Resolve optional Iris classes only when Iris is actually installed.
    private static final class IrisState {
        private static final net.irisshaders.iris.api.v0.IrisApi API = net.irisshaders.iris.api.v0.IrisApi.getInstance();
    }
    public static boolean active() {
        return FabricLoader.getInstance().isModLoaded("iris") && IrisState.API.isShaderPackInUse();
    }
    public static boolean shadowPass() { return active() && IrisState.API.isRenderingShadowPass(); }
    public static void beginFrame() { drawn = false; }

    private static RenderTarget target() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (!active()) { return main; }
        if (overlay == null || overlay.width != main.width || overlay.height != main.height) {
            close();
            overlay = new TextureTarget("risecode shader world overlay", main.width, main.height, true, GpuFormat.RGBA8_UNORM);
        }
        if (!drawn) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(overlay.getColorTexture(), new Vector4f(0, 0, 0, 0));
            // Submit collection precedes terrain: copy scene depth at the first actual draw.
            overlay.copyDepthFrom(main);
            drawn = true;
        }
        return overlay;
    }

    /** LevelRenderer has finished Iris's final pass; GUI processing has not started. */
    public static void composite() {
        if (!drawn || overlay == null) { return; }
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "risecode shader world overlay composite", main.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(Pipelines.COMPOSITE);
            RenderSystem.bindDefaultUniforms(pass);
            // Drawing transparent geometry already premultiplied its RGB into the target.
            pass.bindTexture("InSampler", overlay.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(3, 1, 0, 0);
        }
        drawn = false;
    }
    public static void close() {
        if (overlay != null) { overlay.destroyBuffers(); overlay = null; }
        drawn = false;
    }
}
