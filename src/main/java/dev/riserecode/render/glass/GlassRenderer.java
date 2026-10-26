package dev.riserecode.render.glass;

import com.mojang.blaze3d.*;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.riserecode.render.TextureSource;
import dev.riserecode.render.glass.GlassCapture.Surface;
import dev.riserecode.render.glass.GlassPolicy.View;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** Scene copies occur at execution, never extraction. The native projection/pose and sorted
 * geometry are retained. Three independent sessions prevent preview/hand/world feedback. */
public final class GlassRenderer {
    private static final EnumMap<View,Session> SESSIONS=new EnumMap<>(View.class);
    private static final Map<VertexFormat,RenderPipeline> PIPELINES=new HashMap<>();
    private static final Map<Key,RenderType> TYPES=new HashMap<>();
    private record Key(RenderType original,Surface surface) { }
    private record Deferred(PreparedRenderType type,GpuBuffer vertices,GpuBuffer indices,IndexType indexType,int base,int first,int count,GpuBufferSlice projection) { }
    private static final List<Deferred> DEFERRED=new ArrayList<>();
    private static boolean flushing;
    private static boolean failed;
    private static String status="Ready · Glass is opt-in";
    private static final EnumMap<View,Long> VIEW_DRAWS=new EnumMap<>(View.class);
    private static final EnumMap<GlassPolicy.Component,Long> COMPONENT_DRAWS=new EnumMap<>(GlassPolicy.Component.class);
    public static long draws(View view){return VIEW_DRAWS.getOrDefault(view,0L);}
    public static long draws(GlassPolicy.Component component){return COMPONENT_DRAWS.getOrDefault(component,0L);}
    private static long draws, copies, composites, allocations;
    private static GpuBuffer parameters;
    private static final int PARAM_BYTES=160;
    private GlassRenderer() { }
    public static String status(){return status;}
    public static long draws(){return draws;}
    public static long copies(){return copies;}
    public static long composites(){return composites;}
    public static long allocations(){return allocations;}
    public static RenderTarget coverageTarget(View view){var s=SESSIONS.get(view);return s==null?null:s.coverage;}
    public static long ownedGpuBytes(){long n=parameters==null?0:PARAM_BYTES;for(var s:SESSIONS.values())n+=s.bytes();return n;}
    public static void retry(){failed=false;status="Ready";}
    public static void beginFrame(){DEFERRED.clear();TYPES.clear();SESSIONS.values().forEach(s->{s.drawn=false;s.bloom=false;});}
    private static final class Pipelines {
        static final RenderPipeline BLUR=post("glass_bloom",false),ADD=post("glass_bloom_add",true);
        static RenderPipeline post(String shader,boolean add){return RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id("pipeline/"+shader)).withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/"+shader))
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").withUniform("GlassParams",UniformType.UNIFORM_BUFFER).build())
            .withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(add?new ColorTargetState(BlendFunction.ADDITIVE):ColorTargetState.DEFAULT).build());}
    }
    private static RenderPipeline pipeline(VertexFormat format){
        return PIPELINES.computeIfAbsent(format,f->RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(UiMesh.id("pipeline/glass_surface_"+PIPELINES.size())).withVertexShader(UiMesh.id("core/glass"))
            .withFragmentShader(UiMesh.id("core/glass"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("GlassScene").withSampler("GlassDepth").withSampler("GlassCoverage").withUniform("GlassParams",UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0,f).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)))
            .withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build()));
    }
    public static final class Target extends OutputTarget {
        final RenderType original; final Surface surface;
        Target(RenderType original,Surface surface){super("risecode glass "+surface.component(),()->Minecraft.getInstance().gameRenderer.mainRenderTarget());this.original=original;this.surface=surface;}
    }
    public static RenderType type(RenderType original,Surface surface){
        if(surface!=null&&!GlassIrisAdapter.supported()){status="Native rendering · unsupported Iris version";return original;}
        if(surface==null||failed||surface.owner().view()!=View.PREVIEW&&dev.riserecode.render.ShaderWorldOverlay.shadowPass())return original;
        if(original.toString().contains("glint")||original.isOutline())return original;
        var atlas=((TextureSource)original).riserecode$texture();if(atlas==null)return original;
        try{
            var pipeline=pipeline(original.format());
            if(!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()||!RenderSystem.getDevice().precompilePipeline(Pipelines.BLUR).isValid()||!RenderSystem.getDevice().precompilePipeline(Pipelines.ADD).isValid())throw new IllegalStateException("Glass pipeline unavailable");
            // Reserve all targets before replacing a native surface. Resize and allocation failures
            // leave the native type untouched. Preview reserves its own dimensions in its renderer.
            session(surface.owner().view()).ensure(sourceWidth(),sourceHeight());
            return TYPES.computeIfAbsent(new Key(original,surface),k->RenderType.create("risecode glass",RenderSetup.builder(pipeline).withTexture("Sampler0",atlas).sortOnUpload().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).setOutputTarget(new Target(original,surface)).createRenderSetup()));
        }catch(RuntimeException failure){fail(failure);return original;}
    }
    private static int sourceWidth(){return RenderSystem.outputColorTextureOverride==null?Minecraft.getInstance().gameRenderer.mainRenderTarget().width:RenderSystem.outputColorTextureOverride.getWidth(0);}
    private static int sourceHeight(){return RenderSystem.outputColorTextureOverride==null?Minecraft.getInstance().gameRenderer.mainRenderTarget().height:RenderSystem.outputColorTextureOverride.getHeight(0);}
    private static Session session(View view){return SESSIONS.computeIfAbsent(view,ignored->new Session());}
    public static void previewFailure(RuntimeException failure){fail(failure);}
    private static void fail(RuntimeException failure){failed=true;status="Native rendering restored · "+failure.getClass().getSimpleName();org.slf4j.LoggerFactory.getLogger("riserecode").warn("Glass unavailable; restoring native surfaces",failure);}
    public static void draw(PreparedRenderType prepared,GpuBuffer vertices,GpuBuffer indices,IndexType indexType,int base,int first,int count){
        var target=(Target)prepared.outputTarget();
        if(!flushing&&target.surface.owner().view()!=View.PREVIEW&&GlassIrisAdapter.active()){
            DEFERRED.add(new Deferred(prepared,vertices,indices,indexType,base,first,count,RenderSystem.getProjectionMatrixBuffer()));return;
        }
        if(failed){fallback(prepared,target,vertices,indices,indexType,base,first,count);return;}
        try{
            var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
            var color=RenderSystem.outputColorTextureOverride==null?main.getColorTextureView():RenderSystem.outputColorTextureOverride;
            var depth=RenderSystem.outputDepthTextureOverride==null?main.getDepthTextureView():RenderSystem.outputDepthTextureOverride;
            var s=session(target.surface.owner().view());s.ensure(color.getWidth(0),color.getHeight(0));
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(color.texture(),s.scene.getColorTexture(),0,0,0,0,0,s.width,s.height);
            if(depth!=null)encoder.copyTextureToTexture(depth.texture(),s.scene.getDepthTexture(),0,0,0,0,0,s.width,s.height);
            else encoder.clearDepthTexture(s.scene.getDepthTexture(),0);
            copies++;
            if(!s.drawn){encoder.clearColorTexture(s.glow.getColorTexture(),new Vector4f());s.drawn=true;}
            encoder.clearColorTexture(s.coverage.getColorTexture(),new Vector4f());
            write(target.surface.material(),s.width,s.height,2);
            geometry(prepared,vertices,indices,indexType,base,first,count,s.coverage.getColorTextureView(),depth,s,true);
            write(target.surface.material(),s.width,s.height,0);
            geometry(prepared,vertices,indices,indexType,base,first,count,color,depth,s,false);
            if(target.surface.material().glow()>0&&target.surface.material().spread()>0){
                write(target.surface.material(),s.width,s.height,1);
                geometry(prepared,vertices,indices,indexType,base,first,count,s.glow.getColorTextureView(),depth,s,false);
                s.bloom=true;
            }
            draws++;VIEW_DRAWS.merge(target.surface.owner().view(),1L,Long::sum);COMPONENT_DRAWS.merge(target.surface.component(),1L,Long::sum);status="Ready";
        }catch(RuntimeException failure){fail(failure);fallback(prepared,target,vertices,indices,indexType,base,first,count);}
    }
    private static void geometry(PreparedRenderType prepared,GpuBuffer vertices,GpuBuffer indices,IndexType type,int base,int first,int count,com.mojang.blaze3d.textures.GpuTextureView color,com.mojang.blaze3d.textures.GpuTextureView depth,Session s,boolean coverage){
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"risecode glass surface",color,Optional.empty(),depth,OptionalDouble.empty())){
            pass.setPipeline(prepared.pipeline());RenderSystem.bindDefaultUniforms(pass);pass.setUniform("DynamicTransforms",prepared.dynamicTransforms());pass.setUniform("GlassParams",parameters);
            var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            for(var texture:prepared.textures())pass.bindTexture(texture.name(),texture.textureView(),texture.sampler());
            pass.bindTexture("GlassScene",s.scene.getColorTextureView(),sampler);pass.bindTexture("GlassCoverage",coverage?s.scene.getColorTextureView():s.coverage.getColorTextureView(),sampler);pass.bindTexture("GlassDepth",s.scene.getDepthTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            var scissor=prepared.scissorState();if(scissor.enabled())pass.enableScissor(scissor.x(),scissor.y(),scissor.width(),scissor.height());
            pass.setVertexBuffer(0,vertices.slice());pass.setIndexBuffer(indices,type);pass.drawIndexed(count,1,first,base,0);
        }
    }
    private static void fallback(PreparedRenderType p,Target t,GpuBuffer v,GpuBuffer i,IndexType type,int base,int first,int count){
        var nativeType=t.original.prepare();new PreparedRenderType(nativeType.pipeline(),nativeType.outputTarget(),p.dynamicTransforms(),p.scissorState(),nativeType.textures()).drawFromBuffer(v,i,type,base,first,count);
    }
    private static void write(GlassMaterial m,int width,int height,int mode){
        if(parameters==null)parameters=RenderSystem.getDevice().createBuffer(()->"risecode glass parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,PARAM_BYTES);
        try(var stack=MemoryStack.stackPush()){
            var b=Std140Builder.onStack(stack,PARAM_BYTES);
            paint(b,m.color());paint(b,m.edges());
            b.putVec4((float)m.opacity(),(float)m.frosted(),(float)m.refraction(),(float)m.highlights())
             .putVec4((float)m.edgeStrength(),(float)m.edgeThickness(),(float)m.glow(),(float)m.spread())
             .putVec4(m.shimmer()?(float)m.shimmerStrength():0,(float)m.shimmerSpeed(),(float)m.originalTexture(),(float)Theme.gradientTime())
             .putVec4(1f/width,1f/height,mode,(float)dev.riserecode.render.WorldResolution.pixelScale());
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(parameters.slice(),b.get());
        }
    }
    private static void paint(Std140Builder b,dev.riserecode.setting.ColorSetting.Paint p){
        b.putVec4(channel(p.primary(),16),channel(p.primary(),8),channel(p.primary(),0),channel(p.primary(),24));
        b.putVec4(channel(p.secondary(),16),channel(p.secondary(),8),channel(p.secondary(),0),channel(p.secondary(),24));
        b.putVec4(p.mode().ordinal(),p.direction().ordinal(),(float)p.speed(),(float)p.spacing());
    }
    private static float channel(int color,int shift){return ((color>>>shift)&255)/255f;}
    public static void flush(View view){
        var projection=RenderSystem.getProjectionMatrixBuffer();var projectionType=RenderSystem.getProjectionType();
        flushing=true;
        try{
            var iterator=DEFERRED.iterator();
            while(iterator.hasNext()){
                var d=iterator.next();
                if(((Target)d.type.outputTarget()).surface.owner().view()!=view)continue;
                if(d.projection!=null)RenderSystem.setProjectionMatrix(d.projection,projectionType);
                draw(d.type,d.vertices,d.indices,d.indexType,d.base,d.first,d.count);iterator.remove();
            }
            composite(view);
        }finally{flushing=false;if(projection!=null)RenderSystem.setProjectionMatrix(projection,projectionType);}
    }
    public static void composite(View view){
        var s=SESSIONS.get(view);if(s==null||!s.drawn)return;s.drawn=false;
        if(!s.bloom){return;}s.bloom=false;
        var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var destination=RenderSystem.outputColorTextureOverride==null?main.getColorTextureView():RenderSystem.outputColorTextureOverride;
        // The bloom shader reads per-pixel spread from emission alpha, retaining different radii.
        for(int axis=0;axis<2;axis++){
            if(parameters==null)return;
            try(var stack=MemoryStack.stackPush()){
                var b=Std140Builder.onStack(stack,PARAM_BYTES);for(int j=0;j<9;j++)b.putVec4(0,0,0,0);b.putVec4(1f/s.width,1f/s.height,axis,1);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(parameters.slice(),b.get());
            }
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"risecode glass bloom",axis==0?s.blur.getColorTextureView():destination,Optional.empty())){
                pass.setPipeline(axis==0?Pipelines.BLUR:Pipelines.ADD);pass.setUniform("GlassParams",parameters);
                pass.bindTexture("Sampler0",axis==0?s.glow.getColorTextureView():s.blur.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(3,1,0,0);
            }
        }
        composites++;
    }
    private static final class Session {
        TextureTarget scene,glow,blur,coverage;int width,height;boolean drawn,bloom;
        void ensure(int w,int h){if(scene!=null&&width==w&&height==h)return;close();try{scene=new TextureTarget("glass scene",w,h,true,GpuFormat.RGBA8_UNORM);glow=new TextureTarget("glass emission",w,h,false,GpuFormat.RGBA8_UNORM);blur=new TextureTarget("glass bloom",w,h,false,GpuFormat.RGBA8_UNORM);coverage=new TextureTarget("glass surface normals and coverage",w,h,false,GpuFormat.RGBA8_UNORM);width=w;height=h;allocations++;}catch(RuntimeException e){close();throw e;}}
        long bytes(){return scene==null?0:width*(long)height*20;}
        void close(){if(scene!=null)scene.destroyBuffers();if(glow!=null)glow.destroyBuffers();if(blur!=null)blur.destroyBuffers();if(coverage!=null)coverage.destroyBuffers();scene=glow=blur=coverage=null;drawn=bloom=false;}
    }
    public static void closePreview(){close(View.PREVIEW);}
    public static void closeGameplay(){close(View.WORLD);close(View.HAND);TYPES.clear();freeUniformIfIdle();}
    private static void close(View view){var s=SESSIONS.remove(view);if(s!=null)s.close();freeUniformIfIdle();}
    private static void freeUniformIfIdle(){if(SESSIONS.isEmpty()&&parameters!=null){parameters.close();parameters=null;}}
    public static void close(){DEFERRED.clear();for(var s:SESSIONS.values())s.close();SESSIONS.clear();TYPES.clear();freeUniformIfIdle();retry();}
}
