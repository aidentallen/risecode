package dev.riserecode.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiGraphicsExtractor.class)
public interface GraphicsPointerAccessor {
    @Accessor("mouseX") int risecode$mouseX();
    @Accessor("mouseY") int risecode$mouseY();
}
