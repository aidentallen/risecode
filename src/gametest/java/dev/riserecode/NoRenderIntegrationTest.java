package dev.riserecode;

import dev.riserecode.event.FogEvent;
import dev.riserecode.module.modules.NoRenderModule;
import dev.riserecode.testmixin.GameRendererFogAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Exercises NoRender at the native FogRenderer boundary in a real client. */
public final class NoRenderIntegrationTest {
    private NoRenderIntegrationTest() { }

    public static void run(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            var module = Client.INSTANCE.getModuleManager().getByClass(NoRenderModule.class).orElseThrow();
            var fog = context.computeOnClient(mc -> ((GameRendererFogAccessor) mc.gameRenderer).riserecode$fogRenderer()
                    .setupFog(mc.gameRenderer.mainCamera(), 12, mc.getDeltaTracker(), 0, mc.level));
            check(!allDisabled(fog), "Native fog is present while NoRender is disabled");
            context.runOnClient(mc -> { module.allFog.setValue(true); module.setEnabled(true); });
            context.waitTicks(2);
            fog = context.computeOnClient(mc -> ((GameRendererFogAccessor) mc.gameRenderer).riserecode$fogRenderer()
                    .setupFog(mc.gameRenderer.mainCamera(), 12, mc.getDeltaTracker(), 0, mc.level));
            check(allDisabled(fog), "All Fog disables atmospheric fog");
            context.runOnClient(mc -> { module.allFog.setValue(false); module.fogSetting(FogEvent.Type.OVERWORLD).setValue(true); });
            context.waitTicks(2);
            fog = context.computeOnClient(mc -> ((GameRendererFogAccessor) mc.gameRenderer).riserecode$fogRenderer()
                    .setupFog(mc.gameRenderer.mainCamera(), 12, mc.getDeltaTracker(), 0, mc.level));
            check(allDisabled(fog), "Overworld Fog disables selected atmospheric fog");
            context.runOnClient(mc -> { module.fogSetting(FogEvent.Type.OVERWORLD).setValue(false); module.setEnabled(false); });
            context.waitTicks(2);
            fog = context.computeOnClient(mc -> ((GameRendererFogAccessor) mc.gameRenderer).riserecode$fogRenderer()
                    .setupFog(mc.gameRenderer.mainCamera(), 12, mc.getDeltaTracker(), 0, mc.level));
            check(!allDisabled(fog), "Disabling NoRender restores native atmospheric fog");
            Client.INSTANCE.getLogger().info("NoRender GPU checks passed (4 assertions)");
        }
    }

    private static boolean allDisabled(net.minecraft.client.renderer.fog.FogData fog) {
        return fog.environmentalStart == Float.MAX_VALUE && fog.environmentalEnd == Float.MAX_VALUE
                && fog.renderDistanceStart == Float.MAX_VALUE && fog.renderDistanceEnd == Float.MAX_VALUE
                && fog.skyEnd == Float.MAX_VALUE && fog.cloudEnd == Float.MAX_VALUE;
    }

    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
