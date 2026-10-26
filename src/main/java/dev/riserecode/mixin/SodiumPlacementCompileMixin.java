package dev.riserecode.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.render.placement.PlacementBuildStamp;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask",remap=false)
public abstract class SodiumPlacementCompileMixin {
    @WrapMethod(method="execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
    private ChunkBuildOutput riserecode$compile(ChunkBuildContext context,CancellationToken cancel,Operation<ChunkBuildOutput> original){
        long generation=PlacementAnimations.beginBuild();try{var result=original.call(context,cancel);if(result!=null)((PlacementBuildStamp)(Object)result).riserecode$placementStamp(generation);return result;}finally{PlacementAnimations.endBuild();}
    }
}
