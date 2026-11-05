package dev.riserecode;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.menu.MenuBackdrop;
import dev.riserecode.ui.render.LiquidGlass;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.RenderUtil;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Real production shaders and transformed input in isolated clients; screenshots are review evidence. */
public final class LiquidGlassIntegrationTest {
    private static int checks;
    private LiquidGlassIntegrationTest() { }
    private static ThemeManager theme() { return Client.INSTANCE.getThemeManager(); }
    private static void value(String name, Object next) { ThemeManager.assign(theme().getSetting(name), next); theme().changed(); }
    private static void check(boolean result, String message) { checks++; if (!result) { throw new AssertionError(message); } }
    private static void shot(ClientGameTestContext c, String name) { c.waitTicks(12); c.takeScreenshot("liquid-glass-" + name); }
    private static void click(ClientGameTestContext c, UIComponent.Bounds b) {
        c.runOnClient(mc -> {
            var gui = Client.INSTANCE.getClickGui();
            var event = new MouseButtonEvent(b.centerX() * gui.getCanvasScale(), b.centerY() * gui.getCanvasScale(), new MouseButtonInfo(0, 0));
            gui.mouseClicked(event, false); gui.mouseReleased(event);
        });
    }
    private static void shaders(boolean enabled) {
        var config = net.irisshaders.iris.Iris.getIrisConfig();
        config.setShaderPackName(System.getProperty("riserecode.testShaderPack", "ComplementaryUnbound_r5.9.3.zip"));
        config.setShadersEnabled(enabled);
        try { config.save(); net.irisshaders.iris.Iris.reload(); }
        catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private static final class Controls extends Screen {
        private boolean on = true;
        private final DoubleSetting amount = new DoubleSetting("Refraction", "", .5, 0, 1);
        private final ToggleSwitch toggle = new ToggleSwitch("Enabled", () -> on, next -> on = next);
        private final Slider slider = new Slider(amount, () -> { });
        Controls() { super(Component.literal("Glass controls")); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
            MenuBackdrop.draw(g);
            double x = (width - 260) / 2.0, y = (height - 150) / 2.0;
            MenuBackdrop.drawBlur(g, x, y, 260, 150, 14, .9);
            RenderUtil.drawWindow(g, x, y, 260, 150, 1);
            RenderUtil.text(g, font, "Liquid Glass", x + 120, y + 15, 1.1, Theme.TEXT);
            toggle.setBounds(x + 120, y + 45, 125, 18); toggle.render(g, font, mx, my, .05, 1);
            slider.setBounds(x + 120, y + 78, 125, 24); slider.render(g, font, mx, my, .05, 1);
            RenderUtil.drawButton(g, x + 120, y + 115, 125, 22, .5, 1);
            RenderUtil.text(g, font, "Apply", x + 165, y + 122, .85, Theme.TEXT);
        }
    }
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        c.runOnClient(mc -> {
            check(Client.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"), "production classes load from the packaged release JAR");
            theme().reset(); value("Reduced Motion", true);
            mc.options.enableVsync().set(false); mc.options.framerateLimit().set(120); mc.options.pauseOnLostFocus = false;
            check(LiquidGlass.enabled(), "glass material defaults on without changing stored font or palette");
            theme().applyPreset(ThemeManager.Preset.LIQUID_GLASS);
            check(theme().choice("Font Selection", ThemeManager.Typeface.class) == ThemeManager.Typeface.INTER, "complete preset selects bundled Inter");
            mc.gui.setScreen(new Controls());
        }); shot(c, "controls");
        c.runOnClient(mc -> {
            var controls = (Controls) mc.gui.screen();
            controls.toggle.mouseClicked(controls.toggle.getBounds().centerX(), controls.toggle.getBounds().centerY(), 0);
            check(!controls.on, "capsule switch uses the full row hit target");
            var b = controls.slider.getBounds(); controls.slider.mouseClicked(b.x() + b.width() - 5, b.y() + b.height() - 5, 0);
            controls.slider.mouseReleased(0, 0, 0); check(controls.amount.getValue() == 1, "pill slider reaches maximum without changing track mapping");
            controls.on = true; controls.amount.setValue(.5);
            value("Glass Refraction", 0.0);
        }); shot(c, "refraction-off");
        c.runOnClient(mc -> {
            value("Glass Highlights", 0.0);
        }); shot(c, "optics-off");
        c.runOnClient(mc -> value("Interface Material", ThemeManager.InterfaceMaterial.CLASSIC)); shot(c, "classic-controls");
        c.runOnClient(mc -> {
            value("Interface Material", ThemeManager.InterfaceMaterial.LIQUID_GLASS); value("Glass Refraction", 1.0); value("Glass Highlights", .65);
            mc.gui.setScreen(new TitleScreen());
        }); shot(c, "title");
        c.runOnClient(mc -> { value("Mute Panorama", true); value("Living Panorama", true); });
        c.waitFor(mc -> dev.riserecode.ui.panorama.LivingPanorama.settled(), 1200);
        shot(c, "panorama");
        c.runOnClient(mc -> { check(dev.riserecode.ui.panorama.LivingPanorama.active(), "Living Panorama renders the shared glass button material"); value("Living Panorama", false); });
        c.runOnClient(mc -> { Client.INSTANCE.getClickGui().open(null); Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT); });
        shot(c, "clickgui");
        var row = c.computeOnClient(mc -> Client.INSTANCE.getClickGui().getSidebar().getItemBounds(2));
        click(c, row);
        c.runOnClient(mc -> check(Client.INSTANCE.getClickGui().getPage() == ClickGui.Page.MOVEMENT, "floating navigation hit target matches displayed row"));
        c.runOnClient(mc -> Client.INSTANCE.getClickGui().getSettingsPopup().toggle()); shot(c, "popup");
        c.runOnClient(mc -> { Client.INSTANCE.getClickGui().getSettingsPopup().close(); value("Window Width", 360.0); value("Window Height", 240.0); });
        shot(c, "small-window");
        c.runOnClient(mc -> {
            var b = Client.INSTANCE.getClickGui().getDisplayedWindowBounds();
            check(b.x() >= 0 && b.y() >= 0, "minimum window remains on screen");
            value("Interface Material", ThemeManager.InterfaceMaterial.CLASSIC);
        }); shot(c, "classic-clickgui");
        c.runOnClient(mc -> { value("Interface Material", ThemeManager.InterfaceMaterial.LIQUID_GLASS); value("Window Width", 428.0); value("Window Height", 320.0); });
        CompletableFuture<?> reload = c.computeOnClient(mc -> mc.reloadResourcePacks());
        c.waitFor(mc -> reload.isDone() && mc.gui.overlay() == null, 1200);
        c.runOnClient(mc -> check(!reload.isCompletedExceptionally(), "resource reload recompiles material and rebuilds backdrop"));
        shot(c, "reloaded");
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                check(server.getPlayerList().getPlayers().stream().allMatch(player -> ((net.minecraft.client.server.IntegratedServer) server).isSingleplayerOwner(player.nameAndId())), "disposable world has only its owner before test teardown");
                CameraFovClientTest.ownerOnlyShutdown = true;
            });
            world.getServer().runCommand("time set noon");
            c.runOnClient(mc -> { mc.gui.setScreen(null); Client.INSTANCE.getClickGui().open(null); }); shot(c, "world");
            c.runOnClient(mc -> check(BlurUtil.getRenderedFrames() > 0, "in-world material captures completed world before GUI"));
            world.getServer().runCommand("execute at @a run fill ~-16 ~-1 ~-16 ~16 ~-1 ~16 snow_block");
            world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 80");
            shot(c, "bright-world");
            if (Boolean.getBoolean("riserecode.withShaderPack")) {
                c.runOnClient(mc -> shaders(true));
                c.waitFor(mc -> dev.riserecode.render.ShaderWorldOverlay.active() && mc.gui.overlay() == null, 1200);
                shot(c, "iris-shaders");
                c.runOnClient(mc -> check(dev.riserecode.render.ShaderWorldOverlay.active(), "Iris shader pack is active while glass captures the completed world"));
                c.runOnClient(mc -> shaders(false));
                c.waitFor(mc -> !dev.riserecode.render.ShaderWorldOverlay.active() && mc.gui.overlay() == null, 1200);
            }
            c.runOnClient(mc -> value("GUI Blur Radius", 0.0)); shot(c, "blur-off");
            c.runOnClient(mc -> { value("GUI Blur Radius", 12.0); mc.gui.setScreen(null); }); c.waitTicks(5);
            long frames = c.computeOnClient(mc -> BlurUtil.getRenderedFrames()); c.waitTicks(5);
            c.runOnClient(mc -> check(BlurUtil.getRenderedFrames() == frames, "closed GUI with solid HUD performs no scene captures"));
        } finally { CameraFovClientTest.ownerOnlyShutdown = false; }
        c.runOnClient(mc -> { theme().reset(); mc.gui.setScreen(null); });
        Client.INSTANCE.getLogger().info("Liquid Glass native checks passed ({} assertions)", checks);
    }
}
