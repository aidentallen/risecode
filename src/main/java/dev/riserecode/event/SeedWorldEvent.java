package dev.riserecode.event;

import net.minecraft.client.multiplayer.ClientLevel;

/** After client-thread login/respawn application. Raw packet events are unsuitable. */
public final class SeedWorldEvent extends Event {
    public final ClientLevel level;
    public SeedWorldEvent(ClientLevel level) { this.level = level; }
}
