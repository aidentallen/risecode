package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTransition;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiRenderState.class)
public abstract class MenuOpacityMixin {
    @ModifyVariable(method="addGuiElement",at=@At("HEAD"),argsOnly=true)
    private GuiElementRenderState risecode$foreground(GuiElementRenderState state) { return MenuTransition.capture(state); }
}
