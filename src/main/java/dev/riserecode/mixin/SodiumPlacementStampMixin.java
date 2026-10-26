package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementBuildStamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput",remap=false)
public abstract class SodiumPlacementStampMixin implements PlacementBuildStamp {
    @Unique private long riserecode$stamp;
    @Override public long riserecode$placementStamp(){return riserecode$stamp;}
    @Override public void riserecode$placementStamp(long stamp){riserecode$stamp=stamp;}
}
