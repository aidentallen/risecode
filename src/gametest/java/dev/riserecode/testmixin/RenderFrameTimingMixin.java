package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.RemediationClientTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

/** Measure update/extract/draw/present, excluding Fabric's barriers between renderFrame calls. */
@Mixin(Minecraft.class)
public abstract class RenderFrameTimingMixin {
    @WrapMethod(method="renderFrame")
    private void risecode$frameTime(boolean tick,Operation<Void> original){
        long allocated=RemediationClientTest.allocated(),cpu=RemediationClientTest.cpu(),start=System.nanoTime();
        try{original.call(tick);}finally{long elapsed=System.nanoTime()-start,bytes=RemediationClientTest.allocated()-allocated,spent=RemediationClientTest.cpu()-cpu;RemediationClientTest.rendered(elapsed,bytes,spent);dev.riserecode.GuiPerformanceTest.rendered(elapsed,bytes,spent);dev.riserecode.Release025Timings.rendered(elapsed,bytes,spent);dev.riserecode.GlassTimings.rendered(elapsed,bytes,spent);}
    }
}
