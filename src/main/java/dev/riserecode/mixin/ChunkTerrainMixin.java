package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.riserecode.render.ChunkTerrainRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class ChunkTerrainMixin {
    @WrapOperation(method = "prepareChunkRenders", at = @At(value = "NEW", target = "net/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo"))
    private DynamicUniforms.ChunkSectionInfo riserecode$section(Matrix4fc modelView, int x, int y, int z, float visibility, int width, int height,
            Operation<DynamicUniforms.ChunkSectionInfo> original) {
        float offset = ChunkTerrainRenderer.drawable(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        ChunkTerrainRenderer.vanillaDraw(offset);
        return original.call(offset == 0 ? modelView : new Matrix4f(modelView).translate(0, offset, 0), x, y, z,
                ChunkTerrainRenderer.suppressFade() ? 1f : visibility, width, height);
    }
}
