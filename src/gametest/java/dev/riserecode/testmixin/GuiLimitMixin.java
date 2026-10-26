package dev.riserecode.testmixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class GuiLimitMixin {
    @Inject(method="getFramerateLimit",at=@At("RETURN"),cancellable=true)
    private void riserecode$equalUncapped(CallbackInfoReturnable<Integer> ci){
        if(Boolean.getBoolean("riserecode.guiPerformanceOnly")){ci.setReturnValue(Integer.MAX_VALUE);}
    }
}
