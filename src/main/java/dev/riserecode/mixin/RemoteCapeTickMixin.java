package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CapeTickEvent;
import net.minecraft.client.player.RemotePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemotePlayer.class)
public abstract class RemoteCapeTickMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void risecode$capeBob(CallbackInfo ci) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new CapeTickEvent((RemotePlayer) (Object) this, CapeTickEvent.Phase.BOB)); }
    }
}
