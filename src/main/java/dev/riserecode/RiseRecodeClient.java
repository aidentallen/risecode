package dev.riserecode;

import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.event.UpdateEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;

/** Fabric feeds the custom bus; feature modules do not register Fabric hooks. */
public final class RiseRecodeClient implements ClientModInitializer {
    private static net.minecraft.client.KeyMapping zoomInKey, zoomOutKey, enlargeKey, mapKey, waypointKey, managerKey, trackKey, clearTrackKey, markersKey;
    /** All waypoint-related keys, in the order the keybind screen lists them. */
    public static java.util.List<net.minecraft.client.KeyMapping> waypointKeys() { return java.util.List.of(waypointKey, mapKey, managerKey, trackKey, clearTrackKey, markersKey); }
    public static net.minecraft.client.KeyMapping mapKey() { return mapKey; }
    public static net.minecraft.client.KeyMapping waypointKey() { return waypointKey; }
    /** Tracks (or untracks) the closest visible waypoint of the current dimension. */
    private static void trackNearest(dev.riserecode.map.MapService maps, net.minecraft.world.entity.player.Player player) {
        dev.riserecode.map.WaypointStore.Waypoint nearest = null; double best = Double.MAX_VALUE;
        for (var point : maps.waypoints().all()) {
            if (!point.visible() || !point.dimension().equals(maps.dimension())) { continue; }
            double distance = player.position().distanceToSqr(point.x() + 0.5, point.y() + 0.5, point.z() + 0.5);
            if (distance < best) { best = distance; nearest = point; }
        }
        if (nearest != null) { maps.waypoints().put(nearest.tracked(!nearest.tracked())); maps.saveWaypoints(); }
    }
    private final int[] lastSetting = new int[9], lastMapping = new int[9];
    private boolean keysSynced;
    private static int codeOf(net.minecraft.client.KeyMapping mapping) {
        return mapping.isUnbound() ? -1 : com.mojang.blaze3d.platform.InputConstants.getKey(mapping.saveString()).getValue();
    }
    /**
     * The waypoint keys live in two places: the module page of the menu (KeySetting) and Minecraft's controls (KeyMapping).
     * Whichever side changed since the last tick wins and is copied to the other side.
     */
    private void syncWaypointKeys(Client client, net.minecraft.client.Minecraft minecraft) {
        if (!(client.getModuleManager().getByName("Waypoints").orElse(null) instanceof dev.riserecode.module.modules.WaypointsModule module)) { return; }
        var worldMap = client.getModuleManager().getByClass(dev.riserecode.module.modules.WorldMapModule.class).orElse(null);
        var minimap = client.getModuleManager().getByClass(dev.riserecode.module.modules.hud.MinimapModule.class).orElse(null);
        if (worldMap == null || minimap == null) { return; }
        var settings = new dev.riserecode.setting.KeySetting[] {module.addKey, worldMap.openKey, module.managerKey, module.trackKey, module.clearTrackKey, module.markersKey, minimap.zoomInKey, minimap.zoomOutKey, minimap.enlargeKey};
        var mappings = new java.util.ArrayList<>(waypointKeys());
        mappings.addAll(java.util.List.of(zoomInKey, zoomOutKey, enlargeKey));
        boolean saveOptions = false;
        for (int i = 0; i < settings.length; i++) {
            int fromSetting = settings[i].getValue(), fromMapping = codeOf(mappings.get(i));
            boolean pushToMapping = keysSynced ? fromSetting != lastSetting[i] : fromSetting != settings[i].getDefaultValue();
            boolean pushToSetting = !pushToMapping && (keysSynced ? fromMapping != lastMapping[i] : fromMapping != fromSetting);
            if (pushToMapping && fromSetting != fromMapping) {
                mappings.get(i).setKey(fromSetting < 0 ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(fromSetting));
                saveOptions = true;
            } else if (pushToSetting) { settings[i].setValue(fromMapping); }
            lastSetting[i] = settings[i].getValue(); lastMapping[i] = pushToMapping ? fromSetting : fromMapping;
        }
        if (saveOptions) { net.minecraft.client.KeyMapping.resetMapping(); minecraft.options.save(); }
        keysSynced = true;
    }
    private long tick;
    private long previousHudFrame;

    @Override
    public void onInitializeClient() {
        Client client = Client.INSTANCE;
        client.init();
        var category = net.minecraft.client.KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Client.MOD_ID, "maps"));
        mapKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.map", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 77, category));
        waypointKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 66, category));
        managerKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint_manager", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(), category));
        int unbound = com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue();
        trackKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint_track", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, unbound, category));
        clearTrackKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint_clear_tracking", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, unbound, category));
        markersKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint_markers", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, unbound, category));
        zoomInKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.minimap_zoom_in", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 90, category));
        enlargeKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.minimap_enlarge", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 86, category));
        zoomOutKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.minimap_zoom_out", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 88, category));
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
            .registerReloadListener(Identifier.fromNamespaceAndPath(Client.MOD_ID,"render_cleanup"), (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                dev.riserecode.render.placement.PlacementAnimations.clear(true); client.getEventBus().post(new dev.riserecode.event.RenderResourcesReloadEvent()); dev.riserecode.render.WorldLabels.clearTextCache(); client.getMaps().resourceReload(); client.getCosmetics().resourceReload(); dev.riserecode.ui.font.CustomTextRenderer.close();
                dev.riserecode.render.WorldResolution.close(); dev.riserecode.render.ItemOutlineRenderer.close(); dev.riserecode.render.SilhouetteOutlineRenderer.close(); dev.riserecode.render.glass.GlassRenderer.close(); dev.riserecode.render.glass.GlassPreview.close(); dev.riserecode.render.AmbienceRenderer.reset(); dev.riserecode.util.BlurUtil.close();
                dev.riserecode.render.ChunkTerrainRenderer.reset();
                dev.riserecode.render.ShaderWorldOverlay.close();
                dev.riserecode.ui.component.ResourcesPreviews.close();
                dev.riserecode.ui.panorama.LivingPanorama.reload(); dev.riserecode.ui.menu.MenuBackdrop.close(); dev.riserecode.ui.theme.BrandingLogo.invalidate();
            });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,minecraft)->{dev.riserecode.render.glass.GlassRenderer.closeGameplay();});
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> { client.getClickGui(); dev.riserecode.render.placement.PlacementShaderBridge.init(); });
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            dev.riserecode.ui.panorama.LivingPanorama.tick();
            while (mapKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null && client.getModuleManager().getByClass(dev.riserecode.module.modules.WorldMapModule.class).map(dev.riserecode.module.Module::isEnabled).orElse(true)) { minecraft.gui.setScreen(new dev.riserecode.map.WorldMapScreen(null, client.getMaps())); } }
            while (waypointKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null) { var point = client.getMaps().addAtPlayer(); if (point != null) { minecraft.gui.setScreen(new dev.riserecode.map.WaypointEditScreen(null, client.getMaps(), point)); } } }
            while (managerKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null) { minecraft.gui.setScreen(new dev.riserecode.map.WaypointManagerScreen(null, client.getMaps())); } }
            while (trackKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null) { trackNearest(client.getMaps(), minecraft.player); } }
            while (clearTrackKey.consumeClick()) { var maps = client.getMaps(); for (var point : maps.waypoints().all()) { if (point.tracked()) { maps.waypoints().put(point.tracked(false)); } } maps.saveWaypoints(); }
            client.getModuleManager().getByClass(dev.riserecode.module.modules.hud.MinimapModule.class).ifPresent(mini -> mini.enlarged(enlargeKey.isDown() && minecraft.gui.screen() == null));
            while (zoomInKey.consumeClick()) { client.getModuleManager().getByName("Minimap").ifPresent(m -> { if (m instanceof dev.riserecode.module.modules.hud.MinimapModule mini) { mini.zoomBy(1); } }); }
            while (zoomOutKey.consumeClick()) { client.getModuleManager().getByName("Minimap").ifPresent(m -> { if (m instanceof dev.riserecode.module.modules.hud.MinimapModule mini) { mini.zoomBy(-1); } }); }
            while (markersKey.consumeClick()) { client.getModuleManager().getByName("Waypoints").ifPresent(dev.riserecode.module.Module::toggle); }
            if (client.isInitialized()) { syncWaypointKeys(client, minecraft); }
            if (client.isInitialized()) { dev.riserecode.render.placement.PlacementAnimations.syncLevel(); dev.riserecode.performance.PerformancePolicy.tick(); client.getEventBus().post(new UpdateEvent(minecraft, ++tick)); }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Client.MOD_ID, "hud"), (graphics, deltaTracker) -> {
                    long now = System.nanoTime();
                    double delta = previousHudFrame == 0 ? 0.0 : Math.max(0.0, (now - previousHudFrame) * 1.0e-9);
                    previousHudFrame = now;
                    if (client.isInitialized()) {
                        client.getEventBus().post(new Render2DEvent(graphics, deltaTracker, delta));
                    }
                });
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            if (client.isInitialized()) { client.getEventBus().post(new Render3DEvent(context)); }
        });
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context,state)->!client.isInitialized()||!client.getEventBus().post(new dev.riserecode.event.BlockOutlineEvent()).isCancelled());
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (client.isInitialized()) { client.getWorldRenderer().submit(context); dev.riserecode.render.placement.PlacementAnimations.submit(context.submitNodeCollector(),context.levelState().cameraRenderState.pos); }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> { dev.riserecode.render.placement.PlacementAnimations.clear(false); dev.riserecode.render.ShaderWorldOverlay.close(); dev.riserecode.render.ChunkTerrainRenderer.reset(); client.shutdown(); });
    }
}
