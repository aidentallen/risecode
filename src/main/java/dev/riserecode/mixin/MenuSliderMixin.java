package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class MenuSliderMixin {
    @Shadow protected double value;
    @Inject(method="onClick",at=@At("HEAD"))
    private void risecode$press(net.minecraft.client.input.MouseButtonEvent event,boolean twice,CallbackInfo ci) {
        if(MenuTheme.enabled()) { MenuTheme.press((AbstractSliderButton)(Object)this); }
    }
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void riserecode$slider(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        if (!MenuTheme.enabled()) { return; }
        var widget = (AbstractSliderButton) (Object) this; MenuTheme.button(graphics, widget);
        double left = widget.getX() + 5, top = widget.getY() + widget.getHeight() - 3, w = widget.getWidth() - 10;
        RenderUtil.drawRoundedRect(graphics, left, top, w, 1.5, 0.75, Theme.opacity(Theme.SECONDARY, 0.25));
        RenderUtil.drawPaintRect(graphics, left, top, w * value, 1.5, 0.75, Theme.paint("Primary Accent"), widget.getAlpha());
        RenderUtil.drawRoundedRect(graphics, left + value * w - 2, top - 1, 4, 4, 2, Theme.opacity(Theme.TEXT, widget.getAlpha())); callback.cancel();
    }
}
