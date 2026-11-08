package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.ReachModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class CombatRangeMixin {
    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void riserecode$targeting(CallbackInfoReturnable<Double> ci) {
        var module = riserecode$module();
        if (module != null) { ci.setReturnValue(Math.max(ci.getReturnValue(), module.reach())); }
    }
    @Inject(method = "isWithinAttackRange", at = @At("RETURN"), cancellable = true)
    private void riserecode$attack(ItemStack stack, AABB box, double grow, CallbackInfoReturnable<Boolean> ci) {
        var module = riserecode$module();
        if (module == null || ci.getReturnValueZ()) { return; }
        double reach = module.reach() + grow;
        if (box.distanceToSqr(((Player) (Object) this).getEyePosition()) <= reach * reach) { ci.setReturnValue(true); }
    }
    @org.spongepowered.asm.mixin.Unique private ReachModule riserecode$module() {
        if (!((Object) this instanceof LocalPlayer) || !Client.INSTANCE.isInitialized()) { return null; }
        return Client.INSTANCE.getModuleManager().getByClass(ReachModule.class).filter(ReachModule::isEnabled).orElse(null);
    }
}
