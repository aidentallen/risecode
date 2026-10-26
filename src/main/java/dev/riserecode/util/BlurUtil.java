package dev.riserecode.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.*;
import org.lwjgl.system.MemoryStack;

/** Captures only the completed scene, before GUI/HUD rendering. Never recursively blurs controls. */
public final class BlurUtil {
    private static final class Pipelines {
        static final RenderPipeline COPY=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(UiMesh.id("pipeline/scene_copy")).withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/blur"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0).withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams",UniformType.UNIFORM_BUFFER).build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
        static final RenderPipeline BOX=UiMesh.pipeline("blur_box","blur_box");
    }
    private static TextureTarget snapshot,gui,world;
    private static final GpuBuffer[] PARAMETERS=new GpuBuffer[2];
    private static final int[][] PARAMETER_BITS=new int[2][];
    private static boolean guiRequested,worldRequested;
    private static BlurRegion guiRegion;
    private static double guiRadius,worldRadius,worldOpacity,lastSubmitMilliseconds;
    private static int allocations;
    private static long frames;
    private BlurUtil(){}
    public static int getAllocationCount(){return allocations;}
    public static long getRenderedFrames(){return frames;}
    public static double getLastSubmitMilliseconds(){return lastSubmitMilliseconds;}
    public static int getTargetWidth(){return snapshot==null?0:snapshot.width;}
    public static int getTargetHeight(){return snapshot==null?0:snapshot.height;}
    private static void ensureTargets(){
        var mc=Minecraft.getInstance();int w=Math.max(1,(mc.getWindow().getScreenWidth()+1)/2),h=Math.max(1,(mc.getWindow().getScreenHeight()+1)/2);
        if(snapshot!=null&&snapshot.width==w&&snapshot.height==h){return;}destroyTargets();
        snapshot=new TextureTarget("risecode scene / half",w,h,false,GpuFormat.RGBA8_UNORM);
        gui=new TextureTarget("risecode GUI blur",w,h,false,GpuFormat.RGBA8_UNORM);
        world=new TextureTarget("risecode world blur",w,h,false,GpuFormat.RGBA8_UNORM);allocations++;
    }
    public static void requestWorld(double opacity){
        if(!dev.riserecode.performance.PerformancePolicy.frame().blur()&&opacity>0&&Theme.bool("World Blur",false)&&Theme.number("World Blur Radius",8)>0){ensureTargets();worldRequested=true;worldRadius=Theme.number("World Blur Radius",8);worldOpacity=MathUtil.clamp01(opacity);}
    }
    public static void drawGui(GuiGraphicsExtractor g,double x,double y,double w,double h,double corner,double opening){requestWorld(opening);drawBox(g,x,y,w,h,corner,opening*Theme.number("GUI Blur Opacity",.8));}
    public static void drawBox(GuiGraphicsExtractor g,double x,double y,double w,double h,double corner,double opacity){
        if(dev.riserecode.performance.PerformancePolicy.frame().blur()){return;}
        double radius=Theme.number("GUI Blur Radius",12);if(radius<=0||opacity<=0||w<=0||h<=0){return;}
        ensureTargets();guiRequested=true;guiRadius=radius;int color=Theme.opacity(0xFFFFFFFF,opacity);
        var bounds=new net.minecraft.client.gui.navigation.ScreenRectangle((int)Math.floor(x-1),(int)Math.floor(y-1),(int)Math.ceil(w+3),(int)Math.ceil(h+3)).transformMaxBounds(g.pose());
        var clip=g.scissorStack.peek();if(clip!=null){bounds=bounds.intersection(clip);}
        if(bounds!=null){
            var mc=Minecraft.getInstance();double pixel=mc.getWindow().getGuiScale(),sw=mc.getWindow().getScreenWidth(),sh=mc.getWindow().getScreenHeight();
            var region=new BlurRegion(Math.max(0,(bounds.left()*pixel-2)/sw),Math.max(0,1-(bounds.bottom()*pixel+2)/sh),Math.min(1,(bounds.right()*pixel+2)/sw),Math.min(1,1-(bounds.top()*pixel-2)/sh));
            guiRegion=guiRegion==null?region:guiRegion.union(region);
        }
        UiMesh.quad(g,Pipelines.BOX,TextureSetup.singleTexture(gui.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),x-1,y-1,w+2,h+2,(float)(-w/2-1),(float)(-h/2-1),(float)(w/2+1),(float)(h/2+1),color,color,(int)Math.round(w*16),(int)Math.round(h*16),(int)Math.round(Math.min(Theme.radius(corner),Math.min(w,h)/2)*256),0);
    }
    public static void renderBeforeGui(RenderTarget main){
        if((!guiRequested&&!worldRequested)||snapshot==null){return;}long start=System.nanoTime();
        try{
            // Dual filtering samples a pyramid: capture the full source so its support never
            // reaches stale pixels outside a window's previous region.
            copy(main,snapshot,0,1,0);
            if(guiRequested){DualBlur.apply(snapshot,gui,guiRadius,worldRequested?null:guiRegion);}
            if(worldRequested){
                if(guiRequested&&guiRadius==worldRadius){copy(gui,main,2,worldOpacity,1);}
                else{DualBlur.apply(snapshot,world,worldRadius);copy(world,main,2,worldOpacity,1);}
            }
            frames++;
        }finally{guiRequested=worldRequested=false;guiRegion=null;lastSubmitMilliseconds=(System.nanoTime()-start)/1e6;}
    }
    private static void copy(RenderTarget source,RenderTarget target,int mode,double opacity,int slot){
        if(PARAMETERS[slot]==null){PARAMETERS[slot]=RenderSystem.getDevice().createBuffer(()->"risecode scene copy parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,16);}
        float alpha=(float)opacity;int bits=Float.floatToIntBits(alpha);int[] previous=PARAMETER_BITS[slot];
        if(previous==null||previous[0]!=mode||previous[1]!=bits){
            try(MemoryStack stack=MemoryStack.stackPush()){
                var data=Std140Builder.onStack(stack,16).putVec4(0,0,mode,alpha).get();RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMETERS[slot].slice(),data);
            }
            PARAMETER_BITS[slot]=new int[]{mode,bits};
        }
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"risecode scene copy",target.getColorTextureView(),Optional.empty())){
            pass.setPipeline(Pipelines.COPY);pass.setUniform("BlurParams",PARAMETERS[slot]);pass.bindTexture("Sampler0",source.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(3,1,0,0);
        }
    }
    private static void destroyTargets(){if(snapshot!=null){snapshot.destroyBuffers();gui.destroyBuffers();world.destroyBuffers();snapshot=gui=world=null;}}
    public static void close(){destroyTargets();DualBlur.close();for(int i=0;i<PARAMETERS.length;i++){if(PARAMETERS[i]!=null){PARAMETERS[i].close();PARAMETERS[i]=null;}PARAMETER_BITS[i]=null;}guiRequested=worldRequested=false;guiRegion=null;}
}
