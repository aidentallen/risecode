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
