package dev.riserecode.event;

import net.minecraft.client.player.AbstractClientPlayer;

/** Actual collision-resolved movement, subject to the legacy walk-distance gating. */
public final class CapeMoveEvent extends Event {
    public final AbstractClientPlayer player;
    public final double dx, dz;
    public CapeMoveEvent(AbstractClientPlayer player, double dx, double dz) { this.player = player; this.dx = dx; this.dz = dz; }
}
