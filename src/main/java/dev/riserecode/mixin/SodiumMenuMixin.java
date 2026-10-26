package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium and Sodium Extra share these drawing helpers; input and narration stay native. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget")
public abstract class SodiumMenuMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="isMouseOver")
    private boolean risecode$hit(double x,double y,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        var widget=(net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget)(Object)this;var b=widget.getDimensions();double scale=MenuTheme.customScale(this);
        return original.call(b.getCenterX()+(x-b.getCenterX())/scale,b.getCenterY()+(y-b.getCenterY())/scale);
    }
    @Inject(method = "drawRect", at = @At("HEAD"), cancellable = true, require = 0)
    private void riserecode$surface(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color,CallbackInfo ci) {
        if (MenuTheme.enabled()) {
            var widget=(net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget)(Object)this;
            if(x1-x0>20 && y1-y0>10) { RenderUtil.drawButton(g,x0,y0,x1-x0,y1-y0,MenuTheme.hover(widget,widget.isHovered()||widget.isFocused()),(color>>>24)/255.0); }
            else { RenderUtil.drawPaintRect(g,x0,y0,x1-x0,y1-y0,4,Theme.paint("Surface Color"),(color>>>24)/255.0); }
            ci.cancel();
        }
    }
    @Inject(method = "drawString(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/lang/String;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void riserecode$text(GuiGraphicsExtractor g,String text,int x,int y,int color,CallbackInfo ci) {
        if (MenuTheme.enabled()) { RenderUtil.text(g,net.minecraft.client.Minecraft.getInstance().font,text,x,y,dev.riserecode.ui.component.ButtonMetrics.textScale(dev.riserecode.ui.component.ButtonMetrics.HEIGHT),(color&0xFFFFFF)==0xFFFFFF?Theme.opacity(Theme.TEXT,(color>>>24)/255.0):color); ci.cancel(); }
    }
}
