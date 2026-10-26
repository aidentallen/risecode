package dev.riserecode.render;

import dev.riserecode.util.MathUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One flight per chunk column. No world objects or mesh data are retained. */
public final class ChunkMotionTracker {
    public enum Direction { UP, DOWN }
    public enum Curve {
        OUT_QUART(MathUtil.Easing.OUT_QUART), IN_OUT_CUBIC(MathUtil.Easing.IN_OUT_CUBIC), OUT_ELASTIC(MathUtil.Easing.OUT_ELASTIC);
        private final MathUtil.Easing easing;
        Curve(MathUtil.Easing value) { easing = value; }
        public double apply(double progress) { return easing.apply(progress); }
    }
    public record Settings(Direction direction, double distance, double duration, Curve curve) {
        public Settings {
            Objects.requireNonNull(direction); Objects.requireNonNull(curve);
            if (!Double.isFinite(distance) || distance < 0 || !Double.isFinite(duration) || duration < 0.05) {
                throw new IllegalArgumentException("Invalid chunk animation settings");
            }
        }
        public float initialOffset() { return (float)(direction == Direction.UP ? -distance : distance); }
    }
    private static final class Flight {
        private long start = Long.MIN_VALUE;
        private Settings settings;
    }
    private final Set<Long> resident = new HashSet<>();
    private final Map<Long, Flight> flights = new HashMap<>();
    private volatile Map<Long, Float> offsets = Map.of();
    private long frameTime;
    private Settings defaults;
    private long starts;
    public static long key(int x, int z) { return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32); }
    public void loaded(long chunk) { if (resident.add(chunk)) { flights.put(chunk, new Flight()); } }
    public void unloaded(long chunk) {
        resident.remove(chunk); flights.remove(chunk);
        if (offsets.containsKey(chunk)) { var next = new HashMap<>(offsets); next.remove(chunk); offsets = Map.copyOf(next); }
    }
    public void clear() { resident.clear(); flights.clear(); offsets = Map.of(); defaults = null; }
    public void frame(long now, Settings settings) {
        frameTime = now; defaults = settings;
        var next = new HashMap<Long, Float>();
        flights.entrySet().removeIf(entry -> {
            Flight flight = entry.getValue();
            if (flight.start == Long.MIN_VALUE) { next.put(entry.getKey(), settings.initialOffset()); return false; }
            double progress = Math.max(0, (now - flight.start) * 1.0e-9 / flight.settings.duration());
            if (progress >= 1) { return true; }
            next.put(entry.getKey(), (float)(flight.settings.initialOffset() * (1 - flight.settings.curve().apply(progress))));
            return false;
        });
        offsets = Map.copyOf(next);
    }
    public float drawable(int x, int z) {
        long key = key(x, z); Flight flight = flights.get(key);
        if (flight != null && flight.start == Long.MIN_VALUE && defaults != null) {
            flight.start = frameTime; flight.settings = defaults; starts++;
        }
        return offset(x, z);
    }
    public float offset(int x, int z) { return offsets.getOrDefault(key(x, z), 0f); }
    public boolean moving(int x, int z) { return offsets.containsKey(key(x, z)); }
    public int pendingCount() { return flights.size(); }
    public long starts() { return starts; }
}
