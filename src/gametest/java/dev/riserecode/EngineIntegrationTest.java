package dev.riserecode;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.TracersModule;
import dev.riserecode.testmixin.ChatStateAccessor;
import dev.riserecode.ui.clickgui.ClickGui;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;

/** Uses the already loaded Phase 4 test world; no connections to an external server. */
public final class EngineIntegrationTest {
    private EngineIntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var client = Client.INSTANCE;
        var modules = client.getModuleManager();
        var sprint = context.computeOnClient(minecraft -> modules.getByClass(SprintModule.class).orElseThrow());
        var aura = context.computeOnClient(minecraft -> modules.getByClass(KillAuraModule.class).orElseThrow());
        var tracers = context.computeOnClient(minecraft -> modules.getByClass(TracersModule.class).orElseThrow());
        var noFall = context.computeOnClient(minecraft -> modules.getByClass(NoFallModule.class).orElseThrow());
        AtomicInteger chatPackets = new AtomicInteger();
        AtomicInteger attacks = new AtomicInteger();
        AtomicInteger receivePackets = new AtomicInteger();
        AtomicInteger leakedServerSends = new AtomicInteger();
        AtomicReference<String> receiveThread = new AtomicReference<>();
        var sends = client.getEventBus().subscribe(PacketEvent.Send.class, -200, true, event -> {
            if (event.getPacket() instanceof ServerboundChatPacket) { chatPackets.incrementAndGet(); }
            if (event.getPacket() instanceof ServerboundAttackPacket) { attacks.incrementAndGet(); }
            if (event.getPacket() instanceof ClientboundSystemChatPacket) { leakedServerSends.incrementAndGet(); }
        });
        var receives = client.getEventBus().subscribe(PacketEvent.Receive.class, 100, false, event -> {
            receivePackets.incrementAndGet(); receiveThread.set(Thread.currentThread().getName());
            if (event.getPacket() instanceof ClientboundSystemChatPacket chat) {
                if (chat.content().getString().equals("engine-receive-cancel")) { event.cancel(); }
                if (chat.content().getString().equals("engine-receive-before")) {
                    event.setPacket(new ClientboundSystemChatPacket(Component.literal("engine-receive-after"), false));
                }
            }
        });
        RemotePlayer[] actors = new RemotePlayer[2];
        try {
            command(context, ".help"); command(context, ".unknown");
            check(chatPackets.get() == 0, "local commands never produce chat packets");
            command(context, ".bind Sprint F6");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == InputConstants.KEY_F6), "bind command reaches module manager");
            command(context, ".bind Sprint RIGHT_SHIFT");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == InputConstants.KEY_F6), "GUI key conflict leaves module bind intact");
            command(context, ".t Sprint");
            check(context.computeOnClient(minecraft -> sprint.isEnabled()), "toggle alias initializes module");
            context.getInput().holdKey(InputConstants.KEY_W);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.player.isSprinting()), "forward input requests sprint");
            context.getInput().releaseKey(InputConstants.KEY_W);
            context.runOnClient(minecraft -> sprint.getOmniSprint().setValue(true));
            context.getInput().holdKey(InputConstants.KEY_S);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.player.isSprinting()), "omni input requests sprint");
            context.getInput().releaseKey(InputConstants.KEY_S);
            context.getInput().pressKey(InputConstants.KEY_F6);
            check(context.computeOnClient(minecraft -> !sprint.isEnabled()), "bound real key toggles module off once");
            command(context, ".bind Sprint NONE");
            check(context.computeOnClient(minecraft -> sprint.getKeybind() == -1), "NONE unbinds module");

            context.runOnClient(minecraft -> {
                noFall.getMode().setValue(NoFallModule.Mode.AUTO); modules.setEnabled(noFall, true);
                minecraft.player.fallDistance = 6;
                client.getEventBus().post(new UpdateEvent(minecraft, 0));
                check(minecraft.player.fallDistance == 6, "NoFall never fakes local fall-distance success");
                noFall.getMode().setValue(NoFallModule.Mode.PACKET);
            });
            AtomicBoolean cancelledCallback = new AtomicBoolean();
            AtomicReference<Packet<?>> originalPacket = new AtomicReference<>();
            AtomicReference<Packet<?>> replacementPacket = new AtomicReference<>();
            AtomicBoolean resetAtSend = new AtomicBoolean();
            var replace = client.getEventBus().subscribe(PacketEvent.Send.class, -10, false, event -> {
                if (event.getPacket() == originalPacket.get()) {
                    resetAtSend.set(net.minecraft.client.Minecraft.getInstance().player.fallDistance > 0);
                    event.setPacket(replacementPacket.get()); event.cancel();
                }
            });
            try {
                context.runOnClient(minecraft -> {
                    minecraft.player.fallDistance = 7;
                    var packet = new ServerboundMovePlayerPacket.StatusOnly(false, false);
                    var replacement = new ServerboundMovePlayerPacket.StatusOnly(false, true);
                    originalPacket.set(packet); replacementPacket.set(replacement);
                    minecraft.getConnection().getConnection().send(packet, future -> cancelledCallback.set(!future.isSuccess()));
                    check(minecraft.player.fallDistance > 0 && !packet.isOnGround(), "original movement packet remains immutable");
                });
                context.waitFor(minecraft -> cancelledCallback.get());
                check(resetAtSend.get(), "NoFall observes real network send before cancellation");
            } finally { replace.close(); }
            context.runOnClient(minecraft -> {
                modules.setEnabled(noFall, false);
                minecraft.player.fallDistance = 4;
                client.getEventBus().post(new UpdateEvent(minecraft, 0));
                check(minecraft.player.fallDistance == 4, "disabled NoFall unsubscribes");
                minecraft.player.fallDistance = 0;
            });

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.send(new ClientboundSystemChatPacket(Component.literal("engine-receive-cancel"), false));
                player.connection.send(new ClientboundSystemChatPacket(Component.literal("engine-receive-before"), false));
            });
            context.waitFor(minecraft -> hasChat("engine-receive-after"));
            check(context.computeOnClient(minecraft -> !hasChat("engine-receive-cancel") && !hasChat("engine-receive-before")), "receive cancellation/replacement affect delivered chat");
            check(receivePackets.get() >= 2 && !"Render thread".equals(receiveThread.get()), "receive events run on network thread");
            check(leakedServerSends.get() == 0, "integrated server connection is excluded from client hooks");

            context.runOnClient(minecraft -> {
                double x = minecraft.player.getX(); double y = minecraft.player.getY(); double z = minecraft.player.getZ();
                actors[0] = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), "FriendNPC"));
                actors[1] = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), "EnemyNPC"));
                for (int i = 0; i < actors.length; i++) {
                    actors[i].setId(1_000_000 + i); actors[i].snapTo(x + 1 + i, y, z + 1);
                    actors[i].setOldPosAndRot(); minecraft.level.addEntity(actors[i]);
                }
                aura.getTargets().setValue(KillAuraModule.Targets.PLAYERS);
                aura.getRange().setValue(3.0);
            });
            command(context, ".friend add FriendNPC");
            check(context.computeOnClient(minecraft -> client.getFriendManager().isFriend("friendnpc")), "friend command updates whitelist");
            AtomicReference<UUID> selected = new AtomicReference<>();
            var auraObserver = client.getEventBus().subscribe(AttackEvent.class, 50, false, event -> {
                selected.set(event.getTarget().getUUID()); event.cancel();
            });
            try {
                command(context, ".t KillAura");
                context.waitFor(minecraft -> selected.get() != null);
                check(selected.get().equals(actors[1].getUUID()), "aura skips nearer friend and picks nearest valid player");
                check(attacks.get() == 0, "cancelled attack intent stops game-mode attack");
            } finally { auraObserver.close(); context.runOnClient(minecraft -> modules.setEnabled(aura, false)); }
            command(context, ".t Tracers");
            context.waitFor(minecraft -> client.getWorldRenderer().getLines().size() == 2);
            check(context.computeOnClient(minecraft -> client.getWorldRenderer().getLines().stream()
                    .anyMatch(line -> line.color() == tracers.getFriendColor().getValue())), "friends have a distinct tracer color");
            context.waitTicks(8);
            context.takeScreenshot("riserecode-tracers");

            // A real server-spawned mob verifies the ordinary attack packet and vanilla damage path.
            double[] position = context.computeOnClient(minecraft -> new double[] {
                    minecraft.player.getX() + 2, minecraft.player.getY(), minecraft.player.getZ()});
            world.getServer().runCommand(String.format(java.util.Locale.ROOT,
                    "summon minecraft:pig %.3f %.3f %.3f {Tags:[\"riserecode_engine\"],NoAI:1b}", position[0], position[1], position[2]));
            UUID pigUuid = world.getServer().computeOnServer(server -> java.util.stream.StreamSupport
                    .stream(server.overworld().getAllEntities().spliterator(), false)
                    .filter(entity -> entity.entityTags().contains("riserecode_engine")).map(Entity::getUUID).findFirst().orElseThrow());
            context.waitFor(minecraft -> minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class,
                    minecraft.player.getBoundingBox().inflate(4), entity -> entity.getUUID().equals(pigUuid)).size() == 1);
            context.runOnClient(minecraft -> { aura.getTargets().setValue(KillAuraModule.Targets.MOBS); modules.setEnabled(aura, true); });
            context.waitFor(minecraft -> attacks.get() > 0);
            world.getServer().waitFor(server -> java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                    .filter(entity -> entity.getUUID().equals(pigUuid))
                    .anyMatch(entity -> entity instanceof net.minecraft.world.entity.LivingEntity living && living.getHealth() < living.getMaxHealth()));
            context.runOnClient(minecraft -> modules.setEnabled(aura, false));

            context.runOnClient(minecraft -> {
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), true);
                modules.setKeybind(sprint, InputConstants.KEY_F7);
                client.getHudRenderer().setEnabled(true);
            });
            command(context, ".profile save Engine");
            check(Files.exists(client.getProfileManager().getFile("Engine")), "profile command writes a named snapshot");
            command(context, ".friend add AfterSave");
            context.runOnClient(minecraft -> {
                modules.setEnabled(tracers, false);
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), false);
                modules.setKeybind(sprint, -1); client.getHudRenderer().setEnabled(false);
                client.getCommandManager().setPrefix("-");
            });
            command(context, "-profile load Engine");
            check(context.computeOnClient(minecraft -> tracers.isEnabled() && modules.getByClass(FullBrightModule.class).orElseThrow().isEnabled()
                    && sprint.getKeybind() == InputConstants.KEY_F7 && !client.getHudRenderer().isEnabled()), "module profile restores modules and binds, preserving independent HUD preferences");
            check(context.computeOnClient(minecraft -> client.getFriendManager().isFriend("AfterSave") && client.getCommandManager().getPrefix().equals("-")), "profile preserves global friends and prefix");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            context.waitFor(minecraft -> client.getClickGui().isOpen(), 2000);
            context.runOnClient(minecraft -> check(client.getProfileManager().loadProfile("Engine"), "profile loads while GUI open"));
            check(context.computeOnClient(minecraft -> client.getClickGui().getPanels().stream()
                    .flatMap(panel -> panel.getModuleButtons().stream()).anyMatch(button -> button.getModule() == tracers && button.getModule().isEnabled())), "open GUI reflects profile state");
            context.takeScreenshot("riserecode-phase5-gui");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            command(context, "-friend remove FriendNPC");
            check(context.computeOnClient(minecraft -> !client.getFriendManager().isFriend("FriendNPC")), "friend removal command");
            check(chatPackets.get() == 0, "all commands remain local through chat mixin");
        } finally {
            sends.close(); receives.close();
            context.runOnClient(minecraft -> {
                modules.setEnabled(sprint, false); modules.setEnabled(aura, false); modules.setEnabled(tracers, false); modules.setEnabled(noFall, false);
                modules.setEnabled(modules.getByClass(FullBrightModule.class).orElseThrow(), false);
                modules.setKeybind(sprint, -1); client.getCommandManager().setPrefix(".");
                client.getFriendManager().remove("FriendNPC"); client.getFriendManager().remove("AfterSave");
                for (var actor : actors) { if (actor != null && minecraft.level != null) { minecraft.level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED); } }
                client.getConfigManager().save();
            });
        }
    }
    private static void command(ClientGameTestContext context, String message) {
        context.runOnClient(minecraft -> minecraft.getConnection().sendChat(message)); context.waitTick();
    }
    private static boolean hasChat(String text) {
        var state = net.minecraft.client.Minecraft.getInstance().gui.hud.getChat().storeState();
        return ((ChatStateAccessor) state).riserecode$getMessages().stream().anyMatch(message -> message.content().getString().contains(text));
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
