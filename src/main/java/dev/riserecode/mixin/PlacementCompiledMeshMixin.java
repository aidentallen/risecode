package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementBuildStamp;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CompiledSectionMesh.class)
public abstract class PlacementCompiledMeshMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void riserecode$stamp(TranslucencyPointOfView view,SectionCompiler.Results results,CallbackInfo ci){
        ((PlacementBuildStamp)this).riserecode$placementStamp(((PlacementBuildStamp)(Object)results).riserecode$placementStamp());
    }
}
