package dev.riserecode.mixin;

import dev.riserecode.render.TextureSource;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSetup.class)
public abstract class RenderSetupTextureMixin implements TextureSource {
    @Shadow @Final private Map<String, ?> textures;
    @Override public Identifier riserecode$texture() {
        Object texture = textures.get("Sampler0");
        return texture == null ? null : ((TextureSource) texture).riserecode$texture();
    }
}
