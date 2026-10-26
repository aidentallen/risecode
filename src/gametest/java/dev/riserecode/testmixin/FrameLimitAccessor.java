package dev.riserecode.testmixin;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(FramerateLimitTracker.class)
public interface FrameLimitAccessor {
    @Accessor("latestInputTime") void riserecode$latestInput(long value);
}
