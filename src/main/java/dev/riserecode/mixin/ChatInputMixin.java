package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ChatInputMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void riserecode$sendChat(String message, CallbackInfo callback) {
        if (Client.INSTANCE.isInitialized() && Client.INSTANCE.getCommandManager().handleChat(message)) {
            // Stop before signing/last-seen bookkeeping or creating a chat packet.
            callback.cancel();
        }
    }
}
