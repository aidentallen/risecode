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
import net.minecraft.resources.Identifier;

/** Fabric feeds the custom bus; feature modules do not register Fabric hooks. */
public final class RiseRecodeClient implements ClientModInitializer {
    private long tick;
    private long previousHudFrame;

    @Override
    public void onInitializeClient() {
        Client client = Client.INSTANCE;
        client.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> client.getClickGui());
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (client.isInitialized()) { client.getEventBus().post(new UpdateEvent(minecraft, ++tick)); }
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
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> client.shutdown());
    }
}
