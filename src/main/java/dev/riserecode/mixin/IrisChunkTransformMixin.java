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
        String[] nodes=dev.riserecode.render.IrisTerrainTransform.patch(nativeNodes);
        if(nodes==nativeNodes){ChunkTerrainRenderer.incompatible("Unsupported Iris terrain transformation anchors");return nativeNodes;}
        ChunkTerrainRenderer.irisPatched();return nodes;
    }
}
