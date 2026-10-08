package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Custom attributes follow the standard Position/UV0 fields in the 26.2 GUI vertex builder. */
@Mixin(BufferBuilder.class)
public interface VertexDataAccessor {
    @Accessor("vertexPointer") long riserecode$vertexPointer();
}
