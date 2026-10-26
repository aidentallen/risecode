package dev.riserecode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.DoubleSetting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Engine contracts: input parsing, global identity, atomic profiles, cancellation, immutable geometry. */
public final class EngineChecks {
    private static int assertions;
    private EngineChecks() { }
    public static void main(String[] args) throws Exception {
        check(Arrays.equals(CommandManager.parseArguments("toggle \"Test Module\""), new String[] {"toggle", "Test Module"}), "quoted module names");
        check(Arrays.equals(CommandManager.parseArguments("a 'b c' d\\ e \"\""), new String[] {"a", "b c", "d e", ""}), "quotes, escapes, and empty token");
        expectIllegal(() -> CommandManager.parseArguments("a \"bad"), "unclosed quote rejected");
        expectIllegal(() -> CommandManager.parseArguments("a\\"), "dangling escape rejected");
        check(KeybindManager.parseKey("R") == 82, "letter bind");
        check(KeybindManager.parseKey("F6") == 295, "function bind");
        check(KeybindManager.parseKey("RSHIFT") == 344 && KeybindManager.parseKey("right_shift") == 344, "shift aliases");
        check(KeybindManager.parseKey("NONE") == Module.UNBOUND, "unbind");
        expectIllegal(() -> KeybindManager.parseKey("not-a-key"), "unknown key rejected");
        FriendManager friends = new FriendManager();
        AtomicInteger changes = new AtomicInteger(); friends.setChangeListener(changes::incrementAndGet);
        UUID identity = UUID.randomUUID();
        check(friends.add("Alice", identity) && friends.isFriend("aLiCe"), "case-insensitive friend identity");
        check(!friends.add("Alice", identity) && changes.get() == 1, "idempotent friend add");
        check(friends.isFriend(identity, "Renamed"), "UUID survives name changes");
        var oldFriends = friends.getFriends();
        friends.add("Renamed", identity);
        check(friends.getFriends().size() == 1 && friends.isFriend("Renamed"), "rename consolidates UUID");
        check(oldFriends.getFirst().name().equals("Alice"), "friend snapshot immutable across writes");
        expectIllegal(() -> friends.add("../invalid"), "invalid friend name rejected");
        check(friends.remove(identity) && !friends.remove(identity), "UUID removal");
        friends.add("Alice", identity);
        Path directory = Files.createTempDirectory("riserecode-engine-check-");
        var logger = LoggerFactory.getLogger("RiseRecodeEngineChecks");
        try {
            EventBus bus = new EventBus();
            ModuleManager modules = new ModuleManager(bus, logger);
            CheckModule module = modules.register(new CheckModule(bus));
            AtomicBoolean hud = new AtomicBoolean(true);
            ConfigManager config = new ConfigManager(directory, modules, friends, hud::get, hud::set, logger);
            config.load();
            config.setCommandPrefix("-"); config.save();
            FriendManager restored = new FriendManager();
            ConfigManager loaded = new ConfigManager(directory, modules, restored, hud::get, hud::set, logger);
            loaded.load();
            check(restored.isFriend(identity, "newName") && restored.isFriend("Alice"), "name/UUID config round trip");
            check(loaded.getCommandPrefix().equals("-"), "prefix round trip");
            expectIllegal(() -> config.setCommandPrefix("/"), "slash prefix rejected");
            expectIllegal(() -> config.setCommandPrefix("two words"), "space prefix rejected");
            AtomicInteger refreshes = new AtomicInteger();
            ProfileManager profiles = new ProfileManager(config, refreshes::incrementAndGet, logger);
            module.value.setValue(1.75); modules.setKeybind(module, 82); modules.setEnabled(module, true); hud.set(false);
            check(profiles.saveProfile("PvP"), "profile atomic save");
            check(profiles.getProfiles().equals(java.util.List.of("PvP")), "profile listing");
            friends.add("GlobalFriend");
            module.value.setValue(0.75); modules.setKeybind(module, -1); modules.setEnabled(module, false); hud.set(true);
            check(profiles.loadProfile("PvP"), "profile load");
            check(module.isEnabled() && module.value.getValue() == 1.75 && module.getKeybind() == 82, "profile values and states restore");
            check(module.atEnable == 1.75 && !hud.get(), "all settings restored before activation");
            check(friends.isFriend("GlobalFriend") && config.getCommandPrefix().equals("-"), "profiles preserve global state");
            check(refreshes.get() == 1 && bus.getListenerCount() == 1, "UI refresh and one module subscription");
            JsonObject bad = config.profileSnapshot();
            bad.getAsJsonObject("modules").getAsJsonObject(module.getName()).getAsJsonObject("settings").addProperty("value", 99);
            Files.writeString(profiles.getFile("Bad"), bad.toString());
            check(!profiles.loadProfile("Bad") && module.isEnabled() && module.value.getValue() == 1.75, "invalid profile makes no live changes");
            check(refreshes.get() == 1 && bus.getListenerCount() == 1, "invalid profile leaves UI and subscriptions intact");
            check(!profiles.loadProfile("Missing"), "missing profile does not alter state");
            expectIllegal(() -> profiles.getFile("../outside"), "profile traversal rejected");
            expectIllegal(() -> profiles.getFile("/absolute"), "absolute profile path rejected");
            module.failOnEnable = true;
            check(!profiles.loadProfile("PvP") && module.isEnabled() && module.value.getValue() == 1.75, "activation failure rolls back previous state");
            check(bus.getListenerCount() == 1, "failed activation does not leak listeners");
            ArrayList<String> feedback = new ArrayList<>();
            CommandManager commands = new CommandManager(config::getCommandPrefix, config::setCommandPrefix, feedback::add, logger);
            commands.register(new ToggleCommand(modules, commands));
            commands.register(new FriendCommand(friends, commands, name -> name.equals("Bob") ? identity : null));
            commands.register(new ProfileCommand(profiles, commands));
            commands.register(new HelpCommand(commands));
            check(!commands.handleChat("ordinary chat"), "normal chat not intercepted");
            check(commands.handleChat("-t \"Test Module\"") && !module.isEnabled(), "toggle alias and quoted module");
            check(commands.handleChat("-toggle Test Module") && module.isEnabled(), "case-insensitive module registry command");
            check(commands.handleChat("-nonsense") && feedback.getLast().contains("Unknown command"), "unknown command consumed locally");
            check(commands.handleChat("-toggle \"bad") && feedback.getLast().contains("Unclosed"), "parser error consumed locally");
            check(commands.handleChat("-"), "bare prefix consumed locally");
            commands.handleChat("-friend add Bob"); check(friends.isFriend("Bob"), "friend add command");
            commands.handleChat("-friend remove Bob"); check(!friends.isFriend("Bob"), "friend remove command");
            commands.handleChat("-profile save Ghost"); check(Files.exists(profiles.getFile("Ghost")), "profile save command");
            commands.handleChat("-help"); check(feedback.getLast().contains("-help"), "help prints commands");
            expectIllegal(() -> commands.register(new Command("duplicate", "", "", "t") {
                @Override public void execute(String[] values) { }
            }), "command aliases cannot collide");
            check(commands.getCommands().size() == 4, "failed registration is atomic");
            commands.setPrefix("."); check(commands.handleChat(".t Test Module") && !module.isEnabled(), "live prefix change");
            modules.disableAll();
            check(bus.getListenerCount() == 0, "engine cleanup owns subscriptions");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
        EventBus packets = new EventBus();
        var before = new ServerboundMovePlayerPacket.StatusOnly(false, false);
        var after = new ServerboundMovePlayerPacket.StatusOnly(true, false);
        AtomicInteger delivered = new AtomicInteger();
        try (var replace = packets.subscribe(PacketEvent.Send.class, 10, false, event -> event.setPacket(after));
                var cancel = packets.subscribe(PacketEvent.class, 0, false, Event::cancel);
                var audit = packets.subscribe(PacketEvent.Send.class, -10, true, event -> delivered.incrementAndGet())) {
            PacketEvent.Send event = packets.post(new PacketEvent.Send(before));
            check(event.isCancelled() && event.getPacket() == after && event.getOriginalPacket() == before, "packet replacement/cancellation preserve original");
            check(delivered.get() == 1 && replace.isActive() && cancel.isActive() && audit.isActive(), "polymorphic cancelled packet audit");
            expectIllegal(() -> event.setPacket(new ClientboundSystemChatPacket(Component.literal("wrong direction"), false)), "packet direction replacement rejected");
        }
        check(packets.getListenerCount() == 0, "packet subscriptions released");
        var line = new Render3DEvent.Line(Vec3.ZERO, new Vec3(1, 2, 3), 0x80FFFFFF, 2);
        check(line.width() == 2 && line.to().z == 3, "immutable world line record");
        expectIllegal(() -> new Render3DEvent.Line(Vec3.ZERO, new Vec3(Double.NaN, 0, 0), 0, 1), "non-finite geometry rejected");
        expectIllegal(() -> new Render3DEvent.Line(Vec3.ZERO, Vec3.ZERO, 0, 0), "invalid line width rejected");
        System.out.println("Engine checks passed (" + assertions + " assertions).");
    }
    private static final class CheckEvent extends Event { }
    private static final class CheckModule extends Module {
        private final DoubleSetting value = register(new DoubleSetting("value", "", 1, 0.5, 2));
        private double atEnable;
        private boolean failOnEnable;
        private CheckModule(EventBus bus) { super("Test Module", "", Category.UTILITY, bus); }
        @Override protected void onEnable() {
            if (failOnEnable) { failOnEnable = false; throw new IllegalStateException("intentional activation failure"); }
            atEnable = value.getValue(); listen(CheckEvent.class, event -> { });
        }
    }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) { throw new AssertionError(message); }
    }
    private static void expectIllegal(Runnable action, String message) {
        boolean thrown = false;
        try { action.run(); } catch (IllegalArgumentException expected) { thrown = true; }
        check(thrown, message);
    }
}
