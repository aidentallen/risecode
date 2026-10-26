package dev.riserecode.mixin;

import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value=BBEBlockRenderer.class,remap=false)
public abstract class BbePlacementMeshMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private static void riserecode$hide(BlockEntityExt ext,CallbackInfoReturnable<Boolean> ci){
        if(ext instanceof BlockEntity entity && PlacementAnimations.hidden(entity.getBlockPos(),entity.getBlockState())){PlacementAnimations.bbeSuppressed();ci.setReturnValue(false);}
    }
}
