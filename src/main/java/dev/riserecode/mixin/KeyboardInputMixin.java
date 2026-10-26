package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CameraInputEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("RETURN"))
    private void riserecode$cameraInput(CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        CameraInputEvent event = new CameraInputEvent(keyPresses, moveVector); Client.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) { keyPresses = Input.EMPTY; moveVector = Vec2.ZERO; }
    }
}
