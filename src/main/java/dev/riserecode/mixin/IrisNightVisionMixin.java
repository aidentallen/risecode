package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.FullBrightModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Change only the shader input, preserving real effects and the player's gamma setting. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisNightVisionMixin {
    @Inject(method = "getNightVision", at = @At("RETURN"), cancellable = true, require = 0)
    private static void riserecode$nightVision(CallbackInfoReturnable<Float> callback) {
        if (Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class)
                    .ifPresent(module -> callback.setReturnValue(module.shaderNightVision(callback.getReturnValue())));
        }
    }
}
