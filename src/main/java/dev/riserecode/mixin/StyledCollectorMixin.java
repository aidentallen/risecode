package dev.riserecode.mixin;

import dev.riserecode.ui.font.StyledSequence;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector")
public abstract class StyledCollectorMixin {
    @Shadow @Final private GuiGraphicsExtractor this$0;
    @Shadow public abstract void accept(Style style);
    @Inject(method="accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",at=@At("HEAD"),cancellable=true)
    private void risecode$draw(TextAlignment alignment,int anchor,int y,ActiveTextCollector.Parameters parameters,FormattedCharSequence text,CallbackInfo ci){
        if(!(text instanceof StyledSequence line)){return;}
        var font=Minecraft.getInstance().font;double left=alignment.calculateLeft(anchor,(int)Math.ceil(line.layout().width(font))),scale=line.scale();
        this$0.pose().pushMatrix();this$0.pose().set(parameters.pose());
        try{line.layout().draw(this$0,font,left,y,scale,Theme.opacity(Theme.TEXT,parameters.opacity()*line.progress()));}finally{this$0.pose().popMatrix();}
        var pointer=(GraphicsPointerAccessor)this$0;
        if(parameters.scissor()==null||parameters.scissor().containsPoint(pointer.risecode$mouseX(),pointer.risecode$mouseY())){
            Vector2f local=parameters.pose().invert(new Matrix3x2f()).transformPosition(pointer.risecode$mouseX(),pointer.risecode$mouseY(),new Vector2f());
            if(local.y>=y&&local.y<y+9*Theme.number("Font Scale",1)*scale){Style style=line.layout().styleAt(font,(local.x-left)/scale);if(style!=null){accept(style);}}
        }ci.cancel();
    }
}
