package dev.riserecode.util;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client-thread, visible rotations. Vanilla sends the resulting angles on its next movement tick. */
public final class RotationManager implements AutoCloseable {
    public enum Mode { NONE, SNAPPY, SMOOTH }
    public record Rotation(float yaw, float pitch) {
        public Rotation {
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) { throw new IllegalArgumentException("Angles must be finite"); }
            yaw = wrapDegrees(yaw); pitch = Math.max(-90, Math.min(90, pitch));
            // Canonical zero keeps equivalent orientations equal when cached or compared as records.
            if (yaw == 0) { yaw = 0; }
            if (pitch == 0) { pitch = 0; }
        }
    }
    private record Request(Rotation rotation, Mode mode, double speed, long tick) { }
    private record SavedRotation(float yaw, float pitch, float head, float body) { }

    /** Refresh a request each tick; close the lease when its module releases control. */
    public final class Lease implements AutoCloseable {
        private final Object owner;
        private final int priority;
        private final long sequence;
        private Request request;
        private boolean closed;
        private Lease(Object owner, int priority) { this.owner = owner; this.priority = priority; sequence = nextSequence++; }
        public void request(Rotation rotation, Mode mode, double degreesPerSecond) {
            if (closed || RotationManager.this.closed) { throw new IllegalStateException("Rotation lease is closed"); }
            Objects.requireNonNull(rotation); Objects.requireNonNull(mode);
            if (!Double.isFinite(degreesPerSecond) || degreesPerSecond <= 0) {
                throw new IllegalArgumentException("Rotation speed must be positive and finite");
            }
            request = mode == Mode.NONE ? null : new Request(rotation, mode, degreesPerSecond, tick);
            if (request == null && active == this) { active = null; if (winner().isEmpty()) { restore(); } }
        }
        public Object getOwner() { return owner; }
        public boolean isActive() { return !closed && active == this; }
        @Override
        public void close() {
            if (closed) { return; }
            closed = true; request = null; leases.remove(owner);
            if (active == this) { active = null; if (winner().isEmpty()) { restore(); } }
        }
    }

    private final Minecraft minecraft;
    private final LongSupplier clock;
    private final EventBus.Subscription subscription;
    private final Map<Object, Lease> leases = new IdentityHashMap<>();
    private LocalPlayer controlled;
    private SavedRotation saved;
    private Lease active;
    private Lease readyLease;
    private Rotation previousTickRotation;
    private int readyPlayerTick = Integer.MAX_VALUE;
    private long tick;
    private long nextSequence;
    private long previousNanos;
    private int appliedPlayerTick = Integer.MAX_VALUE;
    private boolean closed;

    public RotationManager(EventBus bus, Minecraft minecraft) { this(bus, minecraft, System::nanoTime); }
    public RotationManager(EventBus bus, Minecraft minecraft, LongSupplier clock) {
        this.minecraft = Objects.requireNonNull(minecraft); this.clock = Objects.requireNonNull(clock);
        // Modules submit at priority 0. Apply after their decisions, before config persistence.
        subscription = bus.subscribe(UpdateEvent.class, -50, false, this::update);
    }
    public Lease acquire(Object owner, int priority) {
        Objects.requireNonNull(owner);
        if (closed) { throw new IllegalStateException("Rotation manager is closed"); }
        if (leases.containsKey(owner)) { throw new IllegalArgumentException("Owner already has a rotation lease"); }
        Lease lease = new Lease(owner, priority); leases.put(owner, lease); return lease;
    }
    public Optional<Object> getActiveOwner() { return active == null ? Optional.empty() : Optional.of(active.owner); }
    public int getLeaseCount() { return leases.size(); }
    /** Wait a player tick after applying angles so the ordinary movement send precedes the attack. */
    public boolean isReady(Lease lease, Rotation desired, double toleranceDegrees) {
        if (!Double.isFinite(toleranceDegrees) || toleranceDegrees < 0) {
            throw new IllegalArgumentException("Tolerance must be finite and non-negative");
        }
        LocalPlayer player = minecraft.player;
        return lease != null && active == lease && readyLease == lease && player != null && controlled == player
                && player.tickCount == readyPlayerTick && previousTickRotation != null
                && angularDistance(previousTickRotation, desired) <= toleranceDegrees
                && angularDistance(new Rotation(player.getYRot(), player.getXRot()), desired) <= toleranceDegrees;
    }
    private Optional<Lease> winner() {
        return leases.values().stream().filter(lease -> !lease.closed && lease.request != null && tick - lease.request.tick() <= 1)
                .min(Comparator.comparingInt((Lease lease) -> lease.priority).reversed().thenComparingLong(lease -> lease.sequence));
    }
    private void update(UpdateEvent event) {
        tick++;
        long now = clock.getAsLong();
        double delta = previousNanos == 0 ? 0.05 : Math.max(0, Math.min(0.1, (now - previousNanos) * 1.0e-9));
        previousNanos = now;
        LocalPlayer player = minecraft.player;
        if (controlled != null && controlled != player) { discard(); }
        if (player == null || !player.isAlive() || player.isSpectator()) { restore(); return; }
        Lease next = winner().orElse(null);
        if (next == null) { restore(); return; }
        Rotation current = new Rotation(player.getYRot(), player.getXRot());
        readyLease = active == next && player.tickCount > appliedPlayerTick ? next : null;
        readyPlayerTick = player.tickCount;
        previousTickRotation = current;
        if (controlled == null) {
            controlled = player; saved = new SavedRotation(player.getYRot(), player.getXRot(), player.yHeadRot, player.yBodyRot);
        }
        active = next;
        Request request = next.request;
        Rotation result = request.mode() == Mode.SNAPPY ? request.rotation() : smooth(current, request.rotation(), request.speed(), delta);
        player.setYRot(result.yaw()); player.setXRot(result.pitch());
        player.yHeadRot = result.yaw(); player.yBodyRot = result.yaw();
        appliedPlayerTick = player.tickCount;
    }
    private void restore() {
        if (controlled != null && controlled == minecraft.player && saved != null) {
            controlled.setYRot(saved.yaw()); controlled.setXRot(saved.pitch());
            controlled.yHeadRot = saved.head(); controlled.yBodyRot = saved.body();
        }
        discard();
    }
    private void discard() {
        controlled = null; saved = null; active = null; readyLease = null; previousTickRotation = null;
        appliedPlayerTick = Integer.MAX_VALUE; readyPlayerTick = Integer.MAX_VALUE;
    }
    public static Rotation toward(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        if (!Double.isFinite(delta.x) || !Double.isFinite(delta.y) || !Double.isFinite(delta.z)) {
            throw new IllegalArgumentException("Positions must be finite");
        }
        double horizontal = Math.hypot(delta.x, delta.z);
        if (delta.lengthSqr() < 1.0e-12) { return new Rotation(0, 0); }
        return new Rotation((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90),
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }
    public static Rotation towardEntity(Player player, LivingEntity entity) { return toward(player.getEyePosition(), TargetUtil.aimPoint(entity)); }
    public static Rotation towardBlock(Player player, BlockPos position) { return toward(player.getEyePosition(), Vec3.atCenterOf(position)); }
    public static Rotation smooth(Rotation current, Rotation desired, double speed, double deltaSeconds) {
        if (!Double.isFinite(speed) || speed <= 0 || !Double.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("Speed must be positive and delta time non-negative; both must be finite");
        }
        float yawDelta = wrapDegrees(desired.yaw() - current.yaw());
        double distance = Math.hypot(yawDelta, desired.pitch() - current.pitch());
        double fraction = distance < 1.0e-9 ? 1 : MathUtil.clamp01(speed * deltaSeconds / distance);
        return new Rotation((float) MathUtil.lerp(current.yaw(), current.yaw() + yawDelta, fraction),
                (float) MathUtil.lerp(current.pitch(), desired.pitch(), fraction));
    }
    public static double angularDistance(Rotation first, Rotation second) {
        return Math.hypot(wrapDegrees(second.yaw() - first.yaw()), second.pitch() - first.pitch());
    }
    public static float wrapDegrees(float degrees) {
        if (!Float.isFinite(degrees)) { throw new IllegalArgumentException("Angle must be finite"); }
        float wrapped = degrees % 360;
        return wrapped >= 180 ? wrapped - 360 : wrapped < -180 ? wrapped + 360 : wrapped;
    }
    /** Optional bounded visual variation; this does not spoof server-facing rotation. */
    public static Rotation randomOffset(Rotation rotation, double amplitude, RandomGenerator random) {
        if (!Double.isFinite(amplitude) || amplitude < 0 || amplitude > 10) {
            throw new IllegalArgumentException("Offset amplitude must be between 0 and 10 degrees");
        }
        Objects.requireNonNull(random);
        return new Rotation((float) (rotation.yaw() + MathUtil.lerp(-amplitude, amplitude, random.nextDouble())),
                (float) (rotation.pitch() + MathUtil.lerp(-amplitude, amplitude, random.nextDouble())));
    }
    public static Vec3 direction(Rotation rotation) {
        double yaw = Math.toRadians(rotation.yaw()); double pitch = Math.toRadians(rotation.pitch());
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, -Math.sin(pitch), Math.cos(yaw) * horizontal);
    }
    @Override
    public void close() {
        if (closed) { return; }
        closed = true; subscription.close();
        for (Lease lease : List.copyOf(leases.values())) { lease.close(); }
        restore();
    }
}
