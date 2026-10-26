package dev.riserecode.testmixin;

import dev.riserecode.GuiPerformanceTest;
import dev.riserecode.ui.menu.MenuTheme;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the native page-gear entrance out of fixed-clock font references. */
@Mixin(MenuTheme.class)
public abstract class GuiMenuClockMixin {
    @Inject(method="entrance",at=@At("HEAD"),cancellable=true)
    private static void riserecode$settledReference(Screen screen,CallbackInfoReturnable<Double> ci){
        if(GuiPerformanceTest.freezeTheme&&Boolean.getBoolean("riserecode.guiReferenceSettledMenus"))ci.setReturnValue(1.0);
    }
}
