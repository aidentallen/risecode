package dev.riserecode.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.event.CosmeticSubmitEvent;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class CosmeticsLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    public CosmeticsLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent){super(parent);}
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        if(Client.INSTANCE.isInitialized()){Client.INSTANCE.getEventBus().post(new CosmeticSubmitEvent(state,getParentModel(),poses,collector,light));}
    }
}
