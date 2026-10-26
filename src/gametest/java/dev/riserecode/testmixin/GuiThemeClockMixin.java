package dev.riserecode.testmixin;

import dev.riserecode.GuiPerformanceTest;
import dev.riserecode.ui.theme.Theme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Theme.class)
public abstract class GuiThemeClockMixin {
    @Shadow private static double gradientSeconds;
    @Inject(method="advance",at=@At("HEAD"),cancellable=true)
    private static void riserecode$frozenScreenshot(long now,CallbackInfo ci){if(GuiPerformanceTest.freezeTheme){gradientSeconds=0;ci.cancel();}}
}
