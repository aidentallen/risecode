package dev.riserecode.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riserecode.Client;
import dev.riserecode.event.ChunkLifecycleEvent;
import dev.riserecode.event.ChunkRenderPolicyEvent;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Frozen frame policy shared by native section transforms and optional shader adapters. */
public final class ChunkTerrainRenderer {
    public static final BindGroupLayout BINDINGS = BindGroupLayout.builder()
            .withUniform("RiseChunkParams", UniformType.UNIFORM_BUFFER)
            .withUniform("u_RiseChunkOffsets", UniformType.TEXEL_BUFFER, GpuFormat.R32_FLOAT).build();
    private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new IdentityHashMap<>();
    private static Object level;
    private static volatile ChunkRenderPolicyEvent policy = new ChunkRenderPolicyEvent();
    private static GpuBuffer parameters, offsets;
    private static ByteBuffer offsetData;
    private static boolean parametersDirty = true;
    private static boolean irisPatched;
    private static String incompatibility;
    private static long vanillaDraws, sodiumDraws, irisDraws;
    private ChunkTerrainRenderer() { }
    public static void syncLevel() {
        if (!Client.INSTANCE.isInitialized()) { return; }
        Object next = Minecraft.getInstance().level;
        if (level != next) { level = next; reset(); }
    }
    public static void frame(long now) {
        syncLevel(); AmbienceRenderer.updateHandoff();
        policy = Client.INSTANCE.isInitialized() && level != null
                ? Client.INSTANCE.getEventBus().post(new ChunkRenderPolicyEvent()) : new ChunkRenderPolicyEvent();
        if (policy.motion() != null) { policy.motion().frame(now, policy.settings()); }
        parametersDirty = true;
    }
    public static void loaded(int x, int z) {
        syncLevel();
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new ChunkLifecycleEvent(ChunkLifecycleEvent.Kind.LOAD, ChunkMotionTracker.key(x, z))); }
    }
    public static void unloaded(long chunk) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new ChunkLifecycleEvent(ChunkLifecycleEvent.Kind.UNLOAD, chunk)); }
    }
    public static boolean suppressFade() { return policy.fadeSuppressed() && supported(); }
    public static boolean motionActive() { return policy.motion() != null && supported(); }
    public static boolean hasOffsets() { return motionActive() && policy.motion().pendingCount() > 0; }
    private static boolean supported() { return RendererCapabilities.chunkAdapters && (!AmbienceRenderer.shaderPackActive() || (irisPatched && incompatibility == null)); }
    public static float offset(int x, int z) { return motionActive() ? policy.motion().offset(x, z) : 0; }
    public static float drawable(int x, int z) { return motionActive() ? policy.motion().drawable(x, z) : 0; }
    public static void vanillaDraw(float offset) { if (offset != 0) { vanillaDraws++; } }
    public static long vanillaDraws() { return vanillaDraws; }
    public static long sodiumDraws() { return sodiumDraws; }
    public static long irisDraws() { return irisDraws; }
    public static void irisPatched() { irisPatched = true; }
    public static void incompatible(String reason) {
        if (incompatibility == null) { incompatibility = reason; Client.INSTANCE.getLogger().warn("ChunkAnimations compatibility: {}", reason); }
    }
    public static String compatibilityStatus() {
        if(!RendererCapabilities.chunkAdapters){return "Renderer adapter signatures unavailable";}
        if (incompatibility != null && AmbienceRenderer.shaderPackActive()) { return "Iris compatibility unavailable · " + incompatibility; }
        return AmbienceRenderer.shaderPackActive() ? "Iris terrain and shadows" : "Native terrain · shader movement";
    }
    public static void reset() {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new ChunkLifecycleEvent(ChunkLifecycleEvent.Kind.RESET, 0)); }
        invalidate();
    }
    public static void invalidate() {
        policy = new ChunkRenderPolicyEvent();
        if (parameters != null) { parameters.close(); parameters = null; }
        if (offsets != null) { offsets.close(); offsets = null; }
        if (offsetData != null) { MemoryUtil.memFree(offsetData); offsetData = null; }
        parametersDirty = true;
    }
    public static void ensureOffsetCapacity(long bytes) {
        int needed = Math.toIntExact(Math.max(1024, bytes));
        if (offsetData != null && offsetData.capacity() >= needed) { return; }
        if (offsets != null) { offsets.close(); }
        if (offsetData != null) { MemoryUtil.memFree(offsetData); }
        offsetData = MemoryUtil.memCalloc(needed);
        offsets = RenderSystem.getDevice().createBuffer(() -> "risecode chunk offsets", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_DST, offsetData);
    }
    /** Rewrites a visible region, including zeroes, so reused section IDs never retain motion. */
    public static ByteBuffer regionData(int region) {
        int start = Math.multiplyExact(region, 1024);
        MemoryUtil.memSet(MemoryUtil.memAddress(offsetData) + start, 0, 1024);
        return offsetData.slice(start, 1024).order(java.nio.ByteOrder.nativeOrder());
    }
    public static void uploadRegion(int region, ByteBuffer data) {
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(offsets.slice((long)region * 1024, 1024), data);
    }
    private static void prepareParameters() {
        if (parameters == null) { parameters = RenderSystem.getDevice().createBuffer(() -> "risecode chunk policy", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16); }
        if (parametersDirty) {
            try (var stack = MemoryStack.stackPush()) {
                var builder = Std140Builder.onStack(stack, 16);
                builder.putVec4(hasOffsets() ? 1 : 0, suppressFade() ? 1 : 0, 0, 0);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(parameters.slice(), builder.get());
            }
            parametersDirty = false;
        }
    }
    public static RenderPipeline adapt(RenderPipeline original) {
        if (AmbienceRenderer.shaderPackActive() || (!motionActive() && !suppressFade())
                || !original.getVertexShader().toString().equals("sodium:blocks/block_layer_opaque")) { return original; }
        return PIPELINES.computeIfAbsent(original, source -> {
            var builder = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("riserecode", "pipeline/chunk_motion_" + PIPELINES.size()))
                    .withVertexShader(Identifier.fromNamespaceAndPath("riserecode", "core/chunk_sodium")).withFragmentShader(source.getFragmentShader())
                    .withCull(source.isCull()).withPolygonMode(source.getPolygonMode()).withPrimitiveTopology(source.getPrimitiveTopology())
                    .withDepthStencilState(Optional.ofNullable(source.getDepthStencilState()));
            source.getBindGroupLayouts().forEach(builder::withBindGroupLayout);
            builder.withBindGroupLayout(BINDINGS);
            for (int i = 0; i < source.getVertexFormatBindings().length; i++) { builder.withVertexBinding(i, source.getVertexFormatBinding(i)); }
            for (int i = 0; i < source.getColorTargetStates().length; i++) { builder.withColorTargetState(i, source.getColorTargetStates()[i]); }
            source.getShaderDefines().flags().forEach(builder::withShaderDefine);
            source.getShaderDefines().values().forEach((key, value) -> builder.withShaderDefine(key, Float.parseFloat(value)));
            return RenderPipelines.register(builder.build());
        });
    }
    public static void bindParameters(RenderPass pass, RenderPipeline pipeline) {
        if(!RendererCapabilities.chunkAdapters || AmbienceRenderer.shaderPackActive()&&!supported()){return;}
        String vertex = pipeline.getVertexShader().toString();
        if (vertex.equals("riserecode:core/chunk_sodium") || vertex.equals("riserecode:core/water_sodium")
                || (AmbienceRenderer.shaderPackActive() && vertex.equals("sodium:blocks/block_layer_opaque"))) {
            ensureOffsetCapacity(1024); prepareParameters();
            pass.setUniform("RiseChunkParams", parameters); pass.setUniform("u_RiseChunkOffsets", offsets);
            sodiumDraws++; if (AmbienceRenderer.shaderPackActive()) { irisDraws++; }
        }
    }
}
