package dev.riserecode.testmixin;

import dev.riserecode.GuiPerformanceTest;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Saving status has a separate wall-clock fade; omit it only in deterministic pixel references. */
@Mixin(Hud.class)
public abstract class GuiSavingIndicatorMixin {
    @Inject(method="extractSavingIndicator",at=@At("HEAD"),cancellable=true)
    private void riserecode$referenceSavingIndicator(CallbackInfo ci){
        if(GuiPerformanceTest.freezeTheme)ci.cancel();
    }
    @Inject(method="extractVignette",at=@At("HEAD"),cancellable=true)
    private void riserecode$referenceVignette(CallbackInfo ci){
        if(GuiPerformanceTest.freezeTheme&&Boolean.getBoolean("riserecode.guiReferenceNoVignette"))ci.cancel();
    }
}
