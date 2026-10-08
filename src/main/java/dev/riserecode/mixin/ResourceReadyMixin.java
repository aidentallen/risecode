package dev.riserecode.mixin;
import dev.riserecode.Client;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CLIENT_STARTED precedes the asynchronous initial shader reload on fast backends. */
@Mixin(Minecraft.class)
public abstract class ResourceReadyMixin {
    @Inject(method="onResourceLoadFinished",at=@At("HEAD"))
    private void riserecode$ready(CallbackInfo ci) { Client.INSTANCE.resourcesReady(true); }
}
