package dev.riserecode.ui.animation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Independent state machines advanced once per frame. No worker or graphics threads. */
public final class AnimationManager {
    private static final AnimationManager UI = new AnimationManager();
    private final Set<Animation> active = ConcurrentHashMap.newKeySet();
    private long previousFrame;
    private boolean driving;
    public static AnimationManager ui() { return UI; }
    public void register(Animation animation) { active.add(animation); }
    public void unregister(Animation animation) { active.remove(animation); }
    public int activeCount() { return active.size(); }
    public boolean isDriving() { return driving; }
    public void frame(long now) {
        double delta = previousFrame == 0 ? 0 : Math.max(0, Math.min(0.25, (now - previousFrame) * 1e-9));
        previousFrame = now; driving = true; update(delta);
    }
    /** Explicit-delta entrypoint also supports deterministic concurrency checks. */
    public void update(double delta) {
        if (!Double.isFinite(delta) || delta < 0) { throw new IllegalArgumentException("Invalid frame delta"); }
        for (Animation animation : active) {
            animation.advance(delta);
            if (!animation.isRunning()) { active.remove(animation); }
        }
    }
    public void clear() { active.clear(); previousFrame = 0; driving = false; }
}
