package dev.riserecode;

import dev.riserecode.module.modules.MaceAssistModule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.LivingEntity;

/** Wind charge launch in survival with a mob next to the landing spot; the smash should land before the ground. */
public final class MaceIntegrationTest {
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        var assist = Client.INSTANCE.getModuleManager().getByClass(MaceAssistModule.class).orElseThrow();
        c.runOnClient(mc -> { mc.options.pauseOnLostFocus = false; mc.gui.setScreen(null); assist.targets.setValue(MaceAssistModule.Targets.MOBS); assist.windKey.setValue(86); assist.setEnabled(true); });
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("time set midnight");
            server.runCommand("difficulty normal");
            server.runCommand("give @a minecraft:mace");
            server.runCommand("give @a minecraft:wind_charge 8");
            server.runCommand("tp @a 0 -60 0 0 0");
            server.runCommand("summon zombie 0 -60 1.2 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
            c.waitTicks(30);
            var smashedInAir = new boolean[1];
            c.getInput().pressKey(86);
            for (int i = 0; i < 90; i++) {
                c.waitTicks(1);
                c.runOnClient(mc -> {
                    var p = mc.player;
                    float hp = -1;
                    for (var e : mc.level.entitiesForRendering()) { if (e instanceof LivingEntity l && !(e instanceof net.minecraft.world.entity.player.Player)) { hp = l.getHealth(); } }
                    if (hp <= 0f && !smashedInAir[0] && !p.onGround()) { smashedInAir[0] = true; }
                });
            }
            if (!smashedInAir[0]) { throw new AssertionError("the wind launched smash never killed the zombie before landing"); }
        }
    }
}
