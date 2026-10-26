package dev.riserecode;

import dev.riserecode.config.SettingCodecs;
import dev.riserecode.event.BlockOutlineEvent;
import dev.riserecode.module.modules.BlockOverlayModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** Pixel checks catch shader overrides that silently drop otherwise valid line submissions. */
public final class BlockOverlayIntegrationTest {
    private static int checks;
    private BlockOverlayIntegrationTest() { }

    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        c.runOnClient(mc -> {
            Client.INSTANCE.getModuleManager().disableAll();
            Client.INSTANCE.getThemeManager().reset();
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(120);
        });
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("fill -5 64 -5 5 64 6 stone");
            world.getServer().runCommand("setblock 0 66 0 stone");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 2.5 65 3.5 145 0");
            c.waitTicks(10);
            c.runOnClient(mc -> {
                mc.player.getAbilities().flying = true;
                mc.player.onUpdateAbilities();
                overlay().setEnabled(true);
                setting("OverlayEnabled", false);
                setting("Smooth Switching", false);
                setting("Pop Animation", false);
                setting("OutlineColor", 0xFFFF00FF);
                setting("OutlineWidth", 5.0);
                setting("OutlineOpacity", 1.0);
                setting("See-through Outline", false);
            });
            c.getInput().lookAt(new BlockPos(0, 66, 0));
            c.waitFor(mc -> mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(new BlockPos(0, 66, 0)), 160);
            verify(c, "native");
            if (Boolean.getBoolean("riserecode.withShaderPack")) {
                check(FabricLoader.getInstance().isModLoaded("iris"), "shader run actually loads Iris");
                c.runOnClient(mc -> shaders(true));
                c.waitFor(mc -> net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse() && mc.gui.overlay() == null, 600);
                verify(c, "shaders");
                var reload = c.computeOnClient(mc -> mc.reloadResourcePacks());
                c.waitFor(mc -> reload.isDone() && mc.gui.overlay() == null, 600);
                shot(c, "shaders-reload");
                check(magenta("shaders-reload") > 300, "outline survives resource reload with shaders");
                c.runOnClient(mc -> shaders(false));
                c.waitFor(mc -> !net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse() && mc.gui.overlay() == null, 600);
                verify(c, "shaders-disabled");
            }
            c.runOnClient(mc -> {
                overlay().setEnabled(false);
                check(!Client.INSTANCE.getEventBus().post(new BlockOutlineEvent()).isCancelled(), "disable restores vanilla block outline");
            });
            shot(c, "disabled");
            check(magenta("disabled") < 30, "disable clears rendered custom edges");
        }
        Client.INSTANCE.getLogger().info("Block overlay pixel checks passed ({} assertions).", checks);
    }

    private static void verify(ClientGameTestContext c, String mode) {
        c.runOnClient(mc -> { setting("OutlineEnabled", false); setting("See-through Outline", false); setting("Outline Style", "FULL"); });
        shot(c, mode + "-off");
        check(magenta(mode + "-off") < 30, mode + ": no outline pixels when disabled");
        c.runOnClient(mc -> setting("OutlineEnabled", true));
        shot(c, mode + "-full");
        int full = magenta(mode + "-full");
        check(full > 300, mode + ": visible full outline (" + full + " pixels)");
        check(Math.abs(background(mode + "-full") - background(mode + "-off")) < 5, mode + ": compositing preserves the world outside the outline");
        c.runOnClient(mc -> setting("OutlineWidth", 1.0));
        shot(c, mode + "-thin");
        int thin = magenta(mode + "-thin");
        check(thin > 40 && thin < full * .7, mode + ": configured width changes rendered edges (" + thin + " vs " + full + ")");
        c.runOnClient(mc -> { setting("OutlineWidth", 5.0); setting("See-through Outline", true); });
        shot(c, mode + "-through");
        check(magenta(mode + "-through") > full * 1.1, mode + ": see-through reveals hidden edges");
        c.runOnClient(mc -> { setting("OutlineColor", 0x86171717); setting("OutlineWidth", 9.5); });
        shot(c, mode + "-dark");
        check(darkened(mode) > 150, mode + ": translucent dark outline remains visible");
        c.runOnClient(mc -> { setting("OutlineColor", 0xFFFF00FF); setting("OutlineWidth", 5.0); });
        c.runOnClient(mc -> { setting("See-through Outline", false); setting("Outline Style", "CORNERS"); setting("Edge Gap", 60.0); });
        shot(c, mode + "-corners");
        int corners = magenta(mode + "-corners");
        check(corners > 100 && corners < full * .8, mode + ": corner gaps remain visible");
        c.runOnClient(mc -> { setting("Outline Style", "FULL"); setting("OverlayEnabled", true); });
        shot(c, mode + "-fill");
        check(magenta(mode + "-fill") > 300, mode + ": fill and outline coexist");
        check(filled(mode) > 300, mode + ": translucent fill reaches the final image");
        c.runOnClient(mc -> setting("OverlayEnabled", false));
    }

    private static void shot(ClientGameTestContext c, String name) { c.waitTicks(15); c.takeScreenshot("block-overlay-" + name); }
    private static int magenta(String name) {
        var image = image(name);
        int count = 0;
        for (int y = image.getHeight() / 4; y < image.getHeight() * 3 / 4; y++) {
            for (int x = image.getWidth() / 4; x < image.getWidth() * 3 / 4; x++) {
                if (magentaPixel(image.getRGB(x, y))) { count++; }
            }
        }
        return count;
    }
    private static boolean magentaPixel(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r > 140 && b > 140 && g < 110 && r > g * 1.7 && b > g * 1.7;
    }
    private static int darkened(String mode) {
        var off = image(mode + "-off"); var full = image(mode + "-full"); var dark = image(mode + "-dark");
        int count = 0;
        for (int y = off.getHeight() / 4; y < off.getHeight() * 3 / 4; y++) {
            for (int x = off.getWidth() / 4; x < off.getWidth() * 3 / 4; x++) {
                if (magentaPixel(full.getRGB(x, y)) && (off.getRGB(x, y) >> 8 & 255) - (dark.getRGB(x, y) >> 8 & 255) > 20) { count++; }
            }
        }
        return count;
    }
    private static int filled(String mode) {
        var off = image(mode + "-off"); var fill = image(mode + "-fill");
        int count = 0;
        for (int y = off.getHeight() / 4; y < off.getHeight() * 3 / 4; y++) {
            for (int x = off.getWidth() / 4; x < off.getWidth() * 3 / 4; x++) {
                int a = off.getRGB(x, y), b = fill.getRGB(x, y);
                if ((b & 255) - (b >> 16 & 255) > (a & 255) - (a >> 16 & 255) + 15) { count++; }
            }
        }
        return count;
    }
    private static double background(String name) {
        var image = image(name);
        long sum = 0; int count = 0;
        for (int y = image.getHeight() / 10; y < image.getHeight() / 4; y++) {
            for (int x = image.getWidth() / 4; x < image.getWidth() * 3 / 4; x++) {
                int rgb = image.getRGB(x, y);
                sum += (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255); count += 3;
            }
        }
        return sum / (double) count;
    }
    private static java.awt.image.BufferedImage image(String name) {
        try (var paths = Files.list(Path.of("screenshots"))) {
            var path = paths.filter(p -> p.getFileName().toString().endsWith("block-overlay-" + name + ".png")).findFirst().orElseThrow();
            return javax.imageio.ImageIO.read(path.toFile());
        } catch (IOException error) { throw new AssertionError("Read block overlay pixels", error); }
    }
    private static BlockOverlayModule overlay() { return Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow(); }
    private static void setting(String name, Object value) {
        SettingCodecs.apply(overlay().getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(), new com.google.gson.Gson().toJsonTree(value));
    }
    private static void shaders(boolean enabled) {
        var config = net.irisshaders.iris.Iris.getIrisConfig();
        config.setShaderPackName("ComplementaryUnbound_r5.9.3.zip");
        config.setShadersEnabled(enabled);
        try {
            if (enabled) { Files.writeString(Path.of("shaderpacks", "ComplementaryUnbound_r5.9.3.zip.txt"), "SELECT_OUTLINE=0\n"); }
            config.save(); net.irisshaders.iris.Iris.reload();
        }
        catch (IOException error) { throw new AssertionError("Toggle test shader pack", error); }
    }
    private static void check(boolean passed, String message) { checks++; if (!passed) { throw new AssertionError(message); } }
}
