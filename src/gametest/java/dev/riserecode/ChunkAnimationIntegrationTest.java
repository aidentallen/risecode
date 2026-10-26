package dev.riserecode;

import dev.riserecode.module.modules.AmbienceModule;
import dev.riserecode.module.modules.ChunkAnimationsModule;
import dev.riserecode.module.modules.NoRenderModule;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.render.ChunkMotionTracker;
import dev.riserecode.render.ChunkTerrainRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

/** Real terrain/shader draws plus fixed-progress screenshots in a disposable world. */
public final class ChunkAnimationIntegrationTest {
    public static volatile long frozenTime = -1;
    private static int checks;
    private ChunkAnimationIntegrationTest() { }
    private static void check(boolean value, String label) { checks++; if (!value) { throw new AssertionError(label); } }
    private static void near(float value, float expected, String label) { check(Math.abs(value - expected) < 0.0001, label + " actual=" + value); }
    private static void fixture(ClientGameTestContext c, ChunkAnimationsModule module, ChunkMotionTracker.Direction direction, double progress) {
        c.runOnClient(mc -> {
            ChunkTerrainRenderer.reset(); frozenTime = 1_000_000_000L;
            module.direction.setValue(direction); module.distance.setValue(8.0); module.duration.setValue(0.5); module.easing.setValue(ChunkMotionTracker.Curve.OUT_QUART);
            module.setEnabled(true); ChunkTerrainRenderer.loaded(6, 6); ChunkTerrainRenderer.frame(frozenTime);
            module.motion().drawable(6, 6); frozenTime += (long)(progress * 500_000_000L); ChunkTerrainRenderer.frame(frozenTime);
        });
        c.waitTicks(4);
    }
    public static void run(ClientGameTestContext c) {
        c.runOnClient(mc -> { mc.options.pauseOnLostFocus = false; mc.options.enableVsync().set(false); });
        var modules = Client.INSTANCE.getModuleManager(); var chunks = modules.getByClass(ChunkAnimationsModule.class).orElseThrow();
        var noRender = modules.getByClass(NoRenderModule.class).orElseThrow(); var ambience = modules.getByClass(AmbienceModule.class).orElseThrow();
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); c.waitTicks(8);
            c.runOnClient(mc -> { modules.disableAll(); mc.options.renderDistance().set(8); });
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("gamerule minecraft:advance_time false");
            world.getServer().runCommand("time set noon"); world.getServer().runCommand("weather clear");
            world.getServer().runCommand("forceload add 96 96 111 111");
            world.getServer().runOnServer(server -> server.overworld().getChunk(6,6));
            c.waitTicks(12);
            world.getServer().runCommand("tp @a 104.5 79.5 88.5 0 25");
            c.waitFor(mc -> mc.player.position().distanceToSqr(104.5,79.5,88.5)<1&&mc.level.hasChunk(6,6),600);
            world.getServer().runCommand("fill 96 70 96 111 70 111 minecraft:stone");
            world.getServer().runCommand("fill 97 71 101 103 72 109 minecraft:water");
            world.getServer().runCommand("fill 106 71 103 110 74 107 minecraft:glass");
            world.getServer().runCommand("setblock 105 71 101 minecraft:chest");
            c.waitFor(mc -> mc.level.getBlockState(new net.minecraft.core.BlockPos(96,70,96)).is(net.minecraft.world.level.block.Blocks.STONE),600);
            world.getServer().runCommand("forceload remove 96 96 111 111");
            c.runOnClient(mc->mc.levelExtractor.allChanged());
            c.waitFor(mc->fixtureReady(),600);c.waitTicks(30);
            c.runOnClient(mc -> mc.gui.setScreen(null));
            check(!chunks.isEnabled(), "Movement starts disabled");
            c.runOnClient(mc -> { noRender.allFog.setValue(false); noRender.chunkFade.setValue(true); noRender.setEnabled(true); }); c.waitTicks(3);
            check(c.computeOnClient(mc -> ChunkTerrainRenderer.suppressFade()), "Native chunk fade suppression active");
            c.runOnClient(mc -> noRender.setEnabled(false)); c.waitTicks(3);
            check(!c.computeOnClient(mc -> ChunkTerrainRenderer.suppressFade()), "Native fade restored");
            c.takeScreenshot("chunks-native");
            for (var direction : ChunkMotionTracker.Direction.values()) {
                fixture(c, chunks, direction, 0);
                near(c.computeOnClient(mc -> ChunkTerrainRenderer.offset(6, 6)), direction == ChunkMotionTracker.Direction.UP ? -8 : 8, "Initial direction " + direction);
                check(c.computeOnClient(mc -> ChunkTerrainRenderer.suppressFade()), "Movement suppresses fade");
                c.takeScreenshot("chunks-" + direction.name().toLowerCase(java.util.Locale.ROOT) + "-start");
                c.runOnClient(mc -> frozenTime += 125_000_000L); c.waitTicks(4);
                near(c.computeOnClient(mc -> ChunkTerrainRenderer.offset(6, 6)), direction == ChunkMotionTracker.Direction.UP ? -2.53125f : 2.53125f, "Quarter progress " + direction);
                c.takeScreenshot("chunks-" + direction.name().toLowerCase(java.util.Locale.ROOT) + "-quarter");
                long starts = chunks.motion().starts();
                world.getServer().runCommand("setblock 109 75 105 minecraft:gold_block"); c.waitTicks(5);
                check(chunks.motion().starts() == starts, "Mesh rebuild does not restart");
                world.getServer().runCommand("setblock 109 75 105 minecraft:air"); c.waitTicks(5);
                c.runOnClient(mc -> frozenTime += 375_000_000L); c.waitTicks(5);
                near(c.computeOnClient(mc -> ChunkTerrainRenderer.offset(6, 6)), 0, "Completed position " + direction);
                c.takeScreenshot("chunks-" + direction.name().toLowerCase(java.util.Locale.ROOT) + "-complete");
            }
            check(FabricLoader.getInstance().isModLoaded("sodium") ? ChunkTerrainRenderer.sodiumDraws() > 10 : ChunkTerrainRenderer.vanillaDraws() > 10, "Renderer adapter executed on real terrain: vanilla="+ChunkTerrainRenderer.vanillaDraws()+", sodium="+ChunkTerrainRenderer.sodiumDraws());
            fixture(c, chunks, ChunkMotionTracker.Direction.UP, 0.25);
            c.runOnClient(mc -> ambience.setEnabled(true)); c.waitTicks(6); c.takeScreenshot("chunks-ambience-water");
            check(c.computeOnClient(mc -> AmbienceRenderer.waterActive()) && AmbienceRenderer.waterDraws() > 0, "Ambience composes with motion");
            c.runOnClient(mc -> chunks.setEnabled(false)); c.waitTicks(3);
            check(!c.computeOnClient(mc -> ChunkTerrainRenderer.motionActive()) && !c.computeOnClient(mc -> ChunkTerrainRenderer.suppressFade()), "Disable immediately restores terrain");
            if (Boolean.getBoolean("riserecode.withShaderPack")) {
                c.runOnClient(mc -> shaderPack(true)); c.waitTicks(12);
                for (var direction : ChunkMotionTracker.Direction.values()) {
                    fixture(c, chunks, direction, 0.25);
                    check(c.computeOnClient(mc -> AmbienceRenderer.shaderPackActive() && ChunkTerrainRenderer.motionActive()), "Iris keeps movement active " + direction);
                    near(c.computeOnClient(mc -> ChunkTerrainRenderer.offset(6, 6)), direction == ChunkMotionTracker.Direction.UP ? -2.53125f : 2.53125f, "Iris offset " + direction);
                    c.takeScreenshot("chunks-iris-" + direction.name().toLowerCase(java.util.Locale.ROOT));
                }
                check(ChunkTerrainRenderer.irisDraws() > 10, "Iris terrain and shadow binding executed");
                check(!c.computeOnClient(mc -> AmbienceRenderer.waterActive()), "Pack retains its own water lighting");
                c.runOnClient(mc -> shaderPack(false)); c.waitTicks(10);
                check(c.computeOnClient(mc -> ChunkTerrainRenderer.motionActive() && AmbienceRenderer.waterActive()), "Shader toggle restores native adapters");
            }
            c.runOnClient(mc -> { frozenTime = -1; chunks.setEnabled(true); ChunkTerrainRenderer.reset(); });
            long starts = chunks.motion().starts();
            world.getServer().runCommand("tp @a 520.5 79.5 504.5 0 25");
            c.waitFor(mc -> mc.level.getChunkSource().getChunk(32, 32, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) != null, 400);
            world.getServer().runCommand("fill 512 70 512 527 70 527 minecraft:stone");
            c.waitFor(mc -> chunks.motion().starts() > starts, 400);
            check(true, "Actual newly streamed terrain starts animation"); c.waitTicks(12);
            world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 80 0");
            c.waitFor(mc -> mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER), 400); c.waitTicks(8);
            check(c.computeOnClient(mc -> ChunkTerrainRenderer.offset(32,32)) == 0, "Dimension change clears old streamed column");
            c.runOnClient(mc -> ChunkTerrainRenderer.loaded(10000, 10000)); c.waitTick();
            check(c.computeOnClient(mc -> chunks.motion().moving(10000, 10000)), "Pre-reload flight present");
            var reload = c.computeOnClient(mc -> mc.reloadResourcePacks()); c.waitFor(mc -> reload.isDone(), 600); c.waitTicks(8);
            check(!c.computeOnClient(mc -> chunks.motion().moving(10000, 10000)), "Resource reload clears previous flights while accepting new chunks");
        } finally {
            frozenTime = -1;
        }
        c.waitTicks(4);
        check(!c.computeOnClient(mc -> ChunkTerrainRenderer.motionActive()) && chunks.motion().pendingCount() == 0, "Disconnect cleanup");
        c.runOnClient(mc -> modules.disableAll());
        Client.INSTANCE.getLogger().info("Chunk animation GPU checks passed ({} assertions)", checks);
    }
    private static boolean fixtureReady(){
        if(!FabricLoader.getInstance().isModLoaded("sodium")){return net.minecraft.client.Minecraft.getInstance().levelRenderer.isSectionCompiledAndVisible(new net.minecraft.core.BlockPos(96,70,96));}
        try{var type=Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");var renderer=type.getMethod("instance").invoke(null);return (boolean)type.getMethod("isSectionReady",int.class,int.class,int.class).invoke(renderer,6,4,6);}
        catch(ReflectiveOperationException failure){throw new AssertionError("Sodium fixture readiness",failure);}
    }
    private static void shaderPack(boolean enabled) {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris"); Object config = iris.getMethod("getIrisConfig").invoke(null);
            config.getClass().getMethod("setShaderPackName", String.class).invoke(config, "ComplementaryUnbound_r5.9.3.zip");
            config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, enabled);
            config.getClass().getMethod("save").invoke(config); iris.getMethod("reload").invoke(null);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Iris pack switch", failure); }
    }
}
