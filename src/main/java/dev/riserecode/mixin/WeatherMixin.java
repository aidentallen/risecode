package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.WeatherChangerModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class WeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void riserecode$rain(float partial, CallbackInfoReturnable<Float> callback) { override(callback, false); }
    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void riserecode$thunder(float partial, CallbackInfoReturnable<Float> callback) { override(callback, true); }
    private void override(CallbackInfoReturnable<Float> callback, boolean thunder) {
        if ((Object) this instanceof ClientLevel && Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getModuleManager().getByClass(WeatherChangerModule.class).filter(WeatherChangerModule::isEnabled)
                    .ifPresent(module -> callback.setReturnValue(thunder ? module.thunder() : module.rain()));
        }
    }
}
