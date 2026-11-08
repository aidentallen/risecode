package dev.riserecode.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Right click cooldown access for FastPlace; vanilla still decides when a click is sent. */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay") int riserecode$rightClickDelay();
    @Accessor("rightClickDelay") void riserecode$setRightClickDelay(int value);
}
