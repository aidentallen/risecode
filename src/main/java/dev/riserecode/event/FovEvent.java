package dev.riserecode.event;
/** Native world-camera FOV after vanilla modifiers. */
public final class FovEvent extends Event {
    private float value;
    public FovEvent(float value) { this.value = value; }
    public float value() { return value; }
    /** Vanilla can report zero before the first camera tick; leave native values alone until zooming. */
    public void applyMagnification(double magnification) {
        if (!Double.isFinite(magnification) || magnification < 1) {
            throw new IllegalArgumentException("Invalid magnification");
        }
        if (magnification == 1 || !Float.isFinite(value) || value <= 0) { return; }
        set((float) Math.clamp(value / magnification, 1.0, 179.0));
    }
    public void set(float value) { if (!Float.isFinite(value) || value <= 0) { throw new IllegalArgumentException("Invalid FOV"); } this.value = Math.clamp(value, 1, 179); }
}
