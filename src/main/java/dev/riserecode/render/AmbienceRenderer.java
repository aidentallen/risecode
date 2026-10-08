package dev.riserecode.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riserecode.module.modules.AmbienceModule;
import dev.riserecode.ui.theme.Theme;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

public final class AmbienceRenderer {
    private static final BindGroupLayout PARAMS = BindGroupLayout.builder().withUniform("AmbienceParams", UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline SKY = RenderPipelines.register(RenderPipeline.builder().withLocation(id("pipeline/ambience_sky"))
            .withVertexShader(id("core/ambience_sky")).withFragmentShader(id("core/ambience_sky"))
            .withBindGroupLayout(PARAMS).withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false).withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES).build());
    private static final Map<RenderPipeline, RenderPipeline> WATER = new IdentityHashMap<>();
    private static AmbienceModule module;
    private static GpuBuffer parameters;
    private static boolean irisActive;
    private static Object iris;
    private static Method irisInUse;
    private static boolean irisChecked;
    private static long frames, waterDraws;
    private static final long CLOCK_START = System.nanoTime();
    private AmbienceRenderer() { }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("riserecode", path); }
    public static void bind(AmbienceModule value) { module = value; updateHandoff(); }
    public static void unbind(AmbienceModule value) { if (module == value) { module = null; reset(); } }
    public static boolean shaderPackActive() { return irisActive; }
    public static long frames() { return frames; }
    public static long waterDraws() { return waterDraws; }
    public static void updateHandoff() {
        if (!irisChecked) {
            irisChecked = true;
            if (FabricLoader.getInstance().isModLoaded("iris")) {
                try { Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi"); iris = api.getMethod("getInstance").invoke(null); irisInUse = api.getMethod("isShaderPackInUse"); }
                catch (ReflectiveOperationException | LinkageError failure) { dev.riserecode.Client.INSTANCE.getLogger().warn("Iris handoff could not be queried; suspending Ambience", failure); irisActive = true; }
            }
        }
        if (irisInUse != null) {
            try { boolean next = (Boolean)irisInUse.invoke(iris); if (next != irisActive) { reset(); irisActive = next; } }
            catch (ReflectiveOperationException failure) { irisActive = true; }
        }
    }
    public static boolean skyActive() { return module != null && module.isEnabled() && module.sky.getValue() && !irisActive && Minecraft.getInstance().level != null; }
    public static boolean waterActive() { return module != null && module.isEnabled() && module.water.getValue() && !irisActive && Minecraft.getInstance().level != null; }
    private static void color(Std140Builder builder, int color) { builder.putVec4((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f); }
    private static void prepare() {
        if (parameters == null) { parameters = RenderSystem.getDevice().createBuffer(() -> "risecode atmosphere parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 192); }
        var mc = Minecraft.getInstance(); var m = module;
        try (var stack = MemoryStack.stackPush()) {
            var b = Std140Builder.onStack(stack, 192);
            color(b, m.theme.getValue() ? Theme.accent() : m.first.getValue()); color(b, m.theme.getValue() ? Theme.secondaryAccent() : m.second.getValue());
            color(b, m.shallow.getValue()); color(b, m.deep.getValue());
            double seconds = (System.nanoTime() - CLOCK_START) / 1e9;
            b.putVec4((float)seconds, m.speed.getValue().floatValue(), m.intensity.getValue().floatValue(), m.mode.getValue().ordinal());
            b.putVec4(m.softness.getValue().floatValue(), m.stars.getValue() ? 1 : 0, m.quality.getValue().ordinal() + 3, m.sky.getValue() ? 1 : 0);
            b.putVec4(m.waves.getValue().floatValue(), m.waterSpeed.getValue().floatValue(), m.transparency.getValue().floatValue(), m.reflection.getValue().floatValue());
            var camera = mc.gameRenderer.mainCamera();
            b.putVec4(camera.yRot() * (float)Math.PI / 180, camera.xRot() * (float)Math.PI / 180, (float)Math.tan(mc.gameRenderer.mainCamera().getFov() * Math.PI / 360), mc.getWindow().getScreenWidth() / (float)mc.getWindow().getScreenHeight());
            var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            for (String sprite : new String[] {"block/water_still", "block/water_flow", "block/water_overlay"}) {
                var s = atlas.getSprite(Identifier.withDefaultNamespace(sprite)); b.putVec4(s.getU0(), s.getV0(), s.getU1(), s.getV1());
            }
            var position = camera.position(); b.putVec4((float)(position.x % 4096), (float)(position.y % 4096), (float)(position.z % 4096), camera.getFluidInCamera() == net.minecraft.world.level.material.FogType.WATER ? 1 : 0);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(parameters.slice(), b.get());
        }
    }
    public static boolean renderSky(com.mojang.blaze3d.pipeline.RenderTarget target) {
        updateHandoff();
        if (!skyActive()) { return false; }
        prepare();
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "risecode procedural sky", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), java.util.OptionalDouble.empty())) {
            pass.setPipeline(SKY); pass.setUniform("AmbienceParams", parameters); pass.draw(3, 1, 0, 0); frames++;
        }
        return true;
    }
    public static RenderPipeline adapt(RenderPipeline original) {
        if (module != null && module.isEnabled()) { updateHandoff(); }
        if (!waterActive() || !original.getLocation().getPath().contains("translucent")
                || !(original.getFragmentShader().toString().equals("minecraft:core/terrain") || original.getFragmentShader().toString().equals("sodium:blocks/block_layer_opaque"))) { return original; }
        return WATER.computeIfAbsent(original, AmbienceRenderer::waterPipeline);
    }
    public static void bindParameters(RenderPass pass, RenderPipeline pipeline) {
        if (WATER.containsValue(pipeline) && waterActive()) { prepare(); pass.setUniform("AmbienceParams", parameters); waterDraws++; }
    }
    private static RenderPipeline waterPipeline(RenderPipeline original) {
        String adapter = original.getVertexShader().getNamespace().equals("sodium") ? "sodium" : "vanilla";
        var builder = RenderPipeline.builder().withLocation(id("pipeline/water_" + adapter + "_" + WATER.size()))
                .withVertexShader(id("core/water_" + adapter)).withFragmentShader(id("core/water_" + adapter))
                .withCull(original.isCull()).withPolygonMode(original.getPolygonMode()).withPrimitiveTopology(original.getPrimitiveTopology())
                .withDepthStencilState(Optional.ofNullable(original.getDepthStencilState()));
        original.getBindGroupLayouts().forEach(builder::withBindGroupLayout); builder.withBindGroupLayout(PARAMS);
        if (adapter.equals("sodium")) { builder.withBindGroupLayout(ChunkTerrainRenderer.BINDINGS); }
        for (int i = 0; i < original.getVertexFormatBindings().length; i++) { builder.withVertexBinding(i, original.getVertexFormatBinding(i)); }
        for (int i = 0; i < original.getColorTargetStates().length; i++) { builder.withColorTargetState(i, original.getColorTargetStates()[i]); }
        original.getShaderDefines().flags().forEach(builder::withShaderDefine);
        original.getShaderDefines().values().forEach((key, value) -> { try { builder.withShaderDefine(key, Float.parseFloat(value)); } catch (NumberFormatException invalid) { throw new IllegalArgumentException("Unsupported terrain define " + key, invalid); } });
        return RenderPipelines.register(builder.build());
    }
    public static void reset() { if (parameters != null) { parameters.close(); parameters = null; } dev.riserecode.ui.theme.BrandingLogo.invalidate(); }
}
