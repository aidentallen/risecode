package dev.riserecode.mixin;

import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget")
public interface SodiumWidgetAccessor {
    @Mutable @Accessor("dim") void risecode$dimensions(Dim2i value);
}
