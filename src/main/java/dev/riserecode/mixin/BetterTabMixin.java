package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.TabDisplayEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
@Mixin(PlayerTabOverlay.class)
public abstract class BetterTabMixin {
    @org.spongepowered.asm.mixin.Shadow private boolean visible;
    @org.spongepowered.asm.mixin.Unique private long risecode$opened;
    @org.spongepowered.asm.mixin.Unique private double risecode$alpha=1;
    @org.spongepowered.asm.mixin.Unique private static dev.riserecode.module.modules.BetterTabModule risecode$module(){return Client.INSTANCE.isInitialized()?Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.BetterTabModule.class).filter(dev.riserecode.module.modules.BetterTabModule::isEnabled).orElse(null):null;}
    @Inject(method="setVisible",at=@At("HEAD"))
    private void risecode$open(boolean value,CallbackInfo ci){if(value&&!visible){risecode$opened=System.nanoTime();}}
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="extractRenderState")
    private void risecode$pop(GuiGraphicsExtractor g,int width,net.minecraft.world.scores.Scoreboard scores,net.minecraft.world.scores.Objective objective,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        var module=risecode$module();if(module==null){original.call(g,width,scores,objective);return;}
        risecode$alpha=dev.riserecode.ui.animation.PopIn.moduleProgress(module,(System.nanoTime()-risecode$opened)*1e-9);double scale=module.popScale(risecode$alpha);
        g.pose().pushMatrix();g.pose().translate(width/2f,10);g.pose().scale((float)scale,(float)scale);g.pose().translate(-width/2f,-10);
        try{original.call(g,width,scores,objective);}finally{g.pose().popMatrix();}
    }
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void risecode$surface(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color){
        if(risecode$module()==null){g.fill(x0,y0,x1,y1,color);return;}
        double alpha=(color>>>24)/255.0*risecode$alpha;
        dev.riserecode.util.RenderUtil.drawDropShadow(g,x0,y0,x1-x0,y1-y0,3,dev.riserecode.ui.theme.Theme.opacity(dev.riserecode.ui.theme.Theme.SHADOW,alpha*.5));
        dev.riserecode.util.RenderUtil.drawRoundedRect(g,x0,y0,x1-x0,y1-y0,3,dev.riserecode.ui.theme.Theme.opacity(dev.riserecode.ui.theme.Theme.SURFACE,alpha));
    }
    @Redirect(method={"extractRenderState","extractTablistScore","extractTablistHearts"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"))
    private int risecode$width(net.minecraft.client.gui.Font font,net.minecraft.network.chat.FormattedText text){return risecode$module()==null?font.width(text):(int)Math.ceil(dev.riserecode.ui.font.StyledText.of(text).width(font));}
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"))
    private int risecode$lineWidth(net.minecraft.client.gui.Font font,net.minecraft.util.FormattedCharSequence text){return risecode$module()==null?font.width(text):(int)Math.ceil(dev.riserecode.ui.font.StyledText.of(text).width(font));}
    @Redirect(method={"extractRenderState","extractTablistScore","extractTablistHearts"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int risecode$stringWidth(net.minecraft.client.gui.Font font,String text){return risecode$module()==null?font.width(text):dev.riserecode.util.RenderUtil.textWidth(font,text,false);}
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"))
    private void risecode$line(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,net.minecraft.util.FormattedCharSequence text,int x,int y,int color){if(risecode$module()==null){g.text(font,text,x,y,color);}else{dev.riserecode.ui.font.StyledText.of(text).draw(g,font,x,y,1,dev.riserecode.ui.theme.Theme.opacity(color,risecode$alpha));}}
    @Redirect(method={"extractRenderState","extractTablistScore","extractTablistHearts"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void risecode$text(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,Component text,int x,int y,int color){if(risecode$module()==null){g.text(font,text,x,y,color);}else{dev.riserecode.ui.font.StyledText.of(text).draw(g,font,x,y,1,dev.riserecode.ui.theme.Theme.opacity(color,risecode$alpha));}}
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<net.minecraft.util.FormattedCharSequence> risecode$wrap(net.minecraft.client.gui.Font font,net.minecraft.network.chat.FormattedText text,int width){
        if(risecode$module()==null||!(text instanceof Component component)){return font.split(text,width);}
        return dev.riserecode.ui.font.StyledText.of(component).wrap(font,width).stream().map(line->(net.minecraft.util.FormattedCharSequence)new dev.riserecode.ui.font.StyledSequence(line,Integer.MIN_VALUE/2)).toList();
    }

    @ModifyReturnValue(method="getNameForDisplay",at=@At("RETURN"))
    private Component risecode$name(Component original,PlayerInfo info){if(!Client.INSTANCE.isInitialized()){return original;}return Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.NAME,info,original,null)).name();}
    @ModifyReturnValue(method="getPlayerInfos",at=@At("RETURN"))
    private List<PlayerInfo> risecode$sort(List<PlayerInfo> original){if(!Client.INSTANCE.isInitialized()){return original;}return Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.SORT,null,null,original)).players();}
    @ModifyConstant(method="getPlayerInfos",constant=@Constant(longValue=80L))
    private long risecode$capacity(long original){return Client.INSTANCE.isInitialized()?Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.CAPACITY,null,null,null)).capacity():original;}
    @ModifyConstant(method="extractRenderState",constant=@Constant(intValue=20))
    private int risecode$rows(int original){return Client.INSTANCE.isInitialized()?Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.ROWS,null,null,null)).rows():original;}
    @Inject(method="extractPingIcon",at=@At("HEAD"),cancellable=true)
    private void risecode$ping(GuiGraphicsExtractor g,int width,int x,int y,PlayerInfo info,CallbackInfo ci){
        if(!Client.INSTANCE.isInitialized()||!Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.PING,info,null,null)).numeric()){return;}
        String text=info.getLatency()+"";var font=Minecraft.getInstance().font;float scale=0.6f;g.pose().pushMatrix();g.pose().translate(x+width-dev.riserecode.util.RenderUtil.textWidth(font,text,false)*scale,y+1);g.pose().scale(scale,scale);dev.riserecode.util.RenderUtil.text(g,font,text,0,0,1,info.getLatency()<100?0xFF79DEAE:info.getLatency()<250?0xFFFFCE69:0xFFFF7777);g.pose().popMatrix();ci.cancel();
    }
}
