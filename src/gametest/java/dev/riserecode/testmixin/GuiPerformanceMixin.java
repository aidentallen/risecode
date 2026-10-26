package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.GuiPerformanceTest;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import java.util.List;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiPerformanceMixin {
    @Shadow @Final private List<?> draws;
    @WrapMethod(method="render")
    private void riserecode$guiTiming(Operation<Void> original){
        GuiPerformanceTest.gpu(0);long start=System.nanoTime();
        try{original.call();}finally{GuiPerformanceTest.guiNanos+=System.nanoTime()-start;GuiPerformanceTest.gpu(3);}
    }
    @Inject(method="prepare",at=@At("RETURN"))
    private void riserecode$draws(CallbackInfo ci){GuiPerformanceTest.draws+=draws.size();}
}
