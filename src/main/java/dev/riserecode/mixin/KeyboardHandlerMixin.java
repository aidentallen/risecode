package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.KeyPressEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void riserecode$keyPress(long window, int action, KeyEvent input, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized() || window != Minecraft.getInstance().getWindow().handle()) { return; }
        var mc = Minecraft.getInstance();
        if (action != 0 && (mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui || mc.gui.screen() instanceof dev.riserecode.ui.hud.HudEditorScreen)
                && dev.riserecode.ui.component.FocusedInput.key(input, mc.keyboardHandler::getClipboard, mc.keyboardHandler::setClipboard)) { callback.cancel(); return; }
        KeyPressEvent event = Client.INSTANCE.getEventBus().post(new KeyPressEvent(input, action));
        if (event.isCancelled()) { callback.cancel(); }
    }
}
