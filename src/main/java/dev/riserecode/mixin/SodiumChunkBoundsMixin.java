package dev.riserecode.mixin;

import dev.riserecode.render.ChunkTerrainRenderer;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.viewport.Viewport", remap = false)
public abstract class SodiumChunkBoundsMixin {
    @Shadow @Final private Frustum frustum;
    @Shadow @Final private CameraTransform transform;
    @Inject(method = {"isBoxVisible", "isBoxVisibleLooser"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void riserecode$bounds(int x, int y, int z, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ()) { return; }
        float offset = ChunkTerrainRenderer.offset(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (offset == 0) { return; }
        float rx = x - transform.intX - transform.fracX, ry = y - transform.intY - transform.fracY, rz = z - transform.intZ - transform.fracZ;
        float radius = Viewport.CHUNK_SECTION_PADDED_RADIUS + 1;
        ci.setReturnValue(frustum.testAab(rx - radius, ry - radius + Math.min(0, offset), rz - radius,
                rx + radius, ry + radius + Math.max(0, offset), rz + radius));
    }
}
