package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import net.minecraft.client.Minecraft;

/** Keeps the crosshair on the best nearby target without attacking. */
public final class AimLockModule extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }
    private final DoubleSetting range = register(DoubleSetting.extended("Range", "Target selection radius", 6, 1, 64));
    private final DoubleSetting fov = register(DoubleSetting.extended("FOV", "Only targets within this angle of the crosshair", 90, 5, 360));
    private final EnumSetting<RotationManager.Mode> rotate = register(new EnumSetting<>("Rotate", "Camera rotation style", RotationManager.Mode.SMOOTH));
    private final DoubleSetting speed = register(DoubleSetting.extended("Speed", "Maximum angular speed in degrees per second", 240, 30, 1080));
    private final EnumSetting<TargetUtil.SortMode> sort = register(new EnumSetting<>("TargetMode", "How eligible targets are ordered", TargetUtil.SortMode.ANGLE));
    private final EnumSetting<Targets> targets = register(new EnumSetting<>("Targets", "Entity types to lock onto", Targets.PLAYERS));
    private final BooleanSetting onlyVisible = register(new BooleanSetting("OnlyVisible", "Require a clear line to the target", true));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("IgnoreFriends", "Skip friends", true));
    private final BooleanSetting pauseInScreens = register(new BooleanSetting("PauseInScreens", "Pause while any screen is open", true));
    private final Minecraft minecraft;
    private final FriendManager friends;
    private final RotationManager rotations;
    private RotationManager.Lease lease;

    public AimLockModule(EventBus bus, Minecraft minecraft, FriendManager friends, RotationManager rotations) {
        super("AimLock", "Locks your aim onto nearby targets.", Category.COMBAT, bus);
        this.minecraft = minecraft; this.friends = friends; this.rotations = rotations;
        speed.setVisibleWhen(() -> rotate.getValue() == RotationManager.Mode.SMOOTH);
    }
    @Override protected void onEnable() { listen(UpdateEvent.class, this::update); }
    private void update(UpdateEvent event) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator() || (pauseInScreens.getValue() && minecraft.gui.screen() != null)) { release(); return; }
        var options = new TargetUtil.Options(range.getValue(), TargetUtil.EntityGroup.valueOf(targets.getValue().name()), sort.getValue(),
                true, ignoreFriends.getValue(), true, onlyVisible.getValue(), true);
        var target = TargetUtil.findTargets(player, friends, options).stream()
                .filter(entity -> TargetUtil.crosshairAngle(player, entity) <= fov.getValue() / 2).findFirst().orElse(null);
        if (target == null || rotate.getValue() == RotationManager.Mode.NONE) { release(); return; }
        if (lease == null) { lease = rotations.acquire(this, 40); }
        lease.request(RotationManager.towardEntity(player, target), rotate.getValue(), speed.getValue());
    }
    private void release() { if (lease != null) { lease.close(); lease = null; } }
    @Override protected void onDisable() { release(); }
}
