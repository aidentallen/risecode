package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CapeTickEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class CapeTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void risecode$capeBegin(CallbackInfo ci) { risecode$capeTick(CapeTickEvent.Phase.BEGIN); }
    @Inject(method = "tick", at = @At("TAIL"))
    private void risecode$capeEnd(CallbackInfo ci) { risecode$capeTick(CapeTickEvent.Phase.END); }
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void risecode$capeBob(CallbackInfo ci) { risecode$capeTick(CapeTickEvent.Phase.BOB); }
    @org.spongepowered.asm.mixin.Unique
    private void risecode$capeTick(CapeTickEvent.Phase phase) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new CapeTickEvent((AbstractClientPlayer) (Object) this, phase)); }
    }
}
