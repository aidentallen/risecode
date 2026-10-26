package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.GuiPerformanceTest;
import dev.riserecode.util.BlurUtil;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlurUtil.class)
public abstract class GuiBlurTimingMixin {
    @WrapMethod(method="renderBeforeGui")
    private static void riserecode$blur(RenderTarget main,Operation<Void> original){
        GuiPerformanceTest.beforeBlur(main);GuiPerformanceTest.gpu(1);try{original.call(main);}finally{GuiPerformanceTest.gpu(2);}
    }
}
