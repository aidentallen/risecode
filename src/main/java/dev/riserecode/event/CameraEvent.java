package dev.riserecode.event;

import net.minecraft.world.phys.Vec3;

/** Applied after vanilla camera alignment, before frustum and projection extraction. */
public final class CameraEvent extends Event {
    private Vec3 position;
    private float yaw, pitch;
    private boolean detached;
    public CameraEvent(Vec3 position, float yaw, float pitch, boolean detached) { this.position = position; this.yaw = yaw; this.pitch = pitch; this.detached = detached; }
    public Vec3 position() { return position; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public boolean detached() { return detached; }
    public void set(Vec3 position, float yaw, float pitch) { this.position = position; this.yaw = yaw; this.pitch = pitch; detached = true; }
}
