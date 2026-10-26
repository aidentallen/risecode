package dev.riserecode.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Direct value access only for the gamma option owned by FullBright; vanilla validation is unchanged. */
@Mixin(OptionInstance.class)
public interface GammaOptionAccessor {
    @Accessor("value") void riserecode$setRawValue(Object value);
}
