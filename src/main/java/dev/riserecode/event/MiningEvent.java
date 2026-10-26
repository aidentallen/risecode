package dev.riserecode.event;

import net.minecraft.core.BlockPos;

/** Client-thread mining boundary; progress adjustments still use vanilla prediction/packets. */
public final class MiningEvent extends Event {
    public enum Phase { START, CONTINUE, STOP }
    private final Phase phase;
    private final BlockPos position;
    private boolean noDelay;
    private double multiplier = 1;
    public MiningEvent(Phase phase, BlockPos position) { this.phase = phase; this.position = position.immutable(); }
    public Phase phase() { return phase; }
    public BlockPos position() { return position; }
    public boolean noDelay() { return noDelay; }
    public void noDelay(boolean value) { noDelay = value; }
    public double multiplier() { return multiplier; }
    public void multiplier(double value) { if (!Double.isFinite(value) || value < 1 || value > 5) { throw new IllegalArgumentException("Mining multiplier"); } multiplier = value; }
}
