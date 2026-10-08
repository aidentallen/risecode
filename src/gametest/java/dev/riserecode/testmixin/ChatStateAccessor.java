package dev.riserecode.testmixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only snapshot access, present only in the isolated test mod. */
@Mixin(ChatComponent.State.class)
public interface ChatStateAccessor {
    @Accessor("messages") List<GuiMessage> riserecode$getMessages();
}
