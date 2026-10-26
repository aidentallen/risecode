package dev.riserecode.event;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class CapePoseEvent extends Event {
    public final AvatarRenderState state;
    public final ModelPart body, cape;
    public CapePoseEvent(AvatarRenderState state, ModelPart body, ModelPart cape) {
        this.state = state; this.body = body; this.cape = cape;
    }
}
