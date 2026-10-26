package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.EspGeometry;
import dev.riserecode.render.LoadedBlockIndex;
import dev.riserecode.render.SilhouetteOutlineRenderer;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;

public final class StorageEspModule extends Module {
    private final EnumSetting<PlayerEspModule.Mode> mode = register(new EnumSetting<>("Mode", "Bounds or exterior model silhouette", PlayerEspModule.Mode.BOX));
    private final ColorSetting color = register(new ColorSetting("Color", "Container color", 0xFFFFC857));
    private final DoubleSetting width = register(DoubleSetting.extended("Width", "Outline pixels", 2, 0.5, 5));
    private final DoubleSetting range = register(DoubleSetting.extended("Range", "Maximum distance", 96, 8, 256));
    private final BooleanSetting visible = register(new BooleanSetting("VisibleOnly", "Depth-test against terrain", false));
    private final BooleanSetting minecarts = register(new BooleanSetting("Minecarts", "Chest and hopper minecarts", true));
    private final Minecraft mc;
    private final LoadedBlockIndex index;
    public StorageEspModule(EventBus bus, Minecraft mc, LoadedBlockIndex index) {
        super("StorageESP", "All loaded containers and storage minecarts.", Category.RENDER, bus);
        this.mc = mc; this.index = index;
    }
    @Override protected void onEnable() { index.containersRequested(true); listen(Render3DEvent.class, this::extract); }
    @Override protected void onDisable() { index.containersRequested(false); }
    private void extract(Render3DEvent event) {
        if (mc.level == null || mc.player == null) { return; }
        float partial = event.getContext().deltaTracker().getGameTimeDeltaPartialTick(false);
        var dispatcher = mc.getBlockEntityRenderDispatcher(); dispatcher.prepare(event.getContext().camera().position());
        for (var container : index.containers()) {
            var pos = container.getBlockPos();
            if(container.isRemoved()||container.getLevel()!=mc.level||!mc.level.hasChunk(pos.getX()>>4,pos.getZ()>>4)){continue;}
            if (pos.distToCenterSqr(mc.player.position()) > range.getValue() * range.getValue()) { continue; }
            var block = mc.level.getBlockState(pos); var shape = block.getShape(mc.level, pos);
            if (shape.isEmpty()) { continue; }
            var bounds = shape.bounds().move(pos);
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { EspGeometry.box(event, bounds, color, width.getValue(), visible.getValue()); }
            else {
                var state = dispatcher.tryExtractRenderState(container, partial, null, true);
                var parts = new ArrayList<BlockStateModelPart>();
                mc.getModelManager().getBlockStateModelSet().get(block).collectParts(RandomSource.create(block.getSeed(pos)), parts);
                var snapshot = List.copyOf(parts); var style = EspGeometry.style(color, width.getValue()); boolean depth = visible.getValue();
                event.submit(context -> {
                    var camera = context.levelState().cameraRenderState; var poses = context.poseStack();
                    var collector = SilhouetteOutlineRenderer.collector("storage", style, depth, bounds.inflate(0.1), camera, context.submitNodeCollector());
                    poses.pushPose(); try {
                        poses.translate(pos.getX() - camera.pos.x, pos.getY() - camera.pos.y, pos.getZ() - camera.pos.z);
                        if (state != null) { dispatcher.submit(state, poses, collector, camera); }
                        if (!snapshot.isEmpty()) { collector.submitBlockModel(poses, RenderTypes.debugQuads(), snapshot, new int[0], 15728880, 0, 0); }
                    } finally { poses.popPose(); }
                });
            }
        }
        if (!minecarts.getValue()) { return; }
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractMinecartContainer) || entity.isRemoved() || mc.player.distanceToSqr(entity) > range.getValue() * range.getValue()) { continue; }
            var position = entity.getPosition(partial); var bounds = entity.getBoundingBox().move(position.subtract(entity.position()));
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { EspGeometry.box(event, bounds, color, width.getValue(), visible.getValue()); }
            else {
                var state = mc.getEntityRenderDispatcher().extractEntity(entity, partial); var style = EspGeometry.style(color, width.getValue()); boolean depth = visible.getValue();
                event.submit(context -> {
                    var camera = context.levelState().cameraRenderState;
                    var collector = SilhouetteOutlineRenderer.collector("storage", style, depth, bounds.inflate(0.3), camera, context.submitNodeCollector());
                    mc.getEntityRenderDispatcher().submit(state, camera, position.x - camera.pos.x, position.y - camera.pos.y, position.z - camera.pos.z, context.poseStack(), collector);
                });
            }
        }
    }
}
