package dev.riserecode.mixin;

import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CycleButton.class)
public interface CycleButtonAccessor {
    @Accessor("displayState") CycleButton.DisplayState risecode$displayState();
}
