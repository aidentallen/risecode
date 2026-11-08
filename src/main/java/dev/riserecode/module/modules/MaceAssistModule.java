package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.KeyPressEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.KeySetting;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import dev.riserecode.util.TargetUtil;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;

/** Falling smash attacks: swaps to the mace, keeps the target in the crosshair, strikes on time and can launch with a wind charge. */
public final class MaceAssistModule extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }
    private enum Launch { IDLE, AIM, RISE }
    private final BooleanSetting smash = register(new BooleanSetting("Smash Trigger", "Attack on its own once a smash would land", true));
    private final DoubleSetting minFall = register(DoubleSetting.extended("Min Fall", "Fall distance in blocks before the smash trigger strikes; the game needs more than 1.5", 1.6, 1.5, 30));
    private final DoubleSetting range = register(DoubleSetting.extended("Lock Range", "How far away a target is picked while you fall, so the swap and aim happen before the hit window", 16, 4, 40));
    public final EnumSetting<Targets> targets = register(new EnumSetting<>("Targets", "Entity types to smash", Targets.PLAYERS));
    private final BooleanSetting autoSwap = register(new BooleanSetting("Auto Swap", "Hold the mace while falling at a target, then restore your slot", true));
    private final BooleanSetting aim = register(new BooleanSetting("Aim While Falling", "Keep the target in the crosshair during the fall", true));
    private final DoubleSetting aimSpeed = register(DoubleSetting.extended("Aim Speed", "Maximum degrees per second", 360, 60, 1080));
    private final BooleanSetting shieldCombo = register(new BooleanSetting("Shield Combo", "Break a raised shield with an axe, then smash", true));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("Ignore Friends", "Never smash friends", true));
    public final KeySetting windKey = register(new KeySetting("Wind Launch Key", "Jump and throw a wind charge at your feet; unbound by default", -1));
    private final Minecraft minecraft;
    private final FriendManager friends;
    private final RotationManager rotations;
    private final AttackManager attacks;
    private AttackManager.Session session;
    private RotationManager.Lease lease;
    private ItemUtil.HotbarSelection selection;
    private LivingEntity target;
    private Launch launch = Launch.IDLE;
    private int launchTicks;
    private boolean launchRequested;
    private Runnable action;

    public MaceAssistModule(EventBus bus, Minecraft minecraft, FriendManager friends, RotationManager rotations, AttackManager attacks) {
        super("MaceAssist", "Smash attack trigger, shield combo and wind charge launch.", Category.COMBAT, bus);
        this.minecraft = minecraft; this.friends = friends; this.rotations = rotations; this.attacks = attacks;
        aimSpeed.setVisibleWhen(aim::getValue);
    }

    @Override protected void onEnable() {
        session = attacks.openSession(this);
        listen(KeyPressEvent.class, event -> { if (event.isPress() && windKey.getValue() >= 0 && event.getKey() == windKey.getValue() && minecraft.gui.screen() == null) { launchRequested = true; } });
        listen(UpdateEvent.class, this::update);
        // Rotation arbitration runs at -50; acting afterwards sees the angles that will actually be sent.
        listen(UpdateEvent.class, -75, false, this::act);
    }

    private void update(UpdateEvent event) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null || !player.isAlive() || player.isSpectator() || minecraft.gui.screen() != null) { stopAll(); return; }
        if (launchRequested) { launchRequested = false; if (launch == Launch.IDLE && slotOf(player, Items.WIND_CHARGE) >= 0) { launch = Launch.AIM; launchTicks = 0; } }
        if (launch != Launch.IDLE) { windLaunch(player); return; }
        if (!smash.getValue()) { stopAll(); return; }
        boolean falling = !player.onGround() && !player.isFallFlying() && player.getDeltaMovement().y < 0;
        int mace = slotOf(player, Items.MACE);
        if (!falling || mace < 0) { release(); return; }
        var options = new TargetUtil.Options(range.getValue(), TargetUtil.EntityGroup.valueOf(targets.getValue().name()), TargetUtil.SortMode.DISTANCE,
                true, ignoreFriends.getValue(), true, true, true);
        target = TargetUtil.bestTarget(player, friends, options).orElse(null);
        if (target == null) { release(); return; }
        if (aim.getValue()) {
            if (lease == null) { lease = rotations.acquire(this, 60); }
            var mode = player.distanceToSqr(target) < 100 ? RotationManager.Mode.SNAPPY : RotationManager.Mode.SMOOTH;
            lease.request(RotationManager.towardEntity(player, target), mode, aimSpeed.getValue());
        }
        int axe = shieldCombo.getValue() && target.isBlocking() ? slotOf(player, stack -> stack.is(ItemTags.AXES)) : -1;
        boolean breakShield = axe >= 0;
        if (autoSwap.getValue()) {
            if (selection == null) { selection = new ItemUtil.HotbarSelection(player); }
            if (selection.wasManuallyOverridden()) { return; }
            selection.select(breakShield ? axe : mace);
        }
        var victim = target;
        // After an axe hit the mace is not recharged, so the follow-up smash ignores the cooldown.
        var policy = new AttackManager.Policy(2, 8, !(breakShield || shieldCombo.getValue()));
        action = () -> {
            if (!(breakShield ? player.getMainHandItem().is(ItemTags.AXES) : player.getMainHandItem().is(Items.MACE))) { return; }
            if (!breakShield && (!MaceItem.canSmashAttack(player) || player.fallDistance < minFall.getValue())) { return; }
            session.attack(victim, policy, AttackManager.SwingMode.BOTH);
        };
    }

    /** Runs after the rotation manager has applied this tick's angles, so readiness checks are meaningful. */
    private void act(UpdateEvent event) {
        Runnable step = action;
        action = null;
        if (step != null && minecraft.player != null && isEnabled()) { step.run(); }
    }

    private void windLaunch(LocalPlayer player) {
        int slot = slotOf(player, Items.WIND_CHARGE);
        if (slot < 0 || ++launchTicks > 30) { stopAll(); return; }
        if (selection == null) { selection = new ItemUtil.HotbarSelection(player); }
        if (selection.wasManuallyOverridden()) { stopAll(); return; }
        selection.select(slot);
        if (lease == null) { lease = rotations.acquire(this, 90); }
        var down = new RotationManager.Rotation(player.getYRot(), 90);
        lease.request(down, RotationManager.Mode.SNAPPY, 1080);
        action = () -> {
            if (!rotations.isReady(lease, down, 4)) { return; }
            if (launch == Launch.AIM) {
                if (player.onGround()) { player.jumpFromGround(); }
                launch = Launch.RISE;
                return;
            }
            if (!player.onGround() && player.getMainHandItem().is(Items.WIND_CHARGE)) {
                minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
                stopAll();
            }
        };
    }

    private static int slotOf(LocalPlayer player, Item item) { return slotOf(player, stack -> stack.is(item)); }
    private static int slotOf(LocalPlayer player, Predicate<ItemStack> test) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) { if (test.test(inventory.getItem(slot))) { return slot; } }
        return -1;
    }

    private void release() {
        target = null;
        if (lease != null) { lease.close(); lease = null; }
        if (selection != null) { if (selection.getPlayer() == minecraft.player) { selection.close(); } else { selection.abandon(); } selection = null; }
    }
    private void stopAll() { action = null; launch = Launch.IDLE; launchTicks = 0; launchRequested = false; release(); if (session != null) { session.reset(); } }
    @Override protected void onDisable() { stopAll(); if (session != null) { session.close(); session = null; } }
}
