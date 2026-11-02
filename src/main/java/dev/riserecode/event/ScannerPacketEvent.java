package dev.riserecode.event;

import net.minecraft.network.protocol.Packet;

/** Accepted packet, after vanilla application on the client thread. Parsing belongs to scanner workers. */
public final class ScannerPacketEvent extends Event {
    public final int x,z;
    public final Packet<?> packet;
    public ScannerPacketEvent(int x,int z,Packet<?> packet){this.x=x;this.z=z;this.packet=packet;}
}
