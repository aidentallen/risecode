package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.EspGeometry;
import dev.riserecode.render.LoadedBlockIndex;
import dev.riserecode.render.SilhouetteOutlineRenderer;
import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public final class BlockEspModule extends Module {
    private final EnumSetting<PlayerEspModule.Mode> mode = register(new EnumSetting<>("Mode", "Bounds or exterior model silhouette", PlayerEspModule.Mode.BOX));
    private final ColorSetting color = register(new ColorSetting("Color", "Selected block color", 0xFF59D5CB));
    private final BooleanSetting outline = register(new BooleanSetting("Outline", "Draw the box lines", true));
    private final BooleanSetting fill = register(new BooleanSetting("Fill", "Color the whole block", false));
    private final DoubleSetting fillOpacity = register(new DoubleSetting("FillOpacity", "Fill strength", 0.4, 0.05, 1));
    private final DoubleSetting width = register(DoubleSetting.extended("Width", "Outline pixels", 2, 0.5, 5));
    private final DoubleSetting range = register(DoubleSetting.extended("Range", "Maximum distance", 64, 8, 128));
    private final DoubleSetting limit = register(DoubleSetting.extended("TargetLimit", "Maximum rendered matches", 256, 16, 1024));
    private final BooleanSetting visible = register(new BooleanSetting("VisibleOnly", "Depth-test against terrain", false));
    private final BooleanSetting tracers = register(new BooleanSetting("Tracers", "Lines from the crosshair to each highlighted block", false));
    private final DoubleSetting tracerWidth = register(DoubleSetting.extended("TracerWidth", "Tracer line pixels", 1.5, 0.5, 4));
    private final StringSetting blockColors = register(new StringSetting("BlockColors", "Per-block colors, set by right-clicking a block in the picker", "", 4096));
    private final BlockListSetting blocks;
    private final Minecraft mc;
    private final LoadedBlockIndex index;
    public BlockEspModule(EventBus bus, Minecraft mc, LoadedBlockIndex index) {
        super("BlockESP", "Loaded-only registry block highlights.", Category.RENDER, bus);
        this.mc = mc; this.index = index;
        blocks = register(new BlockListSetting("Blocks", "Search any registered block", BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals("minecraft") && (id.getPath().endsWith("_ore") || id.getPath().equals("ancient_debris"))).map(Object::toString).toList()));
        blocks.colors(blockColors);
    }
    @Override protected void onEnable() { index.maximum(limit.getValue().intValue()); index.select("BlockESP", blocks.getValue()); listen(Render3DEvent.class, this::extract); }
    @Override protected void onDisable() { index.maximum(1024); index.select("BlockESP", List.of()); }
    private void extract(Render3DEvent event) {
        if (mc.level == null || mc.player == null) { return; }
        index.maximum(limit.getValue().intValue()); index.select("BlockESP", blocks.getValue()); int count = 0;
        var overrides = blocks.colorMap(); var selectedIds = java.util.Set.copyOf(blocks.getValue());
        var start = dev.riserecode.render.TracerOrigin.of(event);
        for (var pos : index.blocks()) {
            if (pos.distToCenterSqr(mc.player.position()) > range.getValue() * range.getValue()) { continue; }
            if (count++ >= limit.getValue()) { break; }
            var state = mc.level.getBlockState(pos);
            if (!selectedIds.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) { continue; }
            var shape = state.getShape(mc.level, pos);
            if (shape.isEmpty()) { continue; }
            var bounds = shape.bounds().move(pos);
            Integer custom = overrides.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            int argb = custom != null ? custom : color.getValue();
            if (fill.getValue()) { for (var box : shape.toAabbs()) { EspGeometry.fill(event, box.move(pos), argb, fillOpacity.getValue(), visible.getValue()); } }
            if (!outline.getValue()) { if (tracers.getValue()) { var end = bounds.getCenter(); event.addLine(start, end, argb, argb, tracerWidth.getValue().floatValue(), visible.getValue()); } continue; }
            if (tracers.getValue()) { var end = bounds.getCenter(); event.addLine(start, end, argb, argb, tracerWidth.getValue().floatValue(), visible.getValue()); }
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { for (var box : shape.toAabbs()) { if (custom != null) { EspGeometry.box(event, box.move(pos), argb, width.getValue(), visible.getValue()); } else { EspGeometry.box(event, box.move(pos), color, width.getValue(), visible.getValue()); } } }
            else {
                var parts = new ArrayList<BlockStateModelPart>();
                mc.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(state.getSeed(pos)), parts);
                var snapshot = List.copyOf(parts); var style = custom != null ? EspGeometry.style(argb, width.getValue()) : EspGeometry.style(color, width.getValue()); boolean depth = visible.getValue();
                event.submit(context -> {
                    var camera = context.levelState().cameraRenderState; var poses = context.poseStack();
                    var collector = SilhouetteOutlineRenderer.collector("blocks", style, depth, bounds, camera, context.submitNodeCollector());
                    poses.pushPose(); try { poses.translate(pos.getX() - camera.pos.x, pos.getY() - camera.pos.y, pos.getZ() - camera.pos.z);
                        collector.submitBlockModel(poses, RenderTypes.debugQuads(), snapshot, new int[0], 15728880, 0, 0);
                    } finally { poses.popPose(); }
                });
            }
        }
    }
}
