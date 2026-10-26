package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CapeExtractEvent;
import dev.riserecode.render.LegacyCapeState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class CapeExtractMixin {
    @Inject(method = "extractCapeState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void risecode$cape(Avatar player, AvatarRenderState state, float partial, CallbackInfo ci) {
        ((LegacyCapeState) state).risecode$legacyCape(null);
        if (player instanceof AbstractClientPlayer clientPlayer && Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getEventBus().post(new CapeExtractEvent(clientPlayer, state, partial));
        }
    }
}
