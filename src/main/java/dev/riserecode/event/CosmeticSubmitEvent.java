package dev.riserecode.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
public final class CosmeticSubmitEvent extends Event {
    public final AvatarRenderState state; public final PlayerModel model; public final PoseStack poses; public final SubmitNodeCollector collector;public final int light;
    public CosmeticSubmitEvent(AvatarRenderState state,PlayerModel model,PoseStack poses,SubmitNodeCollector collector,int light){this.state=state;this.model=model;this.poses=poses;this.collector=collector;this.light=light;}
}
