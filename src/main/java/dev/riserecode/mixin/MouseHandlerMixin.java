package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.event.MouseScrollEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method="onButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean risecode$click(net.minecraft.client.gui.screens.Screen screen,net.minecraft.client.input.MouseButtonEvent event,boolean twice){
        var local=dev.riserecode.ui.menu.MenuTheme.pointer(screen,event);
        for(var child:screen.children()){if(child instanceof dev.riserecode.ui.menu.PageGear gear&&gear.mouseClicked(local,twice)){return true;}}
        return screen.mouseClicked(local,twice);
    }
    @Redirect(method="onButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean risecode$release(net.minecraft.client.gui.screens.Screen screen,net.minecraft.client.input.MouseButtonEvent event){if(event.button()==0){dev.riserecode.ui.animation.ButtonFeedback.releaseAll();}return screen.mouseReleased(dev.riserecode.ui.menu.MenuTheme.pointer(screen,event));}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean risecode$drag(net.minecraft.client.gui.screens.Screen screen,net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){double scale=dev.riserecode.ui.menu.MenuTheme.screenScale(screen);return screen.mouseDragged(dev.riserecode.ui.menu.MenuTheme.pointer(screen,event),dx/scale,dy/scale);}

    @Redirect(method="onScroll",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean risecode$scrollPointer(net.minecraft.client.gui.screens.Screen screen,double x,double y,double horizontal,double vertical){return screen.mouseScrolled(dev.riserecode.ui.menu.MenuTheme.pointerX(screen,x),dev.riserecode.ui.menu.MenuTheme.pointerY(screen,y),horizontal,vertical);}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void risecode$movePointer(net.minecraft.client.gui.screens.Screen screen,double x,double y){screen.mouseMoved(dev.riserecode.ui.menu.MenuTheme.pointerX(screen,x),dev.riserecode.ui.menu.MenuTheme.pointerY(screen,y));}
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void riserecode$scroll(long window,double x,double y,CallbackInfo callback) {
        var mc=Minecraft.getInstance();
        if(Client.INSTANCE.isInitialized() && window==mc.getWindow().handle() && mc.gui.screen()==null && mc.gui.overlay()==null) {
            if(Client.INSTANCE.getEventBus().post(new MouseScrollEvent(x,y)).isCancelled()) { callback.cancel(); }
        }
    }
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void riserecode$mouseRotation(LocalPlayer player, double yaw, double pitch) {
        MouseRotationEvent event = new MouseRotationEvent(yaw, pitch);
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(event); }
        if (!event.isCancelled()) { player.turn(event.rawYaw(), event.rawPitch()); }
    }
}
