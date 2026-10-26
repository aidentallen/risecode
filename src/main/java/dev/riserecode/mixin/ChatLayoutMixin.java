package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.BetterChatModule;
import dev.riserecode.ui.font.StyledText;
import dev.riserecode.ui.font.StyledSequence;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiMessage.class)
public abstract class ChatLayoutMixin {
    @Inject(method="splitLines",at=@At("HEAD"),cancellable=true)
    private void risecode$layout(Font font,int width,CallbackInfoReturnable<List<FormattedCharSequence>> ci){
        if(!Client.INSTANCE.isInitialized()||!Client.INSTANCE.getModuleManager().getByClass(BetterChatModule.class).map(BetterChatModule::isEnabled).orElse(false)){return;}
        GuiMessage message=(GuiMessage)(Object)this;
        ci.setReturnValue(StyledText.of(message.content()).wrap(font,width).stream().map(line->(FormattedCharSequence)new StyledSequence(line,message.addedTime())).toList());
    }
}
