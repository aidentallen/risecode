package dev.riserecode.testmixin;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MultiPlayerGameMode.class)
public interface MiningStateAccessor {
    @Accessor("destroyDelay") void risecode$delay(int value);
    @Accessor("destroyDelay") int risecode$delay();
    @Accessor("destroyProgress") float risecode$progress();
}
