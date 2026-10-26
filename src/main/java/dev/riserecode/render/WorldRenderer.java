package dev.riserecode.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/** Immutable extraction snapshots submitted through dedicated world-overlay pipelines. */
public final class WorldRenderer implements AutoCloseable {
    private record Frame(PackedGeometry geometry,List<java.util.function.Consumer<LevelRenderContext>> submissions) {}
    private volatile Frame frame=new Frame(PackedGeometry.EMPTY,List.of());
    private final EventBus.Subscription subscription;

    public WorldRenderer(EventBus bus) {
        subscription = bus.subscribe(Render3DEvent.class, Integer.MIN_VALUE, false, event -> { frame=new Frame(event.geometry(),event.getSubmissions()); });
    }
    public List<Render3DEvent.Line> getLines() { return frame.geometry.lines(); }
    public List<Render3DEvent.Quad> getQuads() { return frame.geometry.quads(); }

    public void submit(LevelRenderContext context) {
        if (ShaderWorldOverlay.shadowPass()) { return; }
        var current=frame;
        current.submissions.forEach(submit -> submit.accept(context));
        PackedGeometry snapshot=current.geometry;
        submitGeometry(context,snapshot,true);submitGeometry(context,snapshot,false);
    }
    private void submitGeometry(LevelRenderContext context,PackedGeometry p,boolean visible){
        Vec3 camera=context.levelState().cameraRenderState.pos;
        if(p.quadCount(visible)>0){
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(),visible?(ShaderWorldOverlay.active()?Overlay.QUADS:RenderTypes.debugQuads()):throughQuads(),(pose,vertices)->{
                var scratch=new org.joml.Vector3f();
                for(int n=0;n<p.quadCount(visible);n++){int i=p.quadIndex(visible,n);for(int v=0;v<4;v++){pose.pose().transformPosition((float)(p.quad(i,v*3)-camera.x),(float)(p.quad(i,v*3+1)-camera.y),(float)(p.quad(i,v*3+2)-camera.z),scratch);vertices.addVertex(scratch.x,scratch.y,scratch.z).setColor(p.quadColor(i,v));}}
            });
        }
        if(p.lineCount(visible)==0){return;}
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(),visible?(ShaderWorldOverlay.active()?Overlay.LINES:RenderTypes.lines()):throughLines(),(pose,vertices)->{
            var scratch=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
            for(int n=0;n<p.lineCount(visible);n++){
                int i=p.lineIndex(visible,n);double dx=p.line(i,3)-p.line(i,0),dy=p.line(i,4)-p.line(i,1),dz=p.line(i,5)-p.line(i,2),length=dx*dx+dy*dy+dz*dz;
                if(length<1e-12){continue;}double inverse=1/Math.sqrt(length);float nx=(float)(dx*inverse),ny=(float)(dy*inverse),nz=(float)(dz*inverse);
                pose.transformNormal(nx,ny,nz,normal);
                for(int v=0;v<2;v++){pose.pose().transformPosition((float)(p.line(i,v*3)-camera.x),(float)(p.line(i,v*3+1)-camera.y),(float)(p.line(i,v*3+2)-camera.z),scratch);vertices.addVertex(scratch.x,scratch.y,scratch.z).setColor(p.lineColor(i,v)).setNormal(normal.x,normal.y,normal.z).setLineWidth((float)(p.width(i)*WorldResolution.pixelScale()));}
            }
        });
    }
    private static final class Overlay {
        // Iris replaces the vanilla LINES pipeline with the pack's gbuffers_line program.
        // Packs can hide/recolor that program or hard-code its width. Keep our native
        // line shader and composite the transparent overlay after the pack's final pass.
        private static final net.minecraft.client.renderer.rendertype.RenderType LINES = net.minecraft.client.renderer.rendertype.RenderType.create("risecode world overlay lines",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.register(
                    com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
                        .withLocation(dev.riserecode.ui.render.UiMesh.id("pipeline/world_overlay_lines"))
                        .build())).setOutputTarget(ShaderWorldOverlay.OUTPUT).createRenderSetup());
        private static final net.minecraft.client.renderer.rendertype.RenderType QUADS = net.minecraft.client.renderer.rendertype.RenderType.create("risecode shader overlay fill",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.DEBUG_QUADS)
                        .setOutputTarget(ShaderWorldOverlay.OUTPUT).sortOnUpload().createRenderSetup());
    }
    private static net.minecraft.client.renderer.rendertype.RenderType createPopQuads(){
        var original=net.minecraft.client.renderer.RenderPipelines.DEBUG_QUADS;
        var builder=com.mojang.blaze3d.pipeline.RenderPipeline.builder()
                .withLocation(dev.riserecode.ui.render.UiMesh.id("pipeline/block_pop_fill"))
                .withVertexShader(original.getVertexShader()).withFragmentShader(original.getFragmentShader())
                .withVertexBinding(0,original.getVertexFormatBinding(0)).withPrimitiveTopology(original.getPrimitiveTopology())
                .withColorTargetState(original.getColorTargetState()).withCull(true).withDepthStencilState(java.util.Optional.empty());
        for(var layout:original.getBindGroupLayouts()){builder.withBindGroupLayout(layout);}
        return net.minecraft.client.renderer.rendertype.RenderType.create("risecode block pop fill",net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.register(builder.build())).setOutputTarget(ShaderWorldOverlay.OUTPUT).sortOnUpload().createRenderSetup());
    }
    private static final class Through {
        private static final net.minecraft.client.renderer.rendertype.RenderType QUADS=createPopQuads();
        private static final net.minecraft.client.renderer.rendertype.RenderType LINES = net.minecraft.client.renderer.rendertype.RenderType.create("risecode through-wall boxes",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.register(
                    com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
                        .withLocation(dev.riserecode.ui.render.UiMesh.id("pipeline/esp_lines"))
                        .withDepthStencilState(java.util.Optional.empty()).build())).setOutputTarget(ShaderWorldOverlay.OUTPUT).createRenderSetup());
    }
    private static net.minecraft.client.renderer.rendertype.RenderType throughQuads(){return Through.QUADS;}
    private static net.minecraft.client.renderer.rendertype.RenderType throughLines() { return Through.LINES; }
    @Override
    public void close() { subscription.close(); frame=new Frame(PackedGeometry.EMPTY,List.of()); }
}
