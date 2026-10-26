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
    private final DoubleSetting width = register(DoubleSetting.extended("Width", "Outline pixels", 2, 0.5, 5));
    private final DoubleSetting range = register(DoubleSetting.extended("Range", "Maximum distance", 64, 8, 128));
    private final DoubleSetting limit = register(DoubleSetting.extended("TargetLimit", "Maximum rendered matches", 256, 16, 1024));
    private final BooleanSetting visible = register(new BooleanSetting("VisibleOnly", "Depth-test against terrain", false));
    private final BlockListSetting blocks;
    private final Minecraft mc;
    private final LoadedBlockIndex index;
    public BlockEspModule(EventBus bus, Minecraft mc, LoadedBlockIndex index) {
        super("BlockESP", "Loaded-only registry block highlights.", Category.RENDER, bus);
        this.mc = mc; this.index = index;
        blocks = register(new BlockListSetting("Blocks", "Search any registered block", BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals("minecraft") && (id.getPath().endsWith("_ore") || id.getPath().equals("ancient_debris"))).map(Object::toString).toList()));
    }
    @Override protected void onEnable() { index.maximum(limit.getValue().intValue()); index.select(blocks.getValue()); listen(Render3DEvent.class, this::extract); }
    @Override protected void onDisable() { index.maximum(1024); index.select(List.of()); }
    private void extract(Render3DEvent event) {
        if (mc.level == null || mc.player == null) { return; }
        index.maximum(limit.getValue().intValue()); index.select(blocks.getValue()); int count = 0;
        for (var pos : index.blocks()) {
            if (pos.distToCenterSqr(mc.player.position()) > range.getValue() * range.getValue()) { continue; }
            if (count++ >= limit.getValue()) { break; }
            var state = mc.level.getBlockState(pos); var shape = state.getShape(mc.level, pos);
            if (shape.isEmpty()) { continue; }
            var bounds = shape.bounds().move(pos);
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { for (var box : shape.toAabbs()) { EspGeometry.box(event, box.move(pos), color, width.getValue(), visible.getValue()); } }
            else {
                var parts = new ArrayList<BlockStateModelPart>();
                mc.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(state.getSeed(pos)), parts);
                var snapshot = List.copyOf(parts); var style = EspGeometry.style(color, width.getValue()); boolean depth = visible.getValue();
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
