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
                context.runOnClient(minecraft -> client.getClickGui().selectCategory(Category.COMBAT));
                context.waitTicks(5);
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
