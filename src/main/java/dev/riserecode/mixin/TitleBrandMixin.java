package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public abstract class TitleBrandMixin {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
    private void riserecode$logo(LogoRenderer renderer, GuiGraphicsExtractor graphics, int width, float opacity) {
        if (!MenuTheme.enabled()) { renderer.extractRenderState(graphics, width, opacity); return; }
        var font = Minecraft.getInstance().font; double scale = 4;
        RenderUtil.brandText(graphics, font, dev.riserecode.Client.INSTANCE.getName(), (width - RenderUtil.textWidth(font, dev.riserecode.Client.INSTANCE.getName(), false) * scale) / 2, 37, scale, opacity);
        dev.riserecode.ui.theme.BrandingLogo.drawBeside(graphics, (width - RenderUtil.textWidth(font, dev.riserecode.Client.INSTANCE.getName(), false) * scale) / 2, 37, RenderUtil.textWidth(font, dev.riserecode.Client.INSTANCE.getName(), false) * scale, opacity);
        RenderUtil.text(graphics, font, Theme.manager().text("Interface Tagline"), (width - RenderUtil.textWidth(font, Theme.manager().text("Interface Tagline"), false)) / 2, 83, 1, Theme.opacity(Theme.SECONDARY, opacity));
    }
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
    private void riserecode$splash(SplashRenderer splash, GuiGraphicsExtractor graphics, int width, Font font, float opacity) {
        if (!MenuTheme.enabled()) { splash.extractRenderState(graphics, width, font, opacity); }
    }
}
