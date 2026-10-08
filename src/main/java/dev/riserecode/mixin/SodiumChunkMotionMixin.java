package dev.riserecode.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.render.ChunkTerrainRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public abstract class SodiumChunkMotionMixin {
    @Unique private final java.util.Map<RenderRegion, java.util.Set<TerrainRenderPass>> riserecode$moved = new java.util.WeakHashMap<>();
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void riserecode$motion(ChunkRenderMatrices matrices, ChunkRenderListIterable lists, TerrainRenderPass pass,
            CameraTransform camera, FogParameters fog, boolean sorted, GpuSampler sampler, GpuBufferSlice globals, GpuBuffer sectionTimes, CallbackInfo ci) {
        boolean rendering = ChunkTerrainRenderer.motionActive() || ChunkTerrainRenderer.suppressFade() || AmbienceRenderer.waterActive() || AmbienceRenderer.shaderPackActive();
        if (!rendering && riserecode$moved.isEmpty()) { return; }
        if (rendering) { ChunkTerrainRenderer.ensureOffsetCapacity(sectionTimes.size()); }
        if (!ChunkTerrainRenderer.hasOffsets() && riserecode$moved.isEmpty()) { return; }
        var iterator = lists.iterator();
        while (iterator.hasNext()) {
            var list = iterator.next(); var region = list.getRegion(); int id = region.getId();
            if (!ChunkTerrainRenderer.hasOffsets()) {
                var passes = riserecode$moved.get(region);
                if (passes != null && passes.remove(pass)) { region.clearCachedBatchFor(pass); if (passes.isEmpty()) { riserecode$moved.remove(region); } }
                continue;
            }
            if (id < 0 || region.getResources() == null || region.getStorage(pass) == null) { continue; }
            var data = ChunkTerrainRenderer.regionData(id); var sections = list.sectionsWithGeometryIterator(false); boolean moving = false;
            if (sections != null) {
                while (sections.hasNext()) {
                    int section = sections.nextByteAsInt();
                    float offset = ChunkTerrainRenderer.drawable(region.getChunkX() + ((section >>> 5) & 7), region.getChunkZ() + ((section >>> 2) & 7));
                    data.putFloat(section * 4, offset);
                    moving |= offset != 0;
                }
            }
            ChunkTerrainRenderer.uploadRegion(id, data);
            if (moving) {
                region.clearCachedBatchFor(pass);
                riserecode$moved.computeIfAbsent(region, ignored -> new java.util.HashSet<>()).add(pass);
            } else {
                var passes = riserecode$moved.get(region);
                if (passes != null && passes.remove(pass)) { region.clearCachedBatchFor(pass); if (passes.isEmpty()) { riserecode$moved.remove(region); } }
            }
        }
    }
    @WrapMethod(method = "getVisibleFaces", require = 0)
    private static int riserecode$verticalFaces(int cameraX, int cameraY, int cameraZ, int chunkX, int chunkY, int chunkZ, Operation<Integer> original) {
        float offset = ChunkTerrainRenderer.offset(chunkX, chunkZ);
        return original.call(cameraX, offset == 0 ? cameraY : Math.round(cameraY - offset), cameraZ, chunkX, chunkY, chunkZ);
    }
    @Inject(method = "delete", at = @At("HEAD"), require = 0)
    private void riserecode$release(CallbackInfo ci) { riserecode$moved.clear(); }
}
