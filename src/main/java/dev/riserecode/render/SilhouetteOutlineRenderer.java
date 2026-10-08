package dev.riserecode.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.EnchantmentRenderEvent.Style;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Independent union masks per style/depth policy. Geometry is extracted by vanilla renderers;
 * labels, shadows and flames are excluded. Dilation discards every original interior pixel. */
public final class SilhouetteOutlineRenderer {
    private record Key(String channel, Style style, boolean visible) { }
    private static final Map<Key, SilhouetteMask> BATCHES = new HashMap<>();
    private static final java.util.ArrayList<SilhouetteMask> POOL = new java.util.ArrayList<>();
    private static int used;
    private SilhouetteOutlineRenderer() { }
    public static void beginFrame() { POOL.forEach(SilhouetteMask::beginFrame); BATCHES.clear(); used = 0; }
    public static SubmitNodeCollector collector(String channel, Style style, boolean visible, AABB bounds,
            CameraRenderState camera, SubmitNodeCollector output) {
        Key key = new Key(channel, style, visible);
        SilhouetteMask mask = BATCHES.computeIfAbsent(key, ignored -> { if (used == POOL.size()) { POOL.add(new SilhouetteMask()); } return POOL.get(used++); });
        mask.request(style, bounds, camera);
        return proxy(output, mask, visible);
    }
    private static SubmitNodeCollector proxy(OrderedSubmitNodeCollector output, SilhouetteMask mask, boolean visible) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
            if (method.getName().equals("order")) { return proxy(output instanceof SubmitNodeCollector collector ? collector.order((int) args[0]) : output, mask, visible); }
            if (method.isDefault()) { return InvocationHandler.invokeDefault(proxy, method, args); }
            if (method.getName().equals("submitModel")) {
                RenderType original = (RenderType) args[3]; var atlas = ((TextureSource) original).riserecode$texture();
                if (atlas != null) { args[3] = mask.type(atlas, original.format(), visible); args[6] = -1; args[8] = 0; args[9] = null; method.invoke(output, args); }
            } else if (method.getName().equals("submitBlockModel")) {
                @SuppressWarnings("unchecked") List<BlockStateModelPart> parts = (List<BlockStateModelPart>) args[2];
                for (var part : parts) { for (Direction direction : Direction.values()) { quads(output, mask, visible, (PoseStack) args[0], part.getQuads(direction)); } quads(output, mask, visible, (PoseStack) args[0], part.getQuads(null)); }
            } else if (method.getName().equals("submitItem")) {
                @SuppressWarnings("unchecked") List<BakedQuad> list = (List<BakedQuad>) args[6]; quads(output, mask, visible, (PoseStack) args[0], list);
            }
            return null;
        });
    }
    public static void quads(OrderedSubmitNodeCollector output, SilhouetteMask mask, boolean visible, PoseStack pose, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            RenderType type = mask.type(quad.materialInfo().sprite().atlasLocation(), DefaultVertexFormat.POSITION_TEX_COLOR, visible);
            output.submitCustomGeometry(pose, type, (matrix, vertices) -> {
                for (int i = 0; i < 4; i++) { long uv = quad.packedUV(i); vertices.addVertex(matrix, quad.position(i)).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv)).setColor(-1); }
            });
        }
    }
    public static void composite(RenderTarget target) { BATCHES.values().forEach(mask -> mask.composite(target)); }
    public static java.util.List<RenderTarget> maskTargets() { return POOL.stream().map(SilhouetteMask::getMaskTarget).filter(java.util.Objects::nonNull).toList(); }
    public static int allocations() { return POOL.stream().mapToInt(SilhouetteMask::getAllocationCount).sum(); }
    public static long composites() { return POOL.stream().mapToLong(SilhouetteMask::getCompositeCount).sum(); }
    public static int batchCount() { return POOL.size(); }
    public static void close() { POOL.forEach(SilhouetteMask::close); POOL.clear(); BATCHES.clear(); used = 0; }
}
