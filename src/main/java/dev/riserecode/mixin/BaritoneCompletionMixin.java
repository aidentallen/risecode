package dev.riserecode.mixin;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.riserecode.Client;
import dev.riserecode.command.CommandCompletions;
import dev.riserecode.command.ClientCommandCompletions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A single completion owner for client-prefix commands; slash commands remain vanilla. */
@Mixin(CommandSuggestions.class)
public abstract class BaritoneCompletionMixin {
    @Shadow @Final private EditBox input;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow public abstract void showSuggestions(boolean narration);
    @Shadow public abstract void hide();
    @Unique private boolean risecode$owned;
    @Inject(method="updateCommandInfo",at=@At("TAIL"))
    private void riserecode$complete(CallbackInfo ci){
        if(!Client.INSTANCE.isInitialized()){return;}
        var client=Client.INSTANCE;String text=input.getValue();
        if(!text.startsWith(client.getCommandManager().getPrefix())||!client.getClientPreferences().suggestions.getValue()){
            if(risecode$owned && text.startsWith(client.getCommandManager().getPrefix())){hide();}risecode$owned=false;return;
        }
        risecode$owned=true;
        var values=CommandCompletions.suggest(client.getCommandManager(),text,input.getCursorPosition(),ClientCommandCompletions::arguments);
        if(values.isEmpty()){hide();return;}
        var list=values.stream().map(c->new Suggestion(StringRange.between(c.start(),c.end()),c.text())).toList();
        pendingSuggestions=CompletableFuture.completedFuture(Suggestions.create(text,list));showSuggestions(false);
    }
}
