package dev.riserecode.mixin;

import dev.riserecode.render.ChunkTerrainRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class ChunkSectionMixin {
    @Shadow public abstract BlockPos getRenderOrigin();
    @Inject(method = "getVisibility", at = @At("RETURN"), cancellable = true)
    private void riserecode$fade(long now, CallbackInfoReturnable<Float> ci) { if (ChunkTerrainRenderer.suppressFade()) { ci.setReturnValue(1f); } }
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void riserecode$bounds(CallbackInfoReturnable<AABB> ci) {
        var pos = getRenderOrigin(); float offset = ChunkTerrainRenderer.offset(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
        if (offset != 0) { var b = ci.getReturnValue(); ci.setReturnValue(new AABB(b.minX, b.minY + Math.min(0, offset), b.minZ, b.maxX, b.maxY + Math.max(0, offset), b.maxZ)); }
    }
}
