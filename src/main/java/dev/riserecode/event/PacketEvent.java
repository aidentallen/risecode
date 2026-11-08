package dev.riserecode.event;

import java.util.Objects;
import net.minecraft.network.protocol.Packet;

/** Synchronous wire-boundary events. Receive normally runs on a Netty thread. */
public abstract class PacketEvent extends Event {
    private final Packet<?> originalPacket;
    private Packet<?> packet;
    private final net.minecraft.network.Connection source;

    protected PacketEvent(Packet<?> packet) {
        this(packet, null);
    }
    protected PacketEvent(Packet<?> packet, net.minecraft.network.Connection source) {
        super(true); this.packet = Objects.requireNonNull(packet); originalPacket = packet; this.source = source;
    }
    public final net.minecraft.network.Connection source() { return source; }
    public final Packet<?> getPacket() { return packet; }
    public final Packet<?> getOriginalPacket() { return originalPacket; }
    public final void setPacket(Packet<?> replacement) {
        Objects.requireNonNull(replacement);
        if (replacement.type().flow() != originalPacket.type().flow()) {
            throw new IllegalArgumentException("Replacement packet has the wrong direction");
        }
        packet = replacement;
    }
    public static final class Send extends PacketEvent { public Send(Packet<?> packet) { super(packet); } public Send(Packet<?> packet, net.minecraft.network.Connection source) { super(packet, source); } }
    public static final class Receive extends PacketEvent { public Receive(Packet<?> packet) { super(packet); } public Receive(Packet<?> packet, net.minecraft.network.Connection source) { super(packet, source); } }
}
