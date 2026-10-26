package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.menu.MenuTransition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Button-local entrance and press poses; native panel geometry and focus remain stationary. */
@Mixin(AbstractWidget.class)
public abstract class MenuWidgetMotionMixin {
    @WrapMethod(method="extractRenderState")
    private void risecode$pointer(GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        AbstractWidget widget=(AbstractWidget)(Object)this;
        if(widget instanceof net.minecraft.client.gui.components.AbstractButton){dev.riserecode.ui.menu.NativeButtonLayout.compact(widget);}
        original.call(graphics,(int)Math.round(MenuTheme.localX(widget,mx)),(int)Math.round(MenuTheme.localY(widget,my)),delta);
    }
    @WrapMethod(method="isMouseOver")
    private boolean risecode$hit(double x,double y,Operation<Boolean> original) {
        AbstractWidget widget=(AbstractWidget)(Object)this;
        return original.call(MenuTheme.localX(widget,x),MenuTheme.localY(widget,y));
    }
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;containsPointInScissor(II)Z"))
    private boolean risecode$clip(GuiGraphicsExtractor graphics,int x,int y,Operation<Boolean> original) {
        AbstractWidget widget=(AbstractWidget)(Object)this;double scale=MenuTheme.widgetScale(widget);
        return original.call(graphics,(int)Math.round(widget.getX()+widget.getWidth()/2.0+(x-widget.getX()-widget.getWidth()/2.0)*scale),(int)Math.round(widget.getY()+widget.getHeight()/2.0+(y-widget.getY()-widget.getHeight()/2.0)*scale));
    }
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void risecode$pose(AbstractWidget widget,GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        double alpha=widget instanceof net.minecraft.client.gui.components.AbstractButton?MenuTheme.buttonEntranceOpacity():1;
        MenuTransition scope=MenuTransition.widget(graphics,widget.getX()+widget.getWidth()/2f,widget.getY()+widget.getHeight()/2f,MenuTheme.widgetScale(widget),alpha);
        try { original.call(widget,graphics,mx,my,delta); } finally { scope.close(); }
    }
}
