package dev.riserecode.mixin;

import dev.riserecode.render.LegacyCapeMotion;
import dev.riserecode.render.LegacyCapeState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarCapeStateMixin implements LegacyCapeState, dev.riserecode.cosmetics.CosmeticRenderState {
    @Unique private dev.riserecode.cosmetics.CosmeticLoadout risecode$outfit;
    @Unique private boolean risecode$preview;
    @Override public dev.riserecode.cosmetics.CosmeticLoadout risecode$outfit(){return risecode$outfit;}
    @Override public void risecode$outfit(dev.riserecode.cosmetics.CosmeticLoadout value){risecode$outfit=value;}
    @Override public boolean risecode$preview(){return risecode$preview;}
    @Override public void risecode$preview(boolean value){risecode$preview=value;}
    @Unique private LegacyCapeMotion.Angles risecode$angles;
    @Override public LegacyCapeMotion.Angles risecode$legacyCape() { return risecode$angles; }
    @Override public void risecode$legacyCape(LegacyCapeMotion.Angles angles) { risecode$angles = angles; }
}
