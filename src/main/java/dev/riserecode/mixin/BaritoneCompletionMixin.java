package dev.riserecode.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.riserecode.Client;
import dev.riserecode.module.modules.BaritoneModule;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public abstract class BaritoneCompletionMixin {
    @Shadow @Final private EditBox input;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow public abstract void showSuggestions(boolean narration);
    @Inject(method = "updateCommandInfo", at = @At("TAIL"))
    private void riserecode$complete(CallbackInfo ci) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        String text = input.getValue().substring(0, input.getCursorPosition()), prefix = Client.INSTANCE.getCommandManager().getPrefix();
        String command = text.startsWith(prefix + "b ") ? text.substring(prefix.length() + 2) : text.startsWith(prefix + "baritone ") ? text.substring(prefix.length() + 9) : null;
        if (command == null) { return; }
        var module = Client.INSTANCE.getModuleManager().getByClass(BaritoneModule.class).orElseThrow();
        if (!module.access().available()) { return; }
        int start = text.lastIndexOf(' ') + 1; var builder = new SuggestionsBuilder(text, start);
        try { module.access().complete(command).forEach(builder::suggest); pendingSuggestions = builder.buildFuture(); showSuggestions(false); }
        catch (RuntimeException failure) { Client.INSTANCE.getLogger().debug("Baritone completion unavailable", failure); }
    }
}
