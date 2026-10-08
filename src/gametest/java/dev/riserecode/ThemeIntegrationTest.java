package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Real keyboard/mouse, native STB glyphs, live profiles and GPU blur on Minecraft 26.2/OpenGL. */
public final class ThemeIntegrationTest {
    private ThemeIntegrationTest() { }
    public static void verify(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getThemeManager().reset();
            Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(true);
        });
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitForScreen(ClickGui.class);
        context.waitFor(minecraft -> gui().isOpen(), 3000);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.APPEARANCE)); context.waitTicks(6);
        int originalWidth = context.computeOnClient(minecraft -> RenderUtil.textWidth(minecraft.font, "AVATAR", false));
        reveal(context, "Font Scale"); click(context, "Font Scale", 0.65, 0.7);
        check(context.computeOnClient(minecraft -> themes().number("Font Scale") > 1.5
                && RenderUtil.textWidth(minecraft.font, "AVATAR", false) > originalWidth), "real font slider immediately changes glyph metrics");
        number(context, "Font Scale", 1); reveal(context, "Font Selection");
        for (ThemeManager.Typeface choice : new ThemeManager.Typeface[] {ThemeManager.Typeface.QRAFTYS, ThemeManager.Typeface.MINECRAFTIA,
                ThemeManager.Typeface.MONTSERRAT, ThemeManager.Typeface.INTER, ThemeManager.Typeface.GOOGLE_SANS}) {
            click(context, "Font Selection", 0.3, 0.5); context.waitTicks(4);
            check(context.computeOnClient(minecraft -> themes().choice("Font Selection", ThemeManager.Typeface.class) == choice), "font picker " + choice);
            if (choice == ThemeManager.Typeface.QRAFTYS && CustomTextRenderer.available(choice)) {
                context.runOnClient(minecraft -> {
                    check(CustomTextRenderer.status().contains("Qraftys resource-pack glyphs"), "real bitmap pack instead of uppercase fallback");
                    try (ZipFile zip = new ZipFile(minecraft.gameDirectory.toPath().resolve("resourcepacks/qraftys-capitalized-font.zip").toFile());
                            var stream = zip.getInputStream(zip.getEntry("assets/minecraft/textures/font/ascii.png"));
                            NativeImage image = NativeImage.read(stream)) {
                        int expected = 0;
                        for (int code : "risecode".codePoints().toArray()) {
                            int extent = 0;
                            for (int x = 0; x < 8; x++) {
                                for (int y = 0; y < 8; y++) { if ((image.getPixel(code % 16 * 8 + x, code / 16 * 8 + y) >>> 24) != 0) { extent = x + 1; } }
                            }
                            expected += extent + 1;
                        }
                        check(RenderUtil.textWidth(minecraft.font, "risecode", false) == expected, "original small-cap glyph advances");
                    } catch (java.io.IOException failure) { throw new AssertionError("Qraftys pack read", failure); }
                    minecraft.getResourcePackRepository().reload();
                    var pack = minecraft.getResourcePackRepository().getPack("file/qraftys-capitalized-font.zip");
                    check(pack != null && pack.getCompatibility().isCompatible(), "local resource pack accepted by Minecraft 26.2");
                });
                context.takeScreenshot("risecode-font-qraftys");
            } else if (choice == ThemeManager.Typeface.MONTSERRAT || choice == ThemeManager.Typeface.INTER
                    || choice == ThemeManager.Typeface.MINECRAFTIA && CustomTextRenderer.available(choice)) {
                check(CustomTextRenderer.available(choice), "bundled face " + choice);
                context.takeScreenshot("risecode-font-" + choice.name().toLowerCase(Locale.ROOT));
            } else if (!CustomTextRenderer.available(choice)) {
                check(CustomTextRenderer.status().contains("using Inter"), "missing optional face is labelled honestly");
            }
        }
        UIComponent.Bounds reloadButton = context.computeOnClient(minecraft -> gui().getThemeEditor().getActions().getLast().getBounds());
        move(context, reloadButton.x() + reloadButton.width() / 2, reloadButton.y() + reloadButton.height() / 2);
        context.getInput().pressMouse(0); context.waitTicks(4);
        check(CustomTextRenderer.status().contains("STB 8× filled glyphs"), "font atlases reload without a client restart");
        reveal(context, "Primary Accent");
        context.runOnClient(mc -> ((dev.riserecode.ui.component.setting.ColorPicker) gui().getThemeEditor().getControl("Primary Accent")).setExpanded(true));
        context.waitTicks(10);
        UIComponent picker = context.computeOnClient(minecraft -> gui().getThemeEditor().getControl("Primary Accent"));
        int oldColor = themes().color("Primary Accent");
        var b = context.computeOnClient(minecraft -> picker.getBounds());
        move(context, b.x() + (b.width() - 20) * 0.8, b.y() + 42 + 48 * 0.2);
        context.getInput().pressMouse(0);
        check(context.computeOnClient(minecraft -> themes().color("Primary Accent") != oldColor), "HSV square edits the live accent");
        check(picker instanceof ColorPicker, "HSV control adapter"); context.takeScreenshot("risecode-theme-colors");
        reveal(context, "Border Thickness"); click(context, "Border Thickness", 0.01, 0.8);
        check(themes().number("Border Thickness") == 0, "zero border is supported");
        number(context, "Corner Radius", 0); context.takeScreenshot("risecode-theme-sharp");
        number(context, "Corner Radius", 16); number(context, "Border Thickness", 0.8);
        number(context, "Panel Opacity", 0.7); number(context, "GUI Blur Radius", 18); number(context, "World Blur Radius", 26);
        reveal(context, "World Blur"); click(context, "World Blur", 0.3, 0.5);
        check(themes().bool("World Blur"), "world blur toggle");
        context.waitTicks(8); context.takeScreenshot("risecode-theme-world-blur");
        context.runOnClient(minecraft -> check(themes().saveProfile("ThemeTest"), "theme profile save"));
        number(context, "Panel Opacity", 0.95);
        context.runOnClient(minecraft -> check(themes().loadProfile("themetest"), "theme profile load while screen is open"));
        check(themes().number("Panel Opacity") == 0.7 && gui().getPage() == ClickGui.Page.APPEARANCE, "profile updates live controls without replacing screen");
        int allocations = BlurUtil.getAllocationCount(); context.waitTicks(10);
        check(BlurUtil.getAllocationCount() == allocations, "targets reused across frames");
        check(BlurUtil.getTargetWidth() == 1024 && BlurUtil.getTargetHeight() == 576, "half-resolution targets at 2048x1152");
        context.getInput().resizeWindow(1920, 1080); context.waitTicks(6);
        check(BlurUtil.getTargetWidth() == 960 && BlurUtil.getTargetHeight() == 540 && BlurUtil.getAllocationCount() > allocations, "targets resized once with framebuffer");
        long start = System.nanoTime(); long firstFrame = BlurUtil.getRenderedFrames();
        context.waitTicks(20);
        long elapsed = System.nanoTime() - start, rendered = BlurUtil.getRenderedFrames() - firstFrame;
        Client.INSTANCE.getLogger().info("Phase 7 observed blur frames/sec={}, last CPU submission={}ms (not a GPU timing)", rendered * 1e9 / elapsed, BlurUtil.getLastSubmitMilliseconds());
        AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
        context.runOnClient(minecraft -> reload.set(minecraft.reloadResourcePacks()));
        context.waitFor(minecraft -> reload.get().isDone(), 15_000);
        check(!reload.get().isCompletedExceptionally(), "resource reload with live font atlases and custom pipelines");
        context.waitFor(minecraft -> minecraft.gui.overlay() == null, 15_000);
        context.runOnClient(minecraft -> {
            themes().reset(); themes().changed();
        });
        context.getInput().resizeWindow(2048, 1152); context.waitTicks(6);
        reveal(context, "Animation Speed"); click(context, "Animation Speed", 0.7, 0.7);
        check(themes().number("Animation Speed") > 2, "animation speed slider");
        reveal(context, "Easing Curve"); click(context, "Easing Curve", 0.25, 0.5); context.waitTicks(5);
        reveal(context, "Font Selection"); context.waitTicks(6); context.takeScreenshot("risecode-phase7-themes");
        context.runOnClient(minecraft -> {
            themes().reset();
            ((BooleanSetting) themes().getSetting("World Blur")).setValue(true);
            ((DoubleSetting) themes().getSetting("World Blur Radius")).setValue(18.0);
            gui().selectPage(ClickGui.Page.SEARCH);
        });
        context.waitTicks(6); context.takeScreenshot("risecode-phase7-client");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(minecraft -> minecraft.gui.screen() == null, 3000);
        context.runOnClient(minecraft -> setHudBackground(ThemeManager.HudBackground.BLUR_BOX)); context.waitTicks(6);
        long hudFrames = BlurUtil.getRenderedFrames(); context.waitTicks(5);
        check(BlurUtil.getRenderedFrames() > hudFrames, "blur boxes capture the world while the menu is closed");
        context.takeScreenshot("risecode-hud-blur-box");
        context.runOnClient(minecraft -> setHudBackground(ThemeManager.HudBackground.NONE)); context.waitTicks(6);
        hudFrames = BlurUtil.getRenderedFrames(); context.waitTicks(5);
        check(BlurUtil.getRenderedFrames() == hudFrames, "no blur work when HUD and menu no longer request it");
        context.takeScreenshot("risecode-hud-no-background");
        context.runOnClient(minecraft -> {
            themes().reset(); Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(false); themes().save();
        });
    }
    private static ThemeManager themes() { return Client.INSTANCE.getThemeManager(); }
    private static void setHudBackground(ThemeManager.HudBackground value) {
        @SuppressWarnings("unchecked") EnumSetting<ThemeManager.HudBackground> setting = (EnumSetting<ThemeManager.HudBackground>) themes().getSetting("HUD Background");
        setting.setValue(value); themes().changed();
    }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static void number(ClientGameTestContext context, String name, double value) {
        context.runOnClient(minecraft -> { ((DoubleSetting) themes().getSetting(name)).setValue(value); themes().changed(); }); context.waitTicks(3);
    }
    private static void reveal(ClientGameTestContext context, String name) {
        context.runOnClient(minecraft -> gui().getThemeEditor().reveal(name)); context.waitTicks(7);
    }
    private static void click(ClientGameTestContext context, String name, double px, double py) {
        UIComponent.Bounds b = context.computeOnClient(minecraft -> gui().getThemeEditor().getControl(name).getBounds());
        move(context, b.x() + b.width() * px, b.y() + b.height() * py); context.getInput().pressMouse(0);
    }
    private static void move(ClientGameTestContext context, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> new double[] {
                x * gui().getCanvasScale() * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                y * gui().getCanvasScale() * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()});
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTick();
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
