package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext",remap=false)
public abstract class SodiumPlacementFacesMixin {
    @Shadow protected BlockPos pos;
    @Inject(method="shouldDrawSide",at=@At("HEAD"),cancellable=true)
    private void riserecode$face(Direction side,CallbackInfoReturnable<Boolean> ci){if(PlacementAnimations.hiddenNeighbor(pos.relative(side)))ci.setReturnValue(true);}
}
