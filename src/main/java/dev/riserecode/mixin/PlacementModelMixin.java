package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ModelBlockRenderer.class)
public abstract class PlacementModelMixin {
    @Inject(method="tesselateBlock",at=@At("HEAD"),cancellable=true)
    private void riserecode$hide(BlockQuadOutput output,float x,float y,float z,BlockAndTintGetter level,BlockPos pos,BlockState state,BlockStateModel model,long seed,CallbackInfo ci){if(PlacementAnimations.hidden(pos,state))ci.cancel();}
    @Inject(method="shouldRenderFace",at=@At("HEAD"),cancellable=true)
    private void riserecode$neighbors(BlockAndTintGetter level,BlockState state,Direction face,BlockPos neighbor,CallbackInfoReturnable<Boolean> ci){if(PlacementAnimations.hiddenNeighbor(neighbor))ci.setReturnValue(true);}
}
