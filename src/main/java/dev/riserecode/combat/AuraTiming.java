package dev.riserecode.combat;

/** Client-thread attack timing measured in estimated server ticks, never wall-clock CPS. */
public final class AuraTiming {
    private double elapsed = Double.POSITIVE_INFINITY;
    private double switchRemaining;
    private long lastTimePacket;
    private double tps = 20;

    public void tick(boolean sync) {
        double step = sync ? tps / 20 : 1;
        elapsed += step;
        switchRemaining = Math.max(0, switchRemaining - step);
    }
    public void switched(int delay) { switchRemaining = delay; }
    public boolean switchReady() { return switchRemaining <= 0; }
    public boolean ready(boolean custom, int delay, double cooldownTicks, boolean charged) {
        return switchReady() && (custom ? elapsed >= delay : charged && elapsed >= cooldownTicks);
    }
    public void attacked() { elapsed = 0; }
    public void timePacket(long now) {
        if (lastTimePacket != 0) {
            double seconds = (now - lastTimePacket) * 1.0e-9;
            if (seconds > 0) { tps = Math.clamp(20 / seconds, 1, 20); }
        }
        lastTimePacket = now;
    }
    public boolean lagging(long now) { return lastTimePacket != 0 && now - lastTimePacket >= 3_000_000_000L; }
    public double tps() { return tps; }
    public void reset() { elapsed = Double.POSITIVE_INFINITY; switchRemaining = 0; lastTimePacket = 0; tps = 20; }
}
