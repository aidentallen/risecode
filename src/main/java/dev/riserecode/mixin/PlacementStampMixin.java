package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementBuildStamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(targets={"net.minecraft.client.renderer.chunk.SectionCompiler$Results","net.minecraft.client.renderer.chunk.CompiledSectionMesh"})
public abstract class PlacementStampMixin implements PlacementBuildStamp {
    @Unique private long riserecode$stamp;
    @Override public long riserecode$placementStamp(){return riserecode$stamp;}
    @Override public void riserecode$placementStamp(long stamp){riserecode$stamp=stamp;}
}
