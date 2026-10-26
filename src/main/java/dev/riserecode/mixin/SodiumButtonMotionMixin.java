package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.ui.animation.ButtonFeedback;
import dev.riserecode.ui.menu.MenuTheme;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(FlatButtonWidget.class)
public abstract class SodiumButtonMotionMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.network.chat.Component label;
    @Unique private net.caffeinemc.mods.sodium.client.util.Dim2i risecode$slot,risecode$applied;
    @Unique private final ButtonFeedback risecode$press = new ButtonFeedback();
    @Unique private boolean risecode$mouseActivating;
    @WrapMethod(method="mouseClicked")
    private boolean risecode$mouse(net.minecraft.client.input.MouseButtonEvent event,boolean twice,Operation<Boolean> original){
        risecode$mouseActivating=true;try{return original.call(event,twice);}finally{risecode$mouseActivating=false;}
    }
    @Inject(method="doAction",at=@At("HEAD"))
    private void risecode$activate(CallbackInfo ci) {if(MenuTheme.enabled()){if(risecode$mouseActivating){risecode$press.press();}else{risecode$press.activate();}}}
    @WrapMethod(method="extractRenderState")
    private void risecode$render(GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        if(!MenuTheme.enabled()) { original.call(graphics,mx,my,delta); return; }
        var widget=(FlatButtonWidget)(Object)this;var bounds=widget.getDimensions();
        if(risecode$slot==null||!bounds.equals(risecode$applied)){risecode$slot=bounds;}
        int width=(int)Math.min(risecode$slot.width(),dev.riserecode.ui.component.ButtonMetrics.width(net.minecraft.client.Minecraft.getInstance().font,label.getString()));
        int height=(int)dev.riserecode.ui.component.ButtonMetrics.HEIGHT;
        bounds=new net.caffeinemc.mods.sodium.client.util.Dim2i(risecode$slot.getCenterX()-width/2,risecode$slot.getCenterY()-height/2,width,height);
        ((SodiumWidgetAccessor)widget).risecode$dimensions(bounds);risecode$applied=bounds;
        float cx=bounds.getCenterX(),cy=bounds.getCenterY();double scale=risecode$press.scale()*MenuTheme.buttonEntranceScale();
        MenuTheme.customScale(this,scale);
        var scope=dev.riserecode.ui.menu.MenuTransition.widget(graphics,cx,cy,scale,MenuTheme.buttonEntranceOpacity());
        try { original.call(graphics,mx,my,delta); } finally { scope.close(); }
    }
}
