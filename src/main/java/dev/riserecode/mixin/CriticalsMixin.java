package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.CriticalsModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class CriticalsMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void riserecode$crit(Player player, Entity target, CallbackInfo ci) {
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getModuleManager().getByClass(CriticalsModule.class).filter(CriticalsModule::isEnabled).ifPresent(module -> module.beforeAttack(target)); }
    }
}
