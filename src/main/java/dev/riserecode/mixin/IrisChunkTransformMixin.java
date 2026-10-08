package dev.riserecode.mixin;

import dev.riserecode.render.ChunkTerrainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Patches Iris-generated AST declarations, never shader-pack files or pack lighting. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.transformer.SodiumTransformer", remap = false)
public abstract class IrisChunkTransformMixin {
    @ModifyArg(method = "injectVertInit", at = @At(value = "INVOKE", target = "Lio/github/douira/glsl_transformer/ast/node/TranslationUnit;parseAndInjectNodes(Lio/github/douira/glsl_transformer/ast/transform/ASTParser;Lio/github/douira/glsl_transformer/ast/transform/ASTInjectionPoint;[Ljava/lang/String;)V"), index = 2, require = 0)
    private static String[] riserecode$terrain(String[] nativeNodes) {
        var nodes = new java.util.ArrayList<String>();
        boolean translation = false;
        for (String node : nativeNodes) {
            if (node.contains("return _get_relative_chunk_coord(pos) * vec3(16.0f);")) {
                node = node.replace("return _get_relative_chunk_coord(pos) * vec3(16.0f);", "return _get_relative_chunk_coord(pos) * vec3(16.0f) + vec3(0.0, rise_chunk_offset(pos), 0.0);");
                translation = true;
            }
            node = node.replace("mc_chunkFade = (chunkFade < 0) ? 1.0 : fade;", "mc_chunkFade = (rise_ChunkParams.y > 0.5 || chunkFade < 0) ? 1.0 : fade;");
            nodes.add(node);
            if (node.equals("uniform uint u_RegionID;")) {
                nodes.add("layout(std140) uniform RiseChunkParams { vec4 rise_ChunkParams; };");
                nodes.add("uniform samplerBuffer u_RiseChunkOffsets;");
                nodes.add("float rise_chunk_offset(uint section) { return rise_ChunkParams.x > 0.5 ? texelFetch(u_RiseChunkOffsets, int(u_RegionID * 256u + section)).r : 0.0; }");
            }
        }
        if (!translation) { ChunkTerrainRenderer.incompatible("Unsupported Iris terrain transform"); return nativeNodes; }
        ChunkTerrainRenderer.irisPatched();
        return nodes.toArray(String[]::new);
    }
}
