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
