package dev.riserecode;

import com.mojang.authlib.GameProfile;
import dev.riserecode.combat.AuraFilters;
import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Real client selection, intent revalidation, batch dispatch and lifetime regression checks. */
public final class KillAuraIntegrationTest {
    private static int assertions;
    private static final class FixturePlayer extends RemotePlayer {
        private FixturePlayer(net.minecraft.client.multiplayer.ClientLevel level, GameProfile profile) { super(level, profile); }
        private void blocking(boolean enabled) {
            if (enabled) {
                setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
                // Apply the same synced use/hand flags a remote server sends, then advance vanilla shield delay.
                setLivingEntityFlag(2, true); setLivingEntityFlag(1, true);
                for (int i = 0; i < 8; i++) { updateUsingItem(getUseItem()); }
            } else { setLivingEntityFlag(1, false); stopUsingItem(); }
        }
    }
    private KillAuraIntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        Client client = Client.INSTANCE;
        var aura = client.getModuleManager().getByClass(KillAuraModule.class).orElseThrow();
        var snapshot = context.computeOnClient(mc -> client.getConfigManager().profileSnapshot());
        FixturePlayer[] actors = new FixturePlayer[2];
        Pig[] pigs = new Pig[2];
        Wolf[] wolf = new Wolf[1];
        var previousItems = context.computeOnClient(mc -> {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < 9; i++) { items.add(mc.player.getInventory().getItem(i).copy()); }
            return items;
        });
        int previousSlot = context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
        AtomicInteger intents = new AtomicInteger();
        AtomicInteger packets = new AtomicInteger();
        ConcurrentLinkedQueue<Integer> attackTicks = new ConcurrentLinkedQueue<>();
        var observer = client.getEventBus().subscribe(PacketEvent.Send.class, -300, false, event -> {
            if (event.getPacket() instanceof ServerboundAttackPacket) {
                packets.incrementAndGet(); attackTicks.add(net.minecraft.client.Minecraft.getInstance().player.tickCount);
            }
        });
        var cancel = client.getEventBus().subscribe(AttackEvent.class, 100, false, event -> { intents.incrementAndGet(); event.cancel(); });
        try {
            // Existing utility tests leave a safe platform. Remove their server mobs first.
            world.getServer().runCommand("execute at @a run kill @e[type=!minecraft:player,distance=..16]");
            context.waitTicks(4);
            context.runOnClient(mc -> {
                client.getModuleManager().setEnabled(aura, false);
                aura.getTargets().setValue(KillAuraModule.Targets.PLAYERS);
                aura.getAttackWhenHolding().setValue(KillAuraModule.AttackItems.ALL);
                aura.getRotate().setValue(RotationManager.Mode.NONE);
                aura.getMaxTargets().setValue(2.0); aura.getRange().setValue(4.5);
                aura.getCustomDelay().setValue(true); aura.getHitDelay().setValue(0.0);
                for (int i = 0; i < actors.length; i++) {
                    actors[i] = new FixturePlayer(mc.level, new GameProfile(UUID.randomUUID(), "AuraRewrite" + i));
                    actors[i].setId(1_002_000 + i); actors[i].snapTo(mc.player.getX() + 1 + i * 0.5, mc.player.getY(), mc.player.getZ());
                    actors[i].setOldPosAndRot(); mc.level.addEntity(actors[i]);
                    pigs[i] = new Pig(EntityTypes.PIG, mc.level); pigs[i].setId(1_002_010 + i);
                    pigs[i].snapTo(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 1 + i * 0.5);
                    pigs[i].setOldPosAndRot(); mc.level.addEntity(pigs[i]);
                }
                pigs[1].setBaby(true);
                wolf[0] = new Wolf(EntityTypes.WOLF, mc.level); wolf[0].setId(1_002_020);
                wolf[0].snapTo(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 1);
                wolf[0].setOwner(mc.player); wolf[0].setTame(true, false); mc.level.addEntity(wolf[0]);
                client.getModuleManager().setEnabled(aura, true);
                update(); check(aura.getSelectedTargets().size() == 2, "two player targets selected");
                check(intents.get() >= 2 && packets.get() == 0, "cancelled intents emit no batch packets");
                aura.getMaxTargets().setValue(1.0); update(); check(aura.getSelectedTargets().size() == 1, "target cap applied");
                aura.getEntityTypes().setValue("minecraft:pig"); update(); check(aura.getTarget().isEmpty(), "entity IDs intersect entity group");
                aura.getEntityTypes().setValue("typo:missing"); update(); check(aura.getTarget().isEmpty(), "unknown entity IDs fail closed");
                aura.getEntityTypes().setValue("");
                aura.getOnlyOnClick().setValue(true); update(); check(aura.getTarget().isEmpty(), "click condition pauses without held attack");
                aura.getOnlyOnClick().setValue(false); aura.getOnlyOnLook().setValue(true);
                mc.crosshairPickEntity = actors[1]; update(); check(aura.getTarget().orElse(null) == actors[1], "look condition uses picked entity");
                mc.crosshairPickEntity = pigs[0]; update(); check(aura.getTarget().isEmpty(), "look condition still filters entity group");
                aura.getOnlyOnLook().setValue(false); mc.crosshairPickEntity = null;
                actors[0].blocking(true);
                aura.getShieldMode().setValue(KillAuraModule.ShieldMode.IGNORE); update();
                check(actors[0].isBlocking() && !aura.getSelectedTargets().contains(actors[0]), "ignore shield excludes blocking player");
                aura.getShieldMode().setValue(KillAuraModule.ShieldMode.BREAK);
                aura.getOnlyOnLook().setValue(true); mc.crosshairPickEntity = actors[0];
                for (int i = 0; i < 9; i++) { mc.player.getInventory().setItem(i, ItemStack.EMPTY); }
                mc.player.getInventory().setItem(7, new ItemStack(Items.IRON_SWORD));
                mc.player.getInventory().setItem(8, new ItemStack(Items.DIAMOND_AXE));
                mc.player.getInventory().setSelectedSlot(7); aura.getAutoSwitch().setValue(true);
                update(); check(mc.player.getInventory().getSelectedSlot() == 8, "shield break selects an axe");
                client.getModuleManager().setEnabled(aura, false);
                check(mc.player.getInventory().getSelectedSlot() == 7, "shield selection restores previous slot");
                aura.getSwapBack().setValue(false); client.getModuleManager().setEnabled(aura, true); update();
                client.getModuleManager().setEnabled(aura, false);
                check(mc.player.getInventory().getSelectedSlot() == 8, "swap-back off retains selected weapon");
                aura.getSwapBack().setValue(true); mc.player.getInventory().setSelectedSlot(7);
                client.getModuleManager().setEnabled(aura, true); update(); mc.player.getInventory().setSelectedSlot(6);
                update(); client.getModuleManager().setEnabled(aura, false);
                check(mc.player.getInventory().getSelectedSlot() == 6, "manual slot override wins over shield selection");
                aura.getAutoSwitch().setValue(false); client.getModuleManager().setEnabled(aura, true);
                aura.getShieldMode().setValue(KillAuraModule.ShieldMode.NONE); actors[0].blocking(false);
                aura.getOnlyOnLook().setValue(false); mc.crosshairPickEntity = null;
                aura.getTargets().setValue(KillAuraModule.Targets.MOBS); aura.getMaxTargets().setValue(5.0);
                aura.getEntityTypes().setValue("pig"); update();
                check(aura.getSelectedTargets().equals(List.of(pigs[0])), "adult passive age filter");
                aura.getPassiveAge().setValue(KillAuraModule.Age.BABY); update();
                check(aura.getSelectedTargets().equals(List.of(pigs[1])), "baby passive age filter");
                aura.getPassiveAge().setValue(KillAuraModule.Age.BOTH); update(); check(aura.getSelectedTargets().size() == 2, "both passive ages");
                pigs[0].setCustomName(Component.literal("Protected")); aura.getIgnoreNamed().setValue(true); update();
                check(!aura.getSelectedTargets().contains(pigs[0]), "named mob filtering");
                aura.getEntityTypes().setValue("wolf"); aura.getIgnorePassive().setValue(false); aura.getIgnoreTeammates().setValue(false); update();
                check(aura.getTarget().isEmpty(), "owned tamed wolf excluded");
                aura.getIgnoreTamed().setValue(false); update(); check(aura.getTarget().orElse(null) == wolf[0], "tamed filtering can be disabled");
                aura.getIgnoreTamed().setValue(true); aura.getIgnoreTeammates().setValue(true); aura.getIgnorePassive().setValue(true); aura.getEntityTypes().setValue("pig");
                aura.getIgnoreNamed().setValue(false); aura.getTargetPassive().setValue(false); update(); check(aura.getTarget().isEmpty(), "passive exclusion");
                aura.getTargetPassive().setValue(true); aura.getTargets().setValue(KillAuraModule.Targets.PLAYERS); aura.getEntityTypes().setValue("");
                aura.getRange().setValue(0.0); update(); check(aura.getTarget().isEmpty(), "zero radius selects no separated target");
                aura.getRange().setValue(4.5);
                check(AuraFilters.weapon(new ItemStack(Items.IRON_SWORD), AuraFilters.words("sword")), "weapon family covers tiers");
                check(!AuraFilters.weapon(new ItemStack(Items.DIAMOND_AXE), AuraFilters.words("sword")), "weapon family excludes others");
                check(!AuraFilters.weapon(new ItemStack(Items.BREAD), AuraFilters.words("sword axe")), "food rejected as weapon");
                aura.getAttackWhenHolding().setValue(KillAuraModule.AttackItems.WEAPONS); aura.getWeapons().setValue("invalid");
                update(); check(aura.getTarget().isEmpty(), "unknown weapon family fails closed");
                aura.getAttackWhenHolding().setValue(KillAuraModule.AttackItems.ALL);
                client.getModuleManager().setEnabled(aura, false);
            });
            BlockPos wall = context.computeOnClient(mc -> mc.player.blockPosition().offset(1, 1, 0));
            String wallPos = wall.getX() + " " + wall.getY() + " " + wall.getZ();
            world.getServer().runCommand("setblock " + wallPos + " minecraft:stone");
            context.waitFor(mc -> mc.level.getBlockState(wall).is(Blocks.STONE));
            context.runOnClient(mc -> {
                aura.getOnlyOnLook().setValue(true); mc.crosshairPickEntity = actors[0];
                aura.getOnlyVisible().setValue(true); client.getModuleManager().setEnabled(aura, true); update();
                check(aura.getTarget().isEmpty(), "visible-only rejects target behind collision wall");
                aura.getOnlyVisible().setValue(false); aura.getWallsRange().setValue(0.0); update();
                check(aura.getTarget().isEmpty(), "zero wall range rejects occluded target");
                aura.getWallsRange().setValue(2.0); update(); check(aura.getTarget().orElse(null) == actors[0], "wall range permits nearby occluded target");
                client.getModuleManager().setEnabled(aura, false);
                aura.getOnlyOnLook().setValue(false); aura.getOnlyVisible().setValue(true); mc.crosshairPickEntity = null;
            });
            world.getServer().runCommand("setblock " + wallPos + " minecraft:air");
            context.waitFor(mc -> mc.level.getBlockState(wall).isAir());
            cancel.close();
            // Exercise the shared batch API with real wire sends, deduplication and a competing owner.
            context.runOnClient(mc -> {
                try (var batch = client.getAttackManager().openSession(new Object()); var competing = client.getAttackManager().openSession(new Object())) {
                    update();
                    int before = packets.get();
                    var policy = new AttackManager.Policy(20, 20, false);
                    check(batch.attackBatch(List.of(actors[0], actors[0], actors[1]), policy, AttackManager.SwingMode.NONE, entity -> true) == 2,
                            "batch dispatches each distinct target once");
                    check(packets.get() == before + 2, "two ordinary attack packets emitted");
                    check(!competing.attack(actors[0], policy, AttackManager.SwingMode.NONE), "batch reserves global tick against competing owner");
                }
            });
            context.waitTicks(2);
            // Change friend membership inside the intent itself: post-intent checks must reject dispatch.
            var protect = client.getEventBus().subscribe(AttackEvent.class, 100, false,
                    event -> client.getFriendManager().add(event.getTarget().getName().getString(), event.getTarget().getUUID()));
            try {
                context.runOnClient(mc -> {
                    int before = packets.get();
                    aura.getMaxTargets().setValue(2.0);
                    client.getModuleManager().setEnabled(aura, true); update();
                    check(packets.get() == before, "late friend changes reject all prepared attacks");
                    client.getModuleManager().setEnabled(aura, false);
                });
            } finally { protect.close(); }
            context.waitTicks(30);
            context.runOnClient(mc -> {
                try (var session = client.getAttackManager().openSession(new Object());
                        var resetCooldown = client.getEventBus().subscribe(AttackEvent.class, 100, false, event -> mc.player.resetAttackStrengthTicker())) {
                    // Referencing the handle also verifies the test listener is still installed for this intent.
                    check(resetCooldown.isActive() && mc.player.getAttackStrengthScale(0) >= 1 && !session.attack(actors[0], new AttackManager.Policy(20, 20, true), AttackManager.SwingMode.NONE),
                            "post-intent cooldown reset blocks a charged single attack");
                }
            });
            int beforeOnHit = packets.get(); attackTicks.clear();
            context.runOnClient(mc -> {
                for (RemotePlayer actor : actors) { client.getFriendManager().remove(actor.getName().getString()); }
                aura.getMaxTargets().setValue(1.0); aura.getOnlyOnLook().setValue(false); mc.crosshairPickEntity = null;
                aura.getTargetMode().setValue(dev.riserecode.util.TargetUtil.SortMode.DISTANCE);
                aura.getRotate().setValue(RotationManager.Mode.SNAPPY); aura.getRotationTiming().setValue(KillAuraModule.RotationTiming.ON_HIT);
                aura.getHitDelay().setValue(6.0); aura.getSwing().setValue(false);
                client.getModuleManager().setEnabled(aura, true);
            });
            context.waitFor(mc -> packets.get() >= beforeOnHit + 2, 8000);
            var tickTimes = List.copyOf(attackTicks);
            check(tickTimes.get(1) - tickTimes.get(0) >= 6, "custom tick delay spaces native attacks");
            check(context.computeOnClient(mc -> client.getRotationManager().getLeaseCount() == 0), "on-hit rotation releases after dispatch");
            int beforeSlot = packets.get();
            context.runOnClient(mc -> {
                aura.getSwitchDelay().setValue(10.0);
                mc.player.getInventory().setSelectedSlot((mc.player.getInventory().getSelectedSlot() + 1) % 9);
                update();
            });
            context.waitTicks(8); check(packets.get() == beforeSlot, "manual slot change waits configured switch delay");
            context.waitFor(mc -> packets.get() > beforeSlot, 8000);
            context.runOnClient(mc -> { aura.getOnlyOnClick().setValue(true); update(); });
            int beforeClick = packets.get(); context.waitTicks(8); check(packets.get() == beforeClick, "click gate stops live dispatch");
            context.runOnClient(mc -> {
                client.getModuleManager().setEnabled(aura, false);
                int listeners = client.getEventBus().getListenerCount();
                for (int i = 0; i < 10; i++) { client.getModuleManager().setEnabled(aura, true); client.getModuleManager().setEnabled(aura, false); }
                check(client.getEventBus().getListenerCount() == listeners, "repeated toggles release packet and update listeners");
                check(client.getAttackManager().getSessionCount() == 0 && client.getRotationManager().getLeaseCount() == 0,
                        "repeated toggles release combat ownership");
            });
            client.getLogger().info("KillAura rewrite native checks passed ({} assertions)", assertions);
        } finally {
            cancel.close(); observer.close();
            context.runOnClient(mc -> {
                client.getModuleManager().setEnabled(aura, false);
                for (RemotePlayer actor : actors) { if (actor != null) { client.getFriendManager().remove(actor.getName().getString()); mc.level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED); } }
                for (Pig pig : pigs) { if (pig != null) { mc.level.removeEntity(pig.getId(), Entity.RemovalReason.DISCARDED); } }
                if (wolf[0] != null) { mc.level.removeEntity(wolf[0].getId(), Entity.RemovalReason.DISCARDED); }
                for (int i = 0; i < 9; i++) { mc.player.getInventory().setItem(i, previousItems.get(i)); }
                mc.player.getInventory().setSelectedSlot(previousSlot);
                client.getConfigManager().applyProfile(snapshot);
            });
        }
    }
    private static void update() { Client.INSTANCE.getEventBus().post(new UpdateEvent(net.minecraft.client.Minecraft.getInstance(), 0)); }
    private static void check(boolean condition, String label) { if (!condition) { throw new AssertionError(label); } assertions++; }
}
