package dev.riserecode.event;
public final class CrosshairEvent extends Event {
    private double scale=1;
    public double scale() { return scale; }
    public void scale(double value) { if (!Double.isFinite(value) || value < .25 || value > 4) { throw new IllegalArgumentException("Invalid crosshair scale"); } scale=value; }
}
