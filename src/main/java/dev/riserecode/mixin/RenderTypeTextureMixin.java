package dev.riserecode.mixin;

import dev.riserecode.render.TextureSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderType.class)
public abstract class RenderTypeTextureMixin implements TextureSource {
    @Shadow @Final private RenderSetup state;
    @Override public Identifier riserecode$texture() { return ((TextureSource) (Object) state).riserecode$texture(); }
}
