package dev.riserecode.mixin;

import dev.riserecode.ui.menu.PageAppearanceScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The appearance modal suspends a live menu without returning its carried stack. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerLifetimeMixin {
    @Inject(method="removed",at=@At("HEAD"),cancellable=true)
    private void risecode$suspend(CallbackInfo ci){if(PageAppearanceScreen.suspends((net.minecraft.client.gui.screens.Screen)(Object)this)){ci.cancel();}}
}
