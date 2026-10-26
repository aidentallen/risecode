package dev.riserecode.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.ClientLevelData.class)
public interface SeedLevelDataAccessor {
    @Accessor("isFlat") boolean riserecode$isFlat();
}
