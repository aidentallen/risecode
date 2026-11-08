package dev.riserecode.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Sodium overwrites prepareChunkRenders; its own adapter replaces the vanilla hook. */
public final class RendererMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        var loader = FabricLoader.getInstance();
        String name = mixin.substring(mixin.lastIndexOf('.') + 1);
        boolean sodium = loader.isModLoaded("sodium");
        resolveCapabilities();
        if (name.equals("ChunkTerrainMixin")) { return !sodium; }
        if (name.startsWith("Bbe")) { return pinned("betterblockentities", "1.3.9+mc26.2"); }
        if (name.equals("SodiumExtraShaderMixin")) {
            return sodium && pinned("sodium", "0.9.2+mc26.2") && loader.isModLoaded("sodium-extra")
                    && method("me.flashyreese.mods.sodiumextra.client.fog.FogShaderTransformer", "injectRenderDistanceShape", "(Ljava/lang/String;)Ljava/lang/String;");
        }
        if (name.startsWith("Sodium") || name.startsWith("Iris")) {
            // Every adapter uses internal APIs. Exact pinned versions guard menus, placement,
            // lighting and accessors too; no required injection may run on an unknown version.
            if (!sodium || !pinned("sodium", "0.9.2+mc26.2")) { return false; }
            if (loader.isModLoaded("iris") && !pinned("iris", "1.11.4+mc26.2")) { return false; }
            if (name.startsWith("Iris") && !loader.isModLoaded("iris")) { return false; }
            if ((name.contains("SodiumChunk") || name.contains("IrisChunk")) && !chunkAdapters()) { return false; }
        }
        return true;
    }
    private boolean capabilitiesResolved;
    private void resolveCapabilities() {
        if (capabilitiesResolved) { return; }
        capabilitiesResolved = true;
        var loader = FabricLoader.getInstance();
        String sodium = loader.getModContainer("sodium").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
        String iris = loader.getModContainer("iris").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
        boolean versions = sodium == null ? iris == null : dev.riserecode.render.RendererCapabilities.supportedVersions(sodium, iris);
        boolean signatures = sodium == null || versions && chunkAdapters();
        dev.riserecode.render.RendererCapabilities.chunkAdapters = dev.riserecode.render.RendererCapabilities.resolveMotion(sodium, iris, signatures);
    }
    private static boolean pinned(String mod, String expected) {
        var installed = FabricLoader.getInstance().getModContainer(mod);
        if (installed.isEmpty()) { return false; }
        boolean supported = expected.equals(installed.get().getMetadata().getVersion().getFriendlyString());
        if (!supported) { org.slf4j.LoggerFactory.getLogger("riserecode").warn("Unsupported {} version; Risecode adapters for it are disabled (supported: {})", mod, expected); }
        return supported;
    }
    private Boolean chunkAdapters;
    private boolean chunkAdapters(){
        if(chunkAdapters!=null){return chunkAdapters;}
        var loader=FabricLoader.getInstance();
        String sodium=loader.getModContainer("sodium").orElseThrow().getMetadata().getVersion().getFriendlyString();
        String iris=loader.getModContainer("iris").map(mod->mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
        boolean ok=dev.riserecode.render.RendererCapabilities.supportedVersions(sodium,iris);
        ok &= method("net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer","render","(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/util/FogParameters;ZLcom/mojang/blaze3d/textures/GpuSampler;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;)V");
        ok &= method("net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer","getVisibleFaces","(IIIIII)I");
        ok &= method("net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer","delete","()V");
        ok &= method("net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer","begin","(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/util/FogParameters;Lcom/mojang/blaze3d/textures/GpuSampler;)V");
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")){ok &= method("net.irisshaders.iris.pipeline.transform.transformer.SodiumTransformer","injectVertInit","(Lio/github/douira/glsl_transformer/ast/transform/ASTParser;Lio/github/douira/glsl_transformer/ast/node/TranslationUnit;Lio/github/douira/glsl_transformer/ast/query/Root;Lnet/irisshaders/iris/pipeline/transform/parameter/SodiumParameters;Z)V");}
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")){ok &= method("net.irisshaders.iris.pipeline.programs.ExtendedShader","<init>","(ILjava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;ZLnet/irisshaders/iris/gl/framebuffer/GlFramebuffer;Lnet/irisshaders/iris/gl/framebuffer/GlFramebuffer;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;Lnet/irisshaders/iris/gl/blending/AlphaTest;Ljava/util/function/Consumer;Ljava/util/function/BiConsumer;ZLnet/irisshaders/iris/pipeline/IrisRenderingPipeline;Ljava/util/List;Lnet/irisshaders/iris/uniforms/custom/CustomUniforms;Lnet/irisshaders/iris/pipeline/transform/Patch;)V");}
        ok &= method("net.caffeinemc.mods.sodium.client.render.viewport.Viewport","isBoxVisible","(III)Z");
        ok &= method("net.caffeinemc.mods.sodium.client.render.viewport.Viewport","isBoxVisibleLooser","(III)Z");
        ok &= field("net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer","activeProgram","Lcom/mojang/blaze3d/pipeline/RenderPipeline;");
        ok &= field("net.caffeinemc.mods.sodium.client.render.viewport.Viewport","frustum","Lnet/caffeinemc/mods/sodium/client/render/viewport/frustum/Frustum;");
        ok &= field("net.caffeinemc.mods.sodium.client.render.viewport.Viewport","transform","Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;");
        if(FabricLoader.getInstance().isModLoaded("iris")){
            ok &= invocation("net.irisshaders.iris.pipeline.transform.transformer.SodiumTransformer","injectVertInit","io/github/douira/glsl_transformer/ast/node/TranslationUnit","parseAndInjectNodes","(Lio/github/douira/glsl_transformer/ast/transform/ASTParser;Lio/github/douira/glsl_transformer/ast/transform/ASTInjectionPoint;[Ljava/lang/String;)V");
            ok &= invocation("net.irisshaders.iris.pipeline.programs.ExtendedShader","<init>","com/mojang/blaze3d/opengl/GlProgram","setupBindGroupLayouts","(Ljava/util/List;)V");
        }
        dev.riserecode.render.RendererCapabilities.chunkAdapters=ok;
        if(!ok){org.slf4j.LoggerFactory.getLogger("riserecode").warn("Unsupported renderer adapter signatures; chunk motion disabled");}
        chunkAdapters=ok;return ok;
    }
    private static boolean method(String name,String method,String descriptor){
        try(var input=RendererMixinPlugin.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){
            if(input==null){return false;}
            var node=new ClassNode();new org.objectweb.asm.ClassReader(input).accept(node,0);
            return node.methods.stream().anyMatch(m->m.name.equals(method)&&m.desc.equals(descriptor));
        }catch(java.io.IOException | RuntimeException invalid){return false;}
    }
    private static ClassNode classBytes(String name)throws java.io.IOException{
        try(var input=RendererMixinPlugin.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){
            if(input==null){throw new java.io.IOException("Missing adapter class "+name);}
            var node=new ClassNode();new org.objectweb.asm.ClassReader(input).accept(node,0);return node;
        }
    }
    private static boolean field(String name,String field,String descriptor){
        try{return classBytes(name).fields.stream().anyMatch(f->f.name.equals(field)&&f.desc.equals(descriptor));}
        catch(java.io.IOException | RuntimeException invalid){return false;}
    }
    private static boolean invocation(String name,String method,String owner,String called,String descriptor){
        try{for(var m:classBytes(name).methods){if(m.name.equals(method)){for(var instruction:m.instructions){if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call && call.owner.equals(owner)&&call.name.equals(called)&&call.desc.equals(descriptor)){return true;}}}}return false;}
        catch(java.io.IOException | RuntimeException invalid){return false;}
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
