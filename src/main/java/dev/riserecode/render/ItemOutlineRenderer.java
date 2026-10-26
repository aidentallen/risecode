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

/** Exact screen-space item silhouettes. Original geometry is only written to a separate mask. */
public final class ItemOutlineRenderer {
    private record Key(Identifier atlas, boolean held) { }
    private static final Map<Key, RenderType> TYPES = new HashMap<>();
    private static TextureTarget mask, horizontal;
    private static final OutputTarget OUTPUT = new OutputTarget("risecode item mask", () -> depthReady());
    private static final GpuBuffer[] PARAMETERS = new GpuBuffer[2];
    private static Style frameStyle;
    private static boolean requested, fullBounds;
    private static boolean needsDepth, depthCopied;
    private static RenderTarget depthReady() {
        // Native submit collection precedes terrain execution. Copy only at the first mask draw.
        if (needsDepth && !depthCopied) { mask.copyDepthFrom(Minecraft.getInstance().gameRenderer.mainRenderTarget()); depthCopied = true; }
        return mask;
    }
    private static double minX, minY, maxX, maxY;
    private static int allocations;
    private static long composites,specialSubmissions;
    public static long specialSubmissions(){return specialSubmissions;}
    private static final class Pipelines {
        private static final RenderPipeline HELD_MASK = maskPipeline(false);
        private static final RenderPipeline WORLD_MASK = maskPipeline(true);
        private static final RenderPipeline DILATE = postPipeline(false);
        private static final RenderPipeline COMPOSITE = postPipeline(true);
    }
    private ItemOutlineRenderer() { }
    private static long lastRequestTick;
    public static long ownedGpuBytes(){return mask==null?0:mask.width*(long)mask.height*12;}
    public static void beginFrame() { if(mask!=null&&dev.riserecode.performance.PerformancePolicy.ticks()-lastRequestTick>=40){destroyTargets();} requested = false; frameStyle = null; fullBounds = false; needsDepth = depthCopied = false;
        minX = minY = Double.POSITIVE_INFINITY; maxX = maxY = Double.NEGATIVE_INFINITY;
    }
    public static long getCompositeCount() { return composites; }
    public static int getAllocationCount() { return allocations; }
    public static RenderTarget getMaskTarget() { return mask; }
    public static RenderTarget getHorizontalTarget() { return horizontal; }
    private static RenderPipeline maskPipeline(boolean depth) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(UiMesh.id(depth ? "pipeline/item_mask_world" : "pipeline/item_mask_held"))
                .withVertexShader(UiMesh.id("core/enchant")).withFragmentShader(UiMesh.id("core/item_mask"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(depth ? Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)) : Optional.empty())
                .withCull(false).withColorTargetState(ColorTargetState.DEFAULT).build());
    }
    private static RenderPipeline postPipeline(boolean composite) {
        return RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id(composite ? "pipeline/item_outline_composite" : "pipeline/item_outline_dilate"))
                .withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/item_outline"))
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").withSampler("Sampler1")
                        .withUniform("OutlineParams", UniformType.UNIFORM_BUFFER).build())
                .withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(composite ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT).build());
    }
    private static void ensureTargets() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (mask != null && mask.width == main.width && mask.height == main.height) { return; }
        destroyTargets();
        // Full resolution preserves subpixel edge coverage and pixel-based outline width.
        mask = new TextureTarget("risecode item silhouette", main.width, main.height, true, GpuFormat.RGBA8_UNORM);
        horizontal = new TextureTarget("risecode silhouette dilation", main.width, main.height, false, GpuFormat.RGBA8_UNORM);
        allocations++;
    }
    public static void submit(ItemFeatureRenderer.Submit submit, Style style, Function<RenderType, VertexConsumer> buffers) {
        if (!style.outline() || style.intensity() <= 0 || submit.quads().isEmpty()) { return; }
        lastRequestTick=dev.riserecode.performance.PerformancePolicy.ticks();ensureTargets();
        if (!requested) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(), new Vector4f(0, 0, 0, 0));
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(horizontal.getColorTexture(), new Vector4f(0, 0, 0, 0));
        }
        needsDepth |= !submit.displayContext().firstPerson();
        requested = true; frameStyle = style;
        org.joml.Matrix4f transform = null;
        if (submit.displayContext().firstPerson()) {
            transform = ((dev.riserecode.mixin.HandProjectionAccessor) Minecraft.getInstance().gameRenderer).riserecode$handProjection()
                    .getMatrix(new org.joml.Matrix4f()).mul(RenderSystem.getModelViewMatrixCopy()).mul(submit.pose().pose());
        } else { fullBounds = true; }
        for (var quad : submit.quads()) {
            Key key = new Key(quad.materialInfo().sprite().atlasLocation(), submit.displayContext().firstPerson());
            RenderType type = TYPES.computeIfAbsent(key, k -> RenderType.create("risecode item silhouette mask", RenderSetup.builder(k.held ? Pipelines.HELD_MASK : Pipelines.WORLD_MASK)
                    .withTexture("Sampler0", k.atlas).setOutputTarget(OUTPUT).createRenderSetup()));
            VertexConsumer vertices = buffers.apply(type);
            for (int i = 0; i < 4; i++) {
                if (transform != null) {
                    Vector4f point = transform.transform(new Vector4f(quad.position(i), 1));
                    if (point.w <= 0) { fullBounds = true; }
                    else {
                        double x = (point.x / point.w * 0.5 + 0.5) * mask.width, y = (point.y / point.w * 0.5 + 0.5) * mask.height;
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
                }
                long uv = quad.packedUV(i);
                vertices.addVertex(submit.pose(), quad.position(i)).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv)).setColor(0xFFFFFFFF);
            }
        }
    }
    public static <S> void submitModel(net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<S> submit,
            ItemVisualContext context, Style style, Function<RenderType, VertexConsumer> buffers) {
        if (!style.outline() || style.intensity() <= 0) { return; }
        Identifier texture=((TextureSource)(Object)submit.renderType()).riserecode$texture();
        if(texture==null){return;}
        specialSubmissions++;lastRequestTick=dev.riserecode.performance.PerformancePolicy.ticks();ensureTargets();
        if(!requested){RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(),new Vector4f(0,0,0,0));RenderSystem.getDevice().createCommandEncoder().clearColorTexture(horizontal.getColorTexture(),new Vector4f(0,0,0,0));}
        needsDepth|=!context.display().firstPerson();requested=true;frameStyle=style;fullBounds=true;
        Key key=new Key(texture,context.display().firstPerson());
        RenderType type=TYPES.computeIfAbsent(key,k->RenderType.create("risecode special item mask",RenderSetup.builder(k.held?Pipelines.HELD_MASK:Pipelines.WORLD_MASK).withTexture("Sampler0",k.atlas).setOutputTarget(OUTPUT).createRenderSetup()));
        VertexConsumer vertices=EnchantmentRenderer.tint(buffers.apply(type),0xFFFFFFFF);
        if(submit.sprite()!=null){vertices=submit.sprite().wrap(vertices);}
        var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.last().set(submit.pose());
        submit.model().setupAnim(submit.state());submit.model().renderToBuffer(pose,vertices,submit.lightCoords(),submit.overlayCoords(),0xFFFFFFFF);
    }
    /** Called after all world/held-item draws, before GUI scene blur and HUD composition. */
    public static void composite(RenderTarget main) {
        if (!requested || frameStyle == null || mask == null) { return; }
        ColorSetting.Paint paint = resolveTheme(frameStyle.outlinePaint());
        pass(horizontal, paint, frameStyle, 0); pass(main, paint, frameStyle, 1);
        requested = false; composites++;
    }
    private static ColorSetting.Paint resolveTheme(ColorSetting.Paint paint) {
        if (paint.mode() != ColorSetting.Mode.THEME || Theme.manager() == null) { return paint; }
        var themes = Theme.manager(); var mode = themes.choice("Gradient Mode", ColorUtil.GradientMode.class);
        if (mode == ColorUtil.GradientMode.STATIC && themes.colorSetting("Primary Accent").getMode() != ColorSetting.Mode.THEME) {
            return themes.colorSetting("Primary Accent").paint().opacity((paint.primary() >>> 24) / 255.0);
        }
        ColorSetting.Mode actual = switch (mode) { case STATIC -> ColorSetting.Mode.SOLID; case RAINBOW -> ColorSetting.Mode.RAINBOW; case ASTOLFO -> ColorSetting.Mode.ASTOLFO; case SINE -> ColorSetting.Mode.SINE; };
        return new ColorSetting.Paint(themes.color("Primary Accent"), themes.color("Secondary Accent"), actual,
                themes.choice("Gradient Direction", ColorUtil.GradientDirection.class), themes.number("Gradient Speed"), themes.number("Gradient Spacing")).opacity((paint.primary() >>> 24) / 255.0);
    }
    private static void pass(RenderTarget destination, ColorSetting.Paint paint, Style style, int mode) {
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
    private static void destroyTargets() { if (mask != null) { mask.destroyBuffers(); horizontal.destroyBuffers(); mask = horizontal = null; } }
    public static void close() { destroyTargets(); TYPES.clear(); beginFrame(); for (int i = 0; i < PARAMETERS.length; i++) { if (PARAMETERS[i] != null) { PARAMETERS[i].close(); PARAMETERS[i] = null; } } }
}
