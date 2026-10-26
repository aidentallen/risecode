package dev.riserecode.mixin;

import dev.riserecode.ui.font.StyledSequence;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveTextCollector.ClickableStyleFinder.class)
public abstract class StyledClickMixin {
    @Shadow @Final private Font font;
    @Shadow @Final private int testX;
    @Shadow @Final private int testY;
    @Shadow private boolean includeInsertions;
    @Shadow private Style result;
    @Inject(method="accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",at=@At("HEAD"),cancellable=true)
    private void risecode$hit(TextAlignment alignment,int anchor,int y,ActiveTextCollector.Parameters parameters,FormattedCharSequence text,CallbackInfo ci){
        if(!(text instanceof StyledSequence line)){return;}ci.cancel();
        if(parameters.scissor()!=null&&!parameters.scissor().containsPoint(testX,testY)){return;}
        Vector2f local=parameters.pose().invert(new Matrix3x2f()).transformPosition(testX,testY,new Vector2f());double scale=line.scale(),left=alignment.calculateLeft(anchor,(int)Math.ceil(line.layout().width(font)));
        if(local.y>=y&&local.y<y+9*Theme.number("Font Scale",1)*scale){Style style=line.layout().styleAt(font,(local.x-left)/scale);if(style!=null&&(style.getClickEvent()!=null||includeInsertions&&style.getInsertion()!=null)){result=style;}}
    }
}
