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
    @Inject(method = "onClick", at = @At("HEAD"))
    private void risecode$press(net.minecraft.client.input.MouseButtonEvent event, boolean twice, CallbackInfo ci) {
        if (MenuTheme.enabled()) { MenuTheme.hold((AbstractButton)(Object)this); }
    }
    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void risecode$key(net.minecraft.client.input.KeyEvent event, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        var widget = (AbstractButton)(Object)this;
        if (MenuTheme.enabled() && widget.isActive() && event.isSelection()) { MenuTheme.press(widget); }
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void riserecode$button(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        var widget = (AbstractButton) (Object) this;
        if(widget instanceof dev.riserecode.ui.panorama.PanoramaControl){return;}
        if(MenuTheme.enabled()){dev.riserecode.ui.menu.NativeButtonLayout.compact(widget);}
        if(widget instanceof dev.riserecode.ui.menu.PageGear){return;}
        if(widget instanceof net.minecraft.client.gui.components.CycleButton<?> && ((CycleButtonAccessor)widget).risecode$displayState()==net.minecraft.client.gui.components.CycleButton.DisplayState.HIDE){return;}
        if(dev.riserecode.ui.menu.ContainerTheme.active()&&(widget instanceof net.minecraft.client.gui.components.ImageButton||widget instanceof net.minecraft.client.gui.components.SpriteIconButton)){return;}
        if (MenuTheme.enabled() && widget instanceof net.minecraft.client.gui.components.SpriteIconButton && widget.getWidth() <= 32) {
            MenuTheme.iconButton(graphics, widget); callback.cancel(); return;
        }
        if (MenuTheme.enabled() && !widget.getMessage().getString().isBlank() && !(widget instanceof net.minecraft.client.gui.components.SpriteIconButton)) {
            MenuTheme.button(graphics, widget); if (widget.isHovered()) { graphics.requestCursor(widget.isActive() ? com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND : com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED); } callback.cancel();
        }
    }
    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void riserecode$iconFrame(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        if((Object)this instanceof dev.riserecode.ui.menu.PageGear||(Object)this instanceof dev.riserecode.ui.panorama.PanoramaControl){callback.cancel();return;}
        if (MenuTheme.enabled()) {
            var widget = (AbstractButton) (Object) this;
        if(MenuTheme.enabled()){dev.riserecode.ui.menu.NativeButtonLayout.compact(widget);}
            dev.riserecode.util.RenderUtil.drawButton(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), MenuTheme.hover(widget), widget.getAlpha()); callback.cancel();
        }
    }
}
