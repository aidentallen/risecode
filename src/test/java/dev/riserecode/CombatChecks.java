package dev.riserecode;

import dev.riserecode.combat.AuraTiming;
import dev.riserecode.combat.AuraFilters;
import dev.riserecode.module.modules.KillAuraModule;
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
import net.minecraft.world.phys.AABB;

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
        AuraTiming aura = new AuraTiming();
        check(aura.ready(false, 0, 20, true), "first charged attack ready");
        check(!aura.ready(false, 0, 20, false), "vanilla cooldown required");
        aura.attacked();
        for (int i = 0; i < 10; i++) { aura.tick(false); }
        check(!aura.ready(true, 11, 0, false), "custom delay waits full interval");
        aura.tick(false); check(aura.ready(true, 11, 0, false), "custom delay exact boundary");
        check(!aura.ready(false, 0, 12, true), "TPS cooldown floor supplements client charge");
        aura.tick(false); check(aura.ready(false, 0, 12, true), "cooldown floor exact boundary");
        aura.switched(3);
        for (int i = 0; i < 2; i++) { aura.tick(false); }
        check(!aura.ready(true, 0, 0, false), "slot delay remains pending");
        aura.tick(false); check(aura.ready(true, 0, 0, false), "slot delay expires exactly");
        aura.timePacket(1); aura.timePacket(2_000_000_001L); near(aura.tps(), 10, "time packets estimate server TPS");
        aura.attacked();
        for (int i = 0; i < 21; i++) { aura.tick(true); }
        check(!aura.ready(true, 11, 0, false), "10 TPS doubles custom delay");
        aura.tick(true); check(aura.ready(true, 11, 0, false), "10 TPS exact boundary");
        check(!aura.lagging(5_000_000_000L) && aura.lagging(5_000_000_001L), "lag pause boundary");
        aura.timePacket(2_000_000_001L); near(aura.tps(), 10, "duplicate arrival does not corrupt TPS");
        aura.reset(); check(!aura.lagging(Long.MAX_VALUE) && aura.tps() == 20, "world reset discards lag and TPS history");
        aura.attacked(); check(aura.ready(true, 0, 20, false), "zero custom delay bypasses cooldown");
        check(!aura.ready(false, 0, 20, true), "changing back to vanilla retains attack age");
        check(AuraFilters.words(" Sword, AXE  trident sword ").equals(java.util.Set.of("sword", "axe", "trident")), "family parsing normalizes and deduplicates");
        check(AuraFilters.words("   ").isEmpty(), "empty entity filter permits group query");
        AABB box = new AABB(1, 1, 1, 2, 2, 2);
        near(AuraFilters.distanceSquared(new Vec3(1.5, 1.5, 1.5), box), 0, "point inside hitbox");
        near(AuraFilters.distanceSquared(Vec3.ZERO, box), 3, "nearest corner distance");
        near(AuraFilters.distanceSquared(new Vec3(3, 1.5, 1.5), box), 1, "nearest face distance");
        check(KillAuraModule.Age.ADULT.accepts(false) && !KillAuraModule.Age.ADULT.accepts(true), "adult filter");
        check(KillAuraModule.Age.BABY.accepts(true) && !KillAuraModule.Age.BABY.accepts(false), "baby filter");
        check(KillAuraModule.Age.BOTH.accepts(true) && KillAuraModule.Age.BOTH.accepts(false), "all ages filter");
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
