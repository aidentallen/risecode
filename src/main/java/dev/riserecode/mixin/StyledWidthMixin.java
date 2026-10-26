package dev.riserecode.mixin;

import dev.riserecode.ui.font.StyledSequence;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class StyledWidthMixin {
    @Inject(method="width(Lnet/minecraft/util/FormattedCharSequence;)I",at=@At("HEAD"),cancellable=true)
    private void risecode$width(FormattedCharSequence text,CallbackInfoReturnable<Integer> ci){if(text instanceof StyledSequence line){ci.setReturnValue((int)Math.ceil(line.layout().width((Font)(Object)this)));}}
}
