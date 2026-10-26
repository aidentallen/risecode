package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.ChatDisplayEvent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ChatComponent.class)
public abstract class BetterChatMixin {
    /** allMessages keeps the original component for reporting/deletion. Decorate only rendered lines. */
    @ModifyVariable(method="addMessageToDisplayQueue",at=@At("HEAD"),argsOnly=true)
    private GuiMessage risecode$display(GuiMessage message){
        if(!Client.INSTANCE.isInitialized()){return message;}var event=Client.INSTANCE.getEventBus().post(new ChatDisplayEvent(message,message.content(),message.signature()));
        return new GuiMessage(message.addedTime(),event.display(),message.signature(),message.source(),message.tag());
    }
    @ModifyConstant(method={"addMessageToDisplayQueue","addMessageToQueue"},constant=@Constant(intValue=100))
    private int risecode$history(int vanilla){return Client.INSTANCE.isInitialized()?Client.INSTANCE.getEventBus().post(new ChatDisplayEvent.History()).limit():vanilla;}
}
