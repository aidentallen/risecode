package dev.riserecode.mixin;

import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemPhysicsStateMixin implements ItemPhysicsState {
    @Unique private boolean riserecode$ground;
    @Unique private float riserecode$angle;
    @Override public boolean riserecode$onGround() { return riserecode$ground; }
    @Override public float riserecode$rotation() { return riserecode$angle; }
    @Override public void riserecode$physics(boolean ground, float rotation) { riserecode$ground = ground; riserecode$angle = rotation; }
}
