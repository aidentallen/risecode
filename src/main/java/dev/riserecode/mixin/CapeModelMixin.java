package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CapePoseEvent;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerCapeModel.class)
public abstract class CapeModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void risecode$capePose(AvatarRenderState state, CallbackInfo ci) {
        if (Client.INSTANCE.isInitialized()) {
            var body = ((PlayerCapeModel) (Object) this).body;
            Client.INSTANCE.getEventBus().post(new CapePoseEvent(state, body, body.getChild("cape")));
        }
    }
}
