package dev.riserecode.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Explicit adjacent-run batching. Never crosses native fallback draws or clip changes. */
public final class UiBatch {
    private static GuiGraphicsExtractor graphics;
    private static int depth;
    private static final List<GuiElementRenderState> pending = new ArrayList<>();
    private static long batches;
    private UiBatch() { }
    public static long batches() { return batches; }
    public static void begin(GuiGraphicsExtractor g) { if (depth++ == 0) { graphics = g; } }
    public static void end() { if (--depth == 0) { flush(); graphics = null; } }
    public static void submit(GuiGraphicsExtractor g, GuiElementRenderState state) {
        if (depth == 0) { g.guiRenderState.addGuiElement(state); return; }
        if (!pending.isEmpty()) {
            var first = pending.getFirst();
            if (first.pipeline() != state.pipeline() || !first.textureSetup().equals(state.textureSetup()) || !Objects.equals(first.scissorArea(), state.scissorArea())) { flush(); }
        }
        pending.add(state);
    }
    public static void flush() {
        if (pending.isEmpty()) { return; }
        var first = pending.getFirst(); int left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=Integer.MIN_VALUE,bottom=Integer.MIN_VALUE;
        for(var state:pending){ var r=state.bounds();left=Math.min(left,r.left());top=Math.min(top,r.top());right=Math.max(right,r.right());bottom=Math.max(bottom,r.bottom()); }
        graphics.guiRenderState.addGuiElement(new Run(first.pipeline(),first.textureSetup(),first.scissorArea(),new ScreenRectangle(left,top,right-left,bottom-top),List.copyOf(pending)));
        pending.clear(); batches++;
    }
    private record Run(RenderPipeline pipeline,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds,List<GuiElementRenderState> states) implements GuiElementRenderState {
        public void buildVertices(VertexConsumer vertices) { states.forEach(state -> state.buildVertices(vertices)); }
    }
}
