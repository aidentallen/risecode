package dev.riserecode.event;

/** A chunk packet has been applied, including replacements of resident chunks. */
public final class SeedChunkEvent extends Event {
    public final long chunk;
    public SeedChunkEvent(long chunk) { this.chunk=chunk; }
}
