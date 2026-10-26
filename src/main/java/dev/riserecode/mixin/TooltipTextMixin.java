package dev.riserecode.mixin;

import dev.riserecode.ui.font.StyledText;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.menu.TooltipMotion;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientTextTooltip.class)
public abstract class TooltipTextMixin implements TooltipMotion.Text {
    @Shadow @Final private FormattedCharSequence text;
    @Override public StyledText risecode$layout(){return StyledText.of(text);}
    @Inject(method="getWidth",at=@At("HEAD"),cancellable=true)
    private void risecode$width(Font font,CallbackInfoReturnable<Integer> ci){if(MenuTheme.activeMenu()){ci.setReturnValue((int)Math.ceil(risecode$layout().width(font)));}}
    @Inject(method="getHeight",at=@At("HEAD"),cancellable=true)
    private void risecode$height(Font font,CallbackInfoReturnable<Integer> ci){if(MenuTheme.activeMenu()){ci.setReturnValue((int)Math.ceil(9*Theme.number("Font Scale",1)));}}
    @Inject(method="extractText",at=@At("HEAD"),cancellable=true)
    private void risecode$text(GuiGraphicsExtractor g,Font font,int x,int y,CallbackInfo ci){if(MenuTheme.activeMenu()){risecode$layout().draw(g,font,x,y,1,Theme.opacity(Theme.TEXT,TooltipMotion.alpha()));ci.cancel();}}
}
