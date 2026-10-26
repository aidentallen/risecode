package dev.riserecode.mixin;
import dev.riserecode.render.glass.GlassCapture;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(AvatarRenderState.class)
public abstract class GlassAvatarStateMixin implements GlassCapture.State {
    @Unique private GlassCapture.Owner risecode$owner;
    @Override public GlassCapture.Owner risecode$glassOwner(){return risecode$owner;}
    @Override public void risecode$glassOwner(GlassCapture.Owner owner){risecode$owner=owner;}
}
