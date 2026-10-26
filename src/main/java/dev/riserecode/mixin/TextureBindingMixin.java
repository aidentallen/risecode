package dev.riserecode.mixin;

import dev.riserecode.render.TextureSource;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface TextureBindingMixin extends TextureSource {
    @Override @Accessor("location") Identifier riserecode$texture();
}
