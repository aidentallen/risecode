package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.StringWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractStringWidget.class)
public abstract class MenuStringMixin {
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void riserecode$heading(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        if (MenuTheme.enabled() && (Object) this instanceof StringWidget widget) {
            var font = Minecraft.getInstance().font; String text = widget.getMessage().getString();
            double scale = Math.min(1, widget.getWidth() / (double) Math.max(1, RenderUtil.textWidth(font, text, false)));
            RenderUtil.text(graphics, font, text, widget.getX() + (widget.getWidth() - RenderUtil.textWidth(font, text, false) * scale) / 2,
                widget.getY() + (widget.getHeight() - 9 * scale * Theme.number("Font Scale", 1)) / 2, scale, Theme.opacity(Theme.TEXT, widget.getAlpha())); callback.cancel();
        }
    }
}
