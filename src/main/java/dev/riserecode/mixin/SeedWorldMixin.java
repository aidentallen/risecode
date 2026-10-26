package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.SeedWorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SeedWorldMixin {
    @Inject(method = {"handleLogin", "handleRespawn"}, at = @At("TAIL"))
    private void riserecode$worldReady(CallbackInfo ci) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new SeedWorldEvent(Minecraft.getInstance().level)); }
    }
}
