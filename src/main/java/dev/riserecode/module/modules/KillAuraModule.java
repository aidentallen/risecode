package dev.riserecode.module.modules;

import dev.riserecode.Client;
import dev.riserecode.combat.AuraFilters;
import dev.riserecode.combat.AuraTiming;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.StringSetting;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * RiseCode implementation of the combat behaviors inspected in Meteor KillAura.
 * No Meteor classes or source are included. See docs/killaura.md for provenance and differences.
 */
public final class KillAuraModule extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }
    public enum AttackItems { WEAPONS, ALL }
    public enum RotationTiming { ALWAYS, ON_HIT, NONE }
    public enum ShieldMode { NONE, IGNORE, BREAK }
    public enum Age { ADULT, BABY, BOTH;
        public boolean accepts(boolean baby) { return this == BOTH || (this == BABY) == baby; }
    }

    private final EnumSetting<AttackItems> attackWhenHolding = register(new EnumSetting<>("AttackWhenHolding", "Attack with selected weapon families or any held item", AttackItems.WEAPONS));
    private final StringSetting weapons = register(new StringSetting("Weapons", "Families: sword axe trident mace spear pickaxe shovel hoe", "sword axe trident"));
    // Retain compatible setting keys so existing profiles keep their targeting and visual preferences.
    private final EnumSetting<RotationManager.Mode> rotate = register(new EnumSetting<>("Rotate", "Visible camera rotation style", RotationManager.Mode.SMOOTH));
    private final EnumSetting<RotationTiming> rotationTiming = register(new EnumSetting<>("RotationTiming", "Aim at the primary target continuously or only during an attack window", RotationTiming.ALWAYS));
    private final DoubleSetting rotationSpeed = register(DoubleSetting.extended("RotationSpeed", "Maximum degrees per second for smooth rotation", 360, 30, 1080));
    private final BooleanSetting autoSwitch = register(new BooleanSetting("AutoSwitch", "Select the strongest acceptable hotbar weapon", false));
    private final BooleanSetting swapBack = register(new BooleanSetting("SwapBack", "Restore the previous slot when combat ends; manual changes win", true));
    private final EnumSetting<ShieldMode> shieldMode = register(new EnumSetting<>("ShieldMode", "Ignore blocking players, use an axe, or attack normally", ShieldMode.NONE));
    private final BooleanSetting onlyOnClick = register(new BooleanSetting("OnlyOnClick", "Require the attack key to be held", false));
    private final BooleanSetting onlyOnLook = register(new BooleanSetting("OnlyOnLook", "Use only the entity under the crosshair", false));
    private final BooleanSetting pauseBaritone = register(new BooleanSetting("PauseBaritone", "Pause active Baritone navigation while fighting", true));
    private final EnumSetting<Targets> targets = register(new EnumSetting<>("Targets", "Living entity group", Targets.PLAYERS));
    private final StringSetting entityTypes = register(new StringSetting("EntityTypes", "Optional space-separated entity IDs within the selected group; empty allows all", ""));
    private final EnumSetting<TargetUtil.SortMode> targetMode = register(new EnumSetting<>("TargetMode", "Order eligible targets", TargetUtil.SortMode.ANGLE));
    private final DoubleSetting maxTargets = register(new DoubleSetting("MaxTargets", "Maximum targets per attack window (rounded down)", 1, 1, 5));
    private final DoubleSetting range = register(new DoubleSetting("Range", "Distance to the nearest point of the target hitbox; vanilla reach still applies", 4.5, 0, 6));
    private final DoubleSetting wallsRange = register(new DoubleSetting("WallsRange", "Maximum distance for occluded targets when OnlyVisible is off", 3.5, 0, 6));
    private final BooleanSetting onlyVisible = register(new BooleanSetting("OnlyVisible", "Require a clear collision ray; overrides WallsRange", true));
    private final EnumSetting<Age> passiveAge = register(new EnumSetting<>("PassiveAge", "Age filter for animals and villagers", Age.ADULT));
    private final EnumSetting<Age> hostileAge = register(new EnumSetting<>("HostileAge", "Age filter for zombies, piglins, hoglins and zoglins", Age.BOTH));
    private final BooleanSetting targetPassive = register(new BooleanSetting("TargetPassive", "Include non-hostile mobs", true));
    private final BooleanSetting ignoreNamed = register(new BooleanSetting("IgnoreNamed", "Exclude custom-named mobs", false));
    private final BooleanSetting ignorePassive = register(new BooleanSetting("IgnorePassive", "Exclude calm endermen, piglins, zombified piglins and wolves", true));
    private final BooleanSetting ignoreTamed = register(new BooleanSetting("IgnoreTamed", "Exclude your own tamed mobs", true));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("IgnoreFriends", "Exclude friends by name and UUID", true));
    private final BooleanSetting ignoreInvisible = register(new BooleanSetting("IgnoreInvisible", "Exclude invisible entities", true));
    private final BooleanSetting ignoreTeammates = register(new BooleanSetting("IgnoreTeammates", "Exclude allied entities", true));
    private final BooleanSetting pauseOnLag = register(new BooleanSetting("PauseOnLag", "Pause after three seconds without a server time update", true));
    private final BooleanSetting pauseWhileUsingItem = register(new BooleanSetting("PauseWhileUsingItem", "Pause while using an item or breaking a block", true));
    private final BooleanSetting pauseInInventory = register(new BooleanSetting("PauseInInventory", "Pause with a screen open; ClickGUI always pauses", true));
    private final BooleanSetting tpsSync = register(new BooleanSetting("TpsSync", "Scale attack and switch delays to estimated server ticks", true));
    private final BooleanSetting customDelay = register(new BooleanSetting("CustomDelay", "Use HitDelay instead of vanilla weapon cooldown", false));
    private final DoubleSetting hitDelay = register(new DoubleSetting("HitDelay", "Server ticks between attack windows (rounded down)", 11, 0, 60));
    private final DoubleSetting switchDelay = register(new DoubleSetting("SwitchDelay", "Server ticks to wait after a slot change (rounded down)", 0, 0, 10));
    private final BooleanSetting swing = register(new BooleanSetting("Swing", "Send and animate the vanilla main-hand swing", true));
    private final Minecraft minecraft;
    private final FriendManager friends;
    private final RotationManager rotations;
    private final AttackManager attacks;
    private final AuraTiming timing = new AuraTiming();
    private AttackManager.Session attackSession;
    private RotationManager.Lease rotationLease;
    private ItemUtil.HotbarSelection weaponSelection;
    private LocalPlayer observedPlayer;
    private Object observedLevel;
    private List<LivingEntity> selected = List.of();
    private Set<String> weaponFamilies = Set.of();
    private Set<String> typeIds = Set.of();
    private String parsedWeapons;
    private String parsedTypes;
    private Runnable resumePathing;
    private int observedSlot = -1;
    private volatile long generation;
    private boolean prepared;
    private int preparedSlot = -1;

    public KillAuraModule(EventBus bus, Minecraft minecraft, FriendManager friends,
            RotationManager rotations, AttackManager attacks) {
        super("KillAura", "Configurable multi-target melee combat", Category.COMBAT, bus);
        this.minecraft = minecraft; this.friends = friends; this.rotations = rotations; this.attacks = attacks;
        weapons.setVisibleWhen(() -> attackWhenHolding.getValue() == AttackItems.WEAPONS);
        rotationTiming.setVisibleWhen(() -> rotate.getValue() != RotationManager.Mode.NONE);
        rotationSpeed.setVisibleWhen(() -> rotate.getValue() == RotationManager.Mode.SMOOTH);
        swapBack.setVisibleWhen(autoSwitch::getValue);
        maxTargets.setVisibleWhen(() -> !onlyOnLook.getValue());
        wallsRange.setVisibleWhen(() -> !onlyVisible.getValue());
        targetPassive.setVisibleWhen(() -> targets.getValue() != Targets.PLAYERS);
        passiveAge.setVisibleWhen(targetPassive::isVisible); hostileAge.setVisibleWhen(targetPassive::isVisible);
        hitDelay.setVisibleWhen(customDelay::getValue);
    }

    public DoubleSetting getRange() { return range; }
    public EnumSetting<RotationManager.Mode> getRotate() { return rotate; }
    public DoubleSetting getRotationSpeed() { return rotationSpeed; }
    public EnumSetting<TargetUtil.SortMode> getTargetMode() { return targetMode; }
    public EnumSetting<Targets> getTargets() { return targets; }
    public BooleanSetting getTargetPassive() { return targetPassive; }
    public BooleanSetting getOnlyVisible() { return onlyVisible; }
    public BooleanSetting getIgnoreFriends() { return ignoreFriends; }
    public BooleanSetting getIgnoreInvisible() { return ignoreInvisible; }
    public BooleanSetting getIgnoreTeammates() { return ignoreTeammates; }
    public BooleanSetting getAutoSwitch() { return autoSwitch; }
    public BooleanSetting getSwapBack() { return swapBack; }
    public BooleanSetting getIgnorePassive() { return ignorePassive; }
    public BooleanSetting getSwing() { return swing; }
    public BooleanSetting getPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public BooleanSetting getPauseInInventory() { return pauseInInventory; }
    public EnumSetting<AttackItems> getAttackWhenHolding() { return attackWhenHolding; }
    public StringSetting getWeapons() { return weapons; }
    public StringSetting getEntityTypes() { return entityTypes; }
    public DoubleSetting getMaxTargets() { return maxTargets; }
    public DoubleSetting getWallsRange() { return wallsRange; }
    public EnumSetting<ShieldMode> getShieldMode() { return shieldMode; }
    public EnumSetting<Age> getPassiveAge() { return passiveAge; }
    public BooleanSetting getIgnoreNamed() { return ignoreNamed; }
    public BooleanSetting getIgnoreTamed() { return ignoreTamed; }
    public BooleanSetting getOnlyOnClick() { return onlyOnClick; }
    public BooleanSetting getOnlyOnLook() { return onlyOnLook; }
    public BooleanSetting getCustomDelay() { return customDelay; }
    public DoubleSetting getHitDelay() { return hitDelay; }
    public DoubleSetting getSwitchDelay() { return switchDelay; }
    public EnumSetting<RotationTiming> getRotationTiming() { return rotationTiming; }
    public Optional<LivingEntity> getTarget() { return selected.stream().findFirst(); }
    public List<LivingEntity> getSelectedTargets() { return selected; }

    @Override protected void onEnable() {
        generation++;
        timing.reset(); observedSlot = -1;
        attackSession = attacks.openSession(this);
        listen(UpdateEvent.class, this::prepare);
        listen(UpdateEvent.class, -75, false, this::attackTick);
        listen(PacketEvent.Send.class, -200, false, event -> {
            if (event.getPacket() instanceof ServerboundSetCarriedItemPacket) { timing.switched(switchDelay.getValue().intValue()); }
        });
        listen(PacketEvent.Receive.class, -200, false, event -> {
            if (!(event.getPacket() instanceof ClientboundSetTimePacket)) { return; }
            long token = generation;
            long received = System.nanoTime();
            // Packet hooks run on Netty. State changes stay on the client thread and cannot outlive this enable cycle.
            minecraft.execute(() -> { if (isEnabled() && token == generation) { timing.timePacket(received); } });
        });
    }

    private void prepare(UpdateEvent event) {
        prepared = false;
        LocalPlayer player = minecraft.player;
        if (observedPlayer != player || observedLevel != minecraft.level) {
            pause(); timing.reset(); generation++;
            observedPlayer = player; observedLevel = minecraft.level; observedSlot = -1;
        }
        timing.tick(tpsSync.getValue());
        if (shouldPause(player)) { pause(); return; }
        refreshFilters();
        if (observedSlot != player.getInventory().getSelectedSlot()) {
            if (observedSlot >= 0) { timing.switched(switchDelay.getValue().intValue()); }
            observedSlot = player.getInventory().getSelectedSlot();
        }
        if (onlyOnLook.getValue()) {
            selected = minecraft.crosshairPickEntity instanceof LivingEntity living && eligible(living) ? List.of(living) : List.of();
        } else {
            selected = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range.getValue()), this::eligible)
                    .stream().sorted(targetOrder(player)).limit(maxTargets.getValue().intValue()).toList();
        }
        if (selected.isEmpty()) { pause(); return; }
        boolean axe = shieldMode.getValue() == ShieldMode.BREAK && selected.stream().anyMatch(entity -> entity instanceof Player p && p.isBlocking());
        if (autoSwitch.getValue()) {
            if (weaponSelection == null) { weaponSelection = new ItemUtil.HotbarSelection(player); }
            var best = attackWhenHolding.getValue() == AttackItems.ALL && !axe ? java.util.OptionalInt.empty()
                    : ItemUtil.bestHotbarWeapon(player, stack -> acceptable(stack, axe));
            if (best.isPresent() && weaponSelection.select(best.getAsInt())) {
                player.resetAttackStrengthTicker();
                observedSlot = player.getInventory().getSelectedSlot(); timing.switched(switchDelay.getValue().intValue());
                return;
            }
        } else { releaseWeapon(); }
        if (!acceptable(player.getMainHandItem(), axe)) { pause(); return; }
        if (pauseBaritone.getValue()) {
            if (resumePathing == null) {
                resumePathing = Client.INSTANCE.getModuleManager().getByClass(BaritoneModule.class)
                        .flatMap(module -> module.access().pauseForCombat()).orElse(null);
            }
        } else { releasePathing(); }
        boolean aim = rotate.getValue() != RotationManager.Mode.NONE && rotationTiming.getValue() != RotationTiming.NONE
                && (rotationTiming.getValue() == RotationTiming.ALWAYS || delayReady(player));
        if (aim) {
            if (rotationLease == null) { rotationLease = rotations.acquire(this, 50); }
            var desiredRotation = RotationManager.towardEntity(player, selected.getFirst());
            rotationLease.request(desiredRotation, rotate.getValue(), rotationSpeed.getValue());
        } else { releaseRotation(); }
        preparedSlot = player.getInventory().getSelectedSlot();
        prepared = true;
    }

    private Comparator<LivingEntity> targetOrder(LocalPlayer player) {
        Comparator<LivingEntity> order = switch (targetMode.getValue()) {
            case DISTANCE -> Comparator.comparingDouble(entity -> AuraFilters.distanceSquared(player.position(), entity.getBoundingBox()));
            case HEALTH -> Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount());
            case ANGLE -> Comparator.comparingDouble(entity -> TargetUtil.crosshairAngle(player, entity));
            case FARTHEST -> Comparator.<LivingEntity>comparingDouble(entity -> AuraFilters.distanceSquared(player.position(), entity.getBoundingBox())).reversed();
            case HIGHEST_HEALTH -> Comparator.<LivingEntity>comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount()).reversed();
        };
        return order.thenComparingDouble(player::distanceToSqr).thenComparingInt(LivingEntity::getId);
    }
    private void refreshFilters() {
        if (!weapons.getValue().equals(parsedWeapons)) { parsedWeapons = weapons.getValue(); weaponFamilies = AuraFilters.words(parsedWeapons); }
        if (!entityTypes.getValue().equals(parsedTypes)) { parsedTypes = entityTypes.getValue(); typeIds = AuraFilters.words(parsedTypes); }
    }
    private boolean eligible(LivingEntity entity) {
        LocalPlayer player = minecraft.player;
        if (player == null || entity == player || entity == minecraft.getCameraEntity() || entity.level() != minecraft.level
                || entity.isDeadOrDying() || entity.isRemoved() || !entity.isAttackable()
                || (ignoreInvisible.getValue() && entity.isInvisible()) || (ignoreTeammates.getValue() && player.isAlliedTo(entity))
                || AuraFilters.distanceSquared(player.position(), entity.getBoundingBox()) > range.getValue() * range.getValue()
                || !player.isWithinAttackRange(player.getMainHandItem(), entity.getBoundingBox(), 0)) { return false; }
        if (!typeIds.isEmpty()) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (!typeIds.contains(id.toString()) && !typeIds.contains(id.getPath())) { return false; }
        }
        if (entity instanceof Player other) {
            if (targets.getValue() == Targets.MOBS || other.isCreative() || other.isSpectator()
                    || (ignoreFriends.getValue() && friends.isFriend(other.getUUID(), other.getGameProfile().name()))
                    || (shieldMode.getValue() == ShieldMode.IGNORE && other.isBlocking())) { return false; }
        } else {
            if (targets.getValue() == Targets.PLAYERS || (ignoreNamed.getValue() && entity.hasCustomName())
                    || (!targetPassive.getValue() && TargetUtil.isPassive(entity))) { return false; }
            if (ignoreTamed.getValue() && entity instanceof OwnableEntity ownable && player.equals(ownable.getOwner())) { return false; }
            if (ignorePassive.getValue() && ((entity instanceof EnderMan enderman && !enderman.isCreepy())
                    || ((entity instanceof Piglin || entity instanceof ZombifiedPiglin || entity instanceof Wolf)
                    && entity instanceof Mob mob && !mob.isAggressive()))) { return false; }
            if (entity instanceof Zombie || entity instanceof Piglin || entity instanceof Hoglin || entity instanceof Zoglin) {
                if (!hostileAge.getValue().accepts(entity.isBaby())) { return false; }
            } else if (entity instanceof AgeableMob && !(entity instanceof Frog || entity instanceof Parrot)
                    && !passiveAge.getValue().accepts(entity.isBaby())) { return false; }
        }
        return TargetUtil.canSee(player, entity) || (!onlyVisible.getValue()
                && AuraFilters.distanceSquared(player.position(), entity.getBoundingBox()) <= wallsRange.getValue() * wallsRange.getValue());
    }
    private boolean acceptable(ItemStack stack, boolean axe) {
        return axe ? stack.is(ItemTags.AXES) : attackWhenHolding.getValue() == AttackItems.ALL || AuraFilters.weapon(stack, weaponFamilies);
    }
    private boolean delayReady(LocalPlayer player) {
        return timing.ready(customDelay.getValue(), hitDelay.getValue().intValue(), player.getCurrentItemAttackStrengthDelay(), player.getAttackStrengthScale(0.5f) >= 1);
    }
    private void attackTick(UpdateEvent event) {
        if (!prepared || selected.isEmpty() || !isEnabled()) { return; }
        if (shouldPause(minecraft.player) || minecraft.player != observedPlayer || minecraft.level != observedLevel) { pause(); return; }
        refreshFilters();
        if (!eligible(selected.getFirst())) { pause(); return; }
        if (!delayReady(minecraft.player)) { return; }
        if (!rotationReady()) { return; }
        var policy = new AttackManager.Policy(20, 20, !customDelay.getValue());
        int hits = attackSession.attackBatch(selected.subList(0, Math.min(selected.size(), maxTargets.getValue().intValue())), policy, swing.getValue() ? AttackManager.SwingMode.BOTH : AttackManager.SwingMode.NONE,
                entity -> {
                    refreshFilters();
                    boolean axe = shieldMode.getValue() == ShieldMode.BREAK && selected.stream().anyMatch(target -> target instanceof Player p && p.isBlocking());
                    return isEnabled() && prepared && !shouldPause(minecraft.player) && eligible(entity)
                            && minecraft.player.getInventory().getSelectedSlot() == preparedSlot && timing.switchReady() && rotationReady()
                            && selected.indexOf(entity) < maxTargets.getValue().intValue()
                            && (!onlyOnLook.getValue() || minecraft.crosshairPickEntity == entity)
                            && acceptable(minecraft.player.getMainHandItem(), axe);
                });
        if (hits > 0) { timing.attacked(); if (rotationTiming.getValue() == RotationTiming.ON_HIT) { releaseRotation(); } }
    }
    private boolean rotationReady() {
        if (rotate.getValue() == RotationManager.Mode.NONE || rotationTiming.getValue() == RotationTiming.NONE) { return true; }
        return minecraft.player != null && !selected.isEmpty() && rotations.isReady(rotationLease,
                RotationManager.towardEntity(minecraft.player, selected.getFirst()), 2);
    }
    private boolean shouldPause(LocalPlayer player) {
        var screen = minecraft.gui.screen();
        return player == null || minecraft.level == null || minecraft.gameMode == null || minecraft.getConnection() == null
                || !player.isAlive() || player.isRemoved() || player.isSpectator() || minecraft.gui.overlay() != null
                || screen instanceof dev.riserecode.ui.clickgui.ClickGui || (pauseInInventory.getValue() && screen != null)
                || (pauseWhileUsingItem.getValue() && (player.isUsingItem() || minecraft.gameMode.isDestroying()))
                || (onlyOnClick.getValue() && !minecraft.options.keyAttack.isDown())
                || (pauseOnLag.getValue() && timing.lagging(System.nanoTime()));
    }
    private void pause() { prepared = false; selected = List.of(); releaseRotation(); releaseWeapon(); releasePathing(); }
    private void releaseRotation() { if (rotationLease != null) { rotationLease.close(); rotationLease = null; } }
    private void releaseWeapon() {
        if (weaponSelection == null) { return; }
        if (weaponSelection.getPlayer() == minecraft.player && weaponSelection.getPlayer().level() == minecraft.level && swapBack.getValue()) { weaponSelection.close(); }
        else { weaponSelection.abandon(); }
        weaponSelection = null;
    }
    private void releasePathing() { if (resumePathing != null) { resumePathing.run(); resumePathing = null; } }
    @Override protected void onDisable() {
        generation++; pause(); timing.reset(); observedPlayer = null; observedLevel = null; observedSlot = -1;
        if (attackSession != null) { attackSession.close(); attackSession = null; }
    }
}
