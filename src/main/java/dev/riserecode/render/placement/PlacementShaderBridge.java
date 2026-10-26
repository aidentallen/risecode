package dev.riserecode.render.placement;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Lazy optional Iris integration; the block model remains inside the pack's native passes. */
public final class PlacementShaderBridge {
    private PlacementShaderBridge() { }
    public static void init() { if(FabricLoader.getInstance().isModLoaded("iris"))Iris.register(); }
    public static boolean supported() { return FabricLoader.getInstance().isModLoaded("iris")&&Iris.supported(); }
    public static void close() { if(FabricLoader.getInstance().isModLoaded("iris"))Iris.close(); }
    private static final class Iris {
        private static boolean registered;
        private static net.minecraft.client.renderer.RenderBuffers buffers;
        private static net.minecraft.client.renderer.feature.FeatureRenderDispatcher dispatcher;
        private static final net.minecraft.client.renderer.SubmitNodeStorage submits=new net.minecraft.client.renderer.SubmitNodeStorage();
        static void register(){
            if(registered)return;
            if(net.irisshaders.iris.api.v0.IrisApi.getInstance().getMinorApiRevision()<4)return;
            net.irisshaders.iris.api.v0.IrisApi.getInstance().registerShadowRenderCallback((view,projection,x,y,z,delta)->{
                if(PlacementAnimations.draws().isEmpty())return;
                if(dispatcher==null){var mc=Minecraft.getInstance();buffers=new net.minecraft.client.renderer.RenderBuffers(1);
                    dispatcher=new net.minecraft.client.renderer.feature.FeatureRenderDispatcher(buffers,mc.getModelManager(),mc.getAtlasManager(),mc.font,mc.gameRenderer.gameRenderState());}
                PlacementAnimations.submit(submits,new Vec3(x,y,z),true);
                dispatcher.renderAllFeatures(submits);buffers.endFrame();
            });registered=true;
        }
        static boolean supported(){register();var pipeline=net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable();
            return registered&&(!(pipeline instanceof net.irisshaders.iris.pipeline.IrisRenderingPipeline iris)||!iris.skipAllRendering());}
        static void close(){if(dispatcher!=null){dispatcher.close();dispatcher=null;}if(buffers!=null){buffers.close();buffers=null;}}
    }
}
