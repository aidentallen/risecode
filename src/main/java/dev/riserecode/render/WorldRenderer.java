package dev.riserecode.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/** Immutable extraction snapshots submitted through vanilla's depth-tested line pipeline. */
public final class WorldRenderer implements AutoCloseable {
    private record RelativeLine(Vec3 from, Vec3 to, Vec3 normal, int color, float width, int endColor) { }
    private final EventBus.Subscription subscription;
    private volatile List<java.util.function.Consumer<LevelRenderContext>> submissions = List.of();
    private volatile List<Render3DEvent.Quad> quads = List.of();
    private volatile List<Render3DEvent.Line> lines = List.of();

    public WorldRenderer(EventBus bus) {
        subscription = bus.subscribe(Render3DEvent.class, Integer.MIN_VALUE, false, event -> { lines = event.getLines(); quads = event.getQuads(); submissions = event.getSubmissions(); });
    }
    public List<Render3DEvent.Line> getLines() { return lines; }
    public List<Render3DEvent.Quad> getQuads() { return quads; }

    public void submit(LevelRenderContext context) {
        submissions.forEach(submit -> submit.accept(context));
        for (boolean visible : new boolean[] {true, false}) { submitLines(context, visible); }
    }
    private void submitLines(LevelRenderContext context, boolean visible) {
        List<Render3DEvent.Line> snapshot = lines.stream().filter(line -> line.visibleOnly() == visible).toList();
        List<Render3DEvent.Quad> faces = visible ? quads : List.of();
        if (!faces.isEmpty()) {
            Vec3 eye = context.levelState().cameraRenderState.pos;
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugQuads(), (pose, vertices) -> {
                for (Render3DEvent.Quad face : faces) {
                    Vec3[] points = {face.a(), face.b(), face.c(), face.d()}; int[] colors = {face.color(), face.colorB(), face.colorC(), face.colorD()};
                    for (int i = 0; i < points.length; i++) {
                        Vec3 relative = points[i].subtract(eye); vertices.addVertex(pose, (float) relative.x, (float) relative.y, (float) relative.z).setColor(colors[i]);
                    }
                }
            });
        }
        if (snapshot.isEmpty()) { return; }
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<RelativeLine> geometry = new ArrayList<>(snapshot.size());
        for (Render3DEvent.Line line : snapshot) {
            Vec3 direction = line.to().subtract(line.from());
            if (direction.lengthSqr() < 1.0e-12) { continue; }
            geometry.add(new RelativeLine(line.from().subtract(camera), line.to().subtract(camera),
                    direction.normalize(), line.color(), line.width(), line.endColor()));
        }
        List<RelativeLine> immutable = List.copyOf(geometry);
        PoseStack poses = context.poseStack();
        context.submitNodeCollector().submitCustomGeometry(poses, visible ? RenderTypes.lines() : throughLines(), (pose, vertices) -> {
            for (RelativeLine line : immutable) {
                Vec3 normal = line.normal();
                vertices.addVertex(pose, (float) line.from().x, (float) line.from().y, (float) line.from().z)
                        .setColor(line.color()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(line.width());
                vertices.addVertex(pose, (float) line.to().x, (float) line.to().y, (float) line.to().z)
                        .setColor(line.endColor()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(line.width());
            }
        });
    }
    private static final class Through {
        private static final net.minecraft.client.renderer.rendertype.RenderType LINES = net.minecraft.client.renderer.rendertype.RenderType.create("risecode through-wall boxes",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.register(
                    com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
                        .withLocation(dev.riserecode.ui.render.UiMesh.id("pipeline/esp_lines"))
                        .withDepthStencilState(java.util.Optional.empty()).build())).createRenderSetup());
    }
    private static net.minecraft.client.renderer.rendertype.RenderType throughLines() { return Through.LINES; }
    @Override
    public void close() { subscription.close(); lines = List.of(); quads = List.of(); submissions = List.of(); }
}
