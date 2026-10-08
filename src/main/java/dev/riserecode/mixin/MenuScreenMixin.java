package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuBackdrop;
import dev.riserecode.ui.menu.MenuTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MenuScreenMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void riserecode$background(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        if (MenuTheme.photo((Screen) (Object) this)) { MenuBackdrop.draw(graphics); callback.cancel(); }
    }
    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void riserecode$panorama(GuiGraphicsExtractor graphics, float delta, CallbackInfo callback) {
        if (MenuTheme.photo((Screen) (Object) this)) { MenuBackdrop.draw(graphics); callback.cancel(); }
    }
}
