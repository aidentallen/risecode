package dev.riserecode.mixin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractArrow.class)
public interface ArrowPhysicsAccessor {
    @Invoker("isInGround") boolean risecode$grounded();
    @Invoker("getWaterInertia") float risecode$waterDrag();
    @Invoker("getAirDrag") float risecode$airDrag();
}
