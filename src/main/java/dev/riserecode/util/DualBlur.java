package dev.riserecode.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.riserecode.ui.render.UiMesh;
import net.minecraft.client.renderer.*;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;

/** Backend-neutral dual filtering. Reuses a bounded pyramid, with no framebuffer readback. */
public final class DualBlur {
    private static final TextureTarget[] LEVELS=new TextureTarget[6];
    private static final GpuBuffer[] PARAMS=new GpuBuffer[64];
    private static final int[][] PARAMETER_BITS=new int[64][];
    private static final class Pipeline {
        static final RenderPipeline FILTER=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(UiMesh.id("pipeline/dual_blur")).withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/dual_blur"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0).withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams",UniformType.UNIFORM_BUFFER).build())
            .withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    }
    private static long parameterFrame=-1;
    private static int parameterCursor;
    private DualBlur(){}
    public static void apply(RenderTarget source,RenderTarget target,double radius){
        apply(source,target,radius,null);
    }
    public static void apply(RenderTarget source,RenderTarget target,double radius,BlurRegion region){
        if(region!=null&&region.empty()){return;}
        long frame=dev.riserecode.ui.menu.MenuTheme.frameSerial();
        if(parameterFrame!=frame){parameterFrame=frame;parameterCursor=0;}
        int count=Math.clamp((int)Math.ceil(Math.log(Math.max(1,radius/2))/Math.log(2)),1,LEVELS.length);
        int w=source.width,h=source.height;
        for(int i=0;i<count;i++){
            w=Math.max(1,(w+1)/2);h=Math.max(1,(h+1)/2);
            if(LEVELS[i]==null||LEVELS[i].width!=w||LEVELS[i].height!=h){if(LEVELS[i]!=null){LEVELS[i].destroyBuffers();}LEVELS[i]=new TextureTarget("risecode dual blur "+i,w,h,false,GpuFormat.RGBA8_UNORM);}
        }
        double offset=Math.clamp(radius/Math.pow(2,count+1),.25,1.5);
        RenderTarget[] sources=new RenderTarget[count*2],targets=new RenderTarget[count*2];boolean[] up=new boolean[count*2];int step=0;
        RenderTarget current=source;
        for(int i=0;i<count;i++){sources[step]=current;targets[step++]=LEVELS[i];current=LEVELS[i];}
        for(int i=count-2;i>=0;i--){sources[step]=current;targets[step]=LEVELS[i];up[step++]=true;current=LEVELS[i];}
        sources[step]=current;targets[step]=target;up[step]=true;
        BlurRegion[] regions=new BlurRegion[count*2];
        if(region!=null){
            // Work backwards through EVERY pass. Each source footprint includes the filter
            // offsets, bilinear neighbours and outward pixel rounding at the next level.
            // Thus moving windows never sample last frame's unwritten pyramid pixels.
            BlurRegion needed=region;
            for(int i=regions.length-1;i>=0;i--){
                regions[i]=needed.pixels(targets[i].width,targets[i].height);
                double support=(up[i]?offset:offset*.5)+1.5;
                needed=regions[i].expand(support/sources[i].width,support/sources[i].height);
            }
        }
        for(int i=0;i<regions.length;i++){pass(sources[i],targets[i],up[i],offset,parameterCursor++,regions[i]);}
    }
    private static void pass(RenderTarget source,RenderTarget target,boolean up,double offset,int slot,BlurRegion region){
        if(PARAMS[slot]==null){PARAMS[slot]=RenderSystem.getDevice().createBuffer(()->"risecode dual blur parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,16);}
        float texelX=(float)(.5/source.width),texelY=(float)(.5/source.height),mode=up?1:0,amount=(float)offset;
        int xb=Float.floatToIntBits(texelX),yb=Float.floatToIntBits(texelY),mb=Float.floatToIntBits(mode),ab=Float.floatToIntBits(amount);
        int[] previous=PARAMETER_BITS[slot];
        // Stable passes reuse immutable uniform contents, avoiding a driver write/fence every frame.
        if(previous==null||previous[0]!=xb||previous[1]!=yb||previous[2]!=mb||previous[3]!=ab){
            try(MemoryStack stack=MemoryStack.stackPush()){
                var data=Std140Builder.onStack(stack,16).putVec4(texelX,texelY,mode,amount).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMS[slot].slice(),data);
            }
            PARAMETER_BITS[slot]=new int[]{xb,yb,mb,ab};
        }
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"risecode dual filter",target.getColorTextureView(),Optional.empty())){
            pass.setPipeline(Pipeline.FILTER);pass.setUniform("BlurParams",PARAMS[slot]);
            if(region!=null){int x=(int)Math.round(region.left()*target.width),y=(int)Math.round(region.bottom()*target.height);int right=(int)Math.round(region.right()*target.width),top=(int)Math.round(region.top()*target.height);pass.enableScissor(x,y,right-x,top-y);}
            pass.bindTexture("Sampler0",source.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(3,1,0,0);
        }
    }
    public static void close(){parameterFrame=-1;parameterCursor=0;for(int i=0;i<LEVELS.length;i++){if(LEVELS[i]!=null){LEVELS[i].destroyBuffers();LEVELS[i]=null;}}for(int i=0;i<PARAMS.length;i++){if(PARAMS[i]!=null){PARAMS[i].close();PARAMS[i]=null;}PARAMETER_BITS[i]=null;}}
}
