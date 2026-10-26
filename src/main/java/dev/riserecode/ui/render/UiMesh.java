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
    public static final VertexFormat BITMAP_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("UV0",GpuFormat.RG32_FLOAT)
            .addAttribute("Color",GpuFormat.RGBA8_UNORM).addAttribute("UV1",GpuFormat.RG16_SINT)
            .addAttribute("UV2",GpuFormat.RG16_SINT).addAttribute("Clip",GpuFormat.RGBA32_FLOAT).build();
    private static int effectDepth;
    public static boolean effectActive(){return effectDepth>0;}
    public static void effect(Runnable draw) { effectDepth++;try { draw.run(); } finally { effectDepth--; } }
    private UiMesh() { }
    public static RenderPipeline pipeline(String name, String fragment) {
        return RenderPipelines.register(RenderPipeline.builder(fragment.equals("shape") || fragment.equals("window") || fragment.equals("hsv") ? RenderPipelines.GUI_SNIPPET : RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name)).withVertexShader(id("core/ui"))
                .withFragmentShader(id("core/" + fragment)).withVertexBinding(0, FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("riserecode", path); }
    /** Geometry belongs to the immutable font layout. Tints are written once before submission. */
    public static void bitmapRun(GuiGraphicsExtractor g,RenderPipeline pipeline,TextureSetup texture,float[] geometry,int[] colors,int first,int end,double x,double y,int a,int b,int inset,int trailingInset){
        if(first==end)return;
        Matrix3x2f pose=new Matrix3x2f(g.pose());ScreenRectangle clip=g.scissorStack.peek(),scissor=clip;
        boolean effect=effectDepth>0;
        if(clip!=null&&effect){int pad=(int)Math.ceil(dev.riserecode.ui.theme.Theme.number("Glow Radius",5)*Math.hypot(pose.m00(),pose.m01())+4);scissor=expandGlowScissor(clip,pad,g.guiWidth(),g.guiHeight());if(scissor==null)return;}
        int left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=Integer.MIN_VALUE,bottom=Integer.MIN_VALUE;
        for(int glyph=first;glyph<end;glyph++){
            int i=glyph*8;if(geometry[i+2]<=0)continue;
            if(effect&&clip!=null&&bitmapOutside(pose,geometry,i,x,y,clip))continue;
            int gx=(int)Math.floor(x+geometry[i]+inset),gy=(int)Math.floor(y+geometry[i+1]+inset);
            left=Math.min(left,gx);top=Math.min(top,gy);right=Math.max(right,gx+(int)Math.ceil(geometry[i+2]-inset-trailingInset+1));bottom=Math.max(bottom,gy+(int)Math.ceil(geometry[i+3]-inset-trailingInset+1));
        }
        if(left==Integer.MAX_VALUE)return;
        ScreenRectangle bounds=new ScreenRectangle(left,top,right-left,bottom-top).transformMaxBounds(pose);
        if(scissor!=null)bounds=bounds.intersection(scissor);if(bounds==null)return;
        var window=net.minecraft.client.Minecraft.getInstance().getWindow();double pixel=window.getGuiScale();
        float cl=0,ct=0,cr=1,cb=1;
        if(scissor!=null){cl=(float)(scissor.left()*pixel/window.getScreenWidth());ct=(float)(scissor.top()*pixel/window.getScreenHeight());cr=(float)(scissor.right()*pixel/window.getScreenWidth());cb=(float)(scissor.bottom()*pixel/window.getScreenHeight());}
        UiBatch.submit(g,new BitmapGlyphs(pipeline,texture,pose,geometry,colors,first,end,x,y,a,b,(int)Math.round(dev.riserecode.ui.font.CustomTextRenderer.stroke()*256),effect?clip:null,bounds,cl,ct,cr,cb));
    }
    private static boolean bitmapOutside(Matrix3x2f p,float[] data,int i,double x,double y,ScreenRectangle clip){
        float x0=(float)Math.floor(x+data[i]),y0=(float)Math.floor(y+data[i+1]),x1=x0+(float)Math.ceil(data[i+2]+1),y1=y0+(float)Math.ceil(data[i+3]+1);
        float ax=p.m00()*x0+p.m10()*y0+p.m20(),bx=p.m00()*x1+p.m10()*y0+p.m20(),cx=p.m00()*x0+p.m10()*y1+p.m20(),dx=p.m00()*x1+p.m10()*y1+p.m20();
        float ay=p.m01()*x0+p.m11()*y0+p.m21(),by=p.m01()*x1+p.m11()*y0+p.m21(),cy=p.m01()*x0+p.m11()*y1+p.m21(),dy=p.m01()*x1+p.m11()*y1+p.m21();
        float minX=Math.min(Math.min(ax,cx),Math.min(bx,dx)),maxX=Math.max(Math.max(ax,cx),Math.max(bx,dx)),minY=Math.min(Math.min(ay,cy),Math.min(by,dy)),maxY=Math.max(Math.max(ay,cy),Math.max(by,dy));
        int left=(int)Math.floor(minX),top=(int)Math.floor(minY);
        return left+(int)Math.ceil(maxX-minX)<=clip.left()||left>=clip.right()||top+(int)Math.ceil(maxY-minY)<=clip.top()||top>=clip.bottom();
    }
    private record BitmapGlyphs(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,float[] data,int[] colors,int first,int end,double x,double y,int a,int b,int stroke,ScreenRectangle effectClip,ScreenRectangle bounds,float clipLeft,float clipTop,float clipRight,float clipBottom) implements dev.riserecode.ui.menu.MenuTransition.Captured {
        // Per-run clipping travels with the vertices, so exact body/glow clips can coexist in one draw.
        public ScreenRectangle scissorArea(){return null;}
        public void buildVertices(VertexConsumer v){
            for(int glyph=first;glyph<end;glyph++){
                int i=glyph*8;if(data[i+2]<=0)continue;if(effectClip!=null&&bitmapOutside(pose,data,i,x,y,effectClip))continue;
                float gx=(float)(x+data[i]),gy=(float)(y+data[i+1]),w=data[i+2],h=data[i+3],u0=data[i+4],v0=data[i+5],u1=data[i+6],v1=data[i+7];int color=colors[glyph];
                // Keep the original quad and UV interpolation, including nearest-sampling
                // boundary behaviour at fractional scales. Only ordering bounds are tight.
                vertex(v,gx,gy,u0,v0,color);vertex(v,gx,gy+h,u0,v1,color);vertex(v,gx+w,gy+h,u1,v1,color);vertex(v,gx+w,gy,u1,v0,color);
            }
        }
        private void vertex(VertexConsumer v,float x,float y,float u,float t,int color){
            v.addVertexWith2DPose(pose,x,y).setUv(u,t).setColor(color).setUv1(a,b).setUv2(stroke,0);
            long pointer=((dev.riserecode.mixin.VertexDataAccessor)v).riserecode$vertexPointer()+32;
            org.lwjgl.system.MemoryUtil.memPutFloat(pointer,clipLeft);org.lwjgl.system.MemoryUtil.memPutFloat(pointer+4,clipTop);
            org.lwjgl.system.MemoryUtil.memPutFloat(pointer+8,clipRight);org.lwjgl.system.MemoryUtil.memPutFloat(pointer+12,clipBottom);
        }
    }
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
        if (scissor != null && effectDepth > 0) {
            if (bounds.intersection(scissor) == null) { return; }
            // A Gaussian reaches its near-transparent tail before this expanded edge.
            int pad=(int)Math.ceil(dev.riserecode.ui.theme.Theme.number("Glow Radius",5)*Math.hypot(pose.m00(),pose.m01())+4);
            scissor = expandGlowScissor(scissor, pad, graphics.guiWidth(), graphics.guiHeight());
            if (scissor == null) { return; }
        }
        if (scissor != null) { bounds = bounds.intersection(scissor); }
        if (bounds == null) { return; }
        UiBatch.submit(graphics, new Quad(pipeline, texture, pose, (float) x, (float) y,
                (float) width, (float) height, u0, v0, u1, v1, firstColor, secondColor, a, b, c, d, horizontal, scissor, bounds));
    }
    /** One immutable extraction state per contiguous atlas run, rather than per glyph. */
    public static final class GlyphRun {
        private final GuiGraphicsExtractor graphics;
        private final RenderPipeline pipeline;
        private final TextureSetup texture;
        private final Matrix3x2f pose;
        private final ScreenRectangle clip;
        private final int a,b,stroke;
        private final float[] coordinates;
        private final int[] colors;
        private int count;
        private double left=Double.POSITIVE_INFINITY,top=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY,bottom=Double.NEGATIVE_INFINITY;
        public GlyphRun(GuiGraphicsExtractor graphics,RenderPipeline pipeline,TextureSetup texture,int capacity,int a,int b){
            this.graphics=graphics;this.pipeline=pipeline;this.texture=texture;this.a=a;this.b=b;this.stroke=(int)Math.round(dev.riserecode.ui.font.CustomTextRenderer.stroke()*256);
            pose=new Matrix3x2f(graphics.pose());clip=graphics.scissorStack.peek();
            coordinates=new float[capacity*8];colors=new int[capacity];
        }
        public void add(double x,double y,double width,double height,float u0,float v0,float u1,float v1,int color){
            if(width<=0||height<=0){return;}
            if(clip!=null&&effectDepth>0&&outsideClip(x,y,width,height)){return;}
            int i=count*8;coordinates[i]=(float)x;coordinates[i+1]=(float)y;coordinates[i+2]=(float)width;coordinates[i+3]=(float)height;
            coordinates[i+4]=u0;coordinates[i+5]=v0;coordinates[i+6]=u1;coordinates[i+7]=v1;colors[count++]=color;
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+width);bottom=Math.max(bottom,y+height);
        }
        private boolean outsideClip(double x,double y,double width,double height){
            // Match the per-quad visibility test without allocating glyph bounds or pose copies.
            double x0=Math.floor(x),y0=Math.floor(y),x1=x0+Math.ceil(width+1),y1=y0+Math.ceil(height+1);
            double minX=pose.m20()+pose.m00()*(pose.m00()<0?x1:x0)+pose.m10()*(pose.m10()<0?y1:y0);
            double maxX=pose.m20()+pose.m00()*(pose.m00()<0?x0:x1)+pose.m10()*(pose.m10()<0?y0:y1);
            double minY=pose.m21()+pose.m01()*(pose.m01()<0?x1:x0)+pose.m11()*(pose.m11()<0?y1:y0);
            double maxY=pose.m21()+pose.m01()*(pose.m01()<0?x0:x1)+pose.m11()*(pose.m11()<0?y0:y1);
            return Math.ceil(maxX)<=clip.left()||Math.floor(minX)>=clip.right()||Math.ceil(maxY)<=clip.top()||Math.floor(minY)>=clip.bottom();
        }
        public void flush(){
            if(count==0){return;}
            ScreenRectangle bounds=new ScreenRectangle((int)Math.floor(left),(int)Math.floor(top),(int)Math.ceil(right-left+1),(int)Math.ceil(bottom-top+1)).transformMaxBounds(pose);
            ScreenRectangle scissor=clip;
            if(scissor!=null && effectDepth>0){
                if(bounds.intersection(scissor)==null){reset();return;}
                int pad=(int)Math.ceil(dev.riserecode.ui.theme.Theme.number("Glow Radius",5)*Math.hypot(pose.m00(),pose.m01())+4);
                scissor=expandGlowScissor(scissor,pad,graphics.guiWidth(),graphics.guiHeight());
                if(scissor==null){reset();return;}
            }
            if(scissor!=null){bounds=bounds.intersection(scissor);}
            if(bounds!=null){UiBatch.submit(graphics,new Glyphs(pipeline,texture,pose,java.util.Arrays.copyOf(coordinates,count*8),java.util.Arrays.copyOf(colors,count),a,b,stroke,scissor,bounds));}
            reset();
        }
        private void reset(){count=0;left=top=Double.POSITIVE_INFINITY;right=bottom=Double.NEGATIVE_INFINITY;}
    }
    private record Glyphs(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,float[] coordinates,int[] colors,int a,int b,int stroke,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer vertices){
            for(int glyph=0;glyph<colors.length;glyph++){
                int i=glyph*8;float x=coordinates[i],y=coordinates[i+1],w=coordinates[i+2],h=coordinates[i+3],u0=coordinates[i+4],v0=coordinates[i+5],u1=coordinates[i+6],v1=coordinates[i+7];
                vertex(vertices,x,y,u0,v0,colors[glyph]);vertex(vertices,x,y+h,u0,v1,colors[glyph]);vertex(vertices,x+w,y+h,u1,v1,colors[glyph]);vertex(vertices,x+w,y,u1,v0,colors[glyph]);
            }
        }
        private void vertex(VertexConsumer vertices,float x,float y,float u,float v,int color){vertices.addVertexWith2DPose(pose,x,y).setUv(u,v).setColor(color).setUv1(a,b).setUv2(stroke,0);}
    }
    static ScreenRectangle expandGlowScissor(ScreenRectangle scissor, int pad, int width, int height) {
        // Expand in screen coordinates, then keep the soft tail inside the render area.
        // A null intersection must discard the draw, never turn into an unclipped scissor.
        return new ScreenRectangle(scissor.left() - pad, scissor.top() - pad,
                scissor.width() + 2 * pad, scissor.height() + 2 * pad)
                .intersection(new ScreenRectangle(0, 0, width, height));
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
