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
        // The GUI toggle and explicit key capture must precede the text-input owner.
        if (action != 0 && mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui
                && (Client.INSTANCE.getKeybindManager().isCapturing()
                    || Client.INSTANCE.getKeybindManager().getClickGuiKey().matches(input))) {
            KeyPressEvent priority = Client.INSTANCE.getEventBus().post(new KeyPressEvent(input, action));
            if (priority.isCancelled()) { callback.cancel(); return; }
        }
        var auxiliary = dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen());
        if (action != 0 && auxiliary != null) {
            if (auxiliary.key(input)) { callback.cancel(); return; }
            if (input.key() == 266 || input.key() == 267) {
                mc.gui.screen().mouseScrolled(mc.gui.screen().width*.75, mc.gui.screen().height*.5, 0, input.key()==266?8:-8);
                callback.cancel(); return;
            }
        }
        if (action != 0 && input.key() != 258 && (mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui || mc.gui.screen() instanceof dev.riserecode.ui.hud.HudEditorScreen)
                && dev.riserecode.ui.component.FocusedInput.key(input, mc.keyboardHandler::getClipboard, mc.keyboardHandler::setClipboard)) { callback.cancel(); return; }
        KeyPressEvent event = Client.INSTANCE.getEventBus().post(new KeyPressEvent(input, action));
        if (event.isCancelled()) { callback.cancel(); }
    }
}
