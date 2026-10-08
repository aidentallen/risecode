package dev.riserecode.event;

public final class MouseRotationEvent extends Event {
    private final double yaw, pitch;
    public MouseRotationEvent(double yaw, double pitch) { super(true); this.yaw = yaw; this.pitch = pitch; }
    private double multiplier=1;
    public void scaleInput(double value) { if(!Double.isFinite(value) || value<=0) { throw new IllegalArgumentException("Invalid input multiplier"); } multiplier*=value; }
    public double rawYaw() { return yaw*multiplier; }
    public double rawPitch() { return pitch*multiplier; }
    public double yawDelta() { return rawYaw() * 0.15; }
    public double pitchDelta() { return rawPitch() * 0.15; }
}
