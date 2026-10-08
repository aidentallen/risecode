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
