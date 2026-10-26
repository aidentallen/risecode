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
    private net.minecraft.client.KeyMapping mapKey, waypointKey;
    private long tick;
    private long previousHudFrame;

    @Override
    public void onInitializeClient() {
        Client client = Client.INSTANCE;
        client.init();
        var category = net.minecraft.client.KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Client.MOD_ID, "maps"));
        mapKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.map", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 77, category));
        waypointKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping("key.riserecode.waypoint", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 66, category));
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
            while (mapKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null) { minecraft.gui.setScreen(new dev.riserecode.map.WorldMapScreen(null, client.getMaps())); } }
            while (waypointKey.consumeClick()) { if (minecraft.gui.screen() == null && minecraft.player != null) { var point = client.getMaps().addAtPlayer(); if (point != null) { minecraft.gui.setScreen(new dev.riserecode.map.WaypointEditScreen(null, client.getMaps(), point)); } } }
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
