package dev.riserecode.testmixin;

import dev.riserecode.Client;
import dev.riserecode.GuiPerformanceTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Normalize only the version label in old/new screenshot references. */
@Mixin(Client.class)
public abstract class GuiVersionMixin {
    @Inject(method="getVersion",at=@At("HEAD"),cancellable=true)
    private void riserecode$referenceVersion(CallbackInfoReturnable<String> ci){if(GuiPerformanceTest.freezeVersion)ci.setReturnValue("0.24.1");}
}
