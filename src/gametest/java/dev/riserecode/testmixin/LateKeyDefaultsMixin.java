package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Fabric's initial snapshot can precede optional mods registering key mappings. */
@Mixin(Options.class)
public abstract class LateKeyDefaultsMixin {
    @WrapOperation(method = "processOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$FieldAccess;process(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    private String riserecode$lateDefault(@Coerce Object access, String name, String current, Operation<String> original) {
        String restored = original.call(access, name, current);
        return restored == null && name.startsWith("key_") ? current : restored;
    }
}
