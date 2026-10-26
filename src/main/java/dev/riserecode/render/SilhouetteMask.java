package dev.riserecode.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riserecode.event.EnchantmentRenderEvent.Style;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** Reusable screen-space union mask. Native model geometry never alters the scene interior. */
final class SilhouetteMask {
    private TextureTarget mask, horizontal;
    private final OutputTarget OUTPUT = new OutputTarget("risecode silhouette mask", () -> depthReady());
    private final GpuBuffer[] PARAMETERS = new GpuBuffer[2];
    private Style frameStyle;
    private boolean requested, fullBounds;
    private boolean needsDepth, depthCopied;
    private RenderTarget depthReady() {
        // Native submit collection precedes terrain execution. Copy only at the first mask draw.
        if (needsDepth && !depthCopied) { mask.copyDepthFrom(Minecraft.getInstance().gameRenderer.mainRenderTarget()); depthCopied = true; }
        return mask;
    }
    private double minX, minY, maxX, maxY;
    private int allocations;
    private long composites;
    private static final class Pipelines {
        private record PipelineKey(com.mojang.blaze3d.vertex.VertexFormat format,boolean visible) { }
        private static final Map<PipelineKey, RenderPipeline> MASKS = new HashMap<>();
        private static final RenderPipeline DILATE = postPipeline(false);
        private static final RenderPipeline COMPOSITE = postPipeline(true);
    }
    SilhouetteMask() { }
    private long lastRequestTick;
    public long ownedGpuBytes(){return mask==null?0:mask.width*(long)mask.height*12;}
    public void beginFrame() { if(mask!=null&&dev.riserecode.performance.PerformancePolicy.ticks()-lastRequestTick>=40){destroyTargets();} requested = false; frameStyle = null; fullBounds = false; needsDepth = depthCopied = false;
        minX = minY = Double.POSITIVE_INFINITY; maxX = maxY = Double.NEGATIVE_INFINITY;
    }
    public long getCompositeCount() { return composites; }
    public int getAllocationCount() { return allocations; }
    public RenderTarget getMaskTarget() { return mask; }
    public RenderTarget getHorizontalTarget() { return horizontal; }
    private static RenderPipeline maskPipeline(boolean depth, com.mojang.blaze3d.vertex.VertexFormat format) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(UiMesh.id("pipeline/esp_mask_" + (depth ? "visible_" : "through_") + Pipelines.MASKS.size()))
                .withVertexShader(UiMesh.id("core/enchant")).withFragmentShader(UiMesh.id("core/item_mask"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0, format).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(depth ? Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)) : Optional.empty())
                .withCull(false).withColorTargetState(ColorTargetState.DEFAULT).build());
    }
    private static RenderPipeline postPipeline(boolean composite) {
        return RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id(composite ? "pipeline/esp_outline_composite" : "pipeline/esp_outline_dilate"))
                .withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/item_outline"))
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").withSampler("Sampler1")
                        .withUniform("OutlineParams", UniformType.UNIFORM_BUFFER).build())
                .withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(composite ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT).build());
    }
    private void ensureTargets() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (mask != null && mask.width == main.width && mask.height == main.height) { return; }
        destroyTargets();
        // Full resolution preserves subpixel edge coverage and pixel-based outline width.
        mask = new TextureTarget("risecode item silhouette", main.width, main.height, true, GpuFormat.RGBA8_UNORM);
        horizontal = new TextureTarget("risecode silhouette dilation", main.width, main.height, false, GpuFormat.RGBA8_UNORM);
        allocations++;
    }
    void request(Style style, net.minecraft.world.phys.AABB bounds, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        lastRequestTick=dev.riserecode.performance.PerformancePolicy.ticks();ensureTargets();
        if (!requested) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(), new Vector4f(0, 0, 0, 0));
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(horizontal.getColorTexture(), new Vector4f(0, 0, 0, 0));
        }
        requested = true; frameStyle = style;
        var matrix = new org.joml.Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
        var point=new Vector4f();
        for (int i = 0; i < 8; i++) {
            matrix.transform(point.set((float) ((i & 1) == 0 ? bounds.minX - camera.pos.x : bounds.maxX - camera.pos.x),
                    (float) ((i & 2) == 0 ? bounds.minY - camera.pos.y : bounds.maxY - camera.pos.y),
                    (float) ((i & 4) == 0 ? bounds.minZ - camera.pos.z : bounds.maxZ - camera.pos.z), 1));
            if (point.w <= 0) { fullBounds = true; continue; }
            double x = (point.x / point.w * 0.5 + 0.5) * mask.width, y = (point.y / point.w * 0.5 + 0.5) * mask.height;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
    }
    private record MaskType(Identifier atlas, com.mojang.blaze3d.vertex.VertexFormat format, boolean visible) { }
    private final Map<MaskType, RenderType> maskTypes = new HashMap<>();
    RenderType type(Identifier atlas, com.mojang.blaze3d.vertex.VertexFormat format, boolean visible) {
        needsDepth |= visible;
        var key=new MaskType(atlas,format,visible);var cached=maskTypes.get(key);if(cached!=null){return cached;}
        var id=new Pipelines.PipelineKey(format,visible);var pipeline=Pipelines.MASKS.get(id);if(pipeline==null){pipeline=maskPipeline(visible,format);Pipelines.MASKS.put(id,pipeline);}
        var type=RenderType.create("risecode silhouette",RenderSetup.builder(pipeline).withTexture("Sampler0",atlas).setOutputTarget(OUTPUT).createRenderSetup());maskTypes.put(key,type);return type;
    }
    /** Called after all world/held-item draws, before GUI scene blur and HUD composition. */
    public void composite(RenderTarget main) {
        if (!requested || frameStyle == null || mask == null) { return; }
        ColorSetting.Paint paint = resolveTheme(frameStyle.outlinePaint());
        pass(horizontal, paint, frameStyle, 0); pass(main, paint, frameStyle, 1);
        requested = false; composites++;
    }
    private ColorSetting.Paint resolveTheme(ColorSetting.Paint paint) {
        if (paint.mode() != ColorSetting.Mode.THEME || Theme.manager() == null) { return paint; }
        var themes = Theme.manager(); var mode = themes.choice("Gradient Mode", ColorUtil.GradientMode.class);
        if (mode == ColorUtil.GradientMode.STATIC && themes.colorSetting("Primary Accent").getMode() != ColorSetting.Mode.THEME) {
            return themes.colorSetting("Primary Accent").paint().opacity((paint.primary() >>> 24) / 255.0);
        }
        ColorSetting.Mode actual = switch (mode) { case STATIC -> ColorSetting.Mode.SOLID; case RAINBOW -> ColorSetting.Mode.RAINBOW; case ASTOLFO -> ColorSetting.Mode.ASTOLFO; case SINE -> ColorSetting.Mode.SINE; };
        return new ColorSetting.Paint(themes.color("Primary Accent"), themes.color("Secondary Accent"), actual,
                themes.choice("Gradient Direction", ColorUtil.GradientDirection.class), themes.number("Gradient Speed"), themes.number("Gradient Spacing")).opacity((paint.primary() >>> 24) / 255.0);
    }
    private void pass(RenderTarget destination, ColorSetting.Paint paint, Style style, int mode) {
        if (PARAMETERS[mode] == null) { PARAMETERS[mode] = RenderSystem.getDevice().createBuffer(() -> "risecode outline parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 80); }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bytes = Std140Builder.onStack(stack, 80).putVec4(1f / mask.width, 1f / mask.height, (float) (style.width()*WorldResolution.pixelScale()), (float) (style.glow()*WorldResolution.pixelScale()))
                    .putVec4(channel(paint.primary(), 16), channel(paint.primary(), 8), channel(paint.primary(), 0), channel(paint.primary(), 24))
                    .putVec4(channel(paint.secondary(), 16), channel(paint.secondary(), 8), channel(paint.secondary(), 0), channel(paint.secondary(), 24))
                    .putVec4(paint.mode().ordinal(), paint.direction().ordinal(), (float) paint.speed(), (float) paint.spacing())
                    .putVec4((float) Theme.gradientTime(), (float) style.intensity(), mode, (float) (Minecraft.getInstance().getWindow().getGuiScale()*WorldResolution.pixelScale())).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMETERS[mode].slice(), bytes);
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "risecode item silhouette outline", destination.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(mode == 0 ? Pipelines.DILATE : Pipelines.COMPOSITE);
            if (!fullBounds && Double.isFinite(minX)) {
                int margin = (int) Math.ceil(style.width() + style.glow() + 3);
                int left = (int) Math.clamp(Math.floor(minX) - margin, 0, mask.width - 1), bottom = (int) Math.clamp(Math.floor(minY) - margin, 0, mask.height - 1);
                int right = (int) Math.clamp(Math.ceil(maxX) + margin, left + 1, mask.width), top = (int) Math.clamp(Math.ceil(maxY) + margin, bottom + 1, mask.height);
                pass.enableScissor(left, bottom, right - left, top - bottom);
            }
            pass.setUniform("OutlineParams", PARAMETERS[mode]);
            var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            pass.bindTexture("Sampler0", mode == 0 ? mask.getColorTextureView() : horizontal.getColorTextureView(), sampler);
            pass.bindTexture("Sampler1", mask.getColorTextureView(), sampler);
            pass.draw(3, 1, 0, 0);
        }
    }
    private static float channel(int color, int shift) { return ((color >>> shift) & 255) / 255f; }
    private void destroyTargets() { if (mask != null) { mask.destroyBuffers(); horizontal.destroyBuffers(); mask = horizontal = null; } }
    public void close() { destroyTargets(); maskTypes.clear(); beginFrame(); for (int i = 0; i < PARAMETERS.length; i++) { if (PARAMETERS[i] != null) { PARAMETERS[i].close(); PARAMETERS[i] = null; } } }
}
