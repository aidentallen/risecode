package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.render.placement.PlacementBuildStamp;
import java.util.Collection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager",remap=false)
public abstract class SodiumPlacementUploadMixin {
    @Inject(method="uploadResults",at=@At("RETURN"))
    private void riserecode$uploaded(Collection<BuilderTaskOutput> outputs,UniformBufferManager uniforms,CallbackInfo ci){
        for(var output:outputs)if(output instanceof ChunkBuildOutput&&output instanceof PlacementBuildStamp stamp)PlacementAnimations.meshReady(output.section.getPosition().asLong(),stamp.riserecode$placementStamp());
    }
}
