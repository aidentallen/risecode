package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.FovEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public abstract class FovMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void riserecode$fov(float partial,CallbackInfoReturnable<Float> callback) {
        if(Client.INSTANCE.isInitialized()) { callback.setReturnValue(Client.INSTANCE.getEventBus().post(new FovEvent(callback.getReturnValue())).value()); }
    }
}
