package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuBackdrop;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iris shader-option controls retain their value cycling, keyboard access and narration. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gui.GuiUtil")
public abstract class IrisMenuMixin {
    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true, require = 0)
    private static void riserecode$button(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean hovered,boolean disabled,CallbackInfo ci) {
        if (MenuTheme.enabled()) { RenderUtil.drawSurfaceTransition(g,x,y,w,h,5,hovered?1:0,disabled?0.5:1); ci.cancel(); }
    }
    @Inject(method = "drawPanel", at = @At("HEAD"), cancellable = true, require = 0)
    private static void riserecode$panel(GuiGraphicsExtractor g,int x,int y,int w,int h,CallbackInfo ci) {
        if (MenuTheme.enabled()) { if(MenuTheme.photo(net.minecraft.client.Minecraft.getInstance().gui.screen())){MenuBackdrop.drawBlur(g,x,y,w,h,8,0.8);} RenderUtil.drawPaintRect(g,x,y,w,h,8,Theme.paint("Surface Color"),0.75); ci.cancel(); }
    }
}
