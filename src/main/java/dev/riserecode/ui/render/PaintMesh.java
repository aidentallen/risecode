package dev.riserecode.ui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.riserecode.mixin.VertexDataAccessor;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

/** A complete immutable paint snapshot on one shape; shader samples logical screen coordinates. */
public final class PaintMesh {
    private static final VertexFormat FORMAT = VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("Shape", GpuFormat.RGBA32_FLOAT)
            .addAttribute("First", GpuFormat.RGBA32_FLOAT).addAttribute("Second", GpuFormat.RGBA32_FLOAT)
            .addAttribute("Policy", GpuFormat.RGBA32_FLOAT).addAttribute("NextFirst", GpuFormat.RGBA32_FLOAT)
            .addAttribute("NextSecond", GpuFormat.RGBA32_FLOAT).addAttribute("NextPolicy", GpuFormat.RGBA32_FLOAT)
            .addAttribute("MixInfo", GpuFormat.RGBA32_FLOAT).build();
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(UiMesh.id("pipeline/paint")).withVertexShader(UiMesh.id("core/paint"))
            .withFragmentShader(UiMesh.id("core/paint")).withVertexBinding(0, FORMAT).withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    private PaintMesh() { }
    private static ColorSetting.Paint resolved(ColorSetting.Paint paint) {
        if (paint.mode() != ColorSetting.Mode.THEME || Theme.manager() == null) { return paint; }
        var theme = Theme.manager(); var primary = theme.colorSetting("Primary Accent"); ColorSetting.Paint result;
        var mode = theme.choice("Gradient Mode", ColorUtil.GradientMode.class);
        if (mode == ColorUtil.GradientMode.STATIC) { result = primary.getMode() == ColorSetting.Mode.THEME ? primary.defaultPaint() : primary.paint(); }
        else { result = new ColorSetting.Paint(theme.color("Primary Accent"), theme.color("Secondary Accent"), ColorSetting.Mode.valueOf(mode.name()), theme.gradientDirection(), theme.number("Gradient Speed"), theme.number("Gradient Spacing")); }
        return result.opacity((paint.primary() >>> 24) / 255.0);
    }
    public static void shape(GuiGraphicsExtractor g, double x, double y, double w, double h, double radius,
            ColorSetting.Paint paint, ColorSetting.Paint next, double blend, double effect) {
        double pad = effect < 0 ? -effect + 1 : 1;
        var pose = new Matrix3x2f(g.pose()); var scissor = g.scissorStack.peek();
        var bounds = new ScreenRectangle((int)Math.floor(x-pad),(int)Math.floor(y-pad),(int)Math.ceil(w+pad*2+1),(int)Math.ceil(h+pad*2+1)).transformMaxBounds(pose);
        if (scissor != null) { bounds = bounds.intersection(scissor); } if (bounds == null) { return; }
        ColorSetting.Paint first = resolved(paint), second = next == null ? first : resolved(next);
        float[] data = new float[32]; data[0]=(float)w/2; data[1]=(float)h/2; data[2]=(float)Math.min(radius,Math.min(w,h)/2); data[3]=(float)effect;
        pack(data,4,first); pack(data,16,second); data[28]=(float)blend; data[29]=(float)Theme.gradientTime();
        UiBatch.submit(g, new Shape(pose,(float)x,(float)y,(float)w,(float)h,(float)pad,data,scissor,bounds));
    }
    private static void pack(float[] data, int offset, ColorSetting.Paint p) {
        color(data,offset,p.primary()); color(data,offset+4,p.secondary()); data[offset+8]=p.mode().ordinal(); data[offset+9]=p.direction().ordinal(); data[offset+10]=(float)p.speed(); data[offset+11]=(float)p.spacing();
    }
    private static void color(float[] data,int offset,int color) { for(int i=0;i<3;i++){data[offset+i]=((color>>(16-i*8))&255)/255f;} data[offset+3]=(color>>>24)/255f; }
    private record Shape(Matrix3x2f pose,float x,float y,float w,float h,float pad,float[] data,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return PIPELINE; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer v) {
            vertex(v,x-pad,y-pad,-w/2-pad,-h/2-pad); vertex(v,x-pad,y+h+pad,-w/2-pad,h/2+pad);
            vertex(v,x+w+pad,y+h+pad,w/2+pad,h/2+pad); vertex(v,x+w+pad,y-pad,w/2+pad,-h/2-pad);
        }
        private void vertex(VertexConsumer v,float px,float py,float u,float t) {
            v.addVertexWith2DPose(pose,px,py).setUv(u,t);
            long pointer = ((VertexDataAccessor)v).riserecode$vertexPointer() + 20;
            for(int i=0;i<data.length;i++){MemoryUtil.memPutFloat(pointer+i*4L,data[i]);}
        }
    }
}
