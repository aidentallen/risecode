package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import dev.riserecode.util.gameplay.TrajectoryPrediction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Releases a bow you are already drawing, optionally aiming it first. The server still measures the draw time and
 * applies the rotation it receives, so the shot is only released once the bow is really at the chosen power and
 * the aim has been sent; a bow cannot be fired at full power without drawing it.
 */
public final class AutoBowModule extends Module {
    public enum Mode { FULL_CHARGE, ON_TARGET }
    private static final int SOLVER_PASSES = 5;
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Release at full power, or only when the shot would hit a selected target", Mode.ON_TARGET));
    private final DoubleSetting minCharge = register(new DoubleSetting("Min Charge", "Bow power in percent at which it shoots (100 = fully drawn)", 100, 10, 100));
    private final BlockListSetting targets = register(new BlockListSetting("Targets", "Mobs to shoot at", defaultTargets()).kind(BlockListSetting.Kind.ENTITY));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("Ignore Friends", "Never shoot at friends", true));
    private final BooleanSetting aim = register(new BooleanSetting("Aim Assist", "Turn toward the target and lead its movement while the bow is drawn", false));
    private final EnumSetting<RotationManager.Mode> aimMode = register(new EnumSetting<>("Aim Mode", "Snap to the target or turn smoothly", RotationManager.Mode.SMOOTH));
    private final DoubleSetting aimSpeed = register(DoubleSetting.extended("Aim Speed", "Degrees per second when turning smoothly", 540, 60, 1440));
    private final DoubleSetting aimRange = register(DoubleSetting.extended("Aim Range", "Maximum target distance in blocks", 40, 8, 80));
    private final DoubleSetting aimFov = register(DoubleSetting.extended("Aim FOV", "Only targets within this angle of your view", 60, 10, 180));
    private final BooleanSetting lockTarget = register(new BooleanSetting("Lock Target", "Stay on one target until it is dead or out of range", true));
    private LivingEntity locked;
    private final Minecraft mc;
    private final FriendManager friends;
    private final TrajectoriesModule trajectories;
    private final RotationManager rotations;
    private RotationManager.Lease lease;
    private RotationManager.Rotation desired;

    public AutoBowModule(EventBus bus, Minecraft mc, FriendManager friends, TrajectoriesModule trajectories, RotationManager rotations) {
        super("AutoBow", "Releases a drawn bow for you, optionally aiming at selected targets.", Category.COMBAT, bus);
        this.mc = mc; this.friends = friends; this.trajectories = trajectories; this.rotations = rotations;
        targets.setVisibleWhen(() -> mode.getValue() == Mode.ON_TARGET || aim.getValue());
        ignoreFriends.setVisibleWhen(() -> mode.getValue() == Mode.ON_TARGET || aim.getValue());
        for (var setting : List.of(aimMode, aimRange, aimFov, lockTarget)) { setting.setVisibleWhen(aim::getValue); }
        aimSpeed.setVisibleWhen(() -> aim.getValue() && aimMode.getValue() == RotationManager.Mode.SMOOTH);
    }

    @Override protected void onEnable() {
        listen(UpdateEvent.class, event -> prepare());
        listen(UpdateEvent.class, -75, false, event -> release());
    }
    @Override protected void onDisable() { releaseAim(); locked = null; }

    private boolean drawing(Player player) {
        return mc.level != null && mc.gameMode != null && mc.gui.screen() == null && player.isUsingItem() && player.getUseItem().is(Items.BOW);
    }

    /** Runs early in the tick: pick a target and ask for the rotation that makes the arrow arrive there. */
    private void prepare() {
        var player = mc.player;
        desired = null;
        if (player == null || !drawing(player) || !aim.getValue()) { releaseAim(); return; }
        var target = pickTarget(player);
        if (target == null) { releaseAim(); return; }
        desired = solve(player, target);
        if (desired == null) { releaseAim(); return; }
        if (lease == null) { lease = rotations.acquire(this, 40); }
        lease.request(desired, aimMode.getValue(), aimSpeed.getValue());
    }

    /** Runs late in the tick: shoot only when the aim has arrived and the power is high enough. */
    private void release() {
        var player = mc.player;
        if (player == null || !drawing(player)) { return; }
        if (BowItem.getPowerForTime(player.getTicksUsingItem()) * 100 < minCharge.getValue() - 0.001) { return; }
        if (aim.getValue() && (desired == null || lease == null || !rotations.isReady(lease, desired, 2))) { return; }
        if (mode.getValue() == Mode.ON_TARGET && !wouldHitTarget(player)) { return; }
        mc.gameMode.releaseUsingItem(player);
        releaseAim();
    }

    private void releaseAim() { if (lease != null) { lease.close(); lease = null; } desired = null; }

    private LivingEntity pickTarget(Player player) {
        if (lockTarget.getValue() && locked != null && locked.isAlive() && !locked.isRemoved() && locked.level() == mc.level
                && player.distanceTo(locked) <= aimRange.getValue() * 1.5 && wanted(locked)) { return locked; }
        locked = null;
        LivingEntity best = null; double bestAngle = aimFov.getValue();
        Vec3 look = player.getViewVector(1);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive() || living.isSpectator() || !wanted(living)) { continue; }
            if (player.distanceTo(living) > aimRange.getValue() || !TargetUtil.canSee(player, living)) { continue; }
            double angle = TargetUtil.angleDegrees(look, TargetUtil.aimPoint(living).subtract(player.getEyePosition()));
            if (angle < bestAngle) { best = living; bestAngle = angle; }
        }
        locked = best;
        return best;
    }

    /**
     * Iteratively corrects yaw and pitch: simulate the arrow with the current guess, find where it passes the
     * target, move the aim point along the target's own movement for that flight time, and adjust pitch by the miss.
     */
    private RotationManager.Rotation solve(Player player, LivingEntity target) {
        ItemStack stack = player.getUseItem();
        Vec3 eye = player.getEyePosition();
        Vec3 motion = new Vec3(target.getX() - target.xOld, target.getY() - target.yOld, target.getZ() - target.zOld);
        Vec3 point = TargetUtil.aimPoint(target);
        var rotation = RotationManager.toward(eye, point);
        float yaw = rotation.yaw(), pitch = rotation.pitch();
        for (int pass = 0; pass < SOLVER_PASSES; pass++) {
            var projectiles = trajectories.launch(stack, 1, pitch, yaw);
            if (projectiles.isEmpty()) { return null; }
            var points = TrajectoryPrediction.predict(mc.level, projectiles.getFirst(), trajectories.length.getValue().intValue()).points();
            if (points.size() < 2) { return null; }
            int nearest = 0; double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < points.size(); i++) {
                double distance = points.get(i).distanceToSqr(point);
                if (distance < nearestDistance) { nearestDistance = distance; nearest = i; }
            }
            // path points are one tick apart, so the index is the flight time to lead the target by
            point = TargetUtil.aimPoint(target).add(motion.scale(nearest));
            double horizontal = Math.max(0.1, Math.hypot(point.x - eye.x, point.z - eye.z));
            // raise the aim by the angle that closes the vertical miss, then re-aim the yaw at the led point
            pitch -= (float) Math.toDegrees(Math.atan2(point.y - points.get(nearest).y, horizontal));
            pitch = Math.clamp(pitch, -90f, 90f);
            yaw = RotationManager.toward(eye, point).yaw();
        }
        return new RotationManager.Rotation(yaw, pitch);
    }

    private boolean wouldHitTarget(Player player) {
        for (var projectile : trajectories.launch(player.getUseItem(), 1)) {
            var path = TrajectoryPrediction.predict(mc.level, projectile, trajectories.length.getValue().intValue());
            if (path.target() != null && wanted(path.target())) { return true; }
        }
        return false;
    }

    private static List<String> defaultTargets() {
        var ids = new ArrayList<String>();
        ids.add("minecraft:player");
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (type.getCategory() == MobCategory.MONSTER && type.canSummon()) { ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()); }
        });
        return ids;
    }

    private boolean wanted(Entity entity) {
        if (entity instanceof Player other && ignoreFriends.getValue() && friends.isFriend(other.getUUID(), other.getGameProfile().name())) { return false; }
        return targets.getValue().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
