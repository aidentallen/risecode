package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GameLoadCookie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ResourcesReloadMixin {
    @Inject(method="rollbackResourcePacks",at=@At("HEAD"),cancellable=true)
    private void riserecode$recoverResources(Throwable failure,GameLoadCookie cookie,CallbackInfo ci){
        if(Client.INSTANCE.isInitialized() && Client.INSTANCE.getEventBus().post(new dev.riserecode.event.ResourceReloadFailureEvent(failure)).isCancelled())ci.cancel();
    }
}
