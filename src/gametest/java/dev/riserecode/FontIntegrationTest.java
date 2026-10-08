package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Reproduces the reported 1440p/fractional-scale typography and missing-pixel-font fallback. */
public final class FontIntegrationTest {
    private FontIntegrationTest() { }
    public static void verify(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(minecraft -> {
            themes().reset();
            number("Interface Scale", 1.11); number("Corner Radius", 13);
            number("Shadow Offset X", 0.5); number("Shadow Offset Y", 0.7);
            number("Shadow Blur", 0.65); number("Shadow Opacity", 0.6);
            ((ColorSetting) themes().getSetting("Primary Accent")).setValue(-2537745);
            ((ColorSetting) themes().getSetting("Secondary Accent")).setValue(-8609803);
            ((BooleanSetting) themes().getSetting("World Blur")).setValue(true);
            number("World Blur Radius", 6); number("GUI Blur Radius", 10);
            Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(true);
            themes().changed();
        });
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.waitFor(minecraft -> Client.INSTANCE.getClickGui().isOpen(), 240);
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE);
            Client.INSTANCE.getClickGui().getThemeEditor().reveal("Font Selection");
        });
        context.waitTicks(8);
        for (ThemeManager.Typeface font : new ThemeManager.Typeface[] {
                ThemeManager.Typeface.GOOGLE_SANS, ThemeManager.Typeface.MONTSERRAT, ThemeManager.Typeface.INTER}) {
            context.runOnClient(minecraft -> {
                select(font);
                check(!CustomTextRenderer.isPixelFont(), "smooth sampling for " + font);
            });
            context.waitTicks(6);
            context.takeScreenshot("risecode-font-fix-" + font.name().toLowerCase(Locale.ROOT));
        }
        context.runOnClient(minecraft -> {
            select(ThemeManager.Typeface.MONTSERRAT);
            number("Font Scale", 1.37);
            ((BooleanSetting) themes().getSetting("Text Shadow")).setValue(false);
            themes().changed();
        });
        context.waitTicks(6);
        context.takeScreenshot("risecode-font-fix-fractional-no-shadow");
        Path localFont = themes().getFontDirectory().resolve("minecraftia.ttf");
        Path hiddenFont = localFont.resolveSibling("minecraftia.ttf.test-hidden");
        boolean installed = Files.isRegularFile(localFont);
        try {
            context.runOnClient(minecraft -> {
                try { if (installed) { Files.move(localFont, hiddenFont); } }
                catch (IOException failure) { throw new AssertionError("Hide optional test-instance font", failure); }
                CustomTextRenderer.close();
                number("Font Scale", 1);
                ((BooleanSetting) themes().getSetting("Text Shadow")).setValue(true);
                select(ThemeManager.Typeface.INTER);
                int width = RenderUtil.textWidth(minecraft.font, "AVATAR risecode", false);
                select(ThemeManager.Typeface.MINECRAFTIA);
                check(!CustomTextRenderer.available(ThemeManager.Typeface.MINECRAFTIA), "optional face absent");
                check(!CustomTextRenderer.isPixelFont(), "missing Minecraftia uses smooth Inter, not nearest sampling");
                check(RenderUtil.textWidth(minecraft.font, "AVATAR risecode", false) == width, "fallback retains actual Inter metrics");
            });
            context.waitTicks(6);
            context.takeScreenshot("risecode-font-fix-missing-minecraftia");
        } finally {
            context.runOnClient(minecraft -> {
                CustomTextRenderer.close();
                try { if (installed && Files.exists(hiddenFont)) { Files.move(hiddenFont, localFont); } }
                catch (IOException failure) { throw new AssertionError("Restore optional test-instance font", failure); }
                select(ThemeManager.Typeface.GOOGLE_SANS);
            });
        }
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitFor(minecraft -> minecraft.gui.screen() == null, 240);
        context.waitTicks(6);
        context.takeScreenshot("risecode-font-fix-hud");
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().getByName("FullBright").orElseThrow().setEnabled(false);
            themes().reset(); themes().save();
        });
        Client.INSTANCE.getLogger().info("Font regression passed: 1440p, fractional scale, 3 filled faces, shadow-off, smooth missing-Minecraftia fallback and HUD");
    }
    private static ThemeManager themes() { return Client.INSTANCE.getThemeManager(); }
    private static void number(String name, double value) { ((DoubleSetting) themes().getSetting(name)).setValue(value); }
    private static void select(ThemeManager.Typeface font) {
        @SuppressWarnings("unchecked") EnumSetting<ThemeManager.Typeface> setting =
                (EnumSetting<ThemeManager.Typeface>) themes().getSetting("Font Selection");
        setting.setValue(font); themes().changed();
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
