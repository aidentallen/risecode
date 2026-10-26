package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleInterfaceMixin extends Screen {
    protected TitleInterfaceMixin(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void riserecode$interface(CallbackInfo ci) {
        if (Client.INSTANCE.isInitialized()) { addRenderableWidget(Button.builder(Component.literal(Client.INSTANCE.getName() + " Interface"), ignored -> Client.INSTANCE.getClickGui().open(this)).bounds(width/2-80, height-29, 160, 20).build()); }
    }
}
