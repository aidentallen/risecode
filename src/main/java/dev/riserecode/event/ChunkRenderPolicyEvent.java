package dev.riserecode.event;

import dev.riserecode.render.ChunkMotionTracker;

/** Modules contribute once per frame rather than subscribing to individual GPU draws. */
public final class ChunkRenderPolicyEvent extends Event {
    private boolean suppressFade;
    private ChunkMotionTracker motion;
    private ChunkMotionTracker.Settings settings;
    public void suppressFade() { suppressFade = true; }
    public boolean fadeSuppressed() { return suppressFade; }
    public void animate(ChunkMotionTracker tracker, ChunkMotionTracker.Settings value) {
        motion = tracker; settings = value; suppressFade();
    }
    public ChunkMotionTracker motion() { return motion; }
    public ChunkMotionTracker.Settings settings() { return settings; }
}
