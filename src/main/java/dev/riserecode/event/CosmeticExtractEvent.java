package dev.riserecode.event;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
public final class CosmeticExtractEvent extends Event {
    public final Avatar player; public final AvatarRenderState state;
    public CosmeticExtractEvent(Avatar player,AvatarRenderState state){this.player=player;this.state=state;}
}
