package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.CrosshairEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Hud.class)
public abstract class CrosshairMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="extractCrosshair")
    private void riserecode$crosshair(GuiGraphicsExtractor g,DeltaTracker delta,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        double scale=Client.INSTANCE.isInitialized()?Client.INSTANCE.getEventBus().post(new CrosshairEvent()).scale():1;
        g.pose().pushMatrix();
        try {
            g.pose().translate(g.guiWidth()/2f,g.guiHeight()/2f);g.pose().scale((float)scale,(float)scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight()/2f);
            original.call(g,delta);
        } finally { g.pose().popMatrix(); }
    }
}
