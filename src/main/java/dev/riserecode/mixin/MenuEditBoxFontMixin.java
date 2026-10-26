package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native editing/cursor logic, using the same styled metrics as rendered characters. */
@Mixin(EditBox.class)
public abstract class MenuEditBoxFontMixin {
    @Inject(method="applyFormat",at=@At("RETURN"),cancellable=true)
    private void risecode$font(String text,int offset,CallbackInfoReturnable<FormattedCharSequence> ci){if(MenuTheme.enabled()){ci.setReturnValue(Theme.nativeSequence(ci.getReturnValue()));}}
    @Redirect(method={"extractWidgetRenderState","updateTextPosition","getScreenX"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int risecode$width(Font font,String text){return MenuTheme.enabled()?font.width(Theme.nativeText(net.minecraft.network.chat.Component.literal(text))):font.width(text);}
    @Redirect(method={"findClickedPositionInText","extractWidgetRenderState","updateTextPosition","scrollTo"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String risecode$head(Font font,String text,int width){return MenuTheme.enabled()?font.getSplitter().plainHeadByWidth(text,width,Theme.nativeStyle(false)):font.plainSubstrByWidth(text,width);}
    @Redirect(method="scrollTo",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;"))
    private String risecode$tail(Font font,String text,int width,boolean reverse){return MenuTheme.enabled()?(reverse?font.getSplitter().plainTailByWidth(text,width,Theme.nativeStyle(false)):font.getSplitter().plainHeadByWidth(text,width,Theme.nativeStyle(false))):font.plainSubstrByWidth(text,width,reverse);}
}
