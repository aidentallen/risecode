package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseMenuMixin extends Screen {
    protected PauseMenuMixin(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void risecode$gui(CallbackInfo ci) {
        if (((PauseScreen)(Object)this).showsPauseMenu() && Client.INSTANCE.isInitialized()) {
            addRenderableWidget(Button.builder(Component.literal("risecode GUI"), button -> Client.INSTANCE.getClickGui().open(this))
                    .bounds(width - 110, 8, 102, 20).build());
        }
    }
}
