package dev.riserecode.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int risecode$left();
    @Accessor("topPos") int risecode$top();
    @Accessor("imageWidth") int risecode$width();
    @Accessor("imageHeight") int risecode$height();
}
