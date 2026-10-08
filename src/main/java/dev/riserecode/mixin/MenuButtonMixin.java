package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MenuButtonMixin {
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void riserecode$button(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        var widget = (AbstractButton) (Object) this;
        if (MenuTheme.enabled() && widget instanceof net.minecraft.client.gui.components.SpriteIconButton && widget.getWidth() <= 32) {
            MenuTheme.iconButton(graphics, widget); callback.cancel(); return;
        }
        if (MenuTheme.enabled() && !widget.getMessage().getString().isBlank() && !(widget instanceof net.minecraft.client.gui.components.SpriteIconButton)) {
            MenuTheme.button(graphics, widget); if (widget.isHovered()) { graphics.requestCursor(widget.isActive() ? com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND : com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED); } callback.cancel();
        }
    }
    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void riserecode$iconFrame(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        if (MenuTheme.enabled()) {
            var widget = (AbstractButton) (Object) this;
            dev.riserecode.util.RenderUtil.drawSurfaceTransition(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, MenuTheme.hover(widget), widget.getAlpha()); callback.cancel();
        }
    }
}
