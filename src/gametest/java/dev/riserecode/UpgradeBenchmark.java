package dev.riserecode;

import dev.riserecode.ui.clickgui.ClickGui;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Fixed scene and full-frame measurements; test mod only. */
public final class UpgradeBenchmark {
    private static final List<Double> FRAMES = new ArrayList<>();
    private static boolean recording;
    private static long previous;
    private UpgradeBenchmark() { }
    public static void frame() {
        long now = System.nanoTime();
        if (recording && previous != 0 && FRAMES.size() < 600) { FRAMES.add((now - previous) / 1e6); }
        previous = now;
    }
    public static void run(ClientGameTestContext context) {
        context.runOnClient(mc -> { Client.INSTANCE.getThemeManager().reset(); mc.options.enableVsync().set(false); mc.options.framerateLimit().set(260); mc.options.bobView().set(false);
            ((dev.riserecode.setting.BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("World Blur")).setValue(true);
            @SuppressWarnings("unchecked") var gradient=(dev.riserecode.setting.EnumSetting<dev.riserecode.util.ColorUtil.GradientMode>)Client.INSTANCE.getThemeManager().getSetting("Gradient Mode");gradient.setValue(dev.riserecode.util.ColorUtil.GradientMode.SINE); });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -16 62 -16 16 63 16 minecraft:stone");
            world.getServer().runCommand("fill 2 63 2 10 63 10 minecraft:water");
            world.getServer().runCommand("fill -8 64 4 -4 68 8 minecraft:stone_bricks");
            world.getServer().runCommand("tp @a 0.5 67 -12.5 0 10");
            context.waitTicks(5);context.runOnClient(mc->{mc.player.setPos(0.5,67,-12.5);mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setYRot(0);mc.player.setXRot(10);});
            world.getConnection().waitForChunksRender(); context.waitTicks(60);
            measure(context, "closed");
            context.runOnClient(mc -> { var gui = Client.INSTANCE.getClickGui(); gui.open(null); gui.selectPage(ClickGui.Page.RENDER); });
            context.waitTicks(30); context.takeScreenshot("benchmark-interface"); measure(context, "open");
            context.setScreen(FidelityScreen::new);context.waitTicks(12);context.takeScreenshot("benchmark-fidelity");
        }
    }
    private static void measure(ClientGameTestContext context, String state) {
        context.runOnClient(mc -> { FRAMES.clear(); previous = 0; recording = true; });
        context.waitFor(mc -> FRAMES.size() >= 600, 1200);
        var values = context.computeOnClient(mc -> { recording = false; return FRAMES.stream().sorted().toList(); });
        double median = values.get(300), p95 = values.get(570);
        String row = "{\"state\":\"" + state + "\",\"medianMs\":" + median + ",\"p95Ms\":" + p95 + ",\"fps\":" + 1000 / median + "}\n";
        try {
            Path directory = Path.of("../../../upgrade-evidence"); Files.createDirectories(directory);
            Files.writeString(directory.resolve("benchmark-" + System.getProperty("riserecode.benchmarkLabel", "baseline") + ".jsonl"), row, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        Client.INSTANCE.getLogger().info("Benchmark {}", row.trim());
    }
}
