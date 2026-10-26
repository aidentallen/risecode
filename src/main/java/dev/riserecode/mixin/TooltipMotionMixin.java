package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.menu.TooltipMotion;
import dev.riserecode.ui.animation.PopIn;
import java.util.List;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipMotionMixin {
    @WrapMethod(method="tooltip")
    private void risecode$tooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,Identifier sprite,Operation<Void> original){
        if(!MenuTheme.activeMenu()){original.call(font,components,x,y,positioner,sprite);return;}
        var g=(GuiGraphicsExtractor)(Object)this;double previous=TooltipMotion.alpha(),progress=TooltipMotion.progress(font,components),scale=PopIn.windowScale("Tooltips",progress);
        TooltipMotion.alpha(progress);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale((float)scale,(float)scale);g.pose().translate(-x,-y);
        try{original.call(font,components,x,y,positioner,sprite);}finally{g.pose().popMatrix();TooltipMotion.alpha(previous);}
    }
}
