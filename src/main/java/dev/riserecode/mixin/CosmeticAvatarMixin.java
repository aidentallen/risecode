package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.cosmetics.CosmeticsLayer;
import dev.riserecode.event.CosmeticExtractEvent;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class CosmeticAvatarMixin extends LivingEntityRenderer<AbstractClientPlayer,AvatarRenderState,PlayerModel> {
    protected CosmeticAvatarMixin(EntityRendererProvider.Context context,PlayerModel model,float shadow){super(context,model,shadow);}
    @Inject(method="<init>",at=@At("TAIL"))
    private void risecode$layer(CallbackInfo ci){addLayer(new CosmeticsLayer(this));}
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void risecode$extract(Avatar player,AvatarRenderState state,float partial,CallbackInfo ci){
        if(Client.INSTANCE.isInitialized()){Client.INSTANCE.getEventBus().post(new CosmeticExtractEvent(player,state));}
    }
}
