package dev.riserecode.testmixin;

import dev.riserecode.ChunkAnimationIntegrationTest;
import dev.riserecode.render.ChunkTerrainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Deterministic world animation time exists only in the client test fixture. */
@Mixin(value = ChunkTerrainRenderer.class, remap = false)
public abstract class ChunkClockMixin {
    @ModifyVariable(method = "frame", at = @At("HEAD"), argsOnly = true)
    private static long riserecode$clock(long time) { return ChunkAnimationIntegrationTest.frozenTime < 0 ? time : ChunkAnimationIntegrationTest.frozenTime; }
}
