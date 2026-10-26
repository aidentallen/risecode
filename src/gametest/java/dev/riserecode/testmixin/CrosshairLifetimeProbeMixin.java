package dev.riserecode.testmixin;

import dev.riserecode.LifetimeFaults;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class CrosshairLifetimeProbeMixin {
    @Inject(method="extractCrosshair",at=@At("HEAD"),cancellable=true)
    private void risecode$fault(CallbackInfo ci){if(LifetimeFaults.mode==1){ci.cancel();}else if(LifetimeFaults.mode==2){throw LifetimeFaults.FAILURE;}}
}
