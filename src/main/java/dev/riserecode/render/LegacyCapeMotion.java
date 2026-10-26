package dev.riserecode.render;

/** Render-only 1.8.9 cape history. Samples advance on entity ticks, never on render frames. */
public final class LegacyCapeMotion {
    private static final float[] SIN = new float[65536];
    static {
        for (int i = 0; i < SIN.length; i++) { SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0); }
    }
    private double x, y, z, oldX, oldY, oldZ;
    private double cloakX, cloakY, cloakZ, oldCloakX, oldCloakY, oldCloakZ;
    private float yaw, oldYaw, walk, oldWalk, bob, oldBob;
    private boolean ready;

    public record Angles(float flap, float lean, float sway) {
        public float pitch() { return 6.0F + lean / 2.0F + flap; }
    }

    public LegacyCapeMotion(double x, double y, double z, float yaw) {
        this.x = oldX = cloakX = oldCloakX = x;
        this.y = oldY = cloakY = oldCloakY = y;
        this.z = oldZ = cloakZ = oldCloakZ = z;
        this.yaw = oldYaw = yaw;
    }

    public boolean ready() { return ready; }
    public void begin() {
        oldX = x; oldY = y; oldZ = z; oldYaw = yaw;
        oldCloakX = cloakX; oldCloakY = cloakY; oldCloakZ = cloakZ;
        oldWalk = walk; oldBob = bob;
    }

    public void moved(double dx, double dz) {
        // The old renderer used distanceWalkedModified (horizontal distance), not step distance.
        walk = (float) (walk + (double) (float) Math.sqrt(dx * dx + dz * dz) * 0.6D);
    }

    public void updateBob(double vx, double vz, boolean groundedAndAlive) {
        float target = (float) Math.sqrt(vx * vx + vz * vz);
        if (target > 0.1F) { target = 0.1F; }
        if (!groundedAndAlive) { target = 0; }
        bob += (target - bob) * 0.4F;
    }
    public void resetBob() { bob = 0; }

    public void finish(double x, double y, double z, float yaw) {
        this.x = x; this.y = y; this.z = z; this.yaw = yaw;
        double dx = x - cloakX, dy = y - cloakY, dz = z - cloakZ;
        // 1.8.9 snaps each axis before applying the already-computed quarter-step delta.
        if (dx > 10 || dx < -10) { cloakX = oldCloakX = x; }
        if (dy > 10 || dy < -10) { cloakY = oldCloakY = y; }
        if (dz > 10 || dz < -10) { cloakZ = oldCloakZ = z; }
        cloakX += dx * 0.25D; cloakY += dy * 0.25D; cloakZ += dz * 0.25D;
        ready = true;
    }

    public Angles sample(float partial, boolean sneaking, float strength) {
        double dx = lerp(oldCloakX, cloakX, partial) - lerp(oldX, x, partial);
        double dy = lerp(oldCloakY, cloakY, partial) - lerp(oldY, y, partial);
        double dz = lerp(oldCloakZ, cloakZ, partial) - lerp(oldZ, z, partial);
        float angle = (oldYaw + (yaw - oldYaw) * partial) * (float) Math.PI / 180.0F;
        double sin = sin(angle), cos = -cos(angle);
        float flap = Math.clamp((float) dy * 10.0F, -6.0F, 32.0F);
        float lean = (float) (dx * sin + dz * cos) * 100.0F;
        float sway = (float) (dx * cos - dz * sin) * 100.0F;
        if (lean < 0) { lean = 0; }
        float cameraBob = oldBob + (bob - oldBob) * partial;
        flap += sin((oldWalk + (walk - oldWalk) * partial) * 6.0F) * 32.0F * cameraBob;
        return new Angles(flap * strength + (sneaking ? 25.0F : 0), lean * strength, sway * strength);
    }

    private static double lerp(double a, double b, float partial) { return a + (b - a) * partial; }
    private static float sin(float value) { return SIN[(int) (value * 10430.378F) & 65535]; }
    private static float cos(float value) { return SIN[(int) (value * 10430.378F + 16384.0F) & 65535]; }
}
