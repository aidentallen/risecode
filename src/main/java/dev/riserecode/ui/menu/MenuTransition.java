package dev.riserecode.ui.menu;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Foreground-only extraction scope. Opacity is captured into immutable render states. */
public final class MenuTransition implements AutoCloseable {
    private static double opacity = 1;
    private final GuiGraphicsExtractor graphics;
    private final double previous;
    private MenuTransition(GuiGraphicsExtractor graphics, Screen screen) {
        this.graphics = graphics; previous = opacity;
        graphics.pose().pushMatrix();
    }
    private MenuTransition(GuiGraphicsExtractor graphics,float cx,float cy,double scale,double alpha) {
        this.graphics=graphics;previous=opacity;opacity*=alpha;
        graphics.pose().pushMatrix();graphics.pose().translate(cx,cy);graphics.pose().scale((float)scale,(float)scale);graphics.pose().translate(-cx,-cy);
    }
    public static MenuTransition widget(GuiGraphicsExtractor graphics,float cx,float cy,double scale,double alpha) { return new MenuTransition(graphics,cx,cy,scale,alpha); }

    public static MenuTransition begin(GuiGraphicsExtractor graphics, Screen screen) { return new MenuTransition(graphics, screen); }
    public static double opacity() { return opacity; }
    public static int color(int color) { return color(color, opacity); }
    private static int color(int color, double alpha) { return color & 0xFFFFFF | (int) Math.round((color >>> 24) * alpha) << 24; }
    @Override public void close() { graphics.pose().popMatrix(); opacity = previous; }
    /** States whose opacity was already captured, including batches of such states. */
    public interface Captured extends GuiElementRenderState { }
    public static GuiElementRenderState capture(GuiElementRenderState state) {
        return state instanceof Captured || opacity >= 1 ? state : new Faded(state, opacity);
    }
    private record Faded(GuiElementRenderState source, double alpha) implements Captured {
        @Override public RenderPipeline pipeline() { return source.pipeline(); }
        @Override public TextureSetup textureSetup() { return source.textureSetup(); }
        @Override public ScreenRectangle scissorArea() { return source.scissorArea(); }
        @Override public ScreenRectangle bounds() { return source.bounds(); }
        @Override public void buildVertices(VertexConsumer vertices) { source.buildVertices(new FadedVertices(vertices, alpha)); }
    }
    private record FadedVertices(VertexConsumer source, double alpha) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x,float y,float z) { source.addVertex(x,y,z); return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { source.setColor(r,g,b,(int)Math.round(a*alpha)); return this; }
        @Override public VertexConsumer setColor(int value) { source.setColor(color(value,alpha)); return this; }
        @Override public VertexConsumer setUv(float u,float v) { source.setUv(u,v); return this; }
        @Override public VertexConsumer setUv1(int u,int v) { source.setUv1(u,v); return this; }
        @Override public VertexConsumer setUv2(int u,int v) { source.setUv2(u,v); return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) { source.setNormal(x,y,z); return this; }
        @Override public VertexConsumer setLineWidth(float width) { source.setLineWidth(width); return this; }
    }
}
