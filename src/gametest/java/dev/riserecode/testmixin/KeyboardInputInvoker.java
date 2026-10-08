package dev.riserecode.testmixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Fabric TestInput supplies zero modifier flags; exercise the real callback with GLFW flags. */
@Mixin(KeyboardHandler.class)
public interface KeyboardInputInvoker {
    @Invoker("keyPress") void riserecode$press(long window, int action, KeyEvent event);
}
