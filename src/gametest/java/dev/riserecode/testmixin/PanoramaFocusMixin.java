package dev.riserecode.testmixin;

import dev.riserecode.PanoramaIntegrationTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PanoramaFocusMixin {
    @Inject(method="isWindowActive",at=@At("HEAD"),cancellable=true)
    private void risecode$unfocused(CallbackInfoReturnable<Boolean> ci){if(PanoramaIntegrationTest.forceUnfocused)ci.setReturnValue(false);}
}
