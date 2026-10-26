package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.MiningEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(MultiPlayerGameMode.class)
public abstract class MiningMixin {
    @Shadow private int destroyDelay;
    @Shadow private BlockPos destroyBlockPos;
    @org.spongepowered.asm.mixin.Unique private double risecode$multiplier = 1;
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void risecode$start(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) { risecode$event(MiningEvent.Phase.START, pos); }
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void risecode$continue(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) { risecode$event(MiningEvent.Phase.CONTINUE, pos); }
    @Inject(method = "stopDestroyBlock", at = @At("TAIL"))
    private void risecode$stop(CallbackInfo ci) { risecode$event(MiningEvent.Phase.STOP, destroyBlockPos); }
    @org.spongepowered.asm.mixin.Unique private void risecode$event(MiningEvent.Phase phase, BlockPos pos) {
        risecode$multiplier = 1;
        if (!Client.INSTANCE.isInitialized()) { return; }
        var event = Client.INSTANCE.getEventBus().post(new MiningEvent(phase, pos));
        if (event.noDelay()) { destroyDelay = 0; }
        risecode$multiplier = event.multiplier();
    }
    @ModifyExpressionValue(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float risecode$progress(float value) { return (float) (value * risecode$multiplier); }
}
