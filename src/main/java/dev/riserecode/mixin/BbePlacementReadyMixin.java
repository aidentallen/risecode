package dev.riserecode.mixin;

import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BBE's normal dispatcher resumes native models while placement owns their geometry. */
@Mixin(value=BlockEntity.class,priority=900)
public abstract class BbePlacementReadyMixin {
    @Inject(method="bbe$isTerrainMeshReady",at=@At("HEAD"),cancellable=true,remap=false)
    private void riserecode$immediate(CallbackInfoReturnable<Boolean> ci){
        if(PlacementAnimations.immediateBlockEntity(((BlockEntity)(Object)this).getBlockPos()))ci.setReturnValue(false);
    }
}
