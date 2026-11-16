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
import dev.riserecode.setting.StringSetting;
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
    private final BooleanSetting outline = register(new BooleanSetting("Outline", "Draw the box lines", true));
    private final BooleanSetting fill = register(new BooleanSetting("Fill", "Color the whole container", false));
    private final DoubleSetting fillOpacity = register(new DoubleSetting("FillOpacity", "Fill strength", 0.4, 0.05, 1));
    private final DoubleSetting width = register(DoubleSetting.extended("Width", "Outline pixels", 2, 0.5, 5));
    private final DoubleSetting range = register(DoubleSetting.extended("Range", "Maximum distance", 96, 8, 256));
    private final BooleanSetting visible = register(new BooleanSetting("VisibleOnly", "Depth-test against terrain", false));
    private final BooleanSetting minecarts = register(new BooleanSetting("Minecarts", "Chest and hopper minecarts", true));
    private final BooleanSetting autoColors = register(new BooleanSetting("AutoColors", "Color by container type (shulker purple, chest orange, ...) unless you picked a color", true));
    private final BooleanSetting tracers = register(new BooleanSetting("Tracers", "Lines from the crosshair to each container", false));
    private final DoubleSetting tracerWidth = register(DoubleSetting.extended("TracerWidth", "Tracer line pixels", 1.5, 0.5, 4));
    private final StringSetting typeColors = register(new StringSetting("TypeColors", "Per-container colors, set by right-clicking in the picker", "", 4096));
    private final BlockListSetting types = register(new BlockListSetting("Containers", "Only these container types are shown (none selected = nothing)", java.util.List.of()).colors(typeColors).candidates(StorageEspModule::containerIds));
    private final Minecraft mc;
    /** Work blocks that are not containers; found through the shared block index instead of the container list. */
    private static final List<String> WORKSTATIONS = List.of("minecraft:crafting_table", "minecraft:smithing_table", "minecraft:fletching_table",
        "minecraft:cartography_table", "minecraft:loom", "minecraft:grindstone", "minecraft:stonecutter", "minecraft:composter",
        "minecraft:cauldron", "minecraft:enchanting_table", "minecraft:bell", "minecraft:anvil", "minecraft:jukebox");
    private static List<String> containerIdCache;
    /** The color that best stands for a container type, or null to use the module color. */
    private static Integer typeColor(String id) {
        String name = id.substring(id.indexOf(':') + 1);
        if (name.endsWith("shulker_box")) { return 0xFFB266FF; }
        if (name.equals("ender_chest")) { return 0xFF2DB5A0; }
        if (name.equals("chest") || name.equals("trapped_chest")) { return 0xFFFF9F2E; }
        if (name.equals("barrel")) { return 0xFFD2A05A; }
        if (name.equals("furnace") || name.equals("blast_furnace") || name.equals("smoker")) { return 0xFF9AA0A6; }
        if (name.equals("hopper") || name.equals("dropper") || name.equals("dispenser") || name.equals("crafter")) { return 0xFF7F8791; }
        if (WORKSTATIONS.contains(id) || name.equals("lectern") || name.equals("brewing_stand")) { return 0xFF8B5A2B; }
        return null;
    }
    /** Every registered block that StorageESP can show: containers (chests, barrels, shulkers, furnaces, hoppers...) and workstations. */
    public static List<String> containerIds() {
        if (containerIdCache == null) {
            var ids = new ArrayList<String>();
            for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                try {
                    if (block instanceof net.minecraft.world.level.block.EntityBlock entity
                            && entity.newBlockEntity(net.minecraft.core.BlockPos.ZERO, block.defaultBlockState()) instanceof net.minecraft.world.level.block.entity.BlockEntity be
                            && (be instanceof net.minecraft.world.Container || be instanceof net.minecraft.world.level.block.entity.EnderChestBlockEntity || be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity)) {
                        ids.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString());
                    }
                } catch (RuntimeException ignored) { }
            }
            ids.addAll(WORKSTATIONS);
            containerIdCache = ids.stream().distinct().toList();
        }
        return containerIdCache;
    }
    private final LoadedBlockIndex index;
    public StorageEspModule(EventBus bus, Minecraft mc, LoadedBlockIndex index) {
        super("StorageESP", "All loaded containers and storage minecarts.", Category.RENDER, bus);
        this.mc = mc; this.index = index;
    }
    @Override protected void onEnable() { index.containersRequested(true); listen(Render3DEvent.class, this::extract); }
    @Override protected void onDisable() { index.containersRequested(false); index.select("StorageESP", List.of()); }
    private void extract(Render3DEvent event) {
        if (mc.level == null || mc.player == null) { return; }
        float partial = event.getContext().deltaTracker().getGameTimeDeltaPartialTick(false);
        var start = dev.riserecode.render.TracerOrigin.of(event); var only = types.getValue(); var overrides = types.colorMap();
        var workIds = types.getValue().stream().filter(WORKSTATIONS::contains).toList();
        index.select("StorageESP", workIds);
        if (!workIds.isEmpty()) {
            var wanted = java.util.Set.copyOf(workIds);
            for (var pos : index.blocks()) {
                if (pos.distToCenterSqr(mc.player.position()) > range.getValue() * range.getValue()) { continue; }
                var state = mc.level.getBlockState(pos); String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if (!wanted.contains(id)) { continue; }
                var shape = state.getShape(mc.level, pos); if (shape.isEmpty()) { continue; }
                Integer custom = overrides.get(id); if (custom == null && autoColors.getValue()) { custom = typeColor(id); } int argb = custom != null ? custom : color.getValue(); var bounds = shape.bounds().move(pos);
                if (tracers.getValue()) { var end = bounds.getCenter(); event.addLine(start, end, argb, argb, tracerWidth.getValue().floatValue(), visible.getValue()); }
                if (fill.getValue()) { EspGeometry.fill(event, bounds, argb, fillOpacity.getValue(), visible.getValue()); }
                if (outline.getValue()) { EspGeometry.box(event, bounds, argb, width.getValue(), visible.getValue()); }
            }
        }
        var dispatcher = mc.getBlockEntityRenderDispatcher(); dispatcher.prepare(event.getContext().camera().position());
        for (var container : index.containers()) {
            var pos = container.getBlockPos();
            if(container.isRemoved()||container.getLevel()!=mc.level||!mc.level.hasChunk(pos.getX()>>4,pos.getZ()>>4)){continue;}
            if (pos.distToCenterSqr(mc.player.position()) > range.getValue() * range.getValue()) { continue; }
            var block = mc.level.getBlockState(pos); var shape = block.getShape(mc.level, pos);
            if (shape.isEmpty()) { continue; }
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            if (!only.contains(id)) { continue; }
            Integer custom = overrides.get(id); if (custom == null && autoColors.getValue()) { custom = typeColor(id); }
            int argb = custom != null ? custom : color.getValue();
            var bounds = shape.bounds().move(pos);
            if (tracers.getValue()) { var end = bounds.getCenter(); event.addLine(start, end, argb, argb, tracerWidth.getValue().floatValue(), visible.getValue()); }
            if (fill.getValue()) { EspGeometry.fill(event, bounds, argb, fillOpacity.getValue(), visible.getValue()); }
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { if (outline.getValue()) { if (custom != null) { EspGeometry.box(event, bounds, argb, width.getValue(), visible.getValue()); } else { EspGeometry.box(event, bounds, color, width.getValue(), visible.getValue()); } } }
            else if (outline.getValue()) {
                var state = dispatcher.tryExtractRenderState(container, partial, null, true);
                var parts = new ArrayList<BlockStateModelPart>();
                mc.getModelManager().getBlockStateModelSet().get(block).collectParts(RandomSource.create(block.getSeed(pos)), parts);
                var snapshot = List.copyOf(parts); var style = custom != null ? EspGeometry.style(argb, width.getValue()) : EspGeometry.style(color, width.getValue()); boolean depth = visible.getValue();
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
            if (tracers.getValue()) { var end = bounds.getCenter(); event.addLine(start, end, color.getValue(), color.getValue(), tracerWidth.getValue().floatValue(), visible.getValue()); }
            if (fill.getValue()) { EspGeometry.fill(event, bounds, color.getValue(), fillOpacity.getValue(), visible.getValue()); }
            if (mode.getValue() == PlayerEspModule.Mode.BOX) { if (outline.getValue()) { EspGeometry.box(event, bounds, color, width.getValue(), visible.getValue()); } }
            else if (outline.getValue()) {
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
