package dev.riserecode.mixin;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(SpriteContents.class)
public interface SpritePixelsAccessor {
    @Accessor("originalImage") NativeImage risecode$image();
}
