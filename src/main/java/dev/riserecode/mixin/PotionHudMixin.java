package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.hud.PotionEffectsModule;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class PotionHudMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void riserecode$customEffects(CallbackInfo callback) {
        if (Client.INSTANCE.isInitialized() && Client.INSTANCE.getModuleManager().getByClass(PotionEffectsModule.class)
                .map(PotionEffectsModule::hidesVanilla).orElse(false)) { callback.cancel(); }
    }
}
