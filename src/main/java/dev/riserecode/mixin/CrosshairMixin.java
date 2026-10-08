package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.CrosshairEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class CrosshairMixin {
    @Inject(method="extractCrosshair",at=@At("HEAD"))
    private void riserecode$begin(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo callback) {
        double scale=Client.INSTANCE.isInitialized()?Client.INSTANCE.getEventBus().post(new CrosshairEvent()).scale():1;
        g.pose().pushMatrix();g.pose().translate(g.guiWidth()/2f,g.guiHeight()/2f);g.pose().scale((float)scale,(float)scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight()/2f);
    }
    @Inject(method="extractCrosshair",at=@At("RETURN"))
    private void riserecode$end(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo callback) { g.pose().popMatrix(); }
}
