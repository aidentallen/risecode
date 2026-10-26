package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
public abstract class TooltipSurfaceMixin {
    @Inject(method="extractTooltipBackground",at=@At("HEAD"),cancellable=true)
    private static void risecode$surface(GuiGraphicsExtractor g,int x,int y,int width,int height,Identifier sprite,CallbackInfo ci){
        if(MenuTheme.activeMenu()){
            RenderUtil.drawDropShadow(g,x-4,y-4,width+8,height+8,5,Theme.opacity(Theme.SHADOW,dev.riserecode.ui.menu.TooltipMotion.alpha()));
            RenderUtil.drawRoundedRect(g,x-4,y-4,width+8,height+8,5,Theme.opacity(Theme.SURFACE,dev.riserecode.ui.menu.TooltipMotion.alpha()));
            RenderUtil.drawRoundedBorder(g,x-4,y-4,width+8,height+8,5,Theme.opacity(Theme.accent(),.35*dev.riserecode.ui.menu.TooltipMotion.alpha()),.6);ci.cancel();
        }
    }
}
