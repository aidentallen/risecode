package dev.riserecode.testmixin;

import dev.riserecode.GuiPerformanceTest;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiElementCountMixin {
    @Inject(method="addGuiElement",at=@At("HEAD"))
    private void riserecode$count(GuiElementRenderState state,CallbackInfo ci){GuiPerformanceTest.elements++;}
}
