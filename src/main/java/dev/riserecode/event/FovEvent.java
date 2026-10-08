package dev.riserecode.event;
/** Native world-camera FOV after vanilla modifiers. */
public final class FovEvent extends Event {
    private float value;
    public FovEvent(float value) { this.value = value; }
    public float value() { return value; }
    public void set(float value) { if (!Float.isFinite(value) || value <= 0) { throw new IllegalArgumentException("Invalid FOV"); } this.value = Math.clamp(value, 1, 179); }
}
