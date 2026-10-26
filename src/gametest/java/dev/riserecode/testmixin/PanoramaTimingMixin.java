package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.PanoramaIntegrationTest;
import dev.riserecode.RemediationClientTest;
import dev.riserecode.ui.panorama.LivingPanorama;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingPanorama.class)
public abstract class PanoramaTimingMixin {
    @WrapMethod(method="draw")
    private static boolean risecode$sceneCost(GuiGraphicsExtractor g,Operation<Boolean> original){
        if(!PanoramaIntegrationTest.measuring)return original.call(g);
        long frames=LivingPanorama.renderer().frames();
        long bytes=RemediationClientTest.allocated(),cpu=RemediationClientTest.cpu(),start=System.nanoTime();
        try{return original.call(g);}finally{PanoramaIntegrationTest.cost(System.nanoTime()-start,RemediationClientTest.allocated()-bytes,RemediationClientTest.cpu()-cpu,LivingPanorama.renderer().frames()!=frames);}
    }
}
