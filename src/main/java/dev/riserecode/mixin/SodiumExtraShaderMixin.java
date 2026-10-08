package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.shaders.ShaderType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Use Sodium Extra's own fog transformation on replacement terrain shaders. */
@Mixin(ShaderManager.class)
public abstract class SodiumExtraShaderMixin {
    private static final Method riserecode$shapeTransformer = riserecode$findTransformer();

    private static Method riserecode$findTransformer() {
        try {
            return Class.forName("me.flashyreese.mods.sodiumextra.client.fog.FogShaderTransformer")
                    .getMethod("injectRenderDistanceShape", String.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load Sodium Extra's terrain fog transformer", exception);
        }
    }

    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private String riserecode$terrainFog(String source, Identifier id, ShaderType type) {
        if (!id.getNamespace().equals("riserecode")
                || !(id.getPath().equals("core/chunk_sodium") || id.getPath().equals("core/water_sodium"))) { return source; }
        try {
            return (String)riserecode$shapeTransformer.invoke(null, source);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to preserve Sodium Extra's terrain fog shader", exception);
        }
    }
}
