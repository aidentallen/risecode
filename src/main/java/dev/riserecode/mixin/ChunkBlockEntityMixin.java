package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.render.ChunkTerrainRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shared submission path remains native even when Sodium replaces terrain drawing. */
@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class ChunkBlockEntityMixin {
    @WrapOperation(method = "submitBlockEntities", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void riserecode$blockEntity(PoseStack pose, double x, double y, double z, Operation<Void> original, @Local BlockEntityRenderState state) {
        original.call(pose, x, y + ChunkTerrainRenderer.offset(Math.floorDiv(state.blockPos.getX(), 16), Math.floorDiv(state.blockPos.getZ(), 16)), z);
    }
}
