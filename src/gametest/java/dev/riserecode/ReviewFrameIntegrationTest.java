package dev.riserecode;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import dev.riserecode.ui.clickgui.ClickGui;

/** Short current-build baseline, default Inter appearance, no personal configuration or assets. */
public final class ReviewFrameIntegrationTest {
    public static void run(ClientGameTestContext context){
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->{
            Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();
            mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);mc.options.renderDistance().set(6);
            var threads=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();threads.setThreadAllocatedMemoryEnabled(true);threads.setThreadCpuTimeEnabled(true);
            try{
                var metadata=new com.google.gson.JsonObject();metadata.addProperty("scope","Fixed vanilla world; renderFrame update/extract/draw/present wall/CPU/allocation excludes Fabric barriers; GPU timestamps cover GUI/blur passes only. Current baseline, no A/B improvement claim.");
                metadata.addProperty("java",System.getProperty("java.version"));metadata.addProperty("os",System.getProperty("os.name"));metadata.addProperty("processors",Runtime.getRuntime().availableProcessors());metadata.addProperty("systemLoadAverage",java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
                metadata.addProperty("device",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().toString());
                try(var lines=Files.lines(Path.of("/proc/cpuinfo"))){metadata.addProperty("cpu",lines.filter(line->line.startsWith("model name")).findFirst().orElse("unavailable"));}
                Path output=Path.of(System.getProperty("riserecode.guiEvidence"));Files.createDirectories(output);Files.writeString(output.resolve("environment.json"),metadata.toString()+"\n");Files.deleteIfExists(output.resolve("frames.jsonl"));
            }catch(java.io.IOException failure){throw new AssertionError(failure);}
        });
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -16 62 -16 16 63 16 stone");world.getServer().runCommand("fill -5 64 3 5 64 8 water");world.getServer().runCommand("tp @a 0.5 65 -8.5 0 0");context.waitTicks(40);
            GuiPerformanceTest.measure(context,"review-closed");
            context.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("");});context.waitTicks(20);
            GuiPerformanceTest.measure(context,"review-search");
            context.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var button=gui.getActivePanel().getVisibleButtons().getFirst();var bounds=button.getBounds();gui.getActivePanel().mouseClicked(bounds.centerX(),bounds.y()+12,1);});context.waitTicks(20);
            GuiPerformanceTest.measure(context,"review-expanded");context.runOnClient(mc->mc.gui.setScreen(null));
        }finally{context.runOnClient(mc->GuiPerformanceTest.closeMeasurements());}
        Client.INSTANCE.getLogger().info("Review frame baseline completed (3 scenes; 3s warmup and 10s measurement each; no A/B improvement claim)");
    }
}
