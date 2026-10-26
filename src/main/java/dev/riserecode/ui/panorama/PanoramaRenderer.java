package dev.riserecode.ui.panorama;

import com.mojang.blaze3d.*;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.riserecode.ui.render.UiMesh;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;
import org.joml.FrustumIntersection;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Dedicated Blaze3D renderer; no world renderer, shader-pack lighting or global camera state. */
public final class PanoramaRenderer implements AutoCloseable {
    public record Frame(float seconds,float hour,float wind,float rain,float fog,float night,float yaw,float pitch,float cursorX,float cursorZ,float interaction,boolean reduced,int particles,PanoramaInteraction.Snapshot impulses,PanoramaPreferences.Quality quality) { }
    private record Group(int cellX,int cellZ,PanoramaResources.Pass pass,GpuTextureView atlas,GpuBuffer buffer,int vertices,long bytes) { }
    private static final VertexFormat FORMAT=VertexFormat.builder(0).addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("UV0",GpuFormat.RG32_FLOAT).addAttribute("Color",GpuFormat.RGBA8_UNORM).addAttribute("Normal",GpuFormat.RGB32_FLOAT).addAttribute("UV1",GpuFormat.RG32_FLOAT).build();
    private static final BindGroupLayout PARAMETERS=BindGroupLayout.builder().withUniform("Scene",UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout TEXTURES=BindGroupLayout.builder().withSampler("Sampler0").build();
    private static final BindGroupLayout WATER_TEXTURES=BindGroupLayout.builder().withSampler("Sampler0").withSampler("Reflection").build();
    private static final class Pipelines {
        static final RenderPipeline SKY=pipeline("panorama_sky","screenquad",false,false,false);
        static final RenderPipeline OPAQUE=pipeline("panorama_block","panorama_block",true,false,true);
        static final RenderPipeline CUTOUT=pipeline("panorama_block_cutout","panorama_block",true,false,true);
        static final RenderPipeline TRANSLUCENT=pipeline("panorama_block_translucent","panorama_block",true,true,true);
        static final RenderPipeline WATER=pipeline("panorama_water","panorama_block",true,false,true);
        static final RenderPipeline PARTICLES=pipeline("panorama_particle","panorama_particle",false,true,true);
        static final RenderPipeline DRAW=UiMesh.pipeline("panorama_present","menu_photo");
        static RenderPipeline pipeline(String fragment,String vertex,boolean mesh,boolean blend,boolean depth){
            String shader=fragment.startsWith("panorama_block")?"panorama_block":fragment;
            var b=RenderPipeline.builder().withLocation(UiMesh.id("pipeline/"+fragment)).withVertexShader(vertex.equals("screenquad")?net.minecraft.resources.Identifier.withDefaultNamespace("core/screenquad"):UiMesh.id("core/"+vertex)).withFragmentShader(UiMesh.id("core/"+shader))
                .withBindGroupLayout(PARAMETERS).withDepthStencilState(depth?Optional.of(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL,!blend)):Optional.empty())
                .withColorTargetState(blend?new ColorTargetState(BlendFunction.TRANSLUCENT):ColorTargetState.DEFAULT).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
            if(mesh)b.withVertexBinding(0,FORMAT).withBindGroupLayout(fragment.equals("panorama_water")?WATER_TEXTURES:TEXTURES);
            return RenderPipelines.register(b.build());
        }
    }
    private final ArrayList<Group> groups=new ArrayList<>(),staging=new ArrayList<>();
    private GpuBuffer parameters,reflectionParameters;
    private TextureTarget target,blur,reflection;
    private long frames,uploads,draws,allocations,lastReflection;
    private long blurFrame=-1;
    private int uploadIndex;
    private long cpuNanos;
    private GpuQueryPool queries;
    private final boolean[] queryPending=new boolean[8];
    private int querySlot;
    private final ArrayList<Double> gpuSamples=new ArrayList<>(),cpuSamples=new ArrayList<>();
    private Frame latest;
    public boolean ready(){return !groups.isEmpty();}
    public GpuTextureView texture(){return target==null?null:target.getColorTextureView();}
    public int groupCount(){return groups.size()+staging.size();}
    public long uploads(){return uploads;}
    public long frames(){return frames;}
    public long draws(){return draws;}
    public long allocations(){return allocations;}
    public long cpuNanos(){return cpuNanos;}
    public List<Double> gpuSamples(){return List.copyOf(gpuSamples);}
    public List<Double> cpuSamples(){return List.copyOf(cpuSamples);}
    public void resetMeasurements(){gpuSamples.clear();cpuSamples.clear();}
    public long ownedBytes(){long n=groups.stream().mapToLong(Group::bytes).sum()+staging.stream().mapToLong(Group::bytes).sum();if(target!=null)n+=target.width*(long)target.height*8;if(blur!=null)n+=blur.width*(long)blur.height*4;if(reflection!=null)n+=reflection.width*(long)reflection.height*8;return n+(parameters==null?0:512)+(reflectionParameters==null?0:512);}
    /** Upload at most three groups per extraction while the old scene remains available. */
    public boolean upload(PanoramaResources.Prepared prepared){
        if(uploadIndex==0)discardStaging();
        int end=Math.min(prepared.meshes().size(),uploadIndex+3);
        for(;uploadIndex<end;uploadIndex++){
            var mesh=prepared.meshes().get(uploadIndex);ByteBuffer data=MemoryUtil.memAlloc(mesh.bytes().length);
            try{data.put(mesh.bytes()).flip();var atlas=Minecraft.getInstance().getTextureManager().getTexture(mesh.atlas()).getTextureView();var buffer=RenderSystem.getDevice().createBuffer(()->"Living Panorama static mesh",GpuBuffer.USAGE_VERTEX,data);staging.add(new Group(mesh.cellX(),mesh.cellZ(),mesh.pass(),atlas,buffer,mesh.vertices(),mesh.bytes().length));uploads++;}
            finally{MemoryUtil.memFree(data);}
        }
        if(uploadIndex==prepared.meshes().size()){groups.forEach(g->g.buffer.close());groups.clear();groups.addAll(staging);groups.sort(Comparator.comparingDouble((Group g)->-((g.cellX*24.+12)*(g.cellX*24.+12)+(g.cellZ*24.+12-25)*(g.cellZ*24.+12-25))));staging.clear();uploadIndex=0;lastReflection=0;return true;}return false;
    }
    public void discardStaging(){staging.forEach(g->g.buffer.close());staging.clear();uploadIndex=0;}
    public static Matrix4f view(float yaw,float pitch,boolean reflected){
        return new Matrix4f().rotateX((float)Math.toRadians(pitch+5)).rotateY((float)Math.toRadians(yaw)).translate(0,-7.2f,-25).scale(1,reflected?-1:1,1).translate(0,reflected?-.24f:0,0);
    }
    public static Matrix4f projection(float aspect){return new Matrix4f().perspective((float)Math.toRadians(66),aspect,.1f,190,true);}
    private static Matrix4f matrix(Frame f,float aspect,boolean reflected){return projection(aspect).mul(view(f.yaw,f.pitch,reflected));}
    private void targets(Frame f){
        var w=Minecraft.getInstance().getWindow();double scale=f.quality==PanoramaPreferences.Quality.LOW?.75:1;int width=Math.max(1,(int)Math.ceil(w.getScreenWidth()*scale)),height=Math.max(1,(int)Math.ceil(w.getScreenHeight()*scale));
        if(target!=null&&(target.width!=width||target.height!=height)){releaseTargets();}
        if(target==null){target=new TextureTarget("Living Panorama",width,height,true,GpuFormat.RGBA8_UNORM);allocations++;}
        if(f.quality==PanoramaPreferences.Quality.CINEMATIC){if(reflection==null){reflection=new TextureTarget("Living Panorama reflection",Math.max(1,width/2),Math.max(1,height/2),true,GpuFormat.RGBA8_UNORM);allocations++;lastReflection=0;}}
        else if(reflection!=null){reflection.destroyBuffers();reflection=null;}
        if(parameters==null)parameters=RenderSystem.getDevice().createBuffer(()->"Living Panorama frame",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,512);
        if(reflectionParameters==null)reflectionParameters=RenderSystem.getDevice().createBuffer(()->"Living Panorama reflected frame",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,512);
    }
    private void parameters(Frame f,GpuBuffer buffer,boolean reflected){
        float daylight=daylight(f.hour),warm=(float)Math.exp(-Math.pow((f.hour-6)/1.5,2))+(float)Math.exp(-Math.pow((f.hour-18)/1.5,2));
        float dull=1-f.rain*.5f;
        try(var stack=MemoryStack.stackPush()){
            var b=Std140Builder.onStack(stack,512).putMat4f(matrix(f,target.width/(float)target.height,reflected))
                .putVec4(0,7.2f,25,f.seconds).putVec4(f.reduced?0:f.wind,f.rain,f.fog,daylight)
                .putVec4((.025f+daylight*.25f+warm*.14f)*dull,(.045f+daylight*.49f)*dull,(.09f+daylight*.62f)*dull,1)
                .putVec4((.08f+daylight*.52f+warm*.25f)*dull,(.12f+daylight*.56f+warm*.05f)*dull,(.18f+daylight*.53f-warm*.08f)*dull,1)
                .putVec4(.025f+daylight*.06f,.09f+daylight*.18f,.14f+daylight*.12f,1)
                .putVec4(target.width/(float)target.height,reflection!=null&&!reflected?1:0,reflected?1:0,f.particles)
                .putVec4((float)Math.toRadians(f.yaw),(float)Math.toRadians(f.pitch+5),f.hour,f.night)
                .putVec4(f.cursorX,f.cursorZ,f.interaction,f.reduced?1:0);
            for(int i=0;i<8;i++){var list=f.impulses.gusts();if(i<list.size()){var v=list.get(i);b.putVec4(v.x(),v.z(),v.born(),f.reduced?0:v.strength());}else b.putVec4(0,0,-100,0);}
            for(int i=0;i<12;i++){var list=f.impulses.ripples();if(i<list.size()){var v=list.get(i);b.putVec4(v.x(),v.z(),v.born(),f.reduced?0:v.strength());}else b.putVec4(0,0,-100,0);}
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(),b.get());
        }
    }
    public static float daylight(float hour){return (float)Math.clamp(Math.sin((hour-6)*Math.PI/12)*.65+.35,.035,1);}
    public void render(Frame frame){
        long start=System.nanoTime();latest=frame;targets(frame);
        boolean measure=Boolean.getBoolean("riserecode.panoramaOnly");int slot=querySlot++%8;
        if(measure){if(queries==null)queries=RenderSystem.getDevice().createTimestampQueryPool(16);if(queryPending[slot]){var v=queries.getValues(slot*2,2);if(v[0].isPresent()&&v[1].isPresent()){gpuSamples.add((v[1].getAsLong()-v[0].getAsLong())/1e6);queryPending[slot]=false;}}if(!queryPending[slot])RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2);}
        parameters(frame,parameters,false);
        if(reflection!=null&&(lastReflection==0||System.nanoTime()-lastReflection>=66_666_667L)){
            parameters(frame,reflectionParameters,true);drawScene(reflection,reflectionParameters,frame,true);lastReflection=System.nanoTime();
        }
        drawScene(target,parameters,frame,false);frames++;blurFrame=-1;
        if(measure&&!queryPending[slot]){RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2+1);queryPending[slot]=true;}
        cpuNanos=System.nanoTime()-start;if(measure)cpuSamples.add(cpuNanos/1e6);
    }
    private void drawScene(TextureTarget output,GpuBuffer params,Frame f,boolean reflected){
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var pass=encoder.createRenderPass(()->"Living Panorama sky",output.getColorTextureView(),Optional.empty(),output.getDepthTextureView(),OptionalDouble.of(1))){pass.setPipeline(Pipelines.SKY);pass.setUniform("Scene",params);pass.draw(3,1,0,0);draws++;}
        var frustum=new FrustumIntersection(matrix(f,target.width/(float)target.height,reflected));
        for(var phase:PanoramaResources.Pass.values()){
            if(reflected&&phase==PanoramaResources.Pass.WATER)continue;
            var pipeline=switch(phase){case OPAQUE->Pipelines.OPAQUE;case CUTOUT->Pipelines.CUTOUT;case WATER->Pipelines.WATER;case TRANSLUCENT->Pipelines.TRANSLUCENT;};
            try(var pass=encoder.createRenderPass(()->"Living Panorama "+phase,output.getColorTextureView(),Optional.empty(),output.getDepthTextureView(),OptionalDouble.empty())){
                pass.setPipeline(pipeline);pass.setUniform("Scene",params);
                for(var group:groups){if(group.pass!=phase||!frustum.testAab(group.cellX*24-1,-5,group.cellZ*24-1,group.cellX*24+26,35,group.cellZ*24+26))continue;
                    pass.bindTexture("Sampler0",group.atlas,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));if(phase==PanoramaResources.Pass.WATER)pass.bindTexture("Reflection",reflection==null||reflected?group.atlas:reflection.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                    pass.setVertexBuffer(0,group.buffer.slice());pass.draw(group.vertices,1,0,0);draws++;
                }
            }
        }
        if(!reflected&&!f.reduced&&f.particles>0){try(var pass=encoder.createRenderPass(()->"Living Panorama discoveries",output.getColorTextureView(),Optional.empty(),output.getDepthTextureView(),OptionalDouble.empty())){pass.setPipeline(Pipelines.PARTICLES);pass.setUniform("Scene",params);pass.draw(f.particles*6,1,0,0);draws++;}}
    }
    public void present(net.minecraft.client.gui.GuiGraphicsExtractor g,double opacity){if(target==null)return;UiMesh.quad(g,Pipelines.DRAW,net.minecraft.client.gui.render.TextureSetup.singleTexture(texture(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),0,0,g.guiWidth(),g.guiHeight(),0,0,1,1,dev.riserecode.ui.theme.Theme.opacity(-1,opacity),dev.riserecode.ui.theme.Theme.opacity(-1,opacity),0,0,0,0);}
    public GpuTextureView blurred(){
        if(target==null)return null;
        if(blur==null){blur=new TextureTarget("Living Panorama button blur",Math.max(1,target.width/2),Math.max(1,target.height/2),false,GpuFormat.RGBA8_UNORM);allocations++;}
        if(blurFrame!=frames){dev.riserecode.util.DualBlur.apply(target,blur,dev.riserecode.ui.theme.Theme.number("GUI Blur Radius",12));blurFrame=frames;}
        return blur.getColorTextureView();
    }
    public Frame latest(){return latest;}
    private void releaseTargets(){if(target!=null){target.destroyBuffers();target=null;}if(blur!=null){blur.destroyBuffers();blur=null;}if(reflection!=null){reflection.destroyBuffers();reflection=null;}blurFrame=-1;}
    @Override public void close(){discardStaging();groups.forEach(g->g.buffer.close());groups.clear();releaseTargets();if(parameters!=null){parameters.close();parameters=null;}if(reflectionParameters!=null){reflectionParameters.close();reflectionParameters=null;}if(queries!=null){queries.close();queries=null;}Arrays.fill(queryPending,false);latest=null;}
}
