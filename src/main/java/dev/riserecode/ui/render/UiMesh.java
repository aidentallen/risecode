package dev.riserecode.ui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Four-vertex extracted quads. Effects travel in vertex attributes, avoiding mutable shared UBOs. */
public final class UiMesh {
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UV1", GpuFormat.RG16_SINT)
            .addAttribute("UV2", GpuFormat.RG16_SINT).build();
    private UiMesh() { }
    public static RenderPipeline pipeline(String name, String fragment) {
        return RenderPipelines.register(RenderPipeline.builder(fragment.equals("shape") || fragment.equals("window") || fragment.equals("hsv") ? RenderPipelines.GUI_SNIPPET : RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name)).withVertexShader(id("core/ui"))
                .withFragmentShader(id("core/" + fragment)).withVertexBinding(0, FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("riserecode", path); }
    public static void quad(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double width, double height, float u0, float v0, float u1, float v1,
            int topColor, int bottomColor, int a, int b, int c, int d) {
        quad(graphics, pipeline, texture, x, y, width, height, u0, v0, u1, v1, topColor, bottomColor, a, b, c, d, false);
    }
    public static void quad(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double width, double height, float u0, float v0, float u1, float v1,
            int firstColor, int secondColor, int a, int b, int c, int d, boolean horizontal) {
        if (width <= 0 || height <= 0) { return; }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(width + 1), (int) Math.ceil(height + 1)).transformMaxBounds(pose);
        if (scissor != null) { bounds = bounds.intersection(scissor); }
        if (bounds == null) { return; }
        UiBatch.submit(graphics, new Quad(pipeline, texture, pose, (float) x, (float) y,
                (float) width, (float) height, u0, v0, u1, v1, firstColor, secondColor, a, b, c, d, horizontal, scissor, bounds));
    }
    private record Quad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x, float y,
            float width, float height, float u0, float v0, float u1, float v1, int top, int bottom,
            int a, int b, int c, int d, boolean horizontal, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, x, y, u0, v0, top); vertex(vertices, x, y + height, u0, v1, horizontal ? top : bottom);
            vertex(vertices, x + width, y + height, u1, v1, bottom); vertex(vertices, x + width, y, u1, v0, horizontal ? bottom : top);
        }
        private void vertex(VertexConsumer vertices, float px, float py, float u, float v, int color) {
            vertices.addVertexWith2DPose(pose, px, py).setUv(u, v).setColor(color).setUv1(a, b).setUv2(c, d);
        }
    }
}
