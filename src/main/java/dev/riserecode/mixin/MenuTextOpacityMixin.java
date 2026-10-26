package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTransition;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiTextRenderState.class)
public abstract class MenuTextOpacityMixin {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=2)
    private static int risecode$text(int color) { return MenuTransition.color(color); }
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=3)
    private static int risecode$background(int color) { return MenuTransition.color(color); }
}
