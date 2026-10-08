package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MenuListMixin {
    @Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
    private void riserecode$list(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        if (MenuTheme.enabled()) { MenuTheme.list(graphics, (AbstractSelectionList<?>) (Object) this); callback.cancel(); }
    }
    @Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
    private void riserecode$separator(GuiGraphicsExtractor graphics, CallbackInfo callback) { if (MenuTheme.enabled()) { callback.cancel(); } }
}
