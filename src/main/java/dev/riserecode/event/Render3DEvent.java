package dev.riserecode.event;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;

/**
 * Posted after world extraction. Collect render state here; this is not a
 * legacy immediate-mode drawing hook. Do not retain the mutable context.
 */
public final class Render3DEvent extends Event {
    private final LevelExtractionContext context;

    public Render3DEvent(LevelExtractionContext context) { this.context = context; }
    public LevelExtractionContext getContext() { return context; }
}
