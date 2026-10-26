package dev.riserecode.render.glass;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.ui.render.UiMesh;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/** Dedicated preview resources. Neither the live entity nor the game camera is mutated. */
public final class GlassPreview {
    private static TextureTarget target;
    private static ProjectionMatrixBuffer projection;
    private static final SubmitNodeStorage SUBMITS=new SubmitNodeStorage();
    private static final class Pipelines {
        static final RenderPipeline BACKDROP=RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id("pipeline/glass_preview"))
            .withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/glass_preview"))
            .withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES).withColorTargetState(ColorTargetState.DEFAULT).build());
    }
    private GlassPreview() { }
    public static long ownedGpuBytes(){return target==null?0:target.width*(long)target.height*8;}
    public static boolean isPreview(GuiEntityRenderState state){return state.renderState() instanceof GlassCapture.State s&&s.risecode$glassOwner()!=null&&s.risecode$glassOwner().view()==GlassPolicy.View.PREVIEW;}
    public static void render(GuiEntityRenderState state,GuiRenderState gui,FeatureRenderDispatcher dispatcher,int guiScale){
        int w=Math.max(1,(state.x1()-state.x0())*guiScale),h=Math.max(1,(state.y1()-state.y0())*guiScale);
        if(target==null||target.width!=w||target.height!=h){close();target=new TextureTarget("Glass isolated preview",w,h,true,GpuFormat.RGBA8_UNORM);projection=new ProjectionMatrixBuffer("Glass preview");}
        var oldColor=RenderSystem.outputColorTextureOverride;var oldDepth=RenderSystem.outputDepthTextureOverride;
        var oldProjection=RenderSystem.getProjectionMatrixBuffer();var oldProjectionType=RenderSystem.getProjectionType();
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();
        try{
            RenderSystem.outputColorTextureOverride=target.getColorTextureView();RenderSystem.outputDepthTextureOverride=target.getDepthTextureView();
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),new Vector4f(0,0,0,1),target.getDepthTexture(),0);
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"Glass patterned preview backdrop",target.getColorTextureView(),Optional.empty())){pass.setPipeline(Pipelines.BACKDROP);pass.draw(3,1,0,0);}
            var p=new Projection();p.setupOrtho(-1000,1000,w,h,true);RenderSystem.setProjectionMatrix(projection.getBuffer(p),ProjectionType.ORTHOGRAPHIC);
            modelView.identity();
            var poses=new PoseStack();poses.translate(w*.5f,h*.84f,0);float scale=guiScale*state.scale();poses.scale(scale,scale,-scale);
            poses.translate(state.translation().x(),state.translation().y(),state.translation().z());poses.mulPose(state.rotation());
            var camera=new CameraRenderState();camera.orientation=state.rotation().conjugate(new Quaternionf()).rotateY((float)Math.PI);
            Minecraft.getInstance().gameRenderer.lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ENTITY_IN_UI);
            Minecraft.getInstance().getEntityRenderDispatcher().submit(state.renderState(),camera,0,0,0,poses,SUBMITS);
            dispatcher.renderAllFeatures(SUBMITS);GlassRenderer.flush(GlassPolicy.View.PREVIEW);
        }finally{
            modelView.popMatrix();RenderSystem.outputColorTextureOverride=oldColor;RenderSystem.outputDepthTextureOverride=oldDepth;
            if(oldProjection!=null)RenderSystem.setProjectionMatrix(oldProjection,oldProjectionType);
        }
        gui.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,TextureSetup.singleTexture(target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),state.pose(),state.x0(),state.y0(),state.x1(),state.y1(),0,1,1,0,-1,state.scissorArea(),null));
    }
    public static void close(){GlassRenderer.closePreview();if(target!=null){target.destroyBuffers();target=null;}if(projection!=null){projection.close();projection=null;}}
}
