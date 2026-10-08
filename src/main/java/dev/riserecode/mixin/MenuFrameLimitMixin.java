package dev.riserecode.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class MenuFrameLimitMixin {
    @Shadow public abstract FramerateLimitTracker.FramerateThrottleReason getThrottleReason();
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void riserecode$uncapped(CallbackInfoReturnable<Integer> ci) {
        if (!Theme.bool("Uncapped Themed Menus", true) || !MenuTheme.activeMenu()) { return; }
        var reason = getThrottleReason();
        if (reason == FramerateLimitTracker.FramerateThrottleReason.NONE || reason == FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU) { ci.setReturnValue(Integer.MAX_VALUE); }
    }
}
