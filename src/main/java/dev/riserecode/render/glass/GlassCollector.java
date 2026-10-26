package dev.riserecode.render.glass;

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


/** Native submission adapter. Auxiliary effects are forwarded without applying Glass. */
public final class GlassCollector implements SubmitNodeCollector {
        private final OrderedSubmitNodeCollector output;
        private final GlassCapture.Surface surface;
        public GlassCollector(OrderedSubmitNodeCollector output,GlassCapture.Surface surface){this.output=output;this.surface=surface;}
        @Override public OrderedSubmitNodeCollector order(int order){return new GlassCollector(output instanceof SubmitNodeCollector collector?collector.order(order):output,surface);}
        @Override public void submitShadow(PoseStack p0, float p1, List<ShadowPiece> p2) { output.submitShadow(p0,p1,p2); }
        @Override public void submitNameTag(PoseStack p0, Vec3 p1, int p2, Component p3, boolean p4, int p5, CameraRenderState p6) { output.submitNameTag(p0,p1,p2,p3,p4,p5,p6); }
        @Override public void submitText(PoseStack p0, float p1, float p2, FormattedCharSequence p3, boolean p4, DisplayMode p5, int p6, int p7, int p8, int p9) { output.submitText(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9); }
        @Override public void submitFlame(PoseStack p0, EntityRenderState p1, Quaternionf p2) { output.submitFlame(p0,p1,p2); }
        @Override public void submitLeash(PoseStack p0, LeashState p1) { output.submitLeash(p0,p1); }
        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack poses, RenderType type, int light, int overlay, int tint, TextureAtlasSprite sprite, int outline, CrumblingOverlay crumbling) {
            if (!surface.material().glint() && type.toString().contains("glint")) return;
            GlassCapture.with(surface,()->output.submitModel(model,state,poses,GlassRenderer.type(type,surface),light,overlay,tint,sprite,outline,crumbling));
        }
        @Override public void submitMovingBlock(PoseStack p0, MovingBlockRenderState p1, int p2) { output.submitMovingBlock(p0,p1,p2); }
        @Override public void submitBlockModel(PoseStack poses, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) { output.submitBlockModel(poses,GlassRenderer.type(type,surface),parts,tints,light,overlay,outline); }
        @Override public void submitBreakingBlockModel(PoseStack p0, List<BlockStateModelPart> p1, int p2) { output.submitBreakingBlockModel(p0,p1,p2); }
        @Override public void submitShapeOutline(PoseStack p0, VoxelShape p1, RenderType p2, int p3, float p4, boolean p5) { output.submitShapeOutline(p0,p1,p2,p3,p4,p5); }
        @Override public void submitItem(PoseStack poses, ItemDisplayContext display, int light, int overlay, int outline, int[] tints, List<BakedQuad> quads, FoilType foil) {
            GlassCapture.with(surface,()->output.submitItem(poses,display,light,overlay,outline,tints,quads,surface.material().glint()?foil:FoilType.NONE));
        }
        @Override public void submitCustomGeometry(PoseStack p0, RenderType p1, CustomGeometryRenderer p2) { output.submitCustomGeometry(p0,GlassRenderer.type(p1,surface),p2); }
        @Override public void submitQuadParticleGroup(QuadParticleRenderState p0) { output.submitQuadParticleGroup(p0); }
        @Override public void submitGizmoPrimitives(Group p0, CameraRenderState p1, boolean p2) { output.submitGizmoPrimitives(p0,p1,p2); }
    }
