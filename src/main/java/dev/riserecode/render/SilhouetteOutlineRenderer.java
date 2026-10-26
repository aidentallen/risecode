package dev.riserecode.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.EnchantmentRenderEvent.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Group;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

/** Independent union masks per style/depth policy. Geometry is extracted by vanilla renderers;
 * labels, shadows and flames are excluded. Dilation discards every original interior pixel. */
public final class SilhouetteOutlineRenderer {
    private record Key(String channel, Style style, boolean visible) { }
    private static final Map<Key, SilhouetteMask> BATCHES = new java.util.LinkedHashMap<>();
    private static final ArrayList<SilhouetteMask> POOL = new ArrayList<>();
    private static int used;
    private SilhouetteOutlineRenderer() { }
    public static void beginFrame() { POOL.forEach(SilhouetteMask::beginFrame); BATCHES.clear(); used = 0; }
    public static SubmitNodeCollector collector(String channel, Style style, boolean visible, AABB bounds,
            CameraRenderState camera, SubmitNodeCollector output) {
        Key key = new Key(channel, style, visible);
        SilhouetteMask mask=BATCHES.get(key);if(mask==null){if(used==POOL.size()){POOL.add(new SilhouetteMask());}mask=POOL.get(used++);BATCHES.put(key,mask);}
        mask.request(style, bounds, camera);
        return maskCollector(output, mask, visible);
    }
    private static final Direction[] DIRECTIONS=Direction.values();
    private static SubmitNodeCollector maskCollector(OrderedSubmitNodeCollector output, SilhouetteMask mask, boolean visible) {
        return new MaskCollector(output,mask,visible);
    }
    /** Compile-time checked against the target collector API. Non-model effects stay excluded. */
    private static final class MaskCollector implements SubmitNodeCollector {
        private final OrderedSubmitNodeCollector output;
        private final SilhouetteMask mask;
        private final boolean visible;
        MaskCollector(OrderedSubmitNodeCollector output,SilhouetteMask mask,boolean visible){this.output=output;this.mask=mask;this.visible=visible;}
        @Override public OrderedSubmitNodeCollector order(int order){return maskCollector(output instanceof SubmitNodeCollector collector?collector.order(order):output,mask,visible);}
        @Override public void submitShadow(PoseStack p0, float p1, List<ShadowPiece> p2) {  }
        @Override public void submitNameTag(PoseStack p0, Vec3 p1, int p2, Component p3, boolean p4, int p5, CameraRenderState p6) {  }
        @Override public void submitText(PoseStack p0, float p1, float p2, FormattedCharSequence p3, boolean p4, DisplayMode p5, int p6, int p7, int p8, int p9) {  }
        @Override public void submitFlame(PoseStack p0, EntityRenderState p1, Quaternionf p2) {  }
        @Override public void submitLeash(PoseStack p0, LeashState p1) {  }
        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack poses, RenderType type, int light, int overlay, int tint, TextureAtlasSprite sprite, int outline, CrumblingOverlay crumbling) { var atlas=((TextureSource)type).riserecode$texture();
            if(atlas!=null){output.submitModel(model,state,poses,mask.type(atlas,type.format(),visible),light,overlay,-1,sprite,0,null);} }
        @Override public void submitMovingBlock(PoseStack p0, MovingBlockRenderState p1, int p2) {  }
        @Override public void submitBlockModel(PoseStack poses, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) { for(var part:parts){for(Direction direction:DIRECTIONS){quads(output,mask,visible,poses,part.getQuads(direction));}quads(output,mask,visible,poses,part.getQuads(null));} }
        @Override public void submitBreakingBlockModel(PoseStack p0, List<BlockStateModelPart> p1, int p2) {  }
        @Override public void submitShapeOutline(PoseStack p0, VoxelShape p1, RenderType p2, int p3, float p4, boolean p5) {  }
        @Override public void submitItem(PoseStack poses, ItemDisplayContext display, int light, int overlay, int outline, int[] tints, List<BakedQuad> quads, FoilType foil) { quads(output,mask,visible,poses,quads); }
        @Override public void submitCustomGeometry(PoseStack p0, RenderType p1, CustomGeometryRenderer p2) {  }
        @Override public void submitQuadParticleGroup(QuadParticleRenderState p0) {  }
        @Override public void submitGizmoPrimitives(Group p0, CameraRenderState p1, boolean p2) {  }
    }
    public static void quads(OrderedSubmitNodeCollector output, SilhouetteMask mask, boolean visible, PoseStack pose, List<BakedQuad> quads) {
        if(quads.isEmpty()){return;}
        var groups=new java.util.LinkedHashMap<RenderType,ArrayList<BakedQuad>>();
        for(var quad:quads){var type=mask.type(quad.materialInfo().sprite().atlasLocation(),DefaultVertexFormat.POSITION_TEX_COLOR,visible);groups.computeIfAbsent(type,ignored->new ArrayList<>()).add(quad);}
        groups.forEach((type,group)->{
            var snapshot=List.copyOf(group);
            output.submitCustomGeometry(pose,type,(matrix,vertices)->{
                var scratch=new org.joml.Vector3f();
                for(var quad:snapshot){for(int i=0;i<4;i++){long uv=quad.packedUV(i);matrix.pose().transformPosition(quad.position(i),scratch);vertices.addVertex(scratch.x,scratch.y,scratch.z).setUv(UVPair.unpackU(uv),UVPair.unpackV(uv)).setColor(-1);}}
            });
        });
    }
    public static long ownedGpuBytes(){long bytes=0;for(var mask:POOL){bytes+=mask.ownedGpuBytes();}return bytes;}
    public static void composite(RenderTarget target) { BATCHES.values().forEach(mask -> mask.composite(target)); }
    public static List<RenderTarget> maskTargets() { return POOL.stream().map(SilhouetteMask::getMaskTarget).filter(Objects::nonNull).toList(); }
    public static int allocations() { return POOL.stream().mapToInt(SilhouetteMask::getAllocationCount).sum(); }
    public static long composites() { return POOL.stream().mapToLong(SilhouetteMask::getCompositeCount).sum(); }
    public static int batchCount() { return POOL.size(); }
    public static void close() { POOL.forEach(SilhouetteMask::close); POOL.clear(); BATCHES.clear(); used = 0; }
}
