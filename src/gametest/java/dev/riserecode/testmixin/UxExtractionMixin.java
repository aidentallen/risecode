package dev.riserecode.testmixin;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
@Mixin(value={dev.riserecode.ui.clickgui.ClickGui.class,dev.riserecode.ui.component.CosmeticDetailsScreen.class},remap=false)
public abstract class UxExtractionMixin {
    @WrapMethod(method="extractRenderState")
    private void riserecode$uxCost(GuiGraphicsExtractor g,int x,int y,float partial,Operation<Void> original){if(!dev.riserecode.UxPerformanceTest.recording){original.call(g,x,y,partial);return;}long bytes=dev.riserecode.RemediationClientTest.allocated(),start=System.nanoTime();try{original.call(g,x,y,partial);}finally{dev.riserecode.UxPerformanceTest.extracted(System.nanoTime()-start,dev.riserecode.RemediationClientTest.allocated()-bytes);}}
}
