package dev.riserecode.mixin;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import dev.riserecode.render.ChunkTerrainRenderer;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Extend only Iris terrain programs; inactive native pipeline layouts remain intact. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.ExtendedShader", remap = false)
public abstract class IrisChunkBindingsMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlProgram;setupBindGroupLayouts(Ljava/util/List;)V"), index = 0, require = 0)
    private List<BindGroupLayout> riserecode$terrain(List<BindGroupLayout> nativeLayouts) {
        if (nativeLayouts.stream().flatMap(layout -> layout.getUniforms().stream()).noneMatch(uniform -> uniform.name().equals("u_SectionTimeInfo"))) { return nativeLayouts; }
        var layouts = new ArrayList<>(nativeLayouts); layouts.add(ChunkTerrainRenderer.BINDINGS); return layouts;
    }
}
