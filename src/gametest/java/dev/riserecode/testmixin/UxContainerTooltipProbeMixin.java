package dev.riserecode.testmixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
/** Forced long text on a real server-opened menu, exclusively in the test mod. */
@Mixin(AbstractContainerScreen.class)
public abstract class UxContainerTooltipProbeMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void riserecode$tooltip(GuiGraphicsExtractor g,int x,int y,float partial,CallbackInfo ci){if(dev.riserecode.UxTooltipIntegrationTest.containerTooltip){g.setTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font,dev.riserecode.UxTooltipIntegrationTest.content(),java.util.Optional.empty(),x,y);}}
}
