package dev.riserecode.util;

import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Shared vanilla attack dispatch plus independent owner timing and one managed attack per tick. */
public final class AttackManager implements AutoCloseable {
    public enum SwingMode { NONE, CLIENT, SERVER, BOTH }
    public record Policy(double minCps, double maxCps, boolean cooldownAware) {
        public Policy {
            if (!Double.isFinite(minCps) || !Double.isFinite(maxCps) || minCps <= 0 || minCps > maxCps || maxCps > 20) {
                throw new IllegalArgumentException("CPS must satisfy 0 < min <= max <= 20");
            }
        }
    }
    /** One interval is sampled after each dispatched attack; readiness checks never resample it. */
    public static final class TimingGate {
        private final LongSupplier clock;
        private final RandomGenerator random;
        private long nextAttack;
        private boolean scheduled;
        public TimingGate(LongSupplier clock, RandomGenerator random) { this.clock = Objects.requireNonNull(clock); this.random = Objects.requireNonNull(random); }
        public boolean isReady() { return !scheduled || clock.getAsLong() - nextAttack >= 0; }
        public long remainingNanos() { return scheduled ? Math.max(0, nextAttack - clock.getAsLong()) : 0; }
        public void recordAttack(Policy policy) { nextAttack = clock.getAsLong() + sampleDelayNanos(policy, random); scheduled = true; }
        public void reset() { scheduled = false; }
    }
    public final class Session implements AutoCloseable {
        private final Object owner;
        private final TimingGate timer = new TimingGate(clock, random);
        private boolean closed;
        private Session(Object owner) { this.owner = owner; }
        public boolean shouldAttackThisTick(Policy policy) {
            Objects.requireNonNull(policy);
            LocalPlayer player = minecraft.player;
            return !closed && !AttackManager.this.closed && !dispatching && tick != lastAttackTick && player != null
                    && minecraft.level != null && minecraft.gameMode != null && minecraft.getConnection() != null
                    && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                    && timer.isReady() && (!policy.cooldownAware() || player.getAttackStrengthScale(0) >= 1);
        }
        public long remainingNanos() { return timer.remainingNanos(); }
        public OptionalInt bestWeapon() { return minecraft.player == null ? OptionalInt.empty() : ItemUtil.bestHotbarWeapon(minecraft.player); }
        public void reset() { timer.reset(); }
        public boolean attack(LivingEntity target, Policy policy, SwingMode swing) {
            Objects.requireNonNull(target); Objects.requireNonNull(swing);
            if (!shouldAttackThisTick(policy) || !validForAttack(target)) { return false; }
            LocalPlayer player = minecraft.player;
            AttackEvent intent;
            dispatching = true;
            try { intent = bus.post(new AttackEvent(player, target)); }
            finally { dispatching = false; }
            // A listener may disable the module, replace the player, or issue another managed attack.
            if (intent.isCancelled() || minecraft.player != player || !shouldAttackThisTick(policy) || !validForAttack(target)) { return false; }
            lastAttackTick = tick;
            minecraft.gameMode.attack(player, target);
            swing(player, InteractionHand.MAIN_HAND, swing);
            timer.recordAttack(policy);
            return true;
        }
        private boolean validForAttack(LivingEntity target) {
            LocalPlayer player = minecraft.player;
            return target != player && target.level() == player.level() && target.isAlive() && !target.isRemoved()
                    && target.isAttackable() && player.isWithinAttackRange(player.getMainHandItem(), target.getBoundingBox(), 0);
        }
        @Override
        public void close() { if (!closed) { closed = true; sessions.remove(owner); timer.reset(); } }
    }

    private final EventBus bus;
    private final Minecraft minecraft;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private final EventBus.Subscription subscription;
    private final Map<Object, Session> sessions = new IdentityHashMap<>();
    private LocalPlayer observedPlayer;
    private Object observedLevel;
    private long tick;
    private long lastAttackTick = Long.MIN_VALUE;
    private boolean closed;
    private boolean dispatching;

    public AttackManager(EventBus bus, Minecraft minecraft) { this(bus, minecraft, System::nanoTime, RandomGenerator.getDefault()); }
    public AttackManager(EventBus bus, Minecraft minecraft, LongSupplier clock, RandomGenerator random) {
        this.bus = Objects.requireNonNull(bus); this.minecraft = Objects.requireNonNull(minecraft);
        this.clock = Objects.requireNonNull(clock); this.random = Objects.requireNonNull(random);
        subscription = bus.subscribe(UpdateEvent.class, 200, false, event -> {
            tick++;
            if (observedPlayer != minecraft.player || observedLevel != minecraft.level) {
                observedPlayer = minecraft.player; observedLevel = minecraft.level; lastAttackTick = Long.MIN_VALUE;
                sessions.values().forEach(Session::reset);
            }
        });
    }
    public Session openSession(Object owner) {
        Objects.requireNonNull(owner);
        if (closed) { throw new IllegalStateException("Attack manager is closed"); }
        if (sessions.containsKey(owner)) { throw new IllegalArgumentException("Owner already has an attack session"); }
        Session session = new Session(owner); sessions.put(owner, session); return session;
    }
    public int getSessionCount() { return sessions.size(); }
    public static long sampleDelayNanos(Policy policy, RandomGenerator random) {
        double cps = MathUtil.lerp(policy.minCps(), policy.maxCps(), Objects.requireNonNull(random).nextDouble());
        return (long) Math.ceil(1_000_000_000.0 / cps);
    }
    public static void swing(LocalPlayer player, InteractionHand hand, SwingMode mode) {
        switch (mode) {
            case NONE -> { }
            case CLIENT -> player.swing(hand, false);
            case SERVER -> player.connection.send(new ServerboundSwingPacket(hand));
            case BOTH -> player.swing(hand);
        }
    }
    @Override
    public void close() {
        if (closed) { return; }
        closed = true; subscription.close();
        for (Session session : List.copyOf(sessions.values())) { session.close(); }
        observedPlayer = null; observedLevel = null;
    }
}
