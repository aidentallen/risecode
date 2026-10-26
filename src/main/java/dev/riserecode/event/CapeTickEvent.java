package dev.riserecode.event;

import net.minecraft.client.player.AbstractClientPlayer;

public final class CapeTickEvent extends Event {
    public enum Phase { BEGIN, BOB, END }
    public final AbstractClientPlayer player;
    public final Phase phase;
    public CapeTickEvent(AbstractClientPlayer player, Phase phase) { this.player = player; this.phase = phase; }
}
