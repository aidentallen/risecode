package dev.riserecode.mixin;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.sound.UiSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Includes native and supported mod controls, even when they bypass AbstractWidget. */
@Mixin(SoundManager.class)
public abstract class UiSoundMixin {
    @Inject(method="play",at=@At("HEAD"),cancellable=true)
    private void risecode$sound(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci){
        if(!UiSounds.dispatching()&&MenuTheme.activeMenu()&&sound.getIdentifier().toString().equals("minecraft:ui.button.click")){ci.setReturnValue(UiSounds.play());}
    }
}
