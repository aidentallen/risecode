package dev.riserecode.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riserecode.event.EnchantmentRenderEvent.Style;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Matrix4f;

/** Native glint customization; item outlines use a separate exact silhouette mask. */
public final class EnchantmentRenderer {
    private static final Map<OutputTarget, RenderType> FOILS = new HashMap<>();
    private static Style glintStyle;
    private static final class Pipelines {
        private static final RenderPipeline GLINT = pipeline();
    }
    private EnchantmentRenderer() { }
    private static RenderPipeline pipeline() {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(UiMesh.id("pipeline/enchant_glint")).withVertexShader(UiMesh.id("core/enchant"))
                .withFragmentShader(UiMesh.id("core/enchant_glint")).withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0).withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.GLINT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false)).build());
    }
    public static RenderType glintType(Style style, RenderType original) {
        glintStyle = style;
        boolean transparent = net.minecraft.client.Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                && original.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
        OutputTarget output = (transparent ? RenderTypes.glintTranslucent() : RenderTypes.glint()).outputTarget();
        return FOILS.computeIfAbsent(output, target -> RenderType.create("risecode_glint", RenderSetup.builder(Pipelines.GLINT)
                .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM).setOutputTarget(target)
                .setTextureTransform(new TextureTransform("risecode glint", EnchantmentRenderer::glintMatrix)).createRenderSetup()));
    }
    private static Matrix4f glintMatrix() {
        Style style = glintStyle; double t = Theme.gradientTime() * (style == null ? 1 : style.speed());
        return new Matrix4f().translation((float) (-t * 0.025 % 1), (float) (t * 0.04 % 1), 0)
                .rotateZ(0.19f).scale((float) (8 * (style == null ? 1 : style.scale())));
    }
    public static void outline(ItemFeatureRenderer.Submit submit, Style style, Function<RenderType, VertexConsumer> buffers) {
        ItemOutlineRenderer.submit(submit, style, buffers);
    }
    public static VertexConsumer tint(VertexConsumer target, dev.riserecode.setting.ColorSetting.Paint paint) {
        return new VertexConsumer() {
            private int color = paint.primary();
            @Override public VertexConsumer addVertex(float x, float y, float z) {
                double distance = Math.max(0.1, Math.abs(z));
                var window = net.minecraft.client.Minecraft.getInstance().getWindow();
                color = paint.sample((x / distance + 1) * window.getGuiScaledWidth() / 2.0, (1 - y / distance) * window.getGuiScaledHeight() / 2.0, Theme.gradientTime());
                target.addVertex(x, y, z); target.setColor(color); return this;
            }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(color); return this; }
            @Override public VertexConsumer setColor(int value) { target.setColor(color); return this; }
            @Override public VertexConsumer setUv(float u, float v) { target.setUv(u,v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }
        };
    }
    public static VertexConsumer tint(VertexConsumer target, int color) {
        return new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { target.addVertex(x,y,z); target.setColor(color); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(color); return this; }
            @Override public VertexConsumer setColor(int value) { target.setColor(color); return this; }
            @Override public VertexConsumer setUv(float u, float v) { target.setUv(u,v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }
        };
    }
}
