package dev.riserecode.testmixin;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
/** Explicit test-only caps override the production themed-menu uncapping policy for paired pacing samples. */
@Mixin(FramerateLimitTracker.class)
public abstract class UxFrameCapMixin {
    @WrapMethod(method="getFramerateLimit")
    private int riserecode$uxCap(Operation<Integer> original){int value=original.call();int cap=Integer.getInteger("riserecode.uxPerfCap",0);return Boolean.getBoolean("riserecode.uxPerformanceOnly")&&cap>0?cap:value;}
}
