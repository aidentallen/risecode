package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Map;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class ChunkIndexMixin {
    @Unique private boolean riserecode$newChunk;
    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void riserecode$loading(int x, int z, FriendlyByteBuf buffer, Map<Heightmap.Types, long[]> heights,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> tags, CallbackInfoReturnable<LevelChunk> callback) {
        riserecode$newChunk = ((ClientChunkCache)(Object)this).getChunk(x, z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null;
    }
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void riserecode$loaded(int x, int z, FriendlyByteBuf buffer, Map<Heightmap.Types, long[]> heights,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> tags, CallbackInfoReturnable<LevelChunk> callback) {
        if (Client.INSTANCE.isInitialized() && callback.getReturnValue() != null) { Client.INSTANCE.getBlockIndex().loaded(callback.getReturnValue()); }
        if (riserecode$newChunk && callback.getReturnValue() != null) { dev.riserecode.render.ChunkTerrainRenderer.loaded(x, z); }
    }
    @Inject(method = "drop", at = @At("HEAD"))
    private void riserecode$unloaded(ChunkPos pos, CallbackInfo callback) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getBlockIndex().unloaded(pos.pack()); }
    }
}
