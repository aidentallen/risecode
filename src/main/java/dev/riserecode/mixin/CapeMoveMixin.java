package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.Client;
import dev.riserecode.event.CapeMoveEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class CapeMoveMixin {
    @WrapMethod(method = "move")
    private void risecode$capeMove(MoverType type, Vec3 movement, Operation<Void> original) {
        if (!((Object) this instanceof AbstractClientPlayer player) || !Client.INSTANCE.isInitialized()) {
            original.call(type, movement); return;
        }
        double x = player.getX(), z = player.getZ();
        boolean walk = !player.noPhysics && !player.isPassenger() && !player.getAbilities().flying
                && !(player.onGround() && player.isShiftKeyDown());
        original.call(type, movement);
        if (walk) { Client.INSTANCE.getEventBus().post(new CapeMoveEvent(player, player.getX() - x, player.getZ() - z)); }
    }
}
