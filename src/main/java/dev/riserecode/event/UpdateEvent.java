package dev.riserecode.event;

import net.minecraft.client.Minecraft;

/** Posted at the end of a client tick, including ticks outside a world. */
public final class UpdateEvent extends Event {
    private final Minecraft minecraft;
    private final long tick;

    public UpdateEvent(Minecraft minecraft, long tick) {
        this.minecraft = minecraft;
        this.tick = tick;
    }

    public Minecraft getMinecraft() { return minecraft; }
    public long getTick() { return tick; }
}
