package dev.riserecode.event;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class CapeExtractEvent extends Event {
    public final AbstractClientPlayer player;
    public final AvatarRenderState state;
    public final float partial;
    public CapeExtractEvent(AbstractClientPlayer player, AvatarRenderState state, float partial) {
        this.player = player; this.state = state; this.partial = partial;
    }
}
