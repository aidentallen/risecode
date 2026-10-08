# Phase 6 — Advanced module engine

Implemented in `/home/toast/Work/riserecode`, Minecraft 26.2 / Fabric / Java 25,
mod version 0.4.0. These utilities and the KillAura implementation are original
code. No Meteor source was opened, copied, or imported, and no GPL implementation
or Meteor dependency was added. The existing repository license status is unchanged.

The project retains Minecraft 26.2's unobfuscated published names, with the existing
Fabric Loom 1.17.21 workflow and no Yarn dependency. This matches
[Fabric's 26.2 API/mapping guidance](https://fabricmc.net/2026/06/15/262.html).
No preview features or new libraries are needed. Existing setting, EventBus,
MathUtil, UI/HUD, friend, config, profile, and packet systems were preserved.

## 1. Updated project file tree

Generated output, caches, and runtime files are excluded. New production utilities
are in `util/`; the existing KillAura, client wiring, module registration, and
AttackEvent documentation were updated. New checks are in separate source sets.

```text
riserecode/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/
│   └── verify.sh
├── src/
│   ├── gametest/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── testmixin/
│   │   │           │   └── ChatStateAccessor.java
│   │   │           ├── ClientIntegrationTest.java
│   │   │           ├── CombatIntegrationTest.java
│   │   │           └── EngineIntegrationTest.java
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       └── riserecode-test.mixins.json
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── command/
│   │   │           │   ├── commands/
│   │   │           │   │   ├── BindCommand.java
│   │   │           │   │   ├── FriendCommand.java
│   │   │           │   │   ├── HelpCommand.java
│   │   │           │   │   ├── ProfileCommand.java
│   │   │           │   │   └── ToggleCommand.java
│   │   │           │   ├── Command.java
│   │   │           │   └── CommandManager.java
│   │   │           ├── config/
│   │   │           │   ├── ConfigManager.java
│   │   │           │   ├── JsonStorage.java
│   │   │           │   └── ProfileManager.java
│   │   │           ├── event/
│   │   │           │   ├── AttackEvent.java
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── PacketEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── friend/
│   │   │           │   └── FriendManager.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   ├── ChatInputMixin.java
│   │   │           │   ├── ConnectionMixin.java
│   │   │           │   └── KeyboardHandlerMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   ├── FullBrightModule.java
│   │   │           │   │   ├── KillAuraModule.java
│   │   │           │   │   ├── NoFallModule.java
│   │   │           │   │   ├── SprintModule.java
│   │   │           │   │   └── TracersModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
│   │   │           ├── render/
│   │   │           │   └── WorldRenderer.java
│   │   │           ├── setting/
│   │   │           │   ├── BooleanSetting.java
│   │   │           │   ├── ColorSetting.java
│   │   │           │   ├── DoubleSetting.java
│   │   │           │   ├── EnumSetting.java
│   │   │           │   └── Setting.java
│   │   │           ├── ui/
│   │   │           │   ├── animation/
│   │   │           │   │   └── Animation.java
│   │   │           │   ├── clickgui/
│   │   │           │   │   └── ClickGui.java
│   │   │           │   ├── component/
│   │   │           │   │   ├── setting/
│   │   │           │   │   │   ├── ColorSwatch.java
│   │   │           │   │   │   ├── EnumSelector.java
│   │   │           │   │   │   ├── Slider.java
│   │   │           │   │   │   └── ToggleSwitch.java
│   │   │           │   │   ├── ActionButton.java
│   │   │           │   │   ├── ModuleButton.java
│   │   │           │   │   ├── Panel.java
│   │   │           │   │   └── UIComponent.java
│   │   │           │   ├── hud/
│   │   │           │   │   └── HudRenderer.java
│   │   │           │   └── theme/
│   │   │           │       └── Theme.java
│   │   │           ├── util/
│   │   │           │   ├── AttackManager.java
│   │   │           │   ├── ItemUtil.java
│   │   │           │   ├── MathUtil.java
│   │   │           │   ├── RenderUtil.java
│   │   │           │   ├── RotationManager.java
│   │   │           │   └── TargetUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       ├── font/
│   │       │       │   ├── inter_medium.json
│   │       │       │   ├── inter_medium.ttf
│   │       │       │   ├── inter_regular.json
│   │       │       │   └── inter_regular.ttf
│   │       │       └── lang/
│   │       │           └── en_us.json
│   │       ├── licenses/
│   │       │   └── inter-OFL.txt
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── CombatChecks.java
│                   ├── EngineChecks.java
│                   ├── FoundationChecks.java
│                   ├── RuntimeChecks.java
│                   └── UiChecks.java
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── PHASE3.md
├── PHASE4.md
├── PHASE5.md
├── PHASE6.md
├── README.md
└── settings.gradle
```

## 2. Code files

Each listing below is the complete new or updated file currently on disk.

### src/main/java/dev/riserecode/util/TargetUtil.java

```java
package dev.riserecode.util;

import dev.riserecode.friend.FriendManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Client-thread entity queries. Selection does not change any entity or game state. */
public final class TargetUtil {
    public enum EntityGroup { PLAYERS, MOBS, BOTH }
    public enum SortMode { DISTANCE, HEALTH, ANGLE }

    public record Options(double range, EntityGroup group, SortMode sortMode,
            boolean includePassive, boolean ignoreFriends, boolean ignoreInvisible,
            boolean onlyVisible, boolean ignoreTeammates) {
        public Options {
            if (!Double.isFinite(range) || range <= 0) { throw new IllegalArgumentException("Target range must be positive and finite"); }
            Objects.requireNonNull(group); Objects.requireNonNull(sortMode);
        }
    }
    private record Candidate(LivingEntity entity, double distance, double health, double angle) { }
    private TargetUtil() { }

    /** Immutable, consistently sorted snapshot of live entity references, for use on the client thread. */
    public static List<LivingEntity> findTargets(Player player, FriendManager friends, Options options) {
        Objects.requireNonNull(player); Objects.requireNonNull(friends); Objects.requireNonNull(options);
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()) { return List.of(); }
        Comparator<Candidate> order = switch (options.sortMode()) {
            case DISTANCE -> Comparator.comparingDouble(Candidate::distance);
            case HEALTH -> Comparator.comparingDouble(Candidate::health);
            case ANGLE -> Comparator.comparingDouble(Candidate::angle);
        };
        // Stable tie breakers keep equal-score targets from flickering between frames.
        order = order.thenComparingDouble(Candidate::distance).thenComparingInt(candidate -> candidate.entity().getId());
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(options.range()),
                        entity -> isValidTarget(player, entity, friends, options)).stream()
                .map(entity -> new Candidate(entity, player.distanceToSqr(entity), entity.getHealth() + entity.getAbsorptionAmount(),
                        options.sortMode() == SortMode.ANGLE ? crosshairAngle(player, entity) : 0))
                .sorted(order).map(Candidate::entity).toList();
    }
    public static Optional<LivingEntity> bestTarget(Player player, FriendManager friends, Options options) {
        return findTargets(player, friends, options).stream().findFirst();
    }
    public static boolean isValidTarget(Player player, LivingEntity entity, FriendManager friends, Options options) {
        Objects.requireNonNull(player); Objects.requireNonNull(entity); Objects.requireNonNull(friends); Objects.requireNonNull(options);
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || entity == player || entity.level() != player.level() || !entity.isAlive() || entity.isRemoved()
                || !entity.isAttackable() || player.distanceToSqr(entity) > options.range() * options.range()
                || (options.ignoreInvisible() && entity.isInvisible())
                || (options.ignoreTeammates() && player.isAlliedTo(entity))) { return false; }
        if (entity instanceof Player other) {
            if (options.group() == EntityGroup.MOBS || other.isSpectator()
                    || (options.ignoreFriends() && friends.isFriend(other.getUUID(), other.getGameProfile().name()))) { return false; }
        } else if (!(entity instanceof Mob) || options.group() == EntityGroup.PLAYERS
                || (!options.includePassive() && isPassive(entity))) { return false; }
        return !options.onlyVisible() || canSee(player, entity);
    }
    /** Non-Enemy mobs include animals, villagers, and neutral non-hostile mobs. */
    public static boolean isPassive(LivingEntity entity) { return entity instanceof Mob && !(entity instanceof Enemy); }
    public static Vec3 aimPoint(LivingEntity entity) { return entity.getBoundingBox().getCenter(); }
    /** Collision shapes are checked at the same body point used for rotation. */
    public static boolean canSee(Player player, LivingEntity entity) {
        return entity.level() == player.level() && !entity.isRemoved() && raycastClear(player, player.getEyePosition(), aimPoint(entity));
    }
    public static boolean raycastClear(Player player, Vec3 from, Vec3 to) {
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }
    public static double crosshairAngle(Player player, LivingEntity entity) {
        Vec3 look = RotationManager.direction(new RotationManager.Rotation(player.getYRot(), player.getXRot()));
        return angleDegrees(look, aimPoint(entity).subtract(player.getEyePosition()));
    }
    public static double angleDegrees(Vec3 look, Vec3 direction) {
        double denominator = look.length() * direction.length();
        if (!Double.isFinite(denominator)) { throw new IllegalArgumentException("Directions must be finite"); }
        if (denominator < 1.0e-12) { return 0; }
        double cosine = Math.max(-1, Math.min(1, look.dot(direction) / denominator));
        return Math.toDegrees(Math.acos(cosine));
    }
}
```

### src/main/java/dev/riserecode/util/RotationManager.java

```java
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
```

### src/main/java/dev/riserecode/util/AttackManager.java

```java
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
```

### src/main/java/dev/riserecode/util/ItemUtil.java

```java
package dev.riserecode.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Hotbar-only selection; no inventory transactions, synthetic clicks, or hidden slot packets. */
public final class ItemUtil {
    public enum WeaponKind { NONE, SWORD, AXE, TOOL, OTHER }
    private ItemUtil() { }
    public static WeaponKind weaponKind(ItemStack stack) {
        if (stack.isEmpty()) { return WeaponKind.NONE; }
        if (stack.is(ItemTags.SWORDS)) { return WeaponKind.SWORD; }
        if (stack.is(ItemTags.AXES)) { return WeaponKind.AXE; }
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) { return WeaponKind.TOOL; }
        boolean[] damage = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.amount() > 0) { damage[0] = true; }
        });
        return stack.has(DataComponents.WEAPON) || damage[0] ? WeaponKind.OTHER : WeaponKind.NONE;
    }
    public static boolean isWeapon(ItemStack stack) { return weaponKind(stack) != WeaponKind.NONE; }
    public static boolean holdingWeapon(Player player) { return isWeapon(player.getMainHandItem()); }
    public static double attackDamage(Player player, ItemStack stack) { return attributeWithItem(player, stack, Attributes.ATTACK_DAMAGE); }
    public static double attackSpeed(Player player, ItemStack stack) { return attributeWithItem(player, stack, Attributes.ATTACK_SPEED); }

    /** Replace current main-hand modifiers in a copy, preserving effects and other equipped items. */
    private static double attributeWithItem(Player player, ItemStack stack, Holder<Attribute> attribute) {
        var instance = player.getAttribute(attribute);
        if (instance == null) { return 0; }
        Map<Identifier, AttributeModifier> modifiers = new LinkedHashMap<>();
        for (AttributeModifier modifier : instance.getModifiers()) { modifiers.put(modifier.id(), modifier); }
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.equals(attribute)) { modifiers.remove(modifier.id()); }
        });
        stack.forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.equals(attribute)) { modifiers.put(modifier.id(), modifier); }
        });
        return attribute.value().sanitizeValue(evaluateAttribute(instance.getBaseValue(), modifiers.values()));
    }
    /** Vanilla operation order: additions, multiplied base, then multiplied total. */
    public static double evaluateAttribute(double base, Collection<AttributeModifier> modifiers) {
        if (!Double.isFinite(base)) { throw new IllegalArgumentException("Base attribute must be finite"); }
        double additions = 0; double multiplier = 0; double total = 1;
        for (AttributeModifier modifier : modifiers) {
            if (!Double.isFinite(modifier.amount())) { throw new IllegalArgumentException("Attribute modifiers must be finite"); }
            switch (modifier.operation()) {
                case ADD_VALUE -> additions += modifier.amount();
                case ADD_MULTIPLIED_BASE -> multiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> total *= 1 + modifier.amount();
            }
        }
        return (base + additions) * (1 + multiplier) * total;
    }
    /** Rank single-hit damage first, speed second, and retain the current slot on an exact tie. */
    public static OptionalInt bestHotbarWeapon(Player player) {
        int selected = player.getInventory().getSelectedSlot();
        int best = -1; double bestDamage = Double.NEGATIVE_INFINITY; double bestSpeed = Double.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isWeapon(stack)) { continue; }
            double damage = attackDamage(player, stack); double speed = attackSpeed(player, stack);
            if (best < 0 || damage > bestDamage || (Double.compare(damage, bestDamage) == 0
                    && (speed > bestSpeed || (Double.compare(speed, bestSpeed) == 0 && slot == selected)))) {
                best = slot; bestDamage = damage; bestSpeed = speed;
            }
        }
        return best < 0 ? OptionalInt.empty() : OptionalInt.of(best);
    }

    /** A reversible selection session that yields to manual hotbar changes. */
    public static final class HotbarSelection implements AutoCloseable {
        private final Player player;
        private int originalSlot = -1;
        private int managedSlot = -1;
        private boolean manualOverride;
        private boolean closed;
        public HotbarSelection(Player player) { this.player = Objects.requireNonNull(player); }
        public Player getPlayer() { return player; }
        /** Returns true only when this call changes the selected slot. */
        public boolean selectBestWeapon() {
            if (closed) { throw new IllegalStateException("Hotbar selection is closed"); }
            int current = player.getInventory().getSelectedSlot();
            if (managedSlot >= 0 && current != managedSlot) { manualOverride = true; }
            if (manualOverride) { return false; }
            OptionalInt best = bestHotbarWeapon(player);
            if (best.isEmpty() || best.getAsInt() == current) { return false; }
            if (originalSlot < 0) { originalSlot = current; }
            managedSlot = best.getAsInt();
            player.getInventory().setSelectedSlot(managedSlot);
            // Do not spend the old weapon's charged attack on the newly selected weapon.
            player.resetAttackStrengthTicker();
            return true;
        }
        /** Player replacement/disconnection discards ownership without touching the old inventory. */
        public void abandon() { closed = true; }
        @Override
        public void close() {
            if (closed) { return; }
            closed = true;
            if (!manualOverride && originalSlot >= 0 && player.getInventory().getSelectedSlot() == managedSlot) {
                player.getInventory().setSelectedSlot(originalSlot); player.resetAttackStrengthTicker();
            }
        }
    }
}
```

### src/main/java/dev/riserecode/module/modules/KillAuraModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Original composition of reusable utilities; no external client classes or source. */
public final class KillAuraModule extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }

    private final DoubleSetting range = register(new DoubleSetting("Range", "Target selection radius; vanilla attack reach still applies", 4, 1, 6));
    private final DoubleSetting minCps = register(new DoubleSetting("MinCPS", "Minimum requested attacks per second", 6, 1, 20));
    private final DoubleSetting maxCps = register(new DoubleSetting("MaxCPS", "Maximum requested attacks per second", 10, 1, 20));
    private final EnumSetting<RotationManager.Mode> rotate = register(new EnumSetting<>("Rotate", "Visible camera rotation toward the target", RotationManager.Mode.SMOOTH));
    private final DoubleSetting rotationSpeed = register(new DoubleSetting("RotationSpeed", "Maximum angular speed in degrees per second", 360, 30, 1080));
    private final EnumSetting<TargetUtil.SortMode> targetMode = register(new EnumSetting<>("TargetMode", "How eligible targets are ordered", TargetUtil.SortMode.DISTANCE));
    private final EnumSetting<Targets> targets = register(new EnumSetting<>("Targets", "Entity types to consider", Targets.PLAYERS));
    private final BooleanSetting targetPassive = register(new BooleanSetting("TargetPassive", "Include non-hostile mobs in MOBS or BOTH mode", true));
    private final BooleanSetting onlyVisible = register(new BooleanSetting("OnlyVisible", "Require a clear collision ray to the target", true));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("IgnoreFriends", "Exclude names and UUIDs in the friend list", true));
    private final BooleanSetting ignoreInvisible = register(new BooleanSetting("IgnoreInvisible", "Exclude invisible entities", true));
    private final BooleanSetting autoSwitch = register(new BooleanSetting("AutoSwitch", "Select the strongest hotbar weapon and restore the previous slot", false));
    private final BooleanSetting swing = register(new BooleanSetting("Swing", "Animate and send the vanilla main-hand swing", true));
    private final BooleanSetting pauseWhileUsingItem = register(new BooleanSetting("PauseWhileUsingItem", "Pause while eating, blocking, or using an item", true));
    private final BooleanSetting pauseInInventory = register(new BooleanSetting("PauseInInventory", "Pause while any screen is open; ClickGUI always pauses", true));
    private final Minecraft minecraft;
    private final FriendManager friends;
    private final RotationManager rotations;
    private final AttackManager attacks;
    private AttackManager.Session attackSession;
    private RotationManager.Lease rotationLease;
    private ItemUtil.HotbarSelection weaponSelection;
    private LocalPlayer observedPlayer;
    private LivingEntity target;
    private RotationManager.Rotation desiredRotation;
    private boolean prepared;

    public KillAuraModule(EventBus bus, Minecraft minecraft, FriendManager friends,
            RotationManager rotations, AttackManager attacks) {
        super("KillAura", "Attacks nearby targets automatically", Category.COMBAT, bus);
        this.minecraft = minecraft; this.friends = friends; this.rotations = rotations; this.attacks = attacks;
        rotationSpeed.setVisibleWhen(() -> rotate.getValue() == RotationManager.Mode.SMOOTH);
        targetPassive.setVisibleWhen(() -> targets.getValue() != Targets.PLAYERS);
    }

    public DoubleSetting getRange() { return range; }
    public DoubleSetting getMinCps() { return minCps; }
    public DoubleSetting getMaxCps() { return maxCps; }
    public EnumSetting<RotationManager.Mode> getRotate() { return rotate; }
    public DoubleSetting getRotationSpeed() { return rotationSpeed; }
    public EnumSetting<TargetUtil.SortMode> getTargetMode() { return targetMode; }
    public EnumSetting<Targets> getTargets() { return targets; }
    public BooleanSetting getTargetPassive() { return targetPassive; }
    public BooleanSetting getOnlyVisible() { return onlyVisible; }
    public BooleanSetting getIgnoreFriends() { return ignoreFriends; }
    public BooleanSetting getIgnoreInvisible() { return ignoreInvisible; }
    public BooleanSetting getAutoSwitch() { return autoSwitch; }
    public BooleanSetting getSwing() { return swing; }
    public BooleanSetting getPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public BooleanSetting getPauseInInventory() { return pauseInInventory; }
    public Optional<LivingEntity> getTarget() { return Optional.ofNullable(target); }

    @Override
    protected void onEnable() {
        attackSession = attacks.openSession(this);
        listen(UpdateEvent.class, this::update);
        // All modules submit rotation requests before arbitration at -50; attacks follow at -75.
        listen(UpdateEvent.class, -75, false, this::attackTick);
    }

    private void update(UpdateEvent event) {
        prepared = false;
        LocalPlayer player = minecraft.player;
        if (observedPlayer != player) { releaseControl(); observedPlayer = player; attackSession.reset(); }
        if (shouldPause(player)) { pause(); return; }
        target = TargetUtil.bestTarget(player, friends, targetOptions()).orElse(null);
        if (target == null) { pause(); return; }

        if (autoSwitch.getValue()) {
            if (weaponSelection == null) { weaponSelection = new ItemUtil.HotbarSelection(player); }
            if (weaponSelection.selectBestWeapon()) { return; }
        } else { releaseWeapon(); }

        if (rotate.getValue() != RotationManager.Mode.NONE) {
            if (rotationLease == null) { rotationLease = rotations.acquire(this, 50); }
            desiredRotation = RotationManager.towardEntity(player, target);
            rotationLease.request(desiredRotation, rotate.getValue(), rotationSpeed.getValue());
        } else { releaseRotation(); desiredRotation = null; }
        prepared = true;
    }

    private void attackTick(UpdateEvent event) {
        if (!prepared || target == null || minecraft.player != observedPlayer || !isEnabled()) { return; }
        // Other UpdateEvent listeners may open a screen or change friends/settings after selection.
        if (shouldPause(minecraft.player) || !TargetUtil.isValidTarget(minecraft.player, target, friends, targetOptions())) {
            pause(); return;
        }
        if (desiredRotation != null && !rotations.isReady(rotationLease, desiredRotation, 2)) { return; }

        // Sliders may cross independently; normalize the effective pair without editing either setting.
        double minimum = Math.min(minCps.getValue(), maxCps.getValue());
        double maximum = Math.max(minCps.getValue(), maxCps.getValue());
        AttackManager.Policy policy = new AttackManager.Policy(minimum, maximum, true);
        attackSession.attack(target, policy, swing.getValue() ? AttackManager.SwingMode.BOTH : AttackManager.SwingMode.NONE);
    }

    private boolean shouldPause(LocalPlayer player) {
        var screen = minecraft.gui.screen();
        return player == null || minecraft.level == null || minecraft.gameMode == null || minecraft.getConnection() == null
                || !player.isAlive() || player.isRemoved() || player.isSpectator() || minecraft.gui.overlay() != null
                || screen instanceof dev.riserecode.ui.clickgui.ClickGui
                || (pauseInInventory.getValue() && screen != null)
                || (pauseWhileUsingItem.getValue() && player.isUsingItem());
    }
    private TargetUtil.Options targetOptions() {
        return new TargetUtil.Options(range.getValue(), TargetUtil.EntityGroup.valueOf(targets.getValue().name()), targetMode.getValue(),
                targetPassive.getValue(), ignoreFriends.getValue(), ignoreInvisible.getValue(), onlyVisible.getValue(), true);
    }

    private void pause() { prepared = false; target = null; desiredRotation = null; releaseControl(); attackSession.reset(); }
    private void releaseControl() { releaseRotation(); releaseWeapon(); }
    private void releaseRotation() { if (rotationLease != null) { rotationLease.close(); rotationLease = null; } }
    private void releaseWeapon() {
        if (weaponSelection == null) { return; }
        if (weaponSelection.getPlayer() == minecraft.player) { weaponSelection.close(); }
        else { weaponSelection.abandon(); }
        weaponSelection = null;
    }
    @Override
    protected void onDisable() {
        prepared = false; target = null; desiredRotation = null; releaseControl(); observedPlayer = null;
        if (attackSession != null) { attackSession.close(); attackSession = null; }
    }
}
```

### src/main/java/dev/riserecode/module/ModuleManager.java

```java
package dev.riserecode.module;

import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.module.modules.TracersModule;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Client-thread registry. Module owns the subscriptions for its lifecycle. */
public final class ModuleManager {
    private final EventBus eventBus;
    private final Logger logger;
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> view = Collections.unmodifiableList(modules);
    private final Map<String, Module> byName = new LinkedHashMap<>();
    private Runnable changeListener = () -> { };

    public ModuleManager(EventBus eventBus, Logger logger) {
        this.eventBus = Objects.requireNonNull(eventBus);
        this.logger = Objects.requireNonNull(logger);
    }

    public void initDefaults(Minecraft minecraft, FriendManager friends, RotationManager rotations, AttackManager attacks) {
        register(new FullBrightModule(eventBus, minecraft));
        register(new SprintModule(eventBus, minecraft));
        register(new KillAuraModule(eventBus, minecraft, friends, rotations, attacks));
        register(new TracersModule(eventBus, minecraft, friends));
        register(new NoFallModule(eventBus, minecraft));
    }

    public <M extends Module> M register(M module) {
        Objects.requireNonNull(module);
        String key = normalize(module.getName());
        if (byName.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate module: " + module.getName());
        }
        byName.put(key, module);
        modules.add(module);
        return module;
    }

    public List<Module> getModules() { return view; }
    public Optional<Module> getByName(String name) { return Optional.ofNullable(byName.get(normalize(name))); }
    public <M extends Module> Optional<M> getByClass(Class<M> type) {
        return modules.stream().filter(type::isInstance).map(type::cast).findFirst();
    }
    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(module -> module.getCategory() == category).toList();
    }
    public List<Module> getEnabledModules() {
        return modules.stream().filter(Module::isEnabled).toList();
    }

    public boolean toggle(Module module) { return setEnabled(module, !module.isEnabled()); }

    public boolean setEnabled(Module module, boolean enabled) {
        requireRegistered(module);
        if (module.isEnabled() == enabled) { return true; }
        boolean succeeded = true;
        try {
            module.setEnabled(enabled);
        } catch (RuntimeException failure) {
            logger.error("Could not change module {}", module.getName(), failure);
            succeeded = false;
        }
        changeListener.run();
        return succeeded;
    }

    public void setKeybind(Module module, int key) {
        requireRegistered(module);
        module.setKeybind(key);
        changeListener.run();
    }

    public void setChangeListener(Runnable listener) { changeListener = Objects.requireNonNull(listener); }

    /** Shutdown cleanup preserves saved toggle choices and continues after failures. */
    public void disableAll() {
        for (Module module : modules) {
            try {
                module.setEnabled(false);
            } catch (RuntimeException failure) {
                logger.error("Could not clean up module {}", module.getName(), failure);
            }
        }
    }

    private void requireRegistered(Module module) {
        if (byName.get(normalize(module.getName())) != module) {
            throw new IllegalArgumentException("Module is not registered");
        }
    }

    private static String normalize(String name) { return name.strip().toLowerCase(Locale.ROOT); }
}
```

### src/main/java/dev/riserecode/Client.java

```java
package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.BindCommand;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.render.WorldRenderer;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "RiseRecode";
    private final Logger logger = LoggerFactory.getLogger(NAME);
    private final EventBus eventBus = new EventBus((event, failure) ->
            logger.error("Listener failed for {}", event.getClass().getSimpleName(), failure));
    private String version = "development";
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private ProfileManager profileManager;
    private WorldRenderer worldRenderer;
    private RotationManager rotationManager;
    private AttackManager attackManager;
    private HudRenderer hudRenderer;
    private ClickGui clickGui;
    private volatile boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        friendManager = new FriendManager();
        worldRenderer = new WorldRenderer(eventBus);
        rotationManager = new RotationManager(eventBus, minecraft);
        attackManager = new AttackManager(eventBus, minecraft);
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft, friendManager, rotationManager, attackManager);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME + " " + version);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, friendManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.load();
        moduleManager.setChangeListener(configManager::requestSave);
        friendManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        profileManager = new ProfileManager(configManager, () -> {
            if (clickGui != null && minecraft.gui.screen() == clickGui) { clickGui.refresh(); }
        }, logger);
        commandManager = new CommandManager(configManager::getCommandPrefix, configManager::setCommandPrefix, message -> {
            if (minecraft.player != null) {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(NAME + " » ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
            } else { logger.info("[Command] {}", message); }
        }, logger);
        commandManager.register(new ToggleCommand(moduleManager, commandManager));
        commandManager.register(new BindCommand(moduleManager, keybindManager, commandManager));
        commandManager.register(new FriendCommand(friendManager, commandManager, name -> {
            var connection = minecraft.getConnection();
            var info = connection == null ? null : connection.getPlayerInfoIgnoreCase(name);
            return info == null ? null : info.getProfile().id();
        }));
        commandManager.register(new ProfileCommand(profileManager, commandManager));
        commandManager.register(new HelpCommand(commandManager));
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        worldRenderer.close();
        configManager.close();
        moduleManager.disableAll();
        rotationManager.close();
        attackManager.close();
        Minecraft.getInstance().options.save();
        initialized = false;
    }

    public boolean isInitialized() { return initialized; }
    public String getName() { return NAME; }
    public String getVersion() { return version; }
    public Logger getLogger() { return logger; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    public RotationManager getRotationManager() { return rotationManager; }
    public AttackManager getAttackManager() { return attackManager; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    /** Screens capture the font, so create this only after Minecraft initializes it. */
    public ClickGui getClickGui() {
        if (clickGui == null) {
            if (!initialized || Minecraft.getInstance().font == null) {
                throw new IllegalStateException("The Minecraft GUI is not ready");
            }
            clickGui = new ClickGui(moduleManager, hudRenderer, configManager::requestSave);
        }
        return clickGui;
    }
}
```

### src/main/java/dev/riserecode/event/AttackEvent.java

```java
package dev.riserecode.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Cancellable intent before any AttackManager session's ordinary game-mode attack. */
public final class AttackEvent extends Event {
    private final Player attacker;
    private final LivingEntity target;
    public AttackEvent(Player attacker, LivingEntity target) {
        super(true); this.attacker = Objects.requireNonNull(attacker); this.target = Objects.requireNonNull(target);
    }
    public Player getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }
}
```

### Verification files

The deterministic checks and live fixtures below are excluded from the installable
JAR. ClientIntegrationTest invokes both Phase 5 and Phase 6 checks in its isolated
local world. Production code contains no test fixtures or client-only fake players.

#### src/test/java/dev/riserecode/CombatChecks.java

```java
package dev.riserecode;

import dev.riserecode.util.AttackManager;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.RotationManager.Rotation;
import dev.riserecode.util.TargetUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

/** Deterministic math/timing checks; live entity, inventory, and ownership behavior is tested in-game. */
public final class CombatChecks {
    private static int assertions;
    private CombatChecks() { }
    public static void main(String[] args) {
        near(TargetUtil.angleDegrees(new Vec3(0, 0, 1), new Vec3(0, 0, 5)), 0, "aligned angle");
        near(TargetUtil.angleDegrees(new Vec3(0, 0, 1), new Vec3(1, 0, 0)), 90, "perpendicular angle");
        near(TargetUtil.angleDegrees(new Vec3(0, 0, 1), new Vec3(0, 0, -2)), 180, "behind camera angle");
        near(TargetUtil.angleDegrees(Vec3.ZERO, new Vec3(1, 0, 0)), 0, "zero direction defined");
        illegal(() -> TargetUtil.angleDegrees(new Vec3(Double.NaN, 0, 0), Vec3.ZERO), "invalid directions rejected");
        illegal(() -> new TargetUtil.Options(0, TargetUtil.EntityGroup.BOTH, TargetUtil.SortMode.DISTANCE,
                true, true, true, true, true), "invalid query radius rejected");

        Rotation forward = RotationManager.toward(Vec3.ZERO, new Vec3(0, 0, 3));
        near(forward.yaw(), 0, "south yaw"); near(forward.pitch(), 0, "horizontal pitch");
        near(RotationManager.toward(Vec3.ZERO, new Vec3(3, 0, 0)).yaw(), -90, "east yaw");
        near(RotationManager.toward(Vec3.ZERO, new Vec3(0, 3, 0)).pitch(), -90, "upward pitch");
        near(RotationManager.toward(Vec3.ZERO, new Vec3(0, -3, 0)).pitch(), 90, "downward pitch");
        check(RotationManager.toward(Vec3.ZERO, Vec3.ZERO).equals(new Rotation(0, 0)), "coincident positions defined");
        near(new Rotation(541, 100).yaw(), -179, "yaw normalized");
        near(new Rotation(0, 100).pitch(), 90, "pitch clamped");
        illegal(() -> new Rotation(Float.NaN, 0), "invalid rotation rejected");
        illegal(() -> RotationManager.toward(Vec3.ZERO, new Vec3(Double.POSITIVE_INFINITY, 0, 0)), "invalid aim positions rejected");

        Rotation wrapped = RotationManager.smooth(new Rotation(179, 0), new Rotation(-179, 0), 10, 0.1);
        near(Math.abs(wrapped.yaw()), 180, "shortest yaw path across wrap");
        near(RotationManager.angularDistance(new Rotation(179, 0), new Rotation(-179, 0)), 2, "wrapped angular distance");
        Rotation diagonal = RotationManager.smooth(new Rotation(0, 0), new Rotation(90, 60), 100, 0.05);
        near(RotationManager.angularDistance(new Rotation(0, 0), diagonal), 5, "bounded combined angular speed", 1.0e-5);
        check(RotationManager.smooth(forward, new Rotation(30, 40), 100, 0).equals(forward), "zero delta does not advance");
        check(RotationManager.smooth(forward, new Rotation(30, 40), 100, 1).equals(new Rotation(30, 40)), "smooth does not overshoot");
        Rotation twenty = forward; Rotation forty = forward;
        for (int i = 0; i < 20; i++) { twenty = RotationManager.smooth(twenty, new Rotation(90, 30), 60, 0.05); }
        for (int i = 0; i < 40; i++) { forty = RotationManager.smooth(forty, new Rotation(90, 30), 60, 0.025); }
        near(RotationManager.angularDistance(twenty, forty), 0, "delta-time invariant smoothing", 1.0e-4);
        illegal(() -> RotationManager.smooth(forward, forward, 0, 0.05), "zero speed rejected");
        illegal(() -> RotationManager.smooth(forward, forward, 10, -1), "negative delta rejected");
        illegal(() -> RotationManager.smooth(forward, forward, 10, Double.NaN), "invalid delta rejected");
        near(RotationManager.direction(new Rotation(-90, 0)).distanceTo(new Vec3(1, 0, 0)), 0, "rotation direction matches aim");
        near(RotationManager.direction(new Rotation(0, -90)).distanceTo(new Vec3(0, 1, 0)), 0, "vertical direction matches aim");
        check(RotationManager.randomOffset(forward, 0, new Random(1)).equals(forward), "zero jitter preserves orientation");
        boolean bounded = true;
        Random offsets = new Random(4);
        for (int i = 0; i < 1000; i++) {
            Rotation offset = RotationManager.randomOffset(forward, 2, offsets);
            bounded &= Math.abs(offset.yaw()) <= 2 && Math.abs(offset.pitch()) <= 2;
        }
        check(bounded, "all randomized offsets stay bounded");
        illegal(() -> RotationManager.randomOffset(forward, 11, offsets), "excessive jitter rejected");

        AttackManager.Policy fixed = new AttackManager.Policy(5, 5, true);
        near(AttackManager.sampleDelayNanos(fixed, new Random(1)), 200_000_000, "fixed CPS delay");
        near(AttackManager.sampleDelayNanos(new AttackManager.Policy(20, 20, false), new Random(1)), 50_000_000, "one tick maximum CPS");
        illegal(() -> new AttackManager.Policy(0, 10, true), "zero CPS rejected");
        illegal(() -> new AttackManager.Policy(10, 5, true), "reversed CPS range rejected");
        illegal(() -> new AttackManager.Policy(10, 21, true), "above tick-rate CPS rejected");
        illegal(() -> new AttackManager.Policy(Double.NaN, 10, true), "invalid CPS rejected");
        AtomicLong clock = new AtomicLong(1);
        AttackManager.TimingGate timer = new AttackManager.TimingGate(clock::get, new Random(2));
        check(timer.isReady() && timer.remainingNanos() == 0, "new timer ready");
        timer.recordAttack(fixed);
        check(!timer.isReady() && timer.remainingNanos() == 200_000_000, "recorded attack schedules interval");
        check(!timer.isReady() && timer.remainingNanos() == 200_000_000, "readiness does not resample");
        clock.addAndGet(199_999_999);
        check(!timer.isReady() && timer.remainingNanos() == 1, "waits until deadline");
        clock.incrementAndGet(); check(timer.isReady(), "ready at exact deadline");
        timer.recordAttack(fixed); timer.reset(); check(timer.isReady(), "reset releases pending interval");
        clock.set(Long.MAX_VALUE - 10); timer.recordAttack(fixed);
        check(!timer.isReady(), "nano-time wrap keeps interval pending");
        clock.addAndGet(200_000_000); check(timer.isReady(), "nano-time wrap completes interval");
        Random delays = new Random(8); HashSet<Long> unique = new HashSet<>(); boolean inRange = true;
        for (int i = 0; i < 1000; i++) {
            long delay = AttackManager.sampleDelayNanos(new AttackManager.Policy(6, 10, true), delays);
            inRange &= delay >= 100_000_000 && delay <= 166_666_667; unique.add(delay);
        }
        check(inRange && unique.size() > 900, "CPS sampling varies within requested bounds");

        List<AttributeModifier> modifiers = new ArrayList<>(List.of(modifier("add", 2, Operation.ADD_VALUE),
                modifier("base", 0.5, Operation.ADD_MULTIPLIED_BASE), modifier("total", 0.2, Operation.ADD_MULTIPLIED_TOTAL),
                modifier("second_total", 0.1, Operation.ADD_MULTIPLIED_TOTAL)));
        near(ItemUtil.evaluateAttribute(1, modifiers), 5.94, "attribute operation groups");
        Collections.reverse(modifiers); near(ItemUtil.evaluateAttribute(1, modifiers), 5.94, "modifier iteration order does not change score");
        near(ItemUtil.evaluateAttribute(4, List.of()), 4, "empty item modifier set");
        illegal(() -> ItemUtil.evaluateAttribute(Double.NaN, modifiers), "invalid base attribute rejected");
        illegal(() -> ItemUtil.evaluateAttribute(1, List.of(modifier("bad", Double.NaN, Operation.ADD_VALUE))), "invalid modifier rejected");
        System.out.println("Combat checks passed (" + assertions + " assertions).");
    }
    private static AttributeModifier modifier(String name, double value, Operation operation) {
        return new AttributeModifier(Identifier.fromNamespaceAndPath("riserecode_test", name), value, operation);
    }
    private static void near(double actual, double expected, String label) { near(actual, expected, label, 1.0e-8); }
    private static void near(double actual, double expected, String label, double epsilon) { check(Math.abs(actual - expected) <= epsilon, label + ": " + actual); }
    private static void illegal(Runnable operation, String label) {
        try { operation.run(); } catch (IllegalArgumentException expected) { check(true, label); return; }
        throw new AssertionError(label);
    }
    private static void check(boolean condition, String label) { if (!condition) { throw new AssertionError(label); } assertions++; }
}
```

#### src/gametest/java/dev/riserecode/CombatIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;

/** Uses an isolated local world and original utilities; test fixtures never enter the release JAR. */
public final class CombatIntegrationTest {
    private CombatIntegrationTest() { }

    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        Client client = Client.INSTANCE;
        KillAuraModule aura = client.getModuleManager().getByClass(KillAuraModule.class).orElseThrow();
        var previous = context.computeOnClient(minecraft -> client.getConfigManager().profileSnapshot());
        List<ItemStack> previousItems = context.computeOnClient(minecraft -> {
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) { items.add(minecraft.player.getInventory().getItem(slot).copy()); }
            return items;
        });
        int previousSlot = context.computeOnClient(minecraft -> minecraft.player.getInventory().getSelectedSlot());
        RemotePlayer[] actors = new RemotePlayer[5];
        try {
            // Clear a small platform in this disposable test world for deterministic wall/range tests.
            int[] center = context.computeOnClient(minecraft -> new int[] {minecraft.player.blockPosition().getX(),
                    minecraft.player.blockPosition().getY() + 4, minecraft.player.blockPosition().getZ()});
            int x = center[0]; int y = center[1]; int z = center[2];
            world.getServer().runCommand("execute at @a run kill @e[type=!minecraft:player,distance=..16]");
            world.getServer().runCommand("fill " + (x - 8) + " " + (y + 1) + " " + (z - 8) + " " + (x + 8) + " " + (y + 6) + " " + (z + 8) + " minecraft:air");
            world.getServer().runCommand("fill " + (x - 8) + " " + y + " " + (z - 8) + " " + (x + 8) + " " + y + " " + (z + 8) + " minecraft:stone");
            world.getServer().runCommand("tp @a " + (x + 0.5) + " " + (y + 1) + " " + (z + 0.5) + " 0 0");
            context.waitFor(minecraft -> Math.abs(minecraft.player.getY() - (y + 1)) < 0.01
                    && minecraft.level.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE));
            context.runOnClient(minecraft -> {
                String[] names = {"CombatNear", "CombatWeak", "CombatAngle", "CombatFriend", "CombatHidden"};
                double[][] offsets = {{1.25, 1.5}, {-2, 1.8}, {0, 3.5}, {0.5, 0.5}, {-0.4, 0.4}};
                for (int i = 0; i < actors.length; i++) {
                    actors[i] = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), names[i]));
                    actors[i].setId(1_001_000 + i);
                    actors[i].snapTo(x + 0.5 + offsets[i][0], y + 1, z + 0.5 + offsets[i][1]);
                    actors[i].setOldPosAndRot(); minecraft.level.addEntity(actors[i]);
                }
                actors[1].setHealth(2); actors[4].setInvisible(true);
                client.getFriendManager().add("CombatFriend", actors[3].getUUID());
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.DISTANCE, false, true, true)).orElseThrow() == actors[0], "distance selection ignores friend and invisible");
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.HEALTH, false, true, true)).orElseThrow() == actors[1], "health selection chooses low-health entity");
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.ANGLE, false, true, true)).orElseThrow() == actors[2], "angle selection follows the crosshair");
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.DISTANCE, false, false, true)).orElseThrow() == actors[3], "friend filtering is optional");
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.DISTANCE, false, true, false)).orElseThrow() == actors[4], "invisible filtering is optional");
                check(TargetUtil.canSee(minecraft.player, actors[2]), "unobstructed target is visible");
            });
            BlockPos wall = new BlockPos(x, y + 2, z + 2);
            String wallCoordinates = (x - 1) + " " + (y + 1) + " " + (z + 2) + " " + (x + 1) + " " + (y + 3) + " " + (z + 2);
            world.getServer().runCommand("fill " + wallCoordinates + " minecraft:stone");
            context.waitFor(minecraft -> minecraft.level.getBlockState(wall).is(Blocks.STONE));
            context.runOnClient(minecraft -> {
                check(!TargetUtil.canSee(minecraft.player, actors[2]), "collision ray detects wall");
                check(!TargetUtil.findTargets(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.ANGLE, true, true, true)).contains(actors[2]), "visibility filter excludes occluded target");
                check(TargetUtil.bestTarget(minecraft.player, client.getFriendManager(), playerOptions(TargetUtil.SortMode.ANGLE, false, true, true)).orElseThrow() == actors[2], "visibility disabled preserves angle selection");
            });
            world.getServer().runCommand("fill " + wallCoordinates + " minecraft:air");
            context.waitFor(minecraft -> minecraft.level.getBlockState(wall).isAir());

            world.getServer().runCommand("summon minecraft:pig " + (x + 2.5) + " " + (y + 1) + " " + (z + 0.5) + " {Tags:[\"riserecode_combat_pig\"],NoAI:1b}");
            world.getServer().runCommand("summon minecraft:zombie " + (x - 2.5) + " " + (y + 1) + " " + (z + 0.5) + " {Tags:[\"riserecode_combat_zombie\"],NoAI:1b,Invulnerable:1b}");
            UUID pigId = serverEntity(world, "riserecode_combat_pig"); UUID zombieId = serverEntity(world, "riserecode_combat_zombie");
            world.getServer().runOnServer(server -> {
                LivingEntity pig = (LivingEntity) server.overworld().getEntity(pigId);
                pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); pig.setHealth(100);
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int slot = 0; slot < 9; slot++) { player.getInventory().setItem(slot, fixtureItem(slot)); }
                player.inventoryMenu.broadcastChanges();
            });
            context.waitFor(minecraft -> entity(minecraft, pigId) != null && entity(minecraft, zombieId) != null
                    && entity(minecraft, pigId).getHealth() > 80 && minecraft.player.getInventory().getItem(2).is(Items.DIAMOND_AXE));
            context.runOnClient(minecraft -> {
                var hostile = new TargetUtil.Options(6, TargetUtil.EntityGroup.MOBS, TargetUtil.SortMode.DISTANCE, false, true, true, true, true);
                List<LivingEntity> entities = TargetUtil.findTargets(minecraft.player, client.getFriendManager(), hostile);
                check(entities.size() == 1 && entities.getFirst().getUUID().equals(zombieId), "passive-mob filter keeps hostile mob only");
                check(TargetUtil.isPassive(entity(minecraft, pigId)) && !TargetUtil.isPassive(entity(minecraft, zombieId)), "passive classification");
                minecraft.player.getInventory().setSelectedSlot(0);
                check(ItemUtil.weaponKind(new ItemStack(Items.IRON_SWORD)) == ItemUtil.WeaponKind.SWORD, "sword detected by tag");
                check(ItemUtil.weaponKind(new ItemStack(Items.DIAMOND_AXE)) == ItemUtil.WeaponKind.AXE, "axe detected by tag");
                check(ItemUtil.weaponKind(new ItemStack(Items.DIAMOND_PICKAXE)) == ItemUtil.WeaponKind.TOOL, "tool detected by tag");
                check(!ItemUtil.holdingWeapon(minecraft.player), "food is not a weapon");
                check(ItemUtil.bestHotbarWeapon(minecraft.player).orElseThrow() == 2, "highest damage weapon wins");
                ItemStack custom = new ItemStack(Items.DIRT);
                custom.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Identifier.fromNamespaceAndPath("riserecode_test", "custom_weapon"), 12,
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build());
                minecraft.player.getInventory().setItem(4, custom);
                check(ItemUtil.isWeapon(custom) && ItemUtil.bestHotbarWeapon(minecraft.player).orElseThrow() == 4, "custom item attributes are honored");
                minecraft.player.getInventory().setItem(4, ItemStack.EMPTY);
                ItemUtil.HotbarSelection selection = new ItemUtil.HotbarSelection(minecraft.player);
                check(selection.selectBestWeapon() && minecraft.player.getInventory().getSelectedSlot() == 2, "selection switches slot visibly");
                selection.close(); check(minecraft.player.getInventory().getSelectedSlot() == 0, "selection restores previous slot");
                selection = new ItemUtil.HotbarSelection(minecraft.player); selection.selectBestWeapon();
                minecraft.player.getInventory().setSelectedSlot(3);
                check(!selection.selectBestWeapon() && minecraft.player.getInventory().getSelectedSlot() == 3, "manual selection wins");
                selection.close(); check(minecraft.player.getInventory().getSelectedSlot() == 3, "cleanup preserves manual selection");
                minecraft.player.getInventory().setSelectedSlot(0);
                verifyRotationOwnership(minecraft);
            });

            AtomicInteger packets = new AtomicInteger(); AtomicInteger swings = new AtomicInteger();
            AtomicReference<RotationManager.Rotation> lastWire = new AtomicReference<>();
            AtomicBoolean wireMatchesCamera = new AtomicBoolean(true); AtomicBoolean nestedRejected = new AtomicBoolean();
            ConcurrentLinkedQueue<Long> attackTimes = new ConcurrentLinkedQueue<>();
            var packetObserver = client.getEventBus().subscribe(PacketEvent.Send.class, -300, false, event -> {
                if (event.getPacket() instanceof ServerboundMovePlayerPacket movement && movement.hasRotation()) {
                    lastWire.set(new RotationManager.Rotation(movement.getYRot(0), movement.getXRot(0)));
                }
                if (event.getPacket() instanceof ServerboundAttackPacket) {
                    packets.incrementAndGet(); attackTimes.add(System.nanoTime());
                    var player = Minecraft.getInstance().player;
                    var camera = new RotationManager.Rotation(player.getYRot(), player.getXRot());
                    if (lastWire.get() == null || RotationManager.angularDistance(lastWire.get(), camera) > 2.1) { wireMatchesCamera.set(false); }
                }
                if (event.getPacket() instanceof ServerboundSwingPacket) { swings.incrementAndGet(); }
            });
            var nested = context.computeOnClient(minecraft -> client.getAttackManager().openSession(new Object()));
            var intentObserver = client.getEventBus().subscribe(AttackEvent.class, 50, false, event -> {
                nestedRejected.set(!nested.attack(event.getTarget(), new AttackManager.Policy(20, 20, false), AttackManager.SwingMode.NONE));
            });
            try {
                context.runOnClient(minecraft -> {
                    minecraft.player.setYRot(0); minecraft.player.setXRot(0);
                    aura.getTargets().setValue(KillAuraModule.Targets.MOBS); aura.getRange().setValue(4.0);
                    aura.getRotate().setValue(RotationManager.Mode.SMOOTH); aura.getRotationSpeed().setValue(90.0);
                    aura.getTargetMode().setValue(TargetUtil.SortMode.DISTANCE); aura.getAutoSwitch().setValue(true);
                    aura.getMinCps().setValue(6.0); aura.getMaxCps().setValue(10.0); aura.getSwing().setValue(true);
                    client.getModuleManager().setEnabled(aura, true);
                });
                context.waitFor(minecraft -> minecraft.player.getInventory().getSelectedSlot() == 2);
                context.waitFor(minecraft -> packets.get() >= 2, 8000);
                world.getServer().waitFor(server -> ((LivingEntity) server.overworld().getEntity(pigId)).getHealth() < 95);
                check(wireMatchesCamera.get(), "vanilla movement rotations precede attacks and match visible camera");
                check(nestedRejected.get(), "reentrant attacks are rejected");
                check(swings.get() >= 2, "default swing sends vanilla swing packet");
                List<Long> times = List.copyOf(attackTimes);
                check(times.get(1) - times.get(0) >= 750_000_000L, "axe cooldown limits high requested CPS");

                Object priorityOwner = new Object();
                var priorityLease = context.computeOnClient(minecraft -> client.getRotationManager().acquire(priorityOwner, 100));
                var priorityUpdates = client.getEventBus().subscribe(UpdateEvent.class, event -> priorityLease.request(
                        new RotationManager.Rotation(45, 0), RotationManager.Mode.SNAPPY, 360));
                try {
                    context.waitFor(minecraft -> client.getRotationManager().getActiveOwner().orElse(null) == priorityOwner);
                    int before = packets.get(); context.waitTicks(25);
                    check(packets.get() == before, "late-registered higher-priority rotation owner blocks aura attacks");
                } finally { priorityUpdates.close(); context.runOnClient(minecraft -> priorityLease.close()); }

                context.runOnClient(minecraft -> { aura.getPauseInInventory().setValue(false); aura.getSwing().setValue(false); });
                context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitForScreen(ClickGui.class);
                context.waitTicks(2); int pausedCount = packets.get(); context.waitTicks(25);
                check(packets.get() == pausedCount, "ClickGUI always pauses attacks even when inventory pause is off");
                check(context.computeOnClient(minecraft -> aura.getTarget().isEmpty() && client.getRotationManager().getLeaseCount() == 0
                        && minecraft.player.getInventory().getSelectedSlot() == 0), "GUI pause releases rotation and restores slot");
                context.runOnClient(minecraft -> {
                    var panel = client.getClickGui().getPanels().stream().filter(value -> value.getCategory() == Category.COMBAT).findFirst().orElseThrow();
                    var button = panel.getModuleButtons().stream().filter(value -> value.getModule() == aura).findFirst().orElseThrow();
                    if (!button.isExpanded()) { button.mouseClicked(button.getBounds().x() + 16, button.getBounds().y() + 12, 1); }
                    check(client.getProfileManager().saveProfile("CombatEngine"), "advanced settings save in profile");
                    aura.getRotationSpeed().setValue(720.0); aura.getIgnoreFriends().setValue(false);
                    check(client.getProfileManager().loadProfile("CombatEngine") && aura.getRotationSpeed().getValue() == 90
                            && aura.getIgnoreFriends().getValue(), "advanced settings and lifecycles restore from profile");
                });
                context.waitTicks(10); context.takeScreenshot("riserecode-advanced-combat-settings");
                context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(minecraft -> minecraft.gui.screen() == null);
                int attacksBefore = packets.get(); int swingsBefore = swings.get();
                context.waitFor(minecraft -> packets.get() > attacksBefore, 8000);
                check(swings.get() == swingsBefore, "Swing false suppresses animation packet");
                context.runOnClient(minecraft -> {
                    client.getModuleManager().setEnabled(aura, false);
                    check(minecraft.player.getInventory().getSelectedSlot() == 0, "disable restores original hotbar slot");
                    check(Math.abs(minecraft.player.getYRot()) < 0.1 && Math.abs(minecraft.player.getXRot()) < 0.1, "disable restores original rotation");
                    check(client.getRotationManager().getLeaseCount() == 0 && client.getAttackManager().getSessionCount() == 1,
                            "disabled module releases leases and attack session");
                    // The remaining session is the test's independent nested-attack session.
                    // A later selection-phase listener opens the GUI before the attack phase.
                    aura.getRotate().setValue(RotationManager.Mode.NONE); aura.getAutoSwitch().setValue(false);
                    minecraft.player.resetAttackStrengthTicker();
                    int beforeLateGui = packets.get();
                    var lateGui = client.getEventBus().subscribe(UpdateEvent.class, -25, false,
                            event -> client.getClickGui().open(null));
                    client.getModuleManager().setEnabled(aura, true);
                    try {
                        client.getEventBus().post(new UpdateEvent(minecraft, 0));
                        check(aura.getTarget().isEmpty() && packets.get() == beforeLateGui, "late GUI opening clears prepared attack");
                    } finally { lateGui.close(); minecraft.gui.setScreen(null); client.getModuleManager().setEnabled(aura, false); }
                    client.getModuleManager().setEnabled(aura, true);
                    var player = minecraft.player;
                    player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                    client.getEventBus().post(new UpdateEvent(minecraft, 0));
                    check(aura.getTarget().isEmpty(), "using an item pauses target acquisition");
                    player.stopUsingItem();
                    float health = player.getHealth(); player.setHealth(0);
                    client.getEventBus().post(new UpdateEvent(minecraft, 0));
                    check(aura.getTarget().isEmpty(), "dead player pauses combat");
                    player.setHealth(health);
                    minecraft.player = null;
                    try {
                        client.getEventBus().post(new UpdateEvent(minecraft, 0));
                        check(aura.getTarget().isEmpty() && client.getRotationManager().getLeaseCount() == 0, "missing player clears combat state");
                    } finally { minecraft.player = player; client.getModuleManager().setEnabled(aura, false); }
                });
            } finally { packetObserver.close(); intentObserver.close(); context.runOnClient(minecraft -> nested.close()); }
            check(context.computeOnClient(minecraft -> client.getAttackManager().getSessionCount() == 0), "all combat sessions cleaned up");
            client.getLogger().info("Advanced combat integration checks passed");
        } finally {
            context.runOnClient(minecraft -> {
                client.getModuleManager().setEnabled(aura, false);
                if (minecraft.gui.screen() instanceof ClickGui) { minecraft.gui.setScreen(null); }
                client.getFriendManager().remove("CombatFriend");
                for (RemotePlayer actor : actors) { if (actor != null && minecraft.level != null) { minecraft.level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED); } }
                client.getConfigManager().applyProfile(previous);
                for (int slot = 0; slot < previousItems.size(); slot++) { minecraft.player.getInventory().setItem(slot, previousItems.get(slot)); }
                minecraft.player.getInventory().setSelectedSlot(previousSlot);
                client.getConfigManager().save();
            });
        }
    }

    private static void verifyRotationOwnership(Minecraft minecraft) {
        EventBus bus = new EventBus(); AtomicLong clock = new AtomicLong(1);
        float yaw = minecraft.player.getYRot(); float pitch = minecraft.player.getXRot();
        try (RotationManager rotations = new RotationManager(bus, minecraft, clock::get)) {
            Object lowOwner = new Object(); Object highOwner = new Object();
            var low = rotations.acquire(lowOwner, 10); var high = rotations.acquire(highOwner, 20);
            low.request(new RotationManager.Rotation(90, 0), RotationManager.Mode.SNAPPY, 360);
            high.request(new RotationManager.Rotation(-90, 0), RotationManager.Mode.SNAPPY, 360);
            bus.post(new UpdateEvent(minecraft, 1));
            check(minecraft.player.getYRot() == -90 && rotations.getActiveOwner().orElseThrow() == highOwner, "highest priority owns visible rotation");
            high.close(); low.request(new RotationManager.Rotation(90, 0), RotationManager.Mode.SNAPPY, 360);
            clock.addAndGet(50_000_000); bus.post(new UpdateEvent(minecraft, 2));
            check(minecraft.player.getYRot() == 90 && low.isActive(), "closing winner hands control to pending owner");
            low.request(new RotationManager.Rotation(0, 0), RotationManager.Mode.NONE, 360);
            check(minecraft.player.getYRot() == yaw && minecraft.player.getXRot() == pitch, "NONE restores original rotation");
            low.request(new RotationManager.Rotation(90, 0), RotationManager.Mode.SNAPPY, 360);
            clock.addAndGet(50_000_000); bus.post(new UpdateEvent(minecraft, 3));
            clock.addAndGet(50_000_000); bus.post(new UpdateEvent(minecraft, 4));
            check(rotations.getActiveOwner().isEmpty() && minecraft.player.getYRot() == yaw, "unrefreshed request expires and restores state");
            low.close(); check(rotations.getLeaseCount() == 0, "rotation leases close idempotently");
        }
    }
    private static TargetUtil.Options playerOptions(TargetUtil.SortMode mode, boolean visible, boolean ignoreFriends, boolean ignoreInvisible) {
        return new TargetUtil.Options(6, TargetUtil.EntityGroup.PLAYERS, mode, true, ignoreFriends, ignoreInvisible, visible, true);
    }
    private static UUID serverEntity(TestSingleplayerContext world, String tag) {
        return world.getServer().computeOnServer(server -> java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                .filter(entity -> entity.entityTags().contains(tag)).map(Entity::getUUID).findFirst().orElseThrow());
    }
    private static LivingEntity entity(Minecraft minecraft, UUID uuid) {
        return minecraft.level.getEntitiesOfClass(LivingEntity.class, minecraft.player.getBoundingBox().inflate(8),
                entity -> entity.getUUID().equals(uuid)).stream().findFirst().orElse(null);
    }
    private static ItemStack fixtureItem(int slot) {
        return switch (slot) {
            case 0 -> new ItemStack(Items.APPLE);
            case 1 -> new ItemStack(Items.IRON_SWORD);
            case 2 -> new ItemStack(Items.DIAMOND_AXE);
            case 3 -> new ItemStack(Items.WOODEN_SWORD);
            default -> ItemStack.EMPTY;
        };
    }
    private static void check(boolean condition, String label) { if (!condition) { throw new AssertionError(label); } }
}
```

#### src/gametest/java/dev/riserecode/ClientIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.util.RenderUtil;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 2000;
    private enum Mode { FIRST, SECOND }
    private static final class ControlsModule extends Module {
        private final BooleanSetting advanced = register(new BooleanSetting("Advanced", "", false));
        private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "", Mode.FIRST));
        private final ColorSetting color = register(new ColorSetting("Accent", "", 0x803B82F6));
        private final DoubleSetting conditional = register(new DoubleSetting("Conditional", "", 0.5, 0, 1));
        private ControlsModule(String name) {
            super(name, "Client test fixture", Category.UTILITY, Client.INSTANCE.getEventBus());
            conditional.setVisibleWhen(advanced::getValue);
            for (int i = 0; i < 8; i++) { register(new DoubleSetting("Slider " + (i + 1), "", 0.5, 0, 1)); }
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(1280, 800);
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 75, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().getFirst().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue()) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().size() == 2);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 250, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.color.getValue() == 0x80D946EF), "color palette preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-2);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 60, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Close the settings and drag the category through Minecraft's actual mouse handler.
        click(context, controlsButton::getBounds, 1);
        Panel renderPanel = context.computeOnClient(minecraft -> panel(Category.RENDER));
        double startX = context.computeOnClient(minecraft -> renderPanel.getBounds().x());
        move(context, renderPanel::getBounds, 0.4, 0.07);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> renderPanel.getBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> renderPanel.getBounds().y() >= 58), "drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getPanels().stream().allMatch(p -> p.getBounds().x() >= 10)), "small window layout clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(1280, 800);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        AtomicInteger hudFrames = new AtomicInteger();
        AtomicInteger worldFrames = new AtomicInteger();
        var hudSubscription = Client.INSTANCE.getEventBus().subscribe(Render2DEvent.class, event -> hudFrames.incrementAndGet());
        var worldSubscription = Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class, event -> worldFrames.incrementAndGet());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(minecraft -> Client.INSTANCE.getModuleManager().setKeybind(fullBright(), InputConstants.KEY_B));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> fullBright().isEnabled()), "module key toggles once");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == fullBright() && entry.visibility() == 1), ANIMATION_TIMEOUT);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.runOnClient(minecraft -> { controls.setEnabled(true); longer.setEnabled(true); });
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 3);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % 24 == 8), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, entry.module().getName(), false))).orElseThrow();
                return widest.y() == 8;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 8 || entry.y() == 32), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
            EngineIntegrationTest.verify(context, world);
            CombatIntegrationTest.verify(context, world);
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().setKeybind(fullBright(), -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static FullBrightModule fullBright() { return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static Panel panel(Category category) { return gui().getPanels().stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow(); }
    private static ModuleButton button(String name) {
        return gui().getPanels().stream().flatMap(panel -> panel.getModuleButtons().stream())
                .filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().getPanels().stream().allMatch(Panel::isOpen), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.y() + 39 + (body.height() - 49) / 2 - control.centerY()) / 32;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 32));
        context.waitFor(minecraft -> Math.abs(panel.getScrollOffset() - target) < 0.01, ANIMATION_TIMEOUT);
    }
    private static void move(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> {
            var b = bounds.get();
            return new double[] {(b.x() + b.width() * x) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                    (b.y() + b.height() * y) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTick();
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int mouseButton) {
        // Headers stay fixed when a module expands; clicking the center of the full body would edit a setting.
        Supplier<UIComponent.Bounds> header = () -> {
            var b = bounds.get(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), Math.min(28, b.height()));
        };
        move(context, header, 0.4, 0.5);
        context.getInput().pressMouse(mouseButton);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
```

## 3. Registration / config notes

### Registration and initialization

`Client.init()` constructs one shared `RotationManager` and `AttackManager` using
the existing EventBus, before registering modules or loading config. Both are
available through singleton getters for future modules. `ModuleManager.initDefaults`
passes these services and the existing FriendManager into KillAura and registers
it alongside FullBright, Sprint, Tracers, and NoFall. Fresh configurations leave
all modules disabled. Shutdown saves user choices first, disables modules, then
closes the shared services and their subscriptions.

No new mixin or Fabric hook is needed. The existing client tick adapter posts
UpdateEvent, the chat commands use ModuleManager, and packet events continue to
observe vanilla sends. The unchanged world extraction/submission and UI pipelines
remain available for future render modules.

### Event ordering and ownership

| Priority | Responsibility |
| --- | --- |
| 200 | AttackManager advances its managed tick and clears timing on player/world replacement |
| 0 | Module selection, hotbar decisions, and rotation requests |
| -50 | RotationManager arbitrates all current requests and applies visible angles |
| -75 | KillAura rechecks state and dispatches an eligible attack |
| -100 | Existing ConfigManager performs debounced persistence |

All utility state, module settings, and lease/session operations belong to the
client thread. The synchronous EventBus preserves this ordering. Acquire a rotation
lease in module lifecycle code, refresh its request in the selection phase, and
close it when releasing control. Higher priority wins; earlier acquisition breaks
a tie. Unrefreshed requests expire at the following arbitration pass, and closing
an active owner hands off to another pending owner or restores the captured state.

NONE releases rotation control. SNAPPY applies the requested angles; SMOOTH follows
the shortest yaw path at a bounded combined yaw/pitch speed using elapsed seconds.
The smoothing step caps long-frame delta at 0.1 seconds. Static helpers calculate
entity/body and block-center angles, view direction, angular distance, and optional
bounded random offsets. The optional offsets affect visible angles; KillAura does
not enable them automatically.

Rotations are visible and are carried by normal player movement packets. KillAura
waits until the lease has already controlled a preceding player tick and checks
that both the pre-application angles and current angles are within 2 degrees of
the current target. An owner preempted during arbitration cannot attack with its
old grant. No silent rotation, look-packet replacement, fake position, or extra
reach is used. Captured yaw/pitch/head/body angles are restored on final release;
player replacement discards stale state rather than changing an old player object.

AttackManager sessions retain independent CPS timers but share a one-managed-attack
per-tick guard and a recursive-dispatch guard. One randomized CPS interval is
sampled after dispatch; polling readiness never resamples it. Cancelled AttackEvent
intents do not create attack packets or consume the sampled interval. Dispatch
checks the held item's native attack range with zero extra reach, then uses
`minecraft.gameMode.attack(...)`. KillAura always requires full vanilla attack
cooldown. Its requested CPS therefore sets an upper limit rather than overriding
weapon cooldown. Swing helpers support local-only, server-only, both, or none;
KillAura's Swing setting selects both or none.

### Target and weapon policies

TargetUtil returns immutable ordered snapshots of live entity references; consume
them on the client thread and never capture them in deferred rendering callbacks.
It supports player/mob/both groups, optional friends/invisible/non-hostile filters,
teammates, and collision raycasts. Non-hostile/passive means a Mob that does not
implement the vanilla Enemy interface; neutral mobs follow that classification.
OnlyVisible tests collision shapes at the same body-center point used for aiming.
Distance ordering uses squared player/entity origin distance. Health ordering
includes absorption. Angle ordering uses angular distance from the view direction.
Distance then entity ID break score ties deterministically.

ItemUtil classifies weapons through item tags, the weapon component, and main-hand
attack modifiers. It computes candidate attributes by replacing the current
main-hand modifiers in a copy of the player's effective modifier set, preserving
other gear and effects. Vanilla addition/multiplier operation order is respected;
live attributes are never changed. Damage ranks first, attack speed breaks ties,
and the current slot wins an exact tie. Target-conditional enchantment damage is
outside this generic attribute score. Special kinetic/piercing attack behaviors
would require their own future module dispatch.

HotbarSelection changes only a visible hotbar slot, resets charge on switching,
and lets vanilla game-mode handling synchronize the carried item. It restores the
original slot only while it still owns the selection. A manual slot change takes
priority until that selection session ends. Disconnection/player replacement
abandons ownership without altering an old player's inventory. No inventory click
transactions or hidden slot spoofing are implemented.

### Settings and persistence

| Setting | Fresh default | Bounds/options |
| --- | --- | --- |
| Range | 4.0 | 1.0–6.0, selection radius |
| MinCPS | 6.0 | 1.0–20.0 |
| MaxCPS | 10.0 | 1.0–20.0 |
| Rotate | SMOOTH | NONE, SNAPPY, SMOOTH |
| RotationSpeed | 360.0 | 30–1080 degrees/second; visible in SMOOTH |
| TargetMode | DISTANCE | DISTANCE, HEALTH, ANGLE |
| Targets | PLAYERS | PLAYERS, MOBS, BOTH |
| TargetPassive | true | Visible when Targets includes mobs |
| OnlyVisible | true | Collision-ray filtering |
| IgnoreFriends | true | Name/UUID whitelist filtering |
| IgnoreInvisible | true | Invisible entity filtering |
| AutoSwitch | false | Reversible best-hotbar-weapon selection |
| Swing | true | Local animation and vanilla swing packet |
| PauseWhileUsingItem | true | Pause while eating, blocking, or using an item |
| PauseInInventory | true | Pause while a screen is open; ClickGUI always pauses |

Every setting is registered through the existing Module.register method and is
rendered automatically by the existing generic controls. Scroll inside the Combat
panel to access the full settings list. Min/Max CPS sliders may cross; the effective
pair is ordered without silently changing either stored value. The updated module
retains its name and the existing Range/Targets setting names, so older saved
choices remain valid. Newly added settings take their defaults when absent.

ConfigManager needs no changes: the existing Boolean/Double/Enum serialization
saves all these settings to `config/riserecode/config.json`, including hidden
settings. Profiles save them alongside module states, binds, and HUD visibility
in `config/riserecode/profiles/<name>.json`. Friends and command prefix remain
global. Profile application disables/re-enables the module through its lifecycle,
which closes and recreates its attack session and releases rotation/slot ownership.
An open GUI refreshes through the existing profile callback.

Target loss, pause, death, missing/disconnected player, and disable clear combat
state. The attack phase rechecks screens, player validity, friends, and target
validity after selection-phase listeners have run. ClickGUI always pauses even
if PauseInInventory is off. All owned module listeners are removed on disable.

### Build changes

The pinned Minecraft/Fabric/Java dependencies and main mixin configuration are
unchanged. The release version is now 0.4.0, and `check` includes CombatChecks.

#### build.gradle

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    compileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
    testCompileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
}

processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand version: project.version }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-Xlint:all', '-Werror']
}

tasks.register('foundationCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.FoundationChecks'
}

tasks.register('runtimeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.RuntimeChecks'
}

tasks.register('uiCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.UiChecks'
}

tasks.register('engineCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.EngineChecks'
}

tasks.register('combatCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.CombatChecks'
}

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck', 'uiCheck', 'engineCheck', 'combatCheck' }

// The dependency-free checks above use JavaExec rather than a JUnit runner.
tasks.named('test') { enabled = false }

fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'riserecode-test'
        enableGameTests = false
        enableClientGameTests = true
    }
}

loom.runs.clientGameTest {
    runDir = 'build/run/clientGameTest'
    programArgs '--graphicsBackend', providers.gradleProperty('graphicsBackend').getOrElse('OPENGL')
}
```

#### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.2
mod_version=0.4.0
maven_group=dev.riserecode
archives_base_name=riserecode
```

### In-game test steps

Build and run using JDK 25:

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

Open Right Shift, right click KillAura in Combat, and scroll its settings. For
an isolated singleplayer test, choose Targets=MOBS, TargetPassive=true, OnlyVisible=true,
and a nearby mob within the held item's native attack range. Enable using the
module header or `.t KillAura`. Compare DISTANCE/HEALTH/ANGLE ordering, then change
NONE/SNAPPY/SMOOTH and RotationSpeed. AutoSwitch selects the strongest hotbar weapon;
manual slot selection takes priority. Disable to restore captured rotation/slot
state. Open the GUI or use an item to verify pausing. Existing `.bind KillAura F6`
and `.profile save Combat` / `.profile load Combat` use the same managers as before.
Friend filtering can be tested with `.friend add PlayerName` and IgnoreFriends=true.

The completed `build runClientGameTest --offline --no-daemon` run passed with all
compiler warnings treated as errors. **225 deterministic assertions passed:**
82 foundation, 19 runtime, 19 UI, 55 engine, and 50 combat. Combat checks include
rotation wrap/delta-time invariance, bounded offsets, angle geometry, CPS sampling,
deadlines/nano-time wrap, and attribute operation order.

The real OpenGL client test passed Phase 3–5 regressions and Phase 6 live checks:
distance/health/angle selection, wall/friend/invisible/passive filters, custom weapon
attributes, reversible slots/manual precedence, rotation priority/handoff/expiry,
visible camera angles carried by vanilla movement before attacks, axe cooldown,
recursive dispatch rejection, Swing=false, profile restoration, GUI pause even
when inventory pause is off, GUI opening between selection and attack, item-use
pause, death/missing-player guards, and lease/session cleanup. The world is local
and disposable. Test code and fixtures are excluded from the release JAR. Vulkan
was not exercised in this phase.

The release JAR was inspected for Java 25 class version 69, client-only metadata,
all four new utilities, updated module/client registration, unchanged production
mixin list, absence of Meteor class references in these classes, and exclusion of
test classes.

- [Installable riserecode-0.4.0.jar](build/libs/riserecode-0.4.0.jar)
- [Retained build/client-test log](build/verification/phase6/build-client-test.log)
- [Artifact verification record](build/verification/phase6/artifact-check.json)
- [Expanded KillAura GUI screenshot](build/verification/phase6/advanced-combat-settings.png)

The screenshot shows the top of the expanded, scrollable settings list. Additional
CPS, rotation, targeting, weapon, and pause controls continue below its viewport.

## 4. Future module notes

- **TriggerBot:** reuse TargetUtil visibility/angle filters, native attack-range
  checking, AttackManager cooldown/CPS sessions, cancellable intent, and swing modes.
- **ESP:** reuse ordered entity snapshots and friend/passive/invisible policies;
  extract immutable geometry through the existing Render3DEvent/WorldRenderer.
- **Surround / Trap:** reuse target selection, block-center angle calculations,
  and priority rotation leases; add module-specific block placement and item lookup.
- **Other combat modules:** reuse hotbar weapon ranking, reversible selection,
  native range checks, and the shared managed-attack guard.
- **AutoTotem:** the existing event/lifecycle and data-component conventions remain
  applicable; arbitrary inventory-item lookup and totem-specific transactions are
  separate future work rather than hidden in this weapon-selection utility.

For modules that need rotations, submit requests during the selection phase and
perform dependent actions only after arbitration. Release leases/sessions through
onDisable and whenever the module pauses. Rendering, UI components, and persistence
remain outside combat utility logic; future modules need no Meteor imports.
