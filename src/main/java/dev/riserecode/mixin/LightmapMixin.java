package dev.riserecode.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.Client;
import dev.riserecode.module.modules.FullBrightModule;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Inject(method = "getTextureView", at = @At("RETURN"), cancellable = true)
    private void riserecode$lightmap(CallbackInfoReturnable<GpuTextureView> callback) {
        if (Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class)
                    .ifPresent(module -> callback.setReturnValue(module.lightmap(callback.getReturnValue())));
        }
    }
}
