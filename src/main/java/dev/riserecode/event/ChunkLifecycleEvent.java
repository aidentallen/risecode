package dev.riserecode.event;

/** Client-thread chunk residency; packet receipt does not start the visual clock. */
public final class ChunkLifecycleEvent extends Event {
    public enum Kind { LOAD, UNLOAD, RESET }
    private final Kind kind;
    private final long chunk;
    public ChunkLifecycleEvent(Kind kind, long chunk) { this.kind = kind; this.chunk = chunk; }
    public Kind kind() { return kind; }
    public long chunk() { return chunk; }
}
