# Phase 5 — Core engine systems and foundational modules

Implemented in `/home/toast/Work/riserecode` for Minecraft 26.2, Fabric API
0.161.0+26.2, Fabric Loader 0.19.5, and Java 25. Minecraft 26.2 is unobfuscated;
this build uses its published names and `net.fabricmc.fabric-loom` 1.17.21 rather
than Yarn. No preview language features are required.

This delivery contains the full current project tree, complete new and updated
code files, resource/build changes, and integration notes in the requested order.
Existing foundation, setting, animation, and HUD classes were preserved. The two
UI files below add only profile-refresh hooks; their full current contents are
included for review.

**Protocol adaptation:** `ServerboundMovePlayerPacket` has no fall-distance field
in 26.2. NoFall's Packet mode therefore resets local fall distance at the movement
send hook and leaves every outgoing packet field unchanged. Vanilla mode resets
it on client ticks. Neither mode prevents server-side fall damage. There is no
on-ground spoofing or anticheat bypass.

## 1. Updated project file tree

Build output, dependency caches, and runtime directories are excluded. Runtime
configuration is created in `config/riserecode/config.json`; named snapshots are
created in `config/riserecode/profiles/<name>.json`.

```text
riserecode/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/
│   └── verify.sh
├── src/
│   ├── gametest/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── testmixin/
│   │   │           │   └── ChatStateAccessor.java
│   │   │           ├── ClientIntegrationTest.java
│   │   │           └── EngineIntegrationTest.java
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       └── riserecode-test.mixins.json
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── command/
│   │   │           │   ├── commands/
│   │   │           │   │   ├── BindCommand.java
│   │   │           │   │   ├── FriendCommand.java
│   │   │           │   │   ├── HelpCommand.java
│   │   │           │   │   ├── ProfileCommand.java
│   │   │           │   │   └── ToggleCommand.java
│   │   │           │   ├── Command.java
│   │   │           │   └── CommandManager.java
│   │   │           ├── config/
│   │   │           │   ├── ConfigManager.java
│   │   │           │   ├── JsonStorage.java
│   │   │           │   └── ProfileManager.java
│   │   │           ├── event/
│   │   │           │   ├── AttackEvent.java
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── PacketEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── friend/
│   │   │           │   └── FriendManager.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   ├── ChatInputMixin.java
│   │   │           │   ├── ConnectionMixin.java
│   │   │           │   └── KeyboardHandlerMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   ├── FullBrightModule.java
│   │   │           │   │   ├── KillAuraModule.java
│   │   │           │   │   ├── NoFallModule.java
│   │   │           │   │   ├── SprintModule.java
│   │   │           │   │   └── TracersModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
│   │   │           ├── render/
│   │   │           │   └── WorldRenderer.java
│   │   │           ├── setting/
│   │   │           │   ├── BooleanSetting.java
│   │   │           │   ├── ColorSetting.java
│   │   │           │   ├── DoubleSetting.java
│   │   │           │   ├── EnumSetting.java
│   │   │           │   └── Setting.java
│   │   │           ├── ui/
│   │   │           │   ├── animation/
│   │   │           │   │   └── Animation.java
│   │   │           │   ├── clickgui/
│   │   │           │   │   └── ClickGui.java
│   │   │           │   ├── component/
│   │   │           │   │   ├── setting/
│   │   │           │   │   │   ├── ColorSwatch.java
│   │   │           │   │   │   ├── EnumSelector.java
│   │   │           │   │   │   ├── Slider.java
│   │   │           │   │   │   └── ToggleSwitch.java
│   │   │           │   │   ├── ActionButton.java
│   │   │           │   │   ├── ModuleButton.java
│   │   │           │   │   ├── Panel.java
│   │   │           │   │   └── UIComponent.java
│   │   │           │   ├── hud/
│   │   │           │   │   └── HudRenderer.java
│   │   │           │   └── theme/
│   │   │           │       └── Theme.java
│   │   │           ├── util/
│   │   │           │   ├── MathUtil.java
│   │   │           │   └── RenderUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       ├── font/
│   │       │       │   ├── inter_medium.json
│   │       │       │   ├── inter_medium.ttf
│   │       │       │   ├── inter_regular.json
│   │       │       │   └── inter_regular.ttf
│   │       │       └── lang/
│   │       │           └── en_us.json
│   │       ├── licenses/
│   │       │   └── inter-OFL.txt
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── EngineChecks.java
│                   ├── FoundationChecks.java
│                   ├── RuntimeChecks.java
│                   └── UiChecks.java
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── PHASE3.md
├── PHASE4.md
├── PHASE5.md
├── README.md
└── settings.gradle
```

## 2. Code files

Each listing is the complete file currently on disk. Modules continue to own their
custom EventBus subscriptions through `Module.listen(...)`.

### src/main/java/dev/riserecode/Client.java

```java
package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.BindCommand;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.render.WorldRenderer;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "RiseRecode";
    private final Logger logger = LoggerFactory.getLogger(NAME);
    private final EventBus eventBus = new EventBus((event, failure) ->
            logger.error("Listener failed for {}", event.getClass().getSimpleName(), failure));
    private String version = "development";
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private ProfileManager profileManager;
    private WorldRenderer worldRenderer;
    private HudRenderer hudRenderer;
    private ClickGui clickGui;
    private volatile boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        friendManager = new FriendManager();
        worldRenderer = new WorldRenderer(eventBus);
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft, friendManager);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME + " " + version);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, friendManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.load();
        moduleManager.setChangeListener(configManager::requestSave);
        friendManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        profileManager = new ProfileManager(configManager, () -> {
            if (clickGui != null && minecraft.gui.screen() == clickGui) { clickGui.refresh(); }
        }, logger);
        commandManager = new CommandManager(configManager::getCommandPrefix, configManager::setCommandPrefix, message -> {
            if (minecraft.player != null) {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(NAME + " » ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
            } else { logger.info("[Command] {}", message); }
        }, logger);
        commandManager.register(new ToggleCommand(moduleManager, commandManager));
        commandManager.register(new BindCommand(moduleManager, keybindManager, commandManager));
        commandManager.register(new FriendCommand(friendManager, commandManager, name -> {
            var connection = minecraft.getConnection();
            var info = connection == null ? null : connection.getPlayerInfoIgnoreCase(name);
            return info == null ? null : info.getProfile().id();
        }));
        commandManager.register(new ProfileCommand(profileManager, commandManager));
        commandManager.register(new HelpCommand(commandManager));
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        worldRenderer.close();
        configManager.close();
        moduleManager.disableAll();
        Minecraft.getInstance().options.save();
        initialized = false;
    }

    public boolean isInitialized() { return initialized; }
    public String getName() { return NAME; }
    public String getVersion() { return version; }
    public Logger getLogger() { return logger; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    /** Screens capture the font, so create this only after Minecraft initializes it. */
    public ClickGui getClickGui() {
        if (clickGui == null) {
            if (!initialized || Minecraft.getInstance().font == null) {
                throw new IllegalStateException("The Minecraft GUI is not ready");
            }
            clickGui = new ClickGui(moduleManager, hudRenderer, configManager::requestSave);
        }
        return clickGui;
    }
}
```

### src/main/java/dev/riserecode/RiseRecodeClient.java

```java
package dev.riserecode;

import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.event.UpdateEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;

/** Fabric feeds the custom bus; feature modules do not register Fabric hooks. */
public final class RiseRecodeClient implements ClientModInitializer {
    private long tick;
    private long previousHudFrame;

    @Override
    public void onInitializeClient() {
        Client client = Client.INSTANCE;
        client.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> client.getClickGui());
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (client.isInitialized()) { client.getEventBus().post(new UpdateEvent(minecraft, ++tick)); }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Client.MOD_ID, "hud"), (graphics, deltaTracker) -> {
                    long now = System.nanoTime();
                    double delta = previousHudFrame == 0 ? 0.0 : Math.max(0.0, (now - previousHudFrame) * 1.0e-9);
                    previousHudFrame = now;
                    if (client.isInitialized()) {
                        client.getEventBus().post(new Render2DEvent(graphics, deltaTracker, delta));
                    }
                });
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            if (client.isInitialized()) { client.getEventBus().post(new Render3DEvent(context)); }
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (client.isInitialized()) { client.getWorldRenderer().submit(context); }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> client.shutdown());
    }
}
```

### src/main/java/dev/riserecode/command/Command.java

```java
package dev.riserecode.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected Command(String name, String description, String usage, String... aliases) {
        this.name = normalize(name); this.description = Objects.requireNonNull(description);
        this.usage = Objects.requireNonNull(usage);
        this.aliases = Arrays.stream(aliases).map(Command::normalize).distinct().toList();
    }
    public final String getName() { return name; }
    public final List<String> getAliases() { return aliases; }
    public final String getUsage() { return usage; }
    public final String getDescription() { return description; }
    public abstract void execute(String[] args);

    private static String normalize(String name) {
        String normalized = Objects.requireNonNull(name).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9_-]*")) { throw new IllegalArgumentException("Invalid command name " + name); }
        return normalized;
    }
}
```

### src/main/java/dev/riserecode/command/CommandManager.java

```java
package dev.riserecode.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;

/** Prefix messages are consumed locally, including malformed or unknown commands. */
public final class CommandManager {
    private final Map<String, Command> lookup = new LinkedHashMap<>();
    private final List<Command> commands = new ArrayList<>();
    private final Supplier<String> prefix;
    private final Consumer<String> setPrefix;
    private final Consumer<String> feedback;
    private final Logger logger;

    public CommandManager(Supplier<String> prefix, Consumer<String> setPrefix, Consumer<String> feedback, Logger logger) {
        this.prefix = Objects.requireNonNull(prefix); this.setPrefix = Objects.requireNonNull(setPrefix);
        this.feedback = Objects.requireNonNull(feedback); this.logger = Objects.requireNonNull(logger);
    }
    public String getPrefix() { return prefix.get(); }
    public void setPrefix(String prefix) { setPrefix.accept(prefix); }
    public List<Command> getCommands() { return List.copyOf(commands); }
    public void reply(String message) { feedback.accept(message); }

    public void register(Command command) {
        Objects.requireNonNull(command);
        List<String> names = new ArrayList<>(command.getAliases()); names.add(command.getName());
        for (String name : names) {
            if (lookup.containsKey(name)) { throw new IllegalArgumentException("Duplicate command or alias: " + name); }
        }
        for (String name : names) { lookup.put(name, command); }
        commands.add(command);
    }

    public boolean handleChat(String message) {
        String currentPrefix = getPrefix();
        if (!message.startsWith(currentPrefix)) { return false; }
        try {
            String[] parts = parseArguments(message.substring(currentPrefix.length()));
            if (parts.length == 0) { reply("Use " + currentPrefix + "help for commands."); return true; }
            Command command = lookup.get(parts[0].toLowerCase(Locale.ROOT));
            if (command == null) { reply("Unknown command: " + parts[0] + ". Use " + currentPrefix + "help."); return true; }
            command.execute(Arrays.copyOfRange(parts, 1, parts.length));
        } catch (IllegalArgumentException failure) {
            reply(failure.getMessage());
        } catch (RuntimeException failure) {
            logger.error("Client command failed", failure); reply("Command failed. Check the client log.");
        }
        return true;
    }

    /** Supports whitespace, quoted arguments, empty quotes, and backslash escapes. */
    public static String[] parseArguments(String input) {
        List<String> arguments = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean started = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (escaped) { token.append(character); escaped = false; started = true; }
            else if (character == '\\') { escaped = true; started = true; }
            else if (quote != 0) {
                if (character == quote) { quote = 0; } else { token.append(character); }
            } else if (character == '"' || character == '\'') { quote = character; started = true; }
            else if (Character.isWhitespace(character)) {
                if (started) { arguments.add(token.toString()); token.setLength(0); started = false; }
            } else { token.append(character); started = true; }
        }
        if (quote != 0) { throw new IllegalArgumentException("Unclosed quote in command"); }
        if (escaped) { throw new IllegalArgumentException("Command ends with an incomplete escape"); }
        if (started) { arguments.add(token.toString()); }
        return arguments.toArray(String[]::new);
    }
}
```

### src/main/java/dev/riserecode/command/commands/ToggleCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.module.ModuleManager;

public final class ToggleCommand extends Command {
    private final ModuleManager modules;
    private final CommandManager commands;
    public ToggleCommand(ModuleManager modules, CommandManager commands) {
        super("toggle", "Toggle a module", "toggle <module>", "t"); this.modules = modules; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 0) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        var module = modules.getByName(String.join(" ", args))
                .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + String.join(" ", args)));
        if (!modules.toggle(module)) { throw new IllegalStateException("Could not toggle " + module.getName()); }
        commands.reply(module.getName() + (module.isEnabled() ? " enabled" : " disabled"));
    }
}
```

### src/main/java/dev/riserecode/command/commands/BindCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import java.util.Arrays;

public final class BindCommand extends Command {
    private final ModuleManager modules;
    private final KeybindManager keys;
    private final CommandManager commands;
    public BindCommand(ModuleManager modules, KeybindManager keys, CommandManager commands) {
        super("bind", "Assign a key; NONE clears it", "bind <module> <key|NONE>");
        this.modules = modules; this.keys = keys; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        String name = String.join(" ", Arrays.copyOf(args, args.length - 1));
        var module = modules.getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown module: " + name));
        keys.bind(module, args[args.length - 1]);
        commands.reply(module.getName() + " bound to " + KeybindManager.keyName(module.getKeybind()));
    }
}
```

### src/main/java/dev/riserecode/command/commands/FriendCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.friend.FriendManager;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public final class FriendCommand extends Command {
    private final FriendManager friends;
    private final CommandManager commands;
    private final Function<String, UUID> resolveUuid;
    public FriendCommand(FriendManager friends, CommandManager commands, Function<String, UUID> resolveUuid) {
        super("friend", "Manage the global friend list", "friend <add|remove> <player> | friend list", "friends");
        this.friends = friends; this.commands = commands; this.resolveUuid = resolveUuid;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            commands.reply(friends.getFriends().isEmpty() ? "No friends added" : "Friends: "
                    + String.join(", ", friends.getFriends().stream().map(FriendManager.Friend::name).toList()));
            return;
        }
        if (args.length != 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        String name = args[1];
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> commands.reply(friends.add(name, resolveUuid.apply(name)) ? "Added friend " + name : name + " is already a friend");
            case "remove", "delete", "del" -> {
                boolean removed;
                try { removed = friends.remove(UUID.fromString(name)); }
                catch (IllegalArgumentException ignored) { removed = friends.remove(name); }
                commands.reply(removed ? "Removed friend " + name : "Friend not found: " + name);
            }
            default -> throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage());
        }
    }
}
```

### src/main/java/dev/riserecode/command/commands/ProfileCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.config.ProfileManager;
import java.util.Locale;

public final class ProfileCommand extends Command {
    private final ProfileManager profiles;
    private final CommandManager commands;
    public ProfileCommand(ProfileManager profiles, CommandManager commands) {
        super("profile", "Save or load module/HUD configurations", "profile <save|load> <name> | profile list");
        this.profiles = profiles; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var names = profiles.getProfiles(); commands.reply(names.isEmpty() ? "No profiles saved" : "Profiles: " + String.join(", ", names));
            return;
        }
        if (args.length != 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> commands.reply(profiles.saveProfile(args[1]) ? "Saved profile " + args[1] : "Could not save profile " + args[1]);
            case "load" -> commands.reply(profiles.loadProfile(args[1]) ? "Loaded profile " + args[1] : "Profile missing or invalid: " + args[1]);
            default -> throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage());
        }
    }
}
```

### src/main/java/dev/riserecode/command/commands/HelpCommand.java

```java
package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;

public final class HelpCommand extends Command {
    private final CommandManager commands;
    public HelpCommand(CommandManager commands) { super("help", "List local commands", "help", "h"); this.commands = commands; }
    @Override
    public void execute(String[] args) {
        if (args.length != 0) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        for (Command command : commands.getCommands()) {
            commands.reply(commands.getPrefix() + command.getUsage() + " — " + command.getDescription());
        }
    }
}
```

### src/main/java/dev/riserecode/friend/FriendManager.java

```java
package dev.riserecode.friend;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Mutations belong to the client thread; immutable snapshots are safe for readers. */
public final class FriendManager {
    public record Friend(String name, UUID uuid) {
        public Friend { validateName(name); }
    }

    private volatile Map<String, Friend> friends = Map.of();
    private Runnable changeListener = () -> { };

    public List<Friend> getFriends() { return List.copyOf(friends.values()); }
    public void setChangeListener(Runnable listener) { changeListener = Objects.requireNonNull(listener); }
    public boolean add(String name) { return add(name, null); }

    public boolean add(String name, UUID uuid) {
        validateName(name);
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        String key = normalize(name);
        Friend previous = next.get(key);
        if (uuid == null && previous != null) { uuid = previous.uuid(); }
        UUID identity = uuid;
        if (identity != null) { next.values().removeIf(friend -> identity.equals(friend.uuid()) && !normalize(friend.name()).equals(key)); }
        Friend updated = new Friend(name, uuid);
        next.put(key, updated);
        if (next.equals(friends)) { return false; }
        friends = Collections.unmodifiableMap(next);
        changeListener.run();
        return true;
    }

    public boolean remove(String name) {
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        if (next.remove(normalize(name)) == null) { return false; }
        friends = Collections.unmodifiableMap(next); changeListener.run(); return true;
    }

    public boolean remove(UUID uuid) {
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        if (!next.values().removeIf(friend -> uuid.equals(friend.uuid()))) { return false; }
        friends = Collections.unmodifiableMap(next); changeListener.run(); return true;
    }

    public boolean isFriend(String name) { return name != null && friends.containsKey(normalize(name)); }
    public boolean isFriend(UUID uuid, String name) {
        Map<String, Friend> snapshot = friends;
        return (name != null && snapshot.containsKey(normalize(name)))
                || (uuid != null && snapshot.values().stream().anyMatch(friend -> uuid.equals(friend.uuid())));
    }

    public void replace(Collection<Friend> entries) {
        Map<String, Friend> next = new LinkedHashMap<>();
        for (Friend friend : entries) {
            Objects.requireNonNull(friend);
            if (friend.uuid() != null) { next.values().removeIf(existing -> friend.uuid().equals(existing.uuid())); }
            next.put(normalize(friend.name()), friend);
        }
        friends = Collections.unmodifiableMap(next); changeListener.run();
    }

    private static void validateName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Player names must contain 1–16 letters, digits, or underscores");
        }
    }
    private static String normalize(String name) { return Objects.requireNonNull(name).strip().toLowerCase(Locale.ROOT); }
}
```

### src/main/java/dev/riserecode/config/JsonStorage.java

```java
package dev.riserecode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Shared JSON I/O; a failed write never truncates the previous destination. */
final class JsonStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private JsonStorage() { }

    static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) { throw new IllegalArgumentException("JSON root must be an object"); }
            return root.getAsJsonObject();
        }
    }

    static void write(Path file, JsonObject document) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "riserecode-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(document, writer); }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
```

### src/main/java/dev/riserecode/config/ConfigManager.java

```java
package dev.riserecode.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.slf4j.Logger;

/** Global preferences plus reusable, validated module snapshots for profiles. */
public final class ConfigManager implements AutoCloseable {
    private record ModuleState(Module module, boolean enabled, int keybind, Map<Setting<?>, JsonElement> settings) { }
    private record ProfileState(boolean hud, List<ModuleState> modules) { }
    private final Gson gson = new Gson();
    private final Path directory;
    private final Path file;
    private final ModuleManager modules;
    private final FriendManager friends;
    private final BooleanSupplier hudEnabled;
    private final Consumer<Boolean> setHudEnabled;
    private final Logger logger;
    private JsonObject document = new JsonObject();
    private String commandPrefix = ".";
    private EventBus.Subscription subscription;
    private boolean dirty;
    private long saveAfterNanos;

    public ConfigManager(Path directory, ModuleManager modules, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this(directory, modules, new FriendManager(), hudEnabled, setHudEnabled, logger);
    }

    public ConfigManager(Path directory, ModuleManager modules, FriendManager friends, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this.directory = directory; file = directory.resolve("config.json"); this.modules = modules;
        this.friends = friends; this.hudEnabled = hudEnabled; this.setHudEnabled = setHudEnabled; this.logger = logger;
    }

    public Path getDirectory() { return directory; }
    public Path getFile() { return file; }
    public String getCommandPrefix() { return commandPrefix; }
    public void setCommandPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.length() > 8 || prefix.startsWith("/")
                || prefix.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Prefix must have 1–8 non-space characters and cannot start with /");
        }
        commandPrefix = prefix; requestSave();
    }

    public void init(EventBus eventBus) {
        if (subscription != null) { return; }
        subscription = eventBus.subscribe(UpdateEvent.class, -100, false, event -> {
            if (dirty && System.nanoTime() - saveAfterNanos >= 0 && !save()) {
                saveAfterNanos = System.nanoTime() + 5_000_000_000L;
            }
        });
    }
    public void requestSave() { dirty = true; saveAfterNanos = System.nanoTime() + 500_000_000L; }

    public void load() {
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) { save(); return; }
            document = JsonStorage.read(file);
        } catch (IOException | RuntimeException failure) {
            logger.warn("Could not read {}; preserving the bad file and using defaults", file, failure);
            try {
                if (Files.exists(file)) { Files.move(file, directory.resolve("config.invalid-" + System.nanoTime() + ".json")); }
                document = new JsonObject(); save();
            } catch (IOException backupFailure) { logger.error("Could not preserve invalid config", backupFailure); }
            return;
        }
        if (document.has("commandPrefix")) { apply("commandPrefix", () -> setCommandPrefix(stringValue(document.get("commandPrefix")))); }
        loadFriends(document.get("friends"));
        if (document.has("hudEnabled")) { apply("hudEnabled", () -> setHudEnabled.accept(booleanValue(document.get("hudEnabled")))); }
        JsonObject entries = objectOrEmpty(document.get("modules"));
        // Disable before replacing values so lifecycle resources are restored first.
        for (Module module : modules.getModules()) {
            if (entries.has(module.getName())) { modules.setEnabled(module, false); }
        }
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            JsonObject values = objectOrEmpty(entry.get("settings"));
            for (Setting<?> setting : module.getSettings()) {
                if (values.has(setting.getName())) {
                    apply(module.getName() + "." + setting.getName(), () -> loadSetting(setting, values.get(setting.getName())));
                }
            }
            if (entry.has("keybind")) {
                apply(module.getName() + ".keybind", () -> modules.setKeybind(module, keyValue(entry.get("keybind"))));
            }
        }
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            if (entry.has("enabled")) {
                apply(module.getName() + ".enabled", () -> modules.setEnabled(module, booleanValue(entry.get("enabled"))));
            }
        }
    }

    private void loadFriends(JsonElement value) {
        if (value == null) { return; }
        if (!value.isJsonArray()) { logger.warn("Ignoring invalid friends list"); return; }
        List<FriendManager.Friend> next = new ArrayList<>();
        for (JsonElement entry : value.getAsJsonArray()) {
            apply("friend", () -> {
                JsonObject object = requiredObject(entry);
                String name = stringValue(object.get("name"));
                UUID uuid = object.has("uuid") ? UUID.fromString(stringValue(object.get("uuid"))) : null;
                next.add(new FriendManager.Friend(name, uuid));
            });
        }
        friends.replace(next);
    }

    public JsonObject profileSnapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", 2); result.addProperty("hudEnabled", hudEnabled.getAsBoolean());
        JsonObject entries = new JsonObject(); result.add("modules", entries);
        for (Module module : modules.getModules()) { entries.add(module.getName(), moduleSnapshot(module, new JsonObject())); }
        return result;
    }

    private JsonObject moduleSnapshot(Module module, JsonObject entry) {
        entry.addProperty("enabled", module.isEnabled()); entry.addProperty("keybind", module.getKeybind());
        JsonObject values = objectOrEmpty(entry.get("settings")); entry.add("settings", values);
        for (Setting<?> setting : module.getSettings()) { values.add(setting.getName(), encodeSetting(setting, setting.getValue())); }
        return entry;
    }

    private JsonElement encodeSetting(Setting<?> setting, Object value) {
        if (setting instanceof ColorSetting) { return new JsonPrimitive(String.format(java.util.Locale.ROOT, "%08X", (Integer) value)); }
        if (value instanceof Enum<?> option) { return new JsonPrimitive(option.name()); }
        return gson.toJsonTree(value);
    }

    public boolean save() {
        try {
            JsonObject next = document.deepCopy();
            next.addProperty("schemaVersion", 2); next.addProperty("hudEnabled", hudEnabled.getAsBoolean());
            next.addProperty("commandPrefix", commandPrefix);
            JsonArray friendList = new JsonArray(); next.add("friends", friendList);
            for (FriendManager.Friend friend : friends.getFriends()) {
                JsonObject value = new JsonObject(); value.addProperty("name", friend.name());
                if (friend.uuid() != null) { value.addProperty("uuid", friend.uuid().toString()); }
                friendList.add(value);
            }
            JsonObject entries = objectOrEmpty(next.get("modules")); next.add("modules", entries);
            for (Module module : modules.getModules()) {
                entries.add(module.getName(), moduleSnapshot(module, objectOrEmpty(entries.get(module.getName()))));
            }
            JsonStorage.write(file, next); document = next; dirty = false; return true;
        } catch (IOException | RuntimeException failure) {
            logger.error("Could not save {}", file, failure); dirty = true; return false;
        }
    }

    /** Validate first, then restart lifecycles; rollback is attempted on activation failure. */
    public void applyProfile(JsonObject data) {
        ProfileState next = parseProfile(data);
        ProfileState previous = parseProfile(profileSnapshot());
        try { commit(next); }
        catch (RuntimeException failure) {
            try { commit(previous); }
            catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            requestSave(); throw failure;
        }
        requestSave();
    }

    private ProfileState parseProfile(JsonObject data) {
        if (data.has("schemaVersion")) {
            int version = integerValue(data.get("schemaVersion"));
            if (version < 1 || version > 2) { throw new IllegalArgumentException("Unsupported profile schema " + version); }
        }
        JsonObject entries = requiredObject(data.get("modules"));
        boolean hud = data.has("hudEnabled") ? booleanValue(data.get("hudEnabled")) : true;
        List<ModuleState> states = new ArrayList<>();
        for (Module module : modules.getModules()) {
            JsonObject entry = entries.has(module.getName()) ? requiredObject(entries.get(module.getName())) : new JsonObject();
            boolean enabled = entry.has("enabled") && booleanValue(entry.get("enabled"));
            int key = entry.has("keybind") ? keyValue(entry.get("keybind")) : Module.UNBOUND;
            JsonObject values = entry.has("settings") ? requiredObject(entry.get("settings")) : new JsonObject();
            Map<Setting<?>, JsonElement> settings = new LinkedHashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                JsonElement value = values.has(setting.getName()) ? values.get(setting.getName()).deepCopy()
                        : encodeSetting(setting, setting.getDefaultValue());
                validateSetting(setting, value); settings.put(setting, value);
            }
            states.add(new ModuleState(module, enabled, key, settings));
        }
        return new ProfileState(hud, states);
    }

    private void commit(ProfileState state) {
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), false)) { throw new IllegalStateException("Could not disable " + entry.module().getName()); }
        }
        for (ModuleState entry : state.modules()) {
            entry.settings().forEach(ConfigManager::loadSetting); modules.setKeybind(entry.module(), entry.keybind());
        }
        setHudEnabled.accept(state.hud());
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), entry.enabled())) { throw new IllegalStateException("Could not enable " + entry.module().getName()); }
        }
    }

    private static void validateSetting(Setting<?> setting, JsonElement value) {
        if (setting instanceof BooleanSetting) { booleanValue(value); }
        else if (setting instanceof DoubleSetting number) {
            double decoded = numberValue(value).getAsDouble();
            if (!Double.isFinite(decoded) || decoded < number.getMin() || decoded > number.getMax()) {
                throw new IllegalArgumentException("Out-of-range value for " + setting.getName());
            }
        } else if (setting instanceof ColorSetting) { colorValue(value); }
        else if (setting instanceof EnumSetting<?> choice) { enumValue(choice, value); }
        else { throw new IllegalArgumentException("Unsupported setting type " + setting.getClass().getName()); }
    }
    private static void loadSetting(Setting<?> setting, JsonElement value) {
        validateSetting(setting, value);
        if (setting instanceof BooleanSetting flag) { flag.setValue(booleanValue(value)); }
        else if (setting instanceof DoubleSetting number) { number.setValue(numberValue(value).getAsDouble()); }
        else if (setting instanceof ColorSetting color) { color.setValue(colorValue(value)); }
        else if (setting instanceof EnumSetting<?> choice) { loadEnum(choice, value); }
    }
    private static <E extends Enum<E>> E enumValue(EnumSetting<E> setting, JsonElement value) {
        String name = stringValue(value);
        return setting.getOptions().stream().filter(option -> option.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown option " + name));
    }
    private static <E extends Enum<E>> void loadEnum(EnumSetting<E> setting, JsonElement value) { setting.setValue(enumValue(setting, value)); }
    private static int colorValue(JsonElement value) {
        String hex = stringValue(value);
        if (!hex.matches("[0-9a-fA-F]{8}")) { throw new IllegalArgumentException("Color must be eight ARGB hexadecimal digits"); }
        return (int) Long.parseLong(hex, 16);
    }
    private void apply(String label, Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException failure) { logger.warn("Ignoring invalid config value {}: {}", label, failure.getMessage()); }
    }
    private static String stringValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) { throw new IllegalArgumentException("Expected a string"); }
        return value.getAsString();
    }
    private static boolean booleanValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) { throw new IllegalArgumentException("Expected a boolean"); }
        return value.getAsBoolean();
    }
    private static JsonPrimitive numberValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Expected a number"); }
        return value.getAsJsonPrimitive();
    }
    private static int integerValue(JsonElement value) { return numberValue(value).getAsBigDecimal().intValueExact(); }
    private static int keyValue(JsonElement value) {
        int key = integerValue(value);
        if (key < Module.UNBOUND) { throw new IllegalArgumentException("Key must be unbound (-1) or non-negative"); }
        return key;
    }
    private static JsonObject requiredObject(JsonElement value) {
        if (value == null || !value.isJsonObject()) { throw new IllegalArgumentException("Expected an object"); }
        return value.getAsJsonObject();
    }
    private static JsonObject objectOrEmpty(JsonElement value) { return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject(); }
    @Override
    public void close() { if (subscription != null) { subscription.close(); } }
}
```

### src/main/java/dev/riserecode/config/ProfileManager.java

```java
package dev.riserecode.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/** Profiles contain module/HUD choices; friends and command prefix remain global. */
public final class ProfileManager {
    private final Path directory;
    private final ConfigManager config;
    private final Runnable refreshUi;
    private final Logger logger;

    public ProfileManager(ConfigManager config, Runnable refreshUi, Logger logger) {
        this.config = Objects.requireNonNull(config); this.refreshUi = Objects.requireNonNull(refreshUi); this.logger = logger;
        directory = config.getDirectory().resolve("profiles");
        try { Files.createDirectories(directory); }
        catch (IOException failure) { logger.error("Could not create profile directory {}", directory, failure); }
    }
    public Path getDirectory() { return directory; }
    public Path getFile(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}")) {
            throw new IllegalArgumentException("Profile names must contain 1–32 letters, digits, hyphens, or underscores");
        }
        return directory.resolve(name + ".json");
    }
    public boolean saveProfile(String name) {
        Path file = getFile(name);
        try { JsonStorage.write(file, config.profileSnapshot()); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save profile {}", name, failure); return false; }
    }
    public boolean loadProfile(String name) {
        Path file = getFile(name);
        if (!Files.isRegularFile(file)) { return false; }
        try { config.applyProfile(JsonStorage.read(file)); }
        catch (IOException | RuntimeException failure) { logger.error("Could not load profile {}", name, failure); return false; }
        refreshUi.run();
        return true;
    }
    public List<String> getProfiles() {
        if (!Files.isDirectory(directory)) { return List.of(); }
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5)).filter(name -> name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}"))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (IOException failure) { logger.error("Could not list profiles", failure); return List.of(); }
    }
}
```

### src/main/java/dev/riserecode/keybind/KeybindManager.java

```java
package dev.riserecode.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.Client;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.KeyPressEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.clickgui.ClickGui;
import java.util.function.Supplier;
import java.util.Locale;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

public final class KeybindManager implements AutoCloseable {
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final Supplier<ClickGui> clickGuiSupplier;
    private final KeyMapping clickGuiKey;
    private final EventBus.Subscription subscription;

    public KeybindManager(EventBus eventBus, Minecraft minecraft, ModuleManager modules, Supplier<ClickGui> clickGuiSupplier) {
        this.minecraft = minecraft;
        this.modules = modules;
        this.clickGuiSupplier = clickGuiSupplier;
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Client.MOD_ID, "controls"));
        clickGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.riserecode.clickgui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        subscription = eventBus.subscribe(KeyPressEvent.class, 100, false, this::onKey);
    }

    public KeyMapping getClickGuiKey() { return clickGuiKey; }

    public void bind(Module module, String name) {
        int key = parseKey(name);
        if (key != Module.UNBOUND && key == KeyMappingHelper.getBoundKeyOf(clickGuiKey).getValue()) {
            throw new IllegalArgumentException("That key opens the ClickGUI; choose another key");
        }
        modules.setKeybind(module, key);
    }

    public static int parseKey(String name) {
        String normalized = name.strip().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("none") || normalized.equals("unbound")) { return Module.UNBOUND; }
        normalized = switch (normalized) {
            case "rshift", "rightshift", "right_shift" -> "right.shift";
            case "lshift", "leftshift", "left_shift", "shift" -> "left.shift";
            case "rctrl", "rightctrl", "right_control" -> "right.control";
            case "lctrl", "leftctrl", "left_control", "ctrl", "control" -> "left.control";
            case "ralt", "rightalt", "right_alt" -> "right.alt";
            case "lalt", "leftalt", "left_alt", "alt" -> "left.alt";
            case "esc" -> "escape";
            case "return" -> "enter";
            default -> normalized.replace('_', '.');
        };
        try {
            InputConstants.Key key = InputConstants.getKey(normalized.startsWith("key.keyboard.")
                    ? normalized : "key.keyboard." + normalized);
            if (key.getValue() < 32 || key.getValue() > 348) { throw new IllegalArgumentException("Unknown key"); }
            return key.getValue();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown keyboard key: " + name + ". Try R, F6, RIGHT_SHIFT, or NONE");
        }
    }

    public static String keyName(int key) {
        return key == Module.UNBOUND ? "NONE" : InputConstants.Type.KEYSYM.getOrCreate(key).getName()
                .replace("key.keyboard.", "").replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private void onKey(KeyPressEvent event) {
        if (!event.isPress() || minecraft.font == null || minecraft.gui.overlay() != null) { return; }
        Screen screen = minecraft.gui.screen();
        if (clickGuiKey.matches(event.getInput())
                && (screen == null || screen instanceof ClickGui || screen instanceof TitleScreen)) {
            ClickGui clickGui = clickGuiSupplier.get();
            if (screen == clickGui) { clickGui.onClose(); }
            else { clickGui.open(screen); }
            event.cancel();
            return;
        }
        if (screen != null || minecraft.player == null) { return; }
        for (Module module : modules.getModules()) {
            if (module.getKeybind() != Module.UNBOUND && module.getKeybind() == event.getKey()) {
                modules.toggle(module);
            }
        }
    }

    @Override
    public void close() { subscription.close(); }
}
```

### src/main/java/dev/riserecode/module/ModuleManager.java

```java
package dev.riserecode.module;

import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.module.modules.TracersModule;
import dev.riserecode.module.modules.NoFallModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Client-thread registry. Module owns the subscriptions for its lifecycle. */
public final class ModuleManager {
    private final EventBus eventBus;
    private final Logger logger;
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> view = Collections.unmodifiableList(modules);
    private final Map<String, Module> byName = new LinkedHashMap<>();
    private Runnable changeListener = () -> { };

    public ModuleManager(EventBus eventBus, Logger logger) {
        this.eventBus = Objects.requireNonNull(eventBus);
        this.logger = Objects.requireNonNull(logger);
    }

    public void initDefaults(Minecraft minecraft, FriendManager friends) {
        register(new FullBrightModule(eventBus, minecraft));
        register(new SprintModule(eventBus, minecraft));
        register(new KillAuraModule(eventBus, minecraft, friends));
        register(new TracersModule(eventBus, minecraft, friends));
        register(new NoFallModule(eventBus, minecraft));
    }

    public <M extends Module> M register(M module) {
        Objects.requireNonNull(module);
        String key = normalize(module.getName());
        if (byName.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate module: " + module.getName());
        }
        byName.put(key, module);
        modules.add(module);
        return module;
    }

    public List<Module> getModules() { return view; }
    public Optional<Module> getByName(String name) { return Optional.ofNullable(byName.get(normalize(name))); }
    public <M extends Module> Optional<M> getByClass(Class<M> type) {
        return modules.stream().filter(type::isInstance).map(type::cast).findFirst();
    }
    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(module -> module.getCategory() == category).toList();
    }
    public List<Module> getEnabledModules() {
        return modules.stream().filter(Module::isEnabled).toList();
    }

    public boolean toggle(Module module) { return setEnabled(module, !module.isEnabled()); }

    public boolean setEnabled(Module module, boolean enabled) {
        requireRegistered(module);
        if (module.isEnabled() == enabled) { return true; }
        boolean succeeded = true;
        try {
            module.setEnabled(enabled);
        } catch (RuntimeException failure) {
            logger.error("Could not change module {}", module.getName(), failure);
            succeeded = false;
        }
        changeListener.run();
        return succeeded;
    }

    public void setKeybind(Module module, int key) {
        requireRegistered(module);
        module.setKeybind(key);
        changeListener.run();
    }

    public void setChangeListener(Runnable listener) { changeListener = Objects.requireNonNull(listener); }

    /** Shutdown cleanup preserves saved toggle choices and continues after failures. */
    public void disableAll() {
        for (Module module : modules) {
            try {
                module.setEnabled(false);
            } catch (RuntimeException failure) {
                logger.error("Could not clean up module {}", module.getName(), failure);
            }
        }
    }

    private void requireRegistered(Module module) {
        if (byName.get(normalize(module.getName())) != module) {
            throw new IllegalArgumentException("Module is not registered");
        }
    }

    private static String normalize(String name) { return name.strip().toLowerCase(Locale.ROOT); }
}
```

### src/main/java/dev/riserecode/module/modules/SprintModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SprintModule extends Module {
    private final BooleanSetting omniSprint = register(new BooleanSetting("OmniSprint", "Request sprint while moving in any direction", false));
    private final Minecraft minecraft;
    private LocalPlayer controlled;
    private boolean originalSprint;
    public SprintModule(EventBus bus, Minecraft minecraft) {
        super("Sprint", "Automatically request sprint while moving", Category.MOVEMENT, bus); this.minecraft = minecraft;
    }
    public BooleanSetting getOmniSprint() { return omniSprint; }
    @Override
    protected void onEnable() { listen(UpdateEvent.class, this::update); }
    private void update(UpdateEvent event) {
        LocalPlayer player = minecraft.player;
        if (controlled != player) { controlled = null; }
        if (player == null) { return; }
        boolean moving = omniSprint.getValue() ? player.input.getMoveVector().lengthSquared() > 1.0e-4
                : player.input.hasForwardImpulse();
        boolean eligible = moving && minecraft.gui.screen() == null && !player.isSpectator() && !player.isPassenger()
                && !player.isShiftKeyDown() && !player.isUsingItem() && !player.isFallFlying()
                && (!player.horizontalCollision || player.minorHorizontalCollision)
                && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly);
        if (eligible) {
            if (controlled == null) { controlled = player; originalSprint = player.isSprinting(); }
            player.setSprinting(true);
        } else if (controlled != null) { player.setSprinting(false); }
    }
    @Override
    protected void onDisable() {
        if (controlled != null && controlled == minecraft.player) { controlled.setSprinting(originalSprint); }
        controlled = null;
    }
}
```

### src/main/java/dev/riserecode/module/modules/KillAuraModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.AttackEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Basic nearest-target/cooldown demonstration; no rotation or reach bypass. */
public final class KillAuraModule extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }
    private final DoubleSetting range = register(new DoubleSetting("Range", "Client target selection radius", 3, 1, 6));
    private final EnumSetting<Targets> targets = register(new EnumSetting<>("Targets", "Entity types to consider", Targets.PLAYERS));
    private final Minecraft minecraft;
    private final FriendManager friends;
    private final EventBus bus;
    private LivingEntity target;

    public KillAuraModule(EventBus bus, Minecraft minecraft, FriendManager friends) {
        super("KillAura", "Basic nearest-target attack demonstration", Category.COMBAT, bus);
        this.bus = bus; this.minecraft = minecraft; this.friends = friends;
    }
    public DoubleSetting getRange() { return range; }
    public EnumSetting<Targets> getTargets() { return targets; }
    public Optional<LivingEntity> getTarget() { return Optional.ofNullable(target); }
    @Override
    protected void onEnable() { listen(UpdateEvent.class, this::update); }
    private void update(UpdateEvent event) {
        target = null;
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null || minecraft.gui.screen() != null
                || !player.isAlive() || player.isSpectator() || player.isUsingItem()) { return; }
        double radius = range.getValue();
        target = minecraft.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), entity -> {
            if (entity == player || !entity.isAlive() || entity.isRemoved() || entity.isInvisible()
                    || player.distanceToSqr(entity) > radius * radius || player.isAlliedTo(entity)) { return false; }
            if (entity instanceof Player other) {
                return targets.getValue() != Targets.MOBS && !other.isSpectator()
                        && !friends.isFriend(other.getUUID(), other.getGameProfile().name());
            }
            return entity instanceof Mob && targets.getValue() != Targets.PLAYERS;
        }).stream().min(Comparator.comparingDouble((LivingEntity entity) -> player.distanceToSqr(entity))
                .thenComparingInt(LivingEntity::getId)).orElse(null);
        if (target == null || player.getAttackStrengthScale(0) < 1) { return; }
        AttackEvent attack = bus.post(new AttackEvent(player, target));
        if (attack.isCancelled()) { return; }
        minecraft.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
    @Override
    protected void onDisable() { target = null; }
}
```

### src/main/java/dev/riserecode/module/modules/TracersModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class TracersModule extends Module {
    private final ColorSetting color = register(new ColorSetting("Color", "Player line color", 0xFF3B82F6));
    private final ColorSetting friendColor = register(new ColorSetting("FriendColor", "Friend line color", 0xFF34D399));
    private final DoubleSetting width = register(new DoubleSetting("Width", "Line width in pixels", 1.5, 0.5, 4));
    private final Minecraft minecraft;
    private final FriendManager friends;
    public TracersModule(EventBus bus, Minecraft minecraft, FriendManager friends) {
        super("Tracers", "Depth-tested lines to other players", Category.RENDER, bus);
        this.minecraft = minecraft; this.friends = friends;
    }
    public ColorSetting getColor() { return color; }
    public ColorSetting getFriendColor() { return friendColor; }
    public DoubleSetting getWidth() { return width; }
    @Override
    protected void onEnable() { listen(Render3DEvent.class, this::extract); }
    private void extract(Render3DEvent event) {
        if (minecraft.player == null) { return; }
        var context = event.getContext();
        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        var forward = context.camera().forwardVector();
        Vec3 start = context.camera().position().add(forward.x() * 0.3, forward.y() * 0.3, forward.z() * 0.3);
        for (var player : context.level().players()) {
            if (player == minecraft.player || !player.isAlive() || player.isRemoved() || player.isSpectator() || player.isInvisible()) { continue; }
            boolean friend = friends.isFriend(player.getUUID(), player.getGameProfile().name());
            Vec3 end = player.getPosition(partialTick).add(0, player.getBbHeight() / 2.0, 0);
            event.addLine(start, end, friend ? friendColor.getValue() : color.getValue(), width.getValue().floatValue());
        }
    }
}
```

### src/main/java/dev/riserecode/module/modules/NoFallModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Local reset demonstration. The modern wire packet contains no fall-distance field. */
public final class NoFallModule extends Module {
    public enum Mode { PACKET, VANILLA }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Reset locally on tick or movement-packet send", Mode.VANILLA));
    private final Minecraft minecraft;
    public NoFallModule(EventBus bus, Minecraft minecraft) {
        super("NoFall", "Reset local fall distance; server fall damage is unchanged", Category.PLAYER, bus);
        this.minecraft = minecraft;
    }
    public EnumSetting<Mode> getMode() { return mode; }
    @Override
    protected void onEnable() {
        listen(UpdateEvent.class, event -> { if (mode.getValue() == Mode.VANILLA) { reset(); } });
        listen(PacketEvent.Send.class, event -> {
            if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) { return; }
            if (minecraft.isSameThread()) { resetForPacket(); }
            else { minecraft.execute(this::resetForPacket); }
        });
    }
    private void resetForPacket() {
        if (isEnabled() && mode.getValue() == Mode.PACKET) { reset(); }
    }
    private void reset() {
        if (minecraft.player != null && minecraft.player.fallDistance > 0) { minecraft.player.fallDistance = 0; }
    }
}
```

### src/main/java/dev/riserecode/event/PacketEvent.java

```java
package dev.riserecode.event;

import java.util.Objects;
import net.minecraft.network.protocol.Packet;

/** Synchronous wire-boundary events. Receive normally runs on a Netty thread. */
public abstract class PacketEvent extends Event {
    private final Packet<?> originalPacket;
    private Packet<?> packet;

    protected PacketEvent(Packet<?> packet) {
        super(true); this.packet = Objects.requireNonNull(packet); originalPacket = packet;
    }
    public final Packet<?> getPacket() { return packet; }
    public final Packet<?> getOriginalPacket() { return originalPacket; }
    public final void setPacket(Packet<?> replacement) {
        Objects.requireNonNull(replacement);
        if (replacement.type().flow() != originalPacket.type().flow()) {
            throw new IllegalArgumentException("Replacement packet has the wrong direction");
        }
        packet = replacement;
    }
    public static final class Send extends PacketEvent { public Send(Packet<?> packet) { super(packet); } }
    public static final class Receive extends PacketEvent { public Receive(Packet<?> packet) { super(packet); } }
}
```

### src/main/java/dev/riserecode/event/AttackEvent.java

```java
package dev.riserecode.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Cancellable intent before the aura's ordinary game-mode attack. */
public final class AttackEvent extends Event {
    private final Player attacker;
    private final LivingEntity target;
    public AttackEvent(Player attacker, LivingEntity target) {
        super(true); this.attacker = Objects.requireNonNull(attacker); this.target = Objects.requireNonNull(target);
    }
    public Player getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }
}
```

### src/main/java/dev/riserecode/event/Render3DEvent.java

```java
package dev.riserecode.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.world.phys.Vec3;

/** Extraction collects immutable primitives; no entity references survive into drawing. */
public final class Render3DEvent extends Event {
    public record Line(Vec3 from, Vec3 to, int color, float width) {
        public Line {
            Objects.requireNonNull(from); Objects.requireNonNull(to);
            if (!finite(from) || !finite(to) || !Float.isFinite(width) || width <= 0 || width > 16) {
                throw new IllegalArgumentException("Line coordinates and width must be finite; width must be in (0, 16]");
            }
        }
        private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z); }
    }
    private final LevelExtractionContext context;
    private final List<Line> lines = new ArrayList<>();
    public Render3DEvent(LevelExtractionContext context) { this.context = Objects.requireNonNull(context); }
    public LevelExtractionContext getContext() { return context; }
    public void addLine(Vec3 from, Vec3 to, int color, float width) { lines.add(new Line(from, to, color, width)); }
    public List<Line> getLines() { return List.copyOf(lines); }
}
```

### src/main/java/dev/riserecode/render/WorldRenderer.java

```java
package dev.riserecode.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/** Immutable extraction snapshots submitted through vanilla's depth-tested line pipeline. */
public final class WorldRenderer implements AutoCloseable {
    private record RelativeLine(Vec3 from, Vec3 to, Vec3 normal, int color, float width) { }
    private final EventBus.Subscription subscription;
    private volatile List<Render3DEvent.Line> lines = List.of();

    public WorldRenderer(EventBus bus) {
        subscription = bus.subscribe(Render3DEvent.class, Integer.MIN_VALUE, false, event -> lines = event.getLines());
    }
    public List<Render3DEvent.Line> getLines() { return lines; }

    public void submit(LevelRenderContext context) {
        List<Render3DEvent.Line> snapshot = lines;
        if (snapshot.isEmpty()) { return; }
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<RelativeLine> geometry = new ArrayList<>(snapshot.size());
        for (Render3DEvent.Line line : snapshot) {
            Vec3 direction = line.to().subtract(line.from());
            if (direction.lengthSqr() < 1.0e-12) { continue; }
            geometry.add(new RelativeLine(line.from().subtract(camera), line.to().subtract(camera),
                    direction.normalize(), line.color(), line.width()));
        }
        List<RelativeLine> immutable = List.copyOf(geometry);
        PoseStack poses = context.poseStack();
        context.submitNodeCollector().submitCustomGeometry(poses, RenderTypes.lines(), (pose, vertices) -> {
            for (RelativeLine line : immutable) {
                Vec3 normal = line.normal();
                vertices.addVertex(pose, (float) line.from().x, (float) line.from().y, (float) line.from().z)
                        .setColor(line.color()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(line.width());
                vertices.addVertex(pose, (float) line.to().x, (float) line.to().y, (float) line.to().z)
                        .setColor(line.color()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(line.width());
            }
        });
    }
    @Override
    public void close() { subscription.close(); lines = List.of(); }
}
```

### src/main/java/dev/riserecode/mixin/ChatInputMixin.java

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ChatInputMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void riserecode$sendChat(String message, CallbackInfo callback) {
        if (Client.INSTANCE.isInitialized() && Client.INSTANCE.getCommandManager().handleChat(message)) {
            // Stop before signing/last-seen bookkeeping or creating a chat packet.
            callback.cancel();
        }
    }
}
```

### src/main/java/dev/riserecode/mixin/ConnectionMixin.java

```java
package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.Client;
import dev.riserecode.event.PacketEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.CancellationException;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow private Channel channel;

    /** The deepest send boundary covers every overload and queued sends exactly once. */
    @WrapMethod(method = "sendPacket")
    private void riserecode$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if (!riserecode$isClient()) { original.call(packet, listener, flush); return; }
        PacketEvent.Send event = Client.INSTANCE.getEventBus().post(new PacketEvent.Send(packet));
        if (!event.isCancelled()) { original.call(event.getPacket(), listener, flush); return; }
        if (listener != null) {
            // Preserve Netty's callback thread and report cancellation instead of leaving callers waiting.
            channel.eventLoop().execute(() -> {
                try { listener.operationComplete(channel.newFailedFuture(new CancellationException("RiseRecode cancelled packet"))); }
                catch (Exception failure) { Client.INSTANCE.getLogger().error("Cancelled packet callback failed", failure); }
            });
        }
    }

    @WrapMethod(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V")
    private void riserecode$receive(ChannelHandlerContext context, Packet<?> packet, Operation<Void> original) {
        if (!riserecode$isClient() || !context.channel().isOpen()) { original.call(context, packet); return; }
        PacketEvent.Receive event = Client.INSTANCE.getEventBus().post(new PacketEvent.Receive(packet));
        if (!event.isCancelled()) { original.call(context, event.getPacket()); }
    }

    @Unique
    private boolean riserecode$isClient() {
        return Client.INSTANCE.isInitialized() && ((Connection) (Object) this).getReceiving() == PacketFlow.CLIENTBOUND;
    }
}
```

### src/main/java/dev/riserecode/ui/component/Panel.java

```java
package dev.riserecode.ui.component;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Panel extends UIComponent {
    private static final double HEADER = 39;
    private final Category category;
    private final ModuleManager manager;
    private final Runnable changed;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation opening = new Animation(0, 0.28, MathUtil.Easing.OUT_QUART);
    private final Animation positionX = new Animation(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation positionY = new Animation(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation scroll = new Animation(0, 0.16, MathUtil.Easing.OUT_QUART);
    private double targetX;
    private double targetY;
    private double dragOffsetX;
    private double dragOffsetY;
    private double viewportWidth;
    private double viewportHeight;
    private double maxScroll;
    private boolean dragging;
    private ModuleButton captured;

    public Panel(Category category, ModuleManager manager, Runnable changed) {
        this.category = category; this.manager = manager; this.changed = changed;
        syncModules();
    }

    public Category getCategory() { return category; }
    public List<ModuleButton> getModuleButtons() { return List.copyOf(buttons); }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public boolean isOpen() { return !opening.isRunning() && opening.getValue() == 1; }
    public double getScale() { return MathUtil.lerp(0.95, 1, opening.getValue()); }

    public void open() { opening.reset(0); opening.animateTo(1); }

    public void refresh() {
        mouseReleased(0, 0, 0);
        syncModules();
    }

    public void place(double x, double y, double width, double height, double viewportWidth, double viewportHeight) {
        setBounds(x, y, width, height);
        this.viewportWidth = viewportWidth; this.viewportHeight = viewportHeight;
        targetX = x; targetY = y; clampTarget();
        this.x = targetX; this.y = targetY;
        positionX.reset(targetX); positionY.reset(targetY);
    }

    private void syncModules() {
        List<Module> registered = manager.getByCategory(category);
        if (registered.size() == buttons.size()) { return; }
        for (Module module : registered) {
            if (buttons.stream().noneMatch(button -> button.getModule() == module)) {
                buttons.add(new ModuleButton(module, manager, changed));
            }
        }
    }

    private void clampTarget() {
        targetX = Math.max(10, Math.min(viewportWidth - width - 10, targetX));
        targetY = Math.max(58, Math.min(viewportHeight - height - 28, targetY));
    }

    private double localX(double mouseX) { return x + width / 2 + (mouseX - x - width / 2) / getScale(); }
    private double localY(double mouseY) { return y + height / 2 + (mouseY - y - height / 2) / getScale(); }
    private boolean inBody(double mouseX, double mouseY) {
        return mouseX >= x + 8 && mouseX < x + width - 8 && mouseY >= y + HEADER && mouseY < y + height - 10;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        syncModules();
        opening.update(deltaSeconds);
        if (dragging) {
            double weight = 1 - Math.exp(-24 * deltaSeconds);
            x = MathUtil.lerp(x, targetX, weight); y = MathUtil.lerp(y, targetY, weight);
            positionX.reset(x); positionY.reset(y);
        } else {
            positionX.animateTo(targetX); positionY.animateTo(targetY);
            x = positionX.update(deltaSeconds); y = positionY.update(deltaSeconds);
        }
        double contentHeight = 0;
        for (ModuleButton button : buttons) {
            button.update(deltaSeconds); contentHeight += button.getPreferredHeight() + 6;
        }
        maxScroll = Math.max(0, contentHeight - (height - HEADER - 16));
        double scrollTarget = Math.max(0, Math.min(maxScroll, scroll.getTarget()));
        if (scroll.getValue() > maxScroll) { scroll.reset(maxScroll); }
        scroll.animateTo(scrollTarget); scroll.update(deltaSeconds);
        double alpha = opacity * opening.getValue();
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        double hovered = updateHover(pointerX, pointerY, deltaSeconds);
        double scale = getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + width / 2), (float) (y + height / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-x - width / 2), (float) (-y - height / 2));
        try {
            RenderUtil.drawDropShadow(graphics, x, y + 3, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.SHADOW, alpha));
            RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.PANEL, alpha));
            RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.lerp(Theme.BORDER, 0x303B82F6, hovered), alpha));
            RenderUtil.drawText(graphics, font, category.getDisplayName(), x + 14, y + 14,
                    Theme.opacity(Theme.TEXT, alpha), true, false);
            RenderUtil.drawText(graphics, font, Integer.toString(buttons.size()), x + width - 23, y + 15,
                    Theme.opacity(Theme.MUTED, alpha), false, false);
            RenderUtil.drawGradientRect(graphics, x + 12, y + HEADER - 2, width - 24, 1,
                    Theme.opacity(Theme.BORDER, alpha), Theme.opacity(Theme.BORDER, alpha));
            RenderUtil.scissorPush(graphics, x + 8, y + HEADER, width - 16, height - HEADER - 10);
            try {
                double nextY = y + HEADER + 4 - scroll.getValue();
                for (ModuleButton button : buttons) {
                    button.setBounds(x + 10, nextY, width - 20, button.getPreferredHeight());
                    // Invisible rows still update their animation so scrolling never restarts a transition.
                    boolean pointerInside = inBody(pointerX, pointerY);
                    button.render(graphics, font, pointerInside ? pointerX : -Double.MAX_VALUE,
                            pointerInside ? pointerY : -Double.MAX_VALUE, deltaSeconds, alpha);
                    nextY += button.getPreferredHeight() + 6;
                }
                if (buttons.isEmpty()) {
                    RenderUtil.drawText(graphics, font, "No modules", x + 14, y + HEADER + 12,
                            Theme.opacity(Theme.MUTED, alpha), false, false);
                }
            } finally { RenderUtil.scissorPop(graphics); }
            if (maxScroll > 0) {
                double track = height - HEADER - 18;
                double thumb = Math.max(16, track * track / (track + maxScroll));
                double top = y + HEADER + 4 + (track - thumb) * scroll.getValue() / maxScroll;
                RenderUtil.drawRoundedRect(graphics, x + width - 6, top, 2, thumb, 1,
                        Theme.opacity(Theme.BLUE, alpha * 0.65));
            }
        } finally { graphics.pose().popMatrix(); }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        if (!contains(pointerX, pointerY)) { return false; }
        if (pointerY < y + HEADER && button == 0) {
            dragging = true; dragOffsetX = mouseX - x; dragOffsetY = mouseY - y; return true;
        }
        if (inBody(pointerX, pointerY)) {
            for (ModuleButton child : buttons) {
                if (child.mouseClicked(pointerX, pointerY, button)) { captured = child; return true; }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            targetX = mouseX - dragOffsetX; targetY = mouseY - dragOffsetY; clampTarget(); return true;
        }
        return captured != null && captured.mouseDragged(localX(mouseX), localY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        if (captured != null) {
            consumed |= captured.mouseReleased(localX(mouseX), localY(mouseY), button); captured = null;
        }
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        if (!contains(pointerX, pointerY)) { return false; }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - amount * 32)));
        return true;
    }
}
```

### src/main/java/dev/riserecode/ui/clickgui/ClickGui.java

```java
package dev.riserecode.ui.clickgui;

import dev.riserecode.Client;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A native screen host; all internal rendering and input belong to our components. */
public final class ClickGui extends Screen {
    private final List<Panel> panels = new ArrayList<>();
    private final ToggleSwitch hudToggle;
    private final ActionButton closeButton = new ActionButton("Close", this::onClose);
    private final Runnable saveChanges;
    private Screen parent;
    private Panel captured;
    private double canvasScale = 1;
    private double canvasWidth;
    private double canvasHeight;
    private boolean arranged;
    private long previousFrame;

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal(Client.NAME));
        this.saveChanges = saveChanges;
        hudToggle = new ToggleSwitch("HUD", hud::isEnabled, value -> { hud.setEnabled(value); saveChanges.run(); });
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
    }

    public List<Panel> getPanels() { return List.copyOf(panels); }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public double getCanvasScale() { return canvasScale; }

    public void refresh() {
        if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; }
        for (Panel panel : panels) { panel.refresh(); }
    }

    public void open(Screen parent) {
        this.parent = parent;
        previousFrame = System.nanoTime();
        for (Panel panel : panels) { panel.open(); }
        minecraft.gui.setScreen(this);
    }

    @Override
    protected void init() {
        canvasScale = Math.min(1, Math.min(width / 600.0, height / 380.0));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        int columns = Math.min(6, Math.max(3, (int) ((canvasWidth - 20) / 190)));
        int rows = (panels.size() + columns - 1) / columns;
        double panelWidth = Math.min(184, (canvasWidth - 32 - (columns - 1) * 14) / columns);
        double panelHeight = Math.min(240, (canvasHeight - 98 - (rows - 1) * 14) / rows);
        double startX = (canvasWidth - columns * panelWidth - (columns - 1) * 14) / 2;
        for (Panel panel : panels) {
            int index = panel.getCategory().ordinal();
            double left = arranged ? panel.getBounds().x() : startX + (index % columns) * (panelWidth + 14);
            double top = arranged ? panel.getBounds().y() : 64 + (index / columns) * (panelHeight + 14);
            panel.place(left, top, panelWidth, panelHeight, canvasWidth, canvasHeight);
        }
        arranged = true;
        hudToggle.setBounds(canvasWidth - 166, 15, 76, 26);
        closeButton.setBounds(canvasWidth - 76, 14, 60, 28);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) { extractPanorama(graphics, delta); }
        extractBlurredBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1_000_000_000.0);
        previousFrame = now;
        RenderUtil.drawGradientRect(graphics, 0, 0, width, height, 0xB80F1115, 0xDE0F1115);
        double pointerX = mouseX / canvasScale; double pointerY = mouseY / canvasScale;
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        try {
            Panel hovered = null;
            for (int i = panels.size() - 1; i >= 0; i--) {
                if (panels.get(i).contains(pointerX, pointerY)) { hovered = panels.get(i); break; }
            }
            for (Panel panel : panels) {
                graphics.nextStratum();
                panel.render(graphics, font, panel == hovered ? pointerX : -Double.MAX_VALUE,
                        panel == hovered ? pointerY : -Double.MAX_VALUE, seconds, 1);
            }
            graphics.nextStratum();
            RenderUtil.drawRoundedRect(graphics, 16, 15, 27, 27, 8, Theme.BLUE);
            RenderUtil.drawText(graphics, font, "R", 24, 23, Theme.TEXT, true, false);
            RenderUtil.drawText(graphics, font, Client.NAME, 53, 16, Theme.TEXT, true, false);
            RenderUtil.drawText(graphics, font, "Utility client · " + Client.INSTANCE.getVersion(), 53, 32,
                    Theme.SECONDARY, false, false);
            hudToggle.render(graphics, font, pointerX, pointerY, seconds, 1);
            closeButton.render(graphics, font, pointerX, pointerY, seconds, 1);
            RenderUtil.drawText(graphics, font, "Left click · Toggle    Right click · Settings    Drag header · Move",
                    16, canvasHeight - 19, Theme.MUTED, false, false);
        } finally { graphics.pose().popMatrix(); }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pointerX = event.x() / canvasScale; double pointerY = event.y() / canvasScale;
        if (closeButton.mouseClicked(pointerX, pointerY, event.button())
                || hudToggle.mouseClicked(pointerX, pointerY, event.button())) { return true; }
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.mouseClicked(pointerX, pointerY, event.button())) {
                captured = panel;
                panels.remove(i); panels.add(panel);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return captured != null && captured.mouseDragged(event.x() / canvasScale, event.y() / canvasScale, event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(event.x() / canvasScale, event.y() / canvasScale, event.button());
        captured = null;
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mouseX / canvasScale, mouseY / canvasScale, vertical)) { return true; }
        }
        return false;
    }

    @Override
    public void onClose() {
        if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; }
        saveChanges.run();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

### Verification code

These files belong to the test and gametest source sets and are excluded from the
installable JAR. The existing client integration test now invokes the Phase 5
checks in its isolated singleplayer world. The chat accessor is test-only.

#### src/test/java/dev/riserecode/EngineChecks.java

```java
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
```

#### src/gametest/java/dev/riserecode/EngineIntegrationTest.java

```java
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
                noFall.getMode().setValue(NoFallModule.Mode.VANILLA); modules.setEnabled(noFall, true);
                minecraft.player.fallDistance = 6;
                client.getEventBus().post(new UpdateEvent(minecraft, 0));
                check(minecraft.player.fallDistance == 0, "NoFall vanilla local reset");
                noFall.getMode().setValue(NoFallModule.Mode.PACKET);
            });
            AtomicBoolean cancelledCallback = new AtomicBoolean();
            AtomicReference<Packet<?>> originalPacket = new AtomicReference<>();
            AtomicReference<Packet<?>> replacementPacket = new AtomicReference<>();
            AtomicBoolean resetAtSend = new AtomicBoolean();
            var replace = client.getEventBus().subscribe(PacketEvent.Send.class, -10, false, event -> {
                if (event.getPacket() == originalPacket.get()) {
                    resetAtSend.set(net.minecraft.client.Minecraft.getInstance().player.fallDistance == 0);
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
                    check(minecraft.player.fallDistance == 0 && !packet.isOnGround(), "packet-mode reset does not spoof ground state");
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
                    && sprint.getKeybind() == InputConstants.KEY_F7 && client.getHudRenderer().isEnabled()), "profile restores modules, binds and HUD");
            check(context.computeOnClient(minecraft -> client.getFriendManager().isFriend("AfterSave") && client.getCommandManager().getPrefix().equals("-")), "profile preserves global friends and prefix");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            context.waitFor(minecraft -> client.getClickGui().getPanels().stream().allMatch(dev.riserecode.ui.component.Panel::isOpen), 2000);
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
```

#### src/gametest/java/dev/riserecode/testmixin/ChatStateAccessor.java

```java
package dev.riserecode.testmixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only snapshot access, present only in the isolated test mod. */
@Mixin(ChatComponent.State.class)
public interface ChatStateAccessor {
    @Accessor("messages") List<GuiMessage> riserecode$getMessages();
}
```

#### src/gametest/java/dev/riserecode/ClientIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.util.RenderUtil;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 2000;
    private enum Mode { FIRST, SECOND }
    private static final class ControlsModule extends Module {
        private final BooleanSetting advanced = register(new BooleanSetting("Advanced", "", false));
        private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "", Mode.FIRST));
        private final ColorSetting color = register(new ColorSetting("Accent", "", 0x803B82F6));
        private final DoubleSetting conditional = register(new DoubleSetting("Conditional", "", 0.5, 0, 1));
        private ControlsModule(String name) {
            super(name, "Client test fixture", Category.UTILITY, Client.INSTANCE.getEventBus());
            conditional.setVisibleWhen(advanced::getValue);
            for (int i = 0; i < 8; i++) { register(new DoubleSetting("Slider " + (i + 1), "", 0.5, 0, 1)); }
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(1280, 800);
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 75, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().getFirst().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue()) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().size() == 2);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 250, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.color.getValue() == 0x80D946EF), "color palette preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-2);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 60, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Close the settings and drag the category through Minecraft's actual mouse handler.
        click(context, controlsButton::getBounds, 1);
        Panel renderPanel = context.computeOnClient(minecraft -> panel(Category.RENDER));
        double startX = context.computeOnClient(minecraft -> renderPanel.getBounds().x());
        move(context, renderPanel::getBounds, 0.4, 0.07);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> renderPanel.getBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> renderPanel.getBounds().y() >= 58), "drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getPanels().stream().allMatch(p -> p.getBounds().x() >= 10)), "small window layout clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(1280, 800);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        AtomicInteger hudFrames = new AtomicInteger();
        AtomicInteger worldFrames = new AtomicInteger();
        var hudSubscription = Client.INSTANCE.getEventBus().subscribe(Render2DEvent.class, event -> hudFrames.incrementAndGet());
        var worldSubscription = Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class, event -> worldFrames.incrementAndGet());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(minecraft -> Client.INSTANCE.getModuleManager().setKeybind(fullBright(), InputConstants.KEY_B));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> fullBright().isEnabled()), "module key toggles once");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == fullBright() && entry.visibility() == 1), ANIMATION_TIMEOUT);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.runOnClient(minecraft -> { controls.setEnabled(true); longer.setEnabled(true); });
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 3);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % 24 == 8), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, entry.module().getName(), false))).orElseThrow();
                return widest.y() == 8;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 8 || entry.y() == 32), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
            EngineIntegrationTest.verify(context, world);
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().setKeybind(fullBright(), -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static FullBrightModule fullBright() { return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static Panel panel(Category category) { return gui().getPanels().stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow(); }
    private static ModuleButton button(String name) {
        return gui().getPanels().stream().flatMap(panel -> panel.getModuleButtons().stream())
                .filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().getPanels().stream().allMatch(Panel::isOpen), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.y() + 39 + (body.height() - 49) / 2 - control.centerY()) / 32;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 32));
        context.waitFor(minecraft -> Math.abs(panel.getScrollOffset() - target) < 0.01, ANIMATION_TIMEOUT);
    }
    private static void move(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> {
            var b = bounds.get();
            return new double[] {(b.x() + b.width() * x) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                    (b.y() + b.height() * y) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTick();
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int mouseButton) {
        // Headers stay fixed when a module expands; clicking the center of the full body would edit a setting.
        Supplier<UIComponent.Bounds> header = () -> {
            var b = bounds.get(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), Math.min(28, b.height()));
        };
        move(context, header, 0.4, 0.5);
        context.getInput().pressMouse(mouseButton);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
```

## 3. Mixin configuration and resource/build changes

The production configuration now includes the existing keyboard adapter plus
chat and connection adapters. Fabric Loader supplies MixinExtras for the
`@WrapMethod` connection wrappers. Java remains 25 throughout; the mod version is
now 0.3.0. The existing main `fabric.mod.json` already declares the correct client
entrypoint, Minecraft version, Fabric API dependency, and Java requirement; its
full current contents are included below. The test metadata registers only the
test chat-state accessor, which is never shipped in the main JAR.

### src/main/resources/riserecode.mixins.json

```json
{
  "required": true,
  "package": "dev.riserecode.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": ["KeyboardHandlerMixin", "ChatInputMixin", "ConnectionMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

### src/main/resources/fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "riserecode",
  "version": "${version}",
  "name": "RiseRecode",
  "description": "A modular client utility mod with a simple HUD and ClickGUI.",
  "environment": "client",
  "entrypoints": {
    "client": ["dev.riserecode.RiseRecodeClient"]
  },
  "mixins": ["riserecode.mixins.json"],
  "depends": {
    "fabricloader": ">=0.19.5",
    "minecraft": "26.2",
    "java": ">=25",
    "fabric-api": ">=0.161.0+26.2"
  }
}
```

### build.gradle

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    compileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
    testCompileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
}

processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand version: project.version }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-Xlint:all', '-Werror']
}

tasks.register('foundationCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.FoundationChecks'
}

tasks.register('runtimeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.RuntimeChecks'
}

tasks.register('uiCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.UiChecks'
}

tasks.register('engineCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.EngineChecks'
}

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck', 'uiCheck', 'engineCheck' }

// The dependency-free checks above use JavaExec rather than a JUnit runner.
tasks.named('test') { enabled = false }

fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'riserecode-test'
        enableGameTests = false
        enableClientGameTests = true
    }
}

loom.runs.clientGameTest {
    runDir = 'build/run/clientGameTest'
    programArgs '--graphicsBackend', providers.gradleProperty('graphicsBackend').getOrElse('OPENGL')
}
```

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.2
mod_version=0.3.0
maven_group=dev.riserecode
archives_base_name=riserecode
```

### src/gametest/resources/riserecode-test.mixins.json

```json
{
  "required": true,
  "package": "dev.riserecode.testmixin",
  "compatibilityLevel": "JAVA_25",
  "client": ["ChatStateAccessor"],
  "injectors": { "defaultRequire": 1 }
}
```

### src/gametest/resources/fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "riserecode-test",
  "version": "1.0.0",
  "name": "RiseRecode integration checks",
  "environment": "client",
  "mixins": ["riserecode-test.mixins.json"],
  "entrypoints": {
    "fabric-client-gametest": ["dev.riserecode.ClientIntegrationTest"]
  },
  "depends": {
    "riserecode": "*",
    "fabric-client-gametest-api-v1": "*"
  }
}
```

## 4. Setup and integration notes

### Initialization and local commands

`RiseRecodeClient.onInitializeClient()` calls `Client.INSTANCE.init()`. Friends
and world rendering are created first, followed by the default module registry,
HUD, config, keybind manager, profiles, and commands. Config loads friend identity,
settings, and keybinds before activating saved modules. GUI construction stays at
`CLIENT_STARTED`, after Minecraft's font is initialized. All five default modules
are disabled in a fresh configuration.

`ChatInputMixin` injects at the head of `ClientPacketListener.sendChat(String)`.
If the configured prefix matches, `CommandManager` parses and executes the input
and cancels that method before signing, last-seen bookkeeping, or chat packet
creation. Unknown commands, bare prefixes, and malformed quoted arguments are
also consumed locally. Ordinary chat follows Minecraft's original method.
Commands support aliases, quoted names, backslash escapes, and local error messages.

Open chat in a world and try:

```text
.help
.t Sprint
.bind Sprint F6
.bind Sprint NONE
.friend add PlayerName
.friend list
.friend remove PlayerName
.profile save PvP
.profile list
.profile load PvP
.toggle Tracers
```

`.toggle` and `.t` use `ModuleManager`, including lifecycle subscription cleanup.
`.bind` uses `KeybindManager` and persists changes through the same config path as
the UI. Names such as `F6`, `R`, `RIGHT_SHIFT`, and `NONE` are parsed, but assigning
the current ClickGUI key to a module is rejected. Module binds fire only during
gameplay, once per press; they do not toggle modules while typing chat. Right Shift
continues to open/close the GUI, and Escape closes it.

### Friends and profiles

Friends are matched by case-insensitive name or UUID. Adding an online player
captures the UUID from the player list; offline names remain valid. The global
list and command prefix are persisted in `config/riserecode/config.json`.
KillAura excludes friends. Tracers uses its additional `FriendColor` setting for
friends, defaulting to green.

Profiles are written atomically to `config/riserecode/profiles/<name>.json`.
Names begin with a letter or digit and contain at most 32 letters, digits,
hyphens, or underscores. A profile stores module states, binds, typed settings,
and HUD visibility. Friends and the command prefix remain global. Loading validates
all known values before mutation, disables modules before installing settings,
and activates them only after every value is installed. Activation failure attempts
rollback to the previous state. Missing or invalid files return a local error;
invalid values do not partially apply. Missing module entries in older profiles
use disabled, unbound, default settings. Loading refreshes an open ClickGUI while
retaining panel positions and expansion state. Current choices are also saved
through the ordinary config debounce and shutdown path.

### Packet and attack events

`ConnectionMixin` wraps `Connection.sendPacket` rather than individual public send
overloads, so queued and ordinary sends each post one `PacketEvent.Send`.
The typed `channelRead0` method posts `PacketEvent.Receive` before original dispatch.
Both allow cancellation or same-direction packet replacement. The receiving-flow
check limits these adapters to client connections and excludes the integrated
server's connections. Cancelled sends notify any completion listener on the
Netty event loop with a failed future rather than leaving it waiting.

EventBus callbacks remain synchronous. Receive normally runs on a Netty thread;
send runs on its caller's thread. Packet listeners that change game state must
schedule that change with `Minecraft.execute(...)` when outside the client thread.
NoFall does this before inspecting mode or changing the player. `AttackEvent`
currently represents KillAura's cancellable attack intent; it does not wrap every
vanilla attack.

### Modern 3D rendering

`LevelExtractionEvents.END_EXTRACTION` posts the custom `Render3DEvent`. Tracers
reads interpolated player positions at that stage and adds immutable line primitives
containing endpoints, ARGB color, and width. A low-priority `WorldRenderer` listener
captures the finished list. Modules never register Fabric callbacks directly.

`LevelRenderEvents.COLLECT_SUBMITS` calls `WorldRenderer.submit(...)`. It converts
world coordinates to camera-relative positions, derives line normals, and uses
`submitNodeCollector().submitCustomGeometry(...)` with `RenderTypes.lines()`.
The callback emits modern `VertexConsumer` position/color/normal/line-width
attributes. Minecraft owns buffer allocation and draw execution. The shared native
line pipeline is depth-tested, so these tracers do not show through solid terrain.
The start point is just in front of the camera along its crosshair direction; target
endpoints are centered on each player's interpolated body. No entity references are
captured by the deferred drawing callback.

This follows [Fabric's extraction/submission rendering model](https://docs.fabricmc.net/develop/rendering/world).
There are no raw OpenGL calls, legacy immediate-mode draws, private lightmap hooks,
or custom world-rendering mixins. The OpenGL client test passed; Vulkan was not
exercised in Phase 5.

### Module behavior

- **Sprint:** forward movement requests sprinting; `OmniSprint` requests it for
  any movement direction. Hunger, collision, GUI, sneaking, item use, spectator,
  passenger, and fall-flying guards remain. Vanilla physics and server rules apply.
  The captured sprint flag is restored when disabling the module for that player.
- **KillAura:** `Range` is 1–6 with a default of 3; `Targets` selects players, mobs,
  or both. Self, friends, teammates, invisible, dead, removed, and spectator players
  are excluded. The nearest eligible entity is attacked through
  `minecraft.gameMode.attack(...)` after vanilla attack cooldown is ready. There
  are no rotation packets, reach overrides, or server bypasses; a larger selection
  radius does not extend server reach.
- **Tracers:** `Color`, `FriendColor`, and `Width` configure extracted player lines.
  Dead, removed, invisible, and spectator players are excluded. The GUI and animated
  HUD automatically discover the module through the registry.
- **NoFall:** `VANILLA` resets positive local fall distance during UpdateEvent;
  `PACKET` resets it when a movement packet is sent. Movement packets contain no
  fall-distance field, and neither mode changes on-ground flags or server damage.
  This proves event ownership and packet hooks with an explicit local-state scope.

### Build, run, and validation

Use a JDK 25 installation and the included Gradle wrapper:

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

Install [riserecode-0.3.0.jar](build/libs/riserecode-0.3.0.jar) with Fabric Loader and
Fabric API in a Minecraft 26.2 / Java 25 profile. For the checks:

```bash
bash scripts/verify.sh
./gradlew runClientGameTest
```

The completed build used `--offline --no-daemon` with the already-resolved dependency
cache and treated all Java compiler warnings as errors. The dependency-free checks
passed **175 assertions**: 82 foundation, 19 runtime, 19 UI, and 55 engine. New engine
checks cover parsing, registry collisions, friends, persisted identity/prefix,
profile validation and activation rollback, event ownership, packet replacement,
direction validation, and immutable render primitives.

The real OpenGL client test passed existing Phase 3/4 regressions plus local chat
interception with zero outgoing command chat packets, keybind assignment/conflict
rejection, forward and omni sprint flags, local NoFall resets, packet cancellation,
replacement and completion callbacks, real receive dispatch on Netty, friend-aware
target selection, cancellable attack intent, a vanilla attack on a server-spawned
pig, visible tracer lines, and profile loading while the GUI was open. The isolated
test restores config and closes the client automatically.

The release JAR was inspected for Java 25 class version 69, mod metadata, all new
production systems and mixins, bundled Inter fonts/license, and exclusion of test
classes and test mixins.

- [Retained client build/test log](build/verification/phase5/client-test.log)
- [Artifact verification record](build/verification/phase5/artifact-check.json)
- [Tracer screenshot](build/verification/phase5/0005_riserecode-tracers.png)
- [Phase 5 GUI screenshot](build/verification/phase5/0006_riserecode-phase5-gui.png)

The GUI screenshot retains the expanded FullBright settings from the regression
test; Tracers is below them in the scrollable Render panel.
