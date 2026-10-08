package dev.riserecode.mixin;

import dev.riserecode.render.ChunkTerrainRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ChunkUnloadMixin {
    @Inject(method = "unload", at = @At("HEAD"))
    private void riserecode$unload(LevelChunk chunk, CallbackInfo ci) { ChunkTerrainRenderer.unloaded(chunk.getPos().pack()); }
}
