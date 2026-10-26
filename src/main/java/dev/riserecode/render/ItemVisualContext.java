package dev.riserecode.render;

import net.minecraft.world.item.ItemDisplayContext;

/** Captured during extraction and carried by each immutable native model submission. */
public record ItemVisualContext(ItemDisplayContext display, boolean enchanted, boolean foil) {
    public static final ThreadLocal<ItemVisualContext> CURRENT = new ThreadLocal<>();
    public interface State {
        ItemVisualContext risecode$itemContext();
        void risecode$itemContext(ItemVisualContext context);
    }
}
