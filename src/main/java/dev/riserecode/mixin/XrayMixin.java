package dev.riserecode.mixin;

import dev.riserecode.module.modules.XrayModule;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XrayMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void riserecode$shape(CallbackInfoReturnable<RenderShape> ci) { if (XrayModule.hides((BlockState) (Object) this)) { ci.setReturnValue(RenderShape.INVISIBLE); } }
    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void riserecode$occlude(CallbackInfoReturnable<Boolean> ci) { if (XrayModule.hides((BlockState) (Object) this)) { ci.setReturnValue(false); } }
}
