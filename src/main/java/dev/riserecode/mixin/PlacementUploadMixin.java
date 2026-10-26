package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.render.placement.PlacementBuildStamp;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class PlacementUploadMixin {
    @Shadow public abstract long getSectionNode();
    @Inject(method="setSectionMesh",at=@At("RETURN"))
    private void riserecode$uploaded(SectionMesh mesh,CallbackInfoReturnable<SectionMesh> ci){
        if(mesh instanceof PlacementBuildStamp stamp)PlacementAnimations.meshReady(getSectionNode(),stamp.riserecode$placementStamp());
    }
}
