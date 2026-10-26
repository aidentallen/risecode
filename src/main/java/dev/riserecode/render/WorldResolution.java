package dev.riserecode.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.riserecode.Client;
import dev.riserecode.performance.PerformancePreferences.Upscaler;
import dev.riserecode.ui.render.UiMesh;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.system.MemoryStack;

/** Frame-owned world extents. Uses the same GPU API on vanilla/Sodium OpenGL, Iris and Vulkan. */
public final class WorldResolution {
    public record Extents(int worldWidth,int worldHeight,int outputWidth,int outputHeight) {
        public static Extents of(int width,int height,int percent) {
            int value=Math.clamp(percent,25,100);
            return new Extents(Math.max(1,(int)Math.ceil(width*value/100.0)),Math.max(1,(int)Math.ceil(height*value/100.0)),width,height);
        }
        public double pixelScale(){return Math.min(worldWidth/(double)outputWidth,worldHeight/(double)outputHeight);}
    }
    public record Status(int requested,Extents actual,Upscaler upscaler,String reason) {
        public String label(){return requested+"% requested · "+actual.worldWidth+"×"+actual.worldHeight+" → "+actual.outputWidth+"×"+actual.outputHeight+(requested<100?" · "+upscaler:"")+(reason.isEmpty()?"":" · "+reason);}
    }
    private static final List<String> COMPETING=List.of("resolutioncontrol","resolution-control","super_resolution","superresolution","fsr","dlss");
    private static TextureTarget world,easu;
    private static GpuBuffer parameters;
    private static boolean inWorld,checked,fsrSupported,scalingFailed;
    private static String fsrReason="";
    private static double sharpness;
    private static Status status=new Status(100,new Extents(1,1,1,1),Upscaler.FSR1,"");
    private static long passes,allocations;
    private static int lastWorldWidth,lastWorldHeight;
    private static final class Pipelines {
        static final RenderPipeline BILINEAR=pipeline("world_upscale"),EASU=pipeline("fsr_easu"),RCAS=pipeline("fsr_rcas");
        static RenderPipeline pipeline(String shader){
            return RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id("pipeline/"+shader))
                .withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/"+shader))
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").withUniform("ScaleParams",UniformType.UNIFORM_BUFFER).build())
                .withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withColorTargetState(ColorTargetState.DEFAULT).build());
        }
    }
    private WorldResolution(){}
    public static Status status(){return status;}
    public static String statusLabel(){
        if(net.minecraft.client.Minecraft.getInstance().level==null&&Client.INSTANCE.isInitialized())return Math.round(Client.INSTANCE.getPerformance().worldResolution.getValue())+"% requested · world not loaded";
        return status.label();
    }
    public static boolean active(){return inWorld;}
    public static double pixelScale(){return inWorld?status.actual.pixelScale():1;}
    public static long passes(){return passes;}
    public static long allocations(){return allocations;}
    public static long ownedGpuBytes(){return (world==null?0:world.width*(long)world.height*8)+(easu==null?0:easu.width*(long)easu.height*4);}
    public static RenderTarget begin(RenderTarget output){
        int requested=Client.INSTANCE.isInitialized()?(int)Math.round(Client.INSTANCE.getPerformance().worldResolution.getValue()):100;
        var preference=Client.INSTANCE.isInitialized()?Client.INSTANCE.getPerformance():null;
        Upscaler selected=preference==null?Upscaler.FSR1:preference.upscaler.getValue();
        String reason="";
        for(String id:COMPETING)if(FabricLoader.getInstance().isModLoaded(id)){reason="Scaling owned by "+id;break;}
        if(scalingFailed)reason="World target unavailable; using native resolution";
        var extent=Extents.of(output.width,output.height,reason.isEmpty()?requested:100);
        if(requested>=100||!reason.isEmpty()){
            releaseTargets();worldExtent(output.width,output.height);inWorld=false;status=new Status(requested,extent,selected,reason);return output;
        }
        try{
            if(!checked){
                if(!RenderSystem.getDevice().precompilePipeline(Pipelines.BILINEAR).isValid())throw new IllegalStateException("Bilinear pipeline unsupported");
                try{fsrSupported=RenderSystem.getDevice().precompilePipeline(Pipelines.EASU).isValid()&&RenderSystem.getDevice().precompilePipeline(Pipelines.RCAS).isValid();}
                catch(RuntimeException unsupported){fsrSupported=false;}
                fsrReason=fsrSupported?"":"FSR1 unavailable; using Bilinear";checked=true;
            }
            if(selected==Upscaler.FSR1&&!fsrSupported){selected=Upscaler.BILINEAR;reason=fsrReason;}
            ensure(extent,selected==Upscaler.FSR1);
            worldExtent(extent.worldWidth,extent.worldHeight);
            sharpness=preference.sharpness.getValue();
            status=new Status(requested,extent,selected,reason);inWorld=true;return world;
        }catch(RuntimeException failure){
            releaseTargets();scalingFailed=true;inWorld=false;
            status=new Status(requested,Extents.of(output.width,output.height,100),selected,"World scaling unavailable: "+failure.getClass().getSimpleName());
            Client.INSTANCE.getLogger().warn("World scaling unavailable; preserving requested preferences",failure);return output;
        }
    }
    private static void worldExtent(int width,int height){
        if(lastWorldWidth!=width||lastWorldHeight!=height){
            net.minecraft.client.Minecraft.getInstance().levelRenderer.resize(width,height);
            lastWorldWidth=width;lastWorldHeight=height;
        }
    }
    private static void ensure(Extents e,boolean fsr){
        if(world!=null&&(world.width!=e.worldWidth||world.height!=e.worldHeight)){releaseTargets();}
        if(world==null){world=new TextureTarget("risecode scaled world",e.worldWidth,e.worldHeight,true,GpuFormat.RGBA8_UNORM);allocations++;}
        if(easu!=null&&(!fsr||easu.width!=e.outputWidth||easu.height!=e.outputHeight)){easu.destroyBuffers();easu=null;}
        if(fsr&&easu==null){easu=new TextureTarget("risecode FSR1 EASU",e.outputWidth,e.outputHeight,false,GpuFormat.RGBA8_UNORM);allocations++;}
        if(parameters==null)parameters=RenderSystem.getDevice().createBuffer(()->"risecode world scale parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,32);
    }
    /** Called while mainRenderTarget still refers to this frame's world, before GUI blur. */
    public static void compositeOverlays(RenderTarget target){
        ItemOutlineRenderer.composite(target);SilhouetteOutlineRenderer.composite(target);
    }
    public static void finish(RenderTarget output){
        if(!inWorld)return;
        try{
            try(MemoryStack stack=MemoryStack.stackPush()){
                var e=status.actual;
                var bytes=Std140Builder.onStack(stack,32).putVec4(e.worldWidth,e.worldHeight,e.outputWidth,e.outputHeight).putVec4((float)sharpness,0,0,0).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(parameters.slice(),bytes);
            }
            if(status.upscaler==Upscaler.FSR1){
                try{draw(Pipelines.EASU,world,easu);draw(Pipelines.RCAS,easu,output);return;}
                catch(RuntimeException unsupported){fsrSupported=false;fsrReason="FSR1 pass failed; using Bilinear";status=new Status(status.requested,status.actual,Upscaler.BILINEAR,fsrReason);}
            }
            try{draw(Pipelines.BILINEAR,world,output);}
            catch(RuntimeException failure){
                scalingFailed=true;
                status=new Status(status.requested,status.actual,Upscaler.BILINEAR,"Scaling pass failed; next frame uses native resolution");
                Client.INSTANCE.getLogger().warn("World scaling pass failed; returning to native resolution",failure);
                world.blitAndBlendToTexture(output.getColorTextureView(),output.getDepthTextureView());
            }
        }finally{inWorld=false;}
    }
    private static void draw(RenderPipeline pipeline,RenderTarget input,RenderTarget output){
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"risecode world upscale",output.getColorTextureView(),Optional.empty())){
            pass.setPipeline(pipeline);pass.setUniform("ScaleParams",parameters);
            pass.bindTexture("Sampler0",input.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(3,1,0,0);passes++;
        }
    }
    public static void abort(){inWorld=false;}
    private static void releaseTargets(){if(world!=null){world.destroyBuffers();world=null;}if(easu!=null){easu.destroyBuffers();easu=null;}if(parameters!=null){parameters.close();parameters=null;}}
    public static void close(){inWorld=false;releaseTargets();checked=fsrSupported=scalingFailed=false;fsrReason="";}
}
