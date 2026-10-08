package dev.riserecode.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** HUD extraction phase; never retain the extractor after dispatch. */
public final class Render2DEvent extends Event {
    private final GuiGraphicsExtractor graphics;
    private final DeltaTracker deltaTracker;
    private final double deltaSeconds;

    public Render2DEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, double deltaSeconds) {
        this.graphics = graphics;
        this.deltaTracker = deltaTracker;
        this.deltaSeconds = deltaSeconds;
    }

    public GuiGraphicsExtractor getGraphics() { return graphics; }
    public DeltaTracker getDeltaTracker() { return deltaTracker; }
    public double getDeltaSeconds() { return deltaSeconds; }
}
