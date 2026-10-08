# Phase 8 — risecode 0.7.0

Minecraft 26.2, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2.
The project uses Minecraft 26.2's unobfuscated official names through Loom 1.17.21.
This delivery contains the complete contents of every new or modified production,
resource, build and verification file. Unchanged foundations remain in the project.
All added module, camera, animation and shader code is original.

## 1. Updated project file tree

Generated sources, Gradle caches and disposable game worlds are omitted.

```text
riserecode/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/
│   ├── install-qraftys.py
│   └── verify.sh
├── src/
│   ├── gametest/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── testmixin/
│   │   │           │   └── ChatStateAccessor.java
│   │   │           ├── ClientIntegrationTest.java
│   │   │           ├── CombatIntegrationTest.java
│   │   │           ├── EngineIntegrationTest.java
│   │   │           ├── FontIntegrationTest.java
│   │   │           ├── Phase8IntegrationTest.java
│   │   │           └── ThemeIntegrationTest.java
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
│   │   │           │   │   ├── HudCommand.java
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
│   │   │           │   ├── CameraEvent.java
│   │   │           │   ├── CameraInputEvent.java
│   │   │           │   ├── EnchantmentRenderEvent.java
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── ItemRenderEvent.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── MouseRotationEvent.java
│   │   │           │   ├── PacketEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── friend/
│   │   │           │   └── FriendManager.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   ├── AnimationFrameMixin.java
│   │   │           │   ├── CameraMixin.java
│   │   │           │   ├── ChatInputMixin.java
│   │   │           │   ├── ConnectionMixin.java
│   │   │           │   ├── EnchantmentRendererMixin.java
│   │   │           │   ├── GammaOptionAccessor.java
│   │   │           │   ├── GuiRendererMixin.java
│   │   │           │   ├── ItemEntityRendererMixin.java
│   │   │           │   ├── ItemPhysicsStateMixin.java
│   │   │           │   ├── KeyboardHandlerMixin.java
│   │   │           │   ├── KeyboardInputMixin.java
│   │   │           │   ├── LightmapMixin.java
│   │   │           │   ├── MouseHandlerMixin.java
│   │   │           │   ├── PotionHudMixin.java
│   │   │           │   └── WeatherMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   ├── hud/
│   │   │           │   │   │   ├── CoordinatesModule.java
│   │   │           │   │   │   ├── FpsModule.java
│   │   │           │   │   │   ├── HudModule.java
│   │   │           │   │   │   ├── MemoryUsageModule.java
│   │   │           │   │   │   ├── PingModule.java
│   │   │           │   │   │   └── PotionEffectsModule.java
│   │   │           │   │   ├── BlockOverlayModule.java
│   │   │           │   │   ├── EnchantEffectModule.java
│   │   │           │   │   ├── FreeCamModule.java
│   │   │           │   │   ├── FreeLookModule.java
│   │   │           │   │   ├── FullBrightModule.java
│   │   │           │   │   ├── ItemPhysicsModule.java
│   │   │           │   │   ├── KillAuraModule.java
│   │   │           │   │   ├── NoFallModule.java
│   │   │           │   │   ├── SprintModule.java
│   │   │           │   │   ├── TracersModule.java
│   │   │           │   │   └── WeatherChangerModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
│   │   │           ├── render/
│   │   │           │   ├── EnchantmentRenderer.java
│   │   │           │   ├── ItemPhysicsState.java
│   │   │           │   └── WorldRenderer.java
│   │   │           ├── setting/
│   │   │           │   ├── BooleanSetting.java
│   │   │           │   ├── ColorSetting.java
│   │   │           │   ├── DoubleSetting.java
│   │   │           │   ├── EnumSetting.java
│   │   │           │   └── Setting.java
│   │   │           ├── ui/
│   │   │           │   ├── animation/
│   │   │           │   │   ├── Animation.java
│   │   │           │   │   └── AnimationManager.java
│   │   │           │   ├── clickgui/
│   │   │           │   │   └── ClickGui.java
│   │   │           │   ├── component/
│   │   │           │   │   ├── setting/
│   │   │           │   │   │   ├── ColorPicker.java
│   │   │           │   │   │   ├── ColorSwatch.java
│   │   │           │   │   │   ├── EnumSelector.java
│   │   │           │   │   │   ├── Slider.java
│   │   │           │   │   │   └── ToggleSwitch.java
│   │   │           │   │   ├── ActionButton.java
│   │   │           │   │   ├── Icon.java
│   │   │           │   │   ├── ModuleButton.java
│   │   │           │   │   ├── Panel.java
│   │   │           │   │   ├── Sidebar.java
│   │   │           │   │   ├── TextField.java
│   │   │           │   │   ├── ThemeEditor.java
│   │   │           │   │   └── UIComponent.java
│   │   │           │   ├── font/
│   │   │           │   │   ├── BitmapPackFont.java
│   │   │           │   │   └── CustomTextRenderer.java
│   │   │           │   ├── hud/
│   │   │           │   │   ├── HudContent.java
│   │   │           │   │   ├── HudEditorScreen.java
│   │   │           │   │   ├── HudElement.java
│   │   │           │   │   ├── HudLayout.java
│   │   │           │   │   └── HudRenderer.java
│   │   │           │   ├── render/
│   │   │           │   │   └── UiMesh.java
│   │   │           │   └── theme/
│   │   │           │       ├── Theme.java
│   │   │           │       ├── ThemeManager.java
│   │   │           │       └── ThemeProfile.java
│   │   │           ├── util/
│   │   │           │   ├── AttackManager.java
│   │   │           │   ├── BlurUtil.java
│   │   │           │   ├── ColorUtil.java
│   │   │           │   ├── ItemUtil.java
│   │   │           │   ├── MathUtil.java
│   │   │           │   ├── RenderUtil.java
│   │   │           │   ├── RotationManager.java
│   │   │           │   └── TargetUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       ├── font/
│   │       │       │   ├── google_sans_medium.ttf
│   │       │       │   ├── google_sans_regular.ttf
│   │       │       │   ├── inter_medium.json
│   │       │       │   ├── inter_medium.ttf
│   │       │       │   ├── inter_regular.json
│   │       │       │   ├── inter_regular.ttf
│   │       │       │   ├── montserrat_medium.ttf
│   │       │       │   ├── montserrat_regular.ttf
│   │       │       │   ├── roboto_medium.json
│   │       │       │   ├── roboto_medium.ttf
│   │       │       │   ├── roboto_regular.json
│   │       │       │   └── roboto_regular.ttf
│   │       │       ├── lang/
│   │       │       │   └── en_us.json
│   │       │       └── shaders/
│   │       │           └── core/
│   │       │               ├── blur.fsh
│   │       │               ├── blur_box.fsh
│   │       │               ├── enchant.vsh
│   │       │               ├── enchant_glint.fsh
│   │       │               ├── enchant_outline.fsh
│   │       │               ├── font.fsh
│   │       │               ├── hsv.fsh
│   │       │               ├── shape.fsh
│   │       │               ├── ui.vsh
│   │       │               └── window.fsh
│   │       ├── licenses/
│   │       │   ├── google-sans-NOTICE.txt
│   │       │   ├── google-sans-OFL.txt
│   │       │   ├── inter-OFL.txt
│   │       │   ├── montserrat-OFL.txt
│   │       │   └── roboto-LICENSE.txt
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── CombatChecks.java
│                   ├── EngineChecks.java
│                   ├── FoundationChecks.java
│                   ├── Phase8Checks.java
│                   ├── RuntimeChecks.java
│                   ├── ThemeChecks.java
│                   └── UiChecks.java
├── .gitignore
├── AGENTS.md
├── FONT_FIX.md
├── FONT_SOURCES.md
├── PHASE3.md
├── PHASE4.md
├── PHASE5.md
├── PHASE6.md
├── PHASE7.md
├── PHASE8.md
├── README.md
├── UI_REDESIGN.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

## 2. Full code files

### src/main/java/dev/riserecode/Client.java

Updated file.

```java
package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.BindCommand;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.HudCommand;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.render.WorldRenderer;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "risecode";
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
    private RotationManager rotationManager;
    private AttackManager attackManager;
    private HudRenderer hudRenderer;
    private ThemeManager themeManager;
    private ClickGui clickGui;
    private volatile boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        friendManager = new FriendManager();
        worldRenderer = new WorldRenderer(eventBus);
        rotationManager = new RotationManager(eventBus, minecraft);
        attackManager = new AttackManager(eventBus, minecraft);
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft, friendManager, rotationManager, attackManager);
        themeManager = new ThemeManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), logger);
        themeManager.load(); themeManager.bind(eventBus); CustomTextRenderer.configure(themeManager);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME, themeManager);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, friendManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.bindHudStore(hudRenderer::snapshotLayouts, hudRenderer::applyLayouts, hudRenderer::validateLayouts);
        configManager.bindVisualPolicyStore(themeManager::policySnapshot, themeManager::applyPolicy, themeManager::validatePolicy);
        configManager.useExternalVisualPreferences(); configManager.load();
        if (!themeManager.wasLoaded()) {
            var oldUi = configManager.getUiPreferences();
            hudRenderer.setEnabled(configManager.getLegacyHudEnabled());
            try {
                if (oldUi.has("accent")) { ((dev.riserecode.setting.ColorSetting) themeManager.getSetting("Primary Accent")).setValue(oldUi.get("accent").getAsInt()); }
                if (oldUi.has("scale")) { ((dev.riserecode.setting.DoubleSetting) themeManager.getSetting("Interface Scale")).setValue(oldUi.get("scale").getAsDouble()); }
                themeManager.changed(); themeManager.save();
            } catch (RuntimeException invalid) { logger.warn("Could not migrate legacy UI preferences", invalid); }
        }
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
        commandManager.register(new HudCommand());
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        themeManager.close(); BlurUtil.close(); CustomTextRenderer.close();
        worldRenderer.close();
        configManager.close();
        moduleManager.disableAll();
        rotationManager.close();
        attackManager.close();
        Minecraft.getInstance().options.save();
        AnimationManager.ui().clear();
        initialized = false;
    }

    public void openHudEditor() { Minecraft.getInstance().gui.setScreen(new HudEditorScreen(hudRenderer)); }

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
    public RotationManager getRotationManager() { return rotationManager; }
    public AttackManager getAttackManager() { return attackManager; }
    public ThemeManager getThemeManager() { return themeManager; }
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

### src/main/java/dev/riserecode/command/commands/HudCommand.java

New file.

```java
package dev.riserecode.command.commands;

import dev.riserecode.Client;
import dev.riserecode.command.Command;

public final class HudCommand extends Command {
    public HudCommand() { super("hud", "Open the HUD editor", "hud", "hudedit"); }
    @Override public void execute(String[] args) {
        if (args.length != 0) { throw new IllegalArgumentException("Usage: .hud"); }
        Client.INSTANCE.openHudEditor();
    }
}
```

### src/main/java/dev/riserecode/config/ConfigManager.java

Updated file.

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
import dev.riserecode.module.Category;
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
import java.util.function.Supplier;
import org.slf4j.Logger;

/** Global preferences plus reusable, validated module snapshots for profiles. */
public final class ConfigManager implements AutoCloseable {
    private record ModuleState(Module module, boolean enabled, int keybind, Map<Setting<?>, JsonElement> settings) { }
    private record ProfileState(boolean hud, List<ModuleState> modules, JsonObject layouts, JsonObject visuals) { }
    private final Gson gson = new Gson();
    private final Path directory;
    private final Path file;
    private final ModuleManager modules;
    private final FriendManager friends;
    private boolean externalVisualPreferences;
    private final BooleanSupplier hudEnabled;
    private final Consumer<Boolean> setHudEnabled;
    private final Logger logger;
    private JsonObject document = new JsonObject();
    private Supplier<JsonObject> hudSnapshot = JsonObject::new, visualSnapshot = JsonObject::new;
    private Consumer<JsonObject> applyHud = value -> { }, validateHud = value -> { }, applyVisual = value -> { }, validateVisual = value -> { };
    public void bindHudStore(Supplier<JsonObject> snapshot, Consumer<JsonObject> apply, Consumer<JsonObject> validate) {
        hudSnapshot = snapshot; applyHud = apply; validateHud = validate;
    }
    public void bindVisualPolicyStore(Supplier<JsonObject> snapshot, Consumer<JsonObject> apply, Consumer<JsonObject> validate) {
        visualSnapshot = snapshot; applyVisual = apply; validateVisual = validate;
    }
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

    /** Phase 7 clients persist HUD/theme choices independently; legacy consumers retain compatibility. */
    public void useExternalVisualPreferences() { externalVisualPreferences = true; }
    public boolean getLegacyHudEnabled() {
        JsonElement value = document.get("hudEnabled");
        return value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean() || value.getAsBoolean();
    }
    public Path getDirectory() { return directory; }
    public Path getFile() { return file; }
    public String getActiveProfile() {
        JsonElement value = document.get("activeProfile");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) { return null; }
        String name = value.getAsString(); return name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}") ? name : null;
    }
    public void setActiveProfile(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}")) { throw new IllegalArgumentException("Invalid active profile name"); }
        document.addProperty("activeProfile", name); requestSave();
    }
    /** Editor saves touch only HUD modules/layout and visual policy in the selected profile. */
    public JsonObject withCurrentHudState(JsonObject profile) {
        parseProfile(profile);
        JsonObject result = profile.deepCopy(); result.addProperty("schemaVersion", 3);
        result.add("hudLayout", hudSnapshot.get()); result.add("visualPolicy", visualSnapshot.get());
        JsonObject entries = result.getAsJsonObject("modules");
        for (Module module : modules.getModules()) {
            if (module.getCategory() == Category.HUD) { entries.add(module.getName(), moduleSnapshot(module, objectOrEmpty(entries.get(module.getName())))); }
        }
        return result;
    }
    /** UI preferences are global; loading a combat profile never moves the window or changes its theme. */
    public JsonObject getUiPreferences() { return objectOrEmpty(document.get("ui")).deepCopy(); }
    public void setUiPreferences(JsonObject preferences) {
        document.add("ui", preferences.deepCopy()); requestSave();
    }
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
        if (document.has("hudLayout")) { apply("HUD layout", () -> { JsonObject layouts = requiredObject(document.get("hudLayout")); validateHud.accept(layouts); applyHud.accept(layouts); }); }
        if (!externalVisualPreferences && document.has("hudEnabled")) { apply("hudEnabled", () -> setHudEnabled.accept(booleanValue(document.get("hudEnabled")))); }
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
        result.addProperty("schemaVersion", 3);
        result.add("hudLayout", hudSnapshot.get()); result.add("visualPolicy", visualSnapshot.get());
        if (!externalVisualPreferences) { result.addProperty("hudEnabled", hudEnabled.getAsBoolean()); }
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
            next.addProperty("schemaVersion", 3);
            next.add("hudLayout", hudSnapshot.get());
            if (externalVisualPreferences) { next.remove("hudEnabled"); }
            else { next.addProperty("hudEnabled", hudEnabled.getAsBoolean()); }
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
            if (version < 1 || version > 3) { throw new IllegalArgumentException("Unsupported profile schema " + version); }
        }
        JsonObject entries = requiredObject(data.get("modules"));
        boolean hud = !externalVisualPreferences && data.has("hudEnabled") ? booleanValue(data.get("hudEnabled")) : true;
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
        JsonObject layouts = data.has("hudLayout") ? requiredObject(data.get("hudLayout")).deepCopy() : null;
        JsonObject visuals = data.has("visualPolicy") ? requiredObject(data.get("visualPolicy")).deepCopy() : null;
        if (layouts != null) { validateHud.accept(layouts); } if (visuals != null) { validateVisual.accept(visuals); }
        return new ProfileState(hud, states, layouts, visuals);
    }

    private void commit(ProfileState state) {
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), false)) { throw new IllegalStateException("Could not disable " + entry.module().getName()); }
        }
        for (ModuleState entry : state.modules()) {
            entry.settings().forEach(ConfigManager::loadSetting); modules.setKeybind(entry.module(), entry.keybind());
        }
        if (state.layouts() != null) { applyHud.accept(state.layouts()); }
        if (state.visuals() != null) { applyVisual.accept(state.visuals()); }
        if (!externalVisualPreferences) { setHudEnabled.accept(state.hud()); }
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

Updated file.

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
        try { JsonStorage.write(file, config.profileSnapshot()); config.setActiveProfile(name); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save profile {}", name, failure); return false; }
    }
    public boolean loadProfile(String name) {
        Path file = getFile(name);
        if (!Files.isRegularFile(file)) { return false; }
        try { config.applyProfile(JsonStorage.read(file)); }
        catch (IOException | RuntimeException failure) { logger.error("Could not load profile {}", name, failure); return false; }
        config.setActiveProfile(name); refreshUi.run();
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
    public String getActiveProfile() { return config.getActiveProfile(); }
    public boolean saveActiveHudLayout() {
        String name = getActiveProfile();
        if (name == null || !Files.isRegularFile(getFile(name))) { return true; }
        try { JsonStorage.write(getFile(name), config.withCurrentHudState(JsonStorage.read(getFile(name)))); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save HUD layout to profile {}", name, failure); return false; }
    }
}
```

### src/main/java/dev/riserecode/event/CameraEvent.java

New file.

```java
package dev.riserecode.event;

import net.minecraft.world.phys.Vec3;

/** Applied after vanilla camera alignment, before frustum and projection extraction. */
public final class CameraEvent extends Event {
    private Vec3 position;
    private float yaw, pitch;
    private boolean detached;
    public CameraEvent(Vec3 position, float yaw, float pitch, boolean detached) { this.position = position; this.yaw = yaw; this.pitch = pitch; this.detached = detached; }
    public Vec3 position() { return position; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public boolean detached() { return detached; }
    public void set(Vec3 position, float yaw, float pitch) { this.position = position; this.yaw = yaw; this.pitch = pitch; detached = true; }
}
```

### src/main/java/dev/riserecode/event/CameraInputEvent.java

New file.

```java
package dev.riserecode.event;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public final class CameraInputEvent extends Event {
    private final Input keys;
    private final Vec2 movement;
    public CameraInputEvent(Input keys, Vec2 movement) { super(true); this.keys = keys; this.movement = movement; }
    public Input keys() { return keys; }
    public Vec2 movement() { return movement; }
}
```

### src/main/java/dev/riserecode/event/EnchantmentRenderEvent.java

New file.

```java
package dev.riserecode.event;

import net.minecraft.world.item.ItemDisplayContext;

/** Native item renderer asks enabled modules for an immutable visual policy. */
public final class EnchantmentRenderEvent extends Event {
    public record Style(boolean outline, int outlineColor, double intensity, boolean overlay, int overlayColor, double speed, double scale) { }
    private final ItemDisplayContext context;
    private Style style;
    public EnchantmentRenderEvent(ItemDisplayContext context) { this.context = context; }
    public ItemDisplayContext context() { return context; }
    public Style style() { return style; }
    public void style(Style value) { style = value; }
}
```

### src/main/java/dev/riserecode/event/ItemRenderEvent.java

New file.

```java
package dev.riserecode.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;

public final class ItemRenderEvent extends Event {
    private final ItemEntityRenderState state;
    private final PoseStack poses;
    private final SubmitNodeCollector collector;
    public ItemRenderEvent(ItemEntityRenderState state, PoseStack poses, SubmitNodeCollector collector) { super(true); this.state = state; this.poses = poses; this.collector = collector; }
    public ItemEntityRenderState state() { return state; }
    public PoseStack poses() { return poses; }
    public SubmitNodeCollector collector() { return collector; }
}
```

### src/main/java/dev/riserecode/event/MouseRotationEvent.java

New file.

```java
package dev.riserecode.event;

public final class MouseRotationEvent extends Event {
    private final double yaw, pitch;
    public MouseRotationEvent(double yaw, double pitch) { super(true); this.yaw = yaw; this.pitch = pitch; }
    public double yawDelta() { return yaw * 0.15; }
    public double pitchDelta() { return pitch * 0.15; }
}
```

### src/main/java/dev/riserecode/event/Render3DEvent.java

Updated file.

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
    public record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        public Quad {
            for (Vec3 point : List.of(a, b, c, d)) {
                if (!Line.finite(point)) { throw new IllegalArgumentException("Quad coordinates must be finite"); }
            }
        }
    }
    private final List<Quad> quads = new ArrayList<>();
    public void addQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) { quads.add(new Quad(a, b, c, d, color)); }
    public List<Quad> getQuads() { return List.copyOf(quads); }
    private final LevelExtractionContext context;
    private final List<Line> lines = new ArrayList<>();
    public Render3DEvent(LevelExtractionContext context) { this.context = Objects.requireNonNull(context); }
    public LevelExtractionContext getContext() { return context; }
    public void addLine(Vec3 from, Vec3 to, int color, float width) { lines.add(new Line(from, to, color, width)); }
    public List<Line> getLines() { return List.copyOf(lines); }
}
```

### src/main/java/dev/riserecode/keybind/KeybindManager.java

Updated file.

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
            if (screen == clickGui) { clickGui.toggleVisibility(); }
            else { clickGui.open(screen); }
            event.cancel();
            return;
        }
        if (screen != null || minecraft.player == null) { return; }
        for (Module module : modules.getModules()) {
            if (module.getKeybind() != Module.UNBOUND && module.getKeybind() == event.getKey()) {
                if (!(module instanceof dev.riserecode.module.modules.FreeLookModule)) { modules.toggle(module); }
            }
        }
    }

    @Override
    public void close() { subscription.close(); }
}
```

### src/main/java/dev/riserecode/mixin/AnimationFrameMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class AnimationFrameMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void riserecode$animationFrame(CallbackInfo callback) {
        long now = System.nanoTime(); AnimationManager.ui().frame(now); Theme.advance(now);
    }
}
```

### src/main/java/dev/riserecode/mixin/CameraMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CameraEvent;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow public abstract Vec3 position();
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(Vec3 position);
    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void riserecode$camera(float partialTick, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        CameraEvent event = new CameraEvent(position(), yRot(), xRot(), detached);
        Client.INSTANCE.getEventBus().post(event);
        setPosition(event.position()); setRotation(event.yaw(), event.pitch()); detached = event.detached();
    }
}
```

### src/main/java/dev/riserecode/mixin/EnchantmentRendererMixin.java

New file.

```java
package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import dev.riserecode.Client;
import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.render.EnchantmentRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public abstract class EnchantmentRendererMixin extends RenderTypeFeatureRenderer<ItemFeatureRenderer.Submit> {
    @Unique private ItemDisplayContext riserecode$context = ItemDisplayContext.NONE;
    @Inject(method = "prepareMainSubmit", at = @At("TAIL"))
    private void riserecode$outline(ItemFeatureRenderer.Submit submit, CallbackInfo callback) {
        if (submit.foilType() == ItemStackRenderState.FoilType.NONE || !Client.INSTANCE.isInitialized()) { return; }
        EnchantmentRenderEvent event = new EnchantmentRenderEvent(submit.displayContext()); Client.INSTANCE.getEventBus().post(event);
        if (event.style() != null) { EnchantmentRenderer.outline(submit, event.style(), this::getVertexBuilder); }
    }
    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"))
    private void riserecode$foilContext(ItemFeatureRenderer.Submit submit, CallbackInfo callback) { riserecode$context = submit.displayContext(); }
    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private void riserecode$foil(RenderType type, PoseStack.Pose pose, CallbackInfoReturnable<VertexConsumer> callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        EnchantmentRenderEvent event = new EnchantmentRenderEvent(riserecode$context); Client.INSTANCE.getEventBus().post(event);
        if (event.style() != null && event.style().overlay()) {
            VertexConsumer buffer = EnchantmentRenderer.tint(getVertexBuilder(EnchantmentRenderer.glintType(event.style(), type)), event.style().overlayColor());
            if (pose != null) { buffer = new SheetedDecalTextureGenerator(buffer, pose, 0.0078125F); }
            callback.setReturnValue(buffer);
        }
    }
}
```

### src/main/java/dev/riserecode/mixin/GammaOptionAccessor.java

New file.

```java
package dev.riserecode.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Direct value access only for the gamma option owned by FullBright; vanilla validation is unchanged. */
@Mixin(OptionInstance.class)
public interface GammaOptionAccessor {
    @Accessor("value") void riserecode$setRawValue(Object value);
}
```

### src/main/java/dev/riserecode/mixin/ItemEntityRendererMixin.java

New file.

```java
package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("RETURN"))
    private void riserecode$physicsState(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo callback) {
        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        float angle = (float) ((seed & 0xFFFF) / 65536.0 * Math.PI * 2);
        ((ItemPhysicsState) state).riserecode$physics(entity.onGround(), angle);
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void riserecode$physicsSubmit(ItemEntityRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        ItemRenderEvent event = new ItemRenderEvent(state, poses, collector); Client.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) { callback.cancel(); }
    }
}
```

### src/main/java/dev/riserecode/mixin/ItemPhysicsStateMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemPhysicsStateMixin implements ItemPhysicsState {
    @Unique private boolean riserecode$ground;
    @Unique private float riserecode$angle;
    @Override public boolean riserecode$onGround() { return riserecode$ground; }
    @Override public float riserecode$rotation() { return riserecode$angle; }
    @Override public void riserecode$physics(boolean ground, float rotation) { riserecode$ground = ground; riserecode$angle = rotation; }
}
```

### src/main/java/dev/riserecode/mixin/KeyboardInputMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.CameraInputEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("RETURN"))
    private void riserecode$cameraInput(CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        CameraInputEvent event = new CameraInputEvent(keyPresses, moveVector); Client.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) { keyPresses = Input.EMPTY; moveVector = Vec2.ZERO; }
    }
}
```

### src/main/java/dev/riserecode/mixin/LightmapMixin.java

New file.

```java
package dev.riserecode.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.Client;
import dev.riserecode.module.modules.FullBrightModule;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Inject(method = "getTextureView", at = @At("RETURN"), cancellable = true)
    private void riserecode$lightmap(CallbackInfoReturnable<GpuTextureView> callback) {
        if (Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class)
                    .ifPresent(module -> callback.setReturnValue(module.lightmap(callback.getReturnValue())));
        }
    }
}
```

### src/main/java/dev/riserecode/mixin/MouseHandlerMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.MouseRotationEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void riserecode$mouseRotation(LocalPlayer player, double yaw, double pitch) {
        MouseRotationEvent event = new MouseRotationEvent(yaw, pitch);
        if (Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(event); }
        if (!event.isCancelled()) { player.turn(yaw, pitch); }
    }
}
```

### src/main/java/dev/riserecode/mixin/PotionHudMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.hud.PotionEffectsModule;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class PotionHudMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void riserecode$customEffects(CallbackInfo callback) {
        if (Client.INSTANCE.isInitialized() && Client.INSTANCE.getModuleManager().getByClass(PotionEffectsModule.class)
                .map(PotionEffectsModule::hidesVanilla).orElse(false)) { callback.cancel(); }
    }
}
```

### src/main/java/dev/riserecode/mixin/WeatherMixin.java

New file.

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.WeatherChangerModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class WeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void riserecode$rain(float partial, CallbackInfoReturnable<Float> callback) { override(callback, false); }
    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void riserecode$thunder(float partial, CallbackInfoReturnable<Float> callback) { override(callback, true); }
    private void override(CallbackInfoReturnable<Float> callback, boolean thunder) {
        if ((Object) this instanceof ClientLevel && Client.INSTANCE.isInitialized()) {
            Client.INSTANCE.getModuleManager().getByClass(WeatherChangerModule.class).filter(WeatherChangerModule::isEnabled)
                    .ifPresent(module -> callback.setReturnValue(thunder ? module.thunder() : module.rain()));
        }
    }
}
```

### src/main/java/dev/riserecode/module/Category.java

Updated file.

```java
package dev.riserecode.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World"),
    UTILITY("Utility"),
    HUD("HUD");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### src/main/java/dev/riserecode/module/ModuleManager.java

Updated file.

```java
package dev.riserecode.module;

import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.KillAuraModule;
import dev.riserecode.module.modules.TracersModule;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.module.modules.FreeLookModule;
import dev.riserecode.module.modules.FreeCamModule;
import dev.riserecode.module.modules.EnchantEffectModule;
import dev.riserecode.module.modules.ItemPhysicsModule;
import dev.riserecode.module.modules.WeatherChangerModule;
import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.hud.FpsModule;
import dev.riserecode.module.modules.hud.PingModule;
import dev.riserecode.module.modules.hud.CoordinatesModule;
import dev.riserecode.module.modules.hud.PotionEffectsModule;
import dev.riserecode.module.modules.hud.MemoryUsageModule;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
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

    public void initDefaults(Minecraft minecraft, FriendManager friends, RotationManager rotations, AttackManager attacks) {
        register(new FullBrightModule(eventBus, minecraft));
        register(new SprintModule(eventBus, minecraft));
        register(new KillAuraModule(eventBus, minecraft, friends, rotations, attacks));
        register(new TracersModule(eventBus, minecraft, friends));
        register(new NoFallModule(eventBus, minecraft));
        register(new FpsModule(eventBus, minecraft)); register(new PingModule(eventBus, minecraft));
        register(new CoordinatesModule(eventBus, minecraft)); register(new PotionEffectsModule(eventBus, minecraft));
        register(new MemoryUsageModule(eventBus, minecraft));
        register(new FreeLookModule(eventBus, minecraft)); register(new FreeCamModule(eventBus, minecraft));
        register(new EnchantEffectModule(eventBus)); register(new ItemPhysicsModule(eventBus));
        register(new WeatherChangerModule(eventBus)); register(new BlockOverlayModule(eventBus, minecraft));
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

### src/main/java/dev/riserecode/module/modules/BlockOverlayModule.java

New file.

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockOverlayModule extends Module {
    private final Minecraft minecraft;
    private final BooleanSetting fill = register(new BooleanSetting("OverlayEnabled", "Draw filled block shape", true));
    private final ColorSetting fillColor = register(new ColorSetting("FillColor", "Overlay ARGB color", 0x803B82F6));
    private final DoubleSetting fillOpacity = register(new DoubleSetting("FillOpacity", "Additional fill opacity", 0.4, 0, 1));
    private final BooleanSetting outline = register(new BooleanSetting("OutlineEnabled", "Draw shape edges", true));
    private final ColorSetting outlineColor = register(new ColorSetting("OutlineColor", "Edge ARGB color", 0xFF59D5CB));
    private final DoubleSetting width = register(new DoubleSetting("OutlineWidth", "Edge pixels", 2, 0.5, 5));
    private final DoubleSetting outlineOpacity = register(new DoubleSetting("OutlineOpacity", "Additional edge opacity", 1, 0, 1));
    public BlockOverlayModule(EventBus bus, Minecraft mc) {
        super("BlockOverlay", "Independent fill and outline for the targeted block shape.", Category.RENDER, bus); minecraft = mc;
        fillColor.setVisibleWhen(fill::getValue); fillOpacity.setVisibleWhen(fill::getValue);
        outlineColor.setVisibleWhen(outline::getValue); width.setVisibleWhen(outline::getValue); outlineOpacity.setVisibleWhen(outline::getValue);
    }
    @Override protected void onEnable() { listen(Render3DEvent.class, this::extract); }
    private void extract(Render3DEvent event) {
        if (minecraft.level == null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) { return; }
        var position = hit.getBlockPos(); var shape = minecraft.level.getBlockState(position).getShape(minecraft.level, position);
        for (AABB bounds : shape.toAabbs()) { box(event, bounds.move(position).inflate(0.002)); }
    }
    private void box(Render3DEvent event, AABB b) {
        Vec3[] v = {new Vec3(b.minX,b.minY,b.minZ), new Vec3(b.maxX,b.minY,b.minZ), new Vec3(b.maxX,b.maxY,b.minZ), new Vec3(b.minX,b.maxY,b.minZ),
                new Vec3(b.minX,b.minY,b.maxZ), new Vec3(b.maxX,b.minY,b.maxZ), new Vec3(b.maxX,b.maxY,b.maxZ), new Vec3(b.minX,b.maxY,b.maxZ)};
        if (fill.getValue()) {
            int color = Theme.opacity(fillColor.getValue(), fillOpacity.getValue());
            for (int[] face : new int[][] {{0,3,2,1},{4,5,6,7},{0,1,5,4},{3,7,6,2},{0,4,7,3},{1,2,6,5}}) { event.addQuad(v[face[0]],v[face[1]],v[face[2]],v[face[3]],color); }
        }
        if (outline.getValue()) {
            int color = Theme.opacity(outlineColor.getValue(), outlineOpacity.getValue());
            for (int[] edge : new int[][] {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}}) {
                event.addLine(v[edge[0]], v[edge[1]], color, width.getValue().floatValue());
            }
        }
    }
}
```

### src/main/java/dev/riserecode/module/modules/EnchantEffectModule.java

New file.

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.world.item.ItemDisplayContext;

public final class EnchantEffectModule extends Module {
    private final BooleanSetting outline = register(new BooleanSetting("Outline", "Soft enchanted-item silhouette", true));
    private final ColorSetting outlineColor = register(new ColorSetting("OutlineColor", "Outline color", 0xFF59D5CB));
    private final DoubleSetting intensity = register(new DoubleSetting("OutlineIntensity", "Glow opacity", 0.7, 0, 1));
    private final BooleanSetting scene = register(new BooleanSetting("WorldItems", "Include frames and dropped items", false));
    private final BooleanSetting overlay = register(new BooleanSetting("Overlay", "Customize the enchantment glint", true));
    private final ColorSetting overlayColor = register(new ColorSetting("OverlayColor", "Glint color", 0xB0D946EF));
    private final DoubleSetting speed = register(new DoubleSetting("GlintSpeed", "Glint motion multiplier", 1, 0, 5));
    private final DoubleSetting scale = register(new DoubleSetting("GlintScale", "Glint texture scale", 1, 0.1, 4));
    public EnchantEffectModule(EventBus bus) {
        super("EnchantEffect", "Soft item outlines and custom animated enchantment glint.", Category.RENDER, bus);
        outlineColor.setVisibleWhen(outline::getValue); intensity.setVisibleWhen(outline::getValue); scene.setVisibleWhen(outline::getValue);
        overlayColor.setVisibleWhen(overlay::getValue); speed.setVisibleWhen(overlay::getValue); scale.setVisibleWhen(overlay::getValue);
    }
    @Override protected void onEnable() {
        listen(EnchantmentRenderEvent.class, event -> {
            boolean world = event.context() == ItemDisplayContext.GROUND || event.context() == ItemDisplayContext.FIXED;
            event.style(new EnchantmentRenderEvent.Style(outline.getValue() && (event.context().firstPerson() || scene.getValue() && world),
                    outlineColor.getValue(), intensity.getValue(), overlay.getValue(), overlayColor.getValue(), speed.getValue(), scale.getValue()));
        });
    }
}
```

### src/main/java/dev/riserecode/module/modules/FreeCamModule.java

New file.

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.CameraEvent;
import dev.riserecode.event.CameraInputEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Camera-only flight: no dummy entity, player teleport, packet replacement or server camera change. */
public final class FreeCamModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting speed = register(new DoubleSetting("Speed", "Camera blocks per tick", 0.5, 0.05, 5));
    private final BooleanSetting vertical = register(new BooleanSetting("VerticalMovement", "Jump/sneak moves vertically", true));
    private final BooleanSetting rotate = register(new BooleanSetting("Rotate", "Mouse rotates the detached camera", true));
    private LocalPlayer player;
    private ClientLevel level;
    private Vec3 position, previous;
    private float yaw, pitch;
    public FreeCamModule(EventBus bus, Minecraft mc) { super("FreeCam", "Client-side detached fly camera.", Category.RENDER, bus); minecraft = mc; }
    @Override protected void onEnable() {
        capture();
        listen(UpdateEvent.class, ignored -> { if (minecraft.player == null || minecraft.player != player || minecraft.level != level) { capture(); } });
        listen(CameraInputEvent.class, event -> {
            if (player == null || minecraft.gui.screen() != null) { return; }
            previous = position;
            double radians = Math.toRadians(yaw), forward = event.movement().y, left = event.movement().x;
            double dy = vertical.getValue() ? (event.keys().jump() ? 1 : 0) - (event.keys().shift() ? 1 : 0) : 0;
            Vec3 direction = new Vec3(-Math.sin(radians) * forward + Math.cos(radians) * left, dy,
                    Math.cos(radians) * forward + Math.sin(radians) * left);
            if (direction.lengthSqr() > 1) { direction = direction.normalize(); }
            position = position.add(direction.scale(speed.getValue())); event.cancel();
        });
        listen(MouseRotationEvent.class, 200, false, event -> {
            if (player == null) { return; }
            if (rotate.getValue()) { yaw += (float) event.yawDelta(); pitch = Math.clamp(pitch + (float) event.pitchDelta(), -90, 90); }
            event.cancel();
        });
        listen(CameraEvent.class, 200, false, event -> {
            if (player != null && position != null && minecraft.player == player) {
                float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                event.set(previous.lerp(position, partial), yaw, pitch);
            }
        });
    }
    private void capture() {
        player = minecraft.player; level = minecraft.level;
        if (player != null) { position = previous = player.getEyePosition(); yaw = player.getYRot(); pitch = player.getXRot(); }
        else { position = previous = null; }
    }
    public Vec3 cameraPosition() { return position; }
    @Override protected void onDisable() { player = null; level = null; position = previous = null; }
}
```

### src/main/java/dev/riserecode/module/modules/FreeLookModule.java

New file.

```java
package dev.riserecode.module.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.CameraEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Hold-to-look changes camera angles only; body rotation stays under vanilla control. */
public final class FreeLookModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting holdKey = register(new DoubleSetting("HoldKey", "GLFW key; .bind FreeLook also assigns the hold key", InputConstants.KEY_LALT, 32, 348));
    private final DoubleSetting sensitivity = register(new DoubleSetting("Sensitivity", "Mouse multiplier", 1, 0.1, 3));
    private final BooleanSetting perspective = register(new BooleanSetting("Perspective", "Use third-person while held", true));
    private CameraType originalView;
    private boolean held;
    private float yaw, pitch;
    public FreeLookModule(EventBus bus, Minecraft mc) { super("FreeLook", "Hold a key to look without rotating your body.", Category.PLAYER, bus); minecraft = mc; }
    @Override protected void onEnable() {
        listen(UpdateEvent.class, ignored -> {
            boolean pressed = minecraft.player != null && minecraft.gui.screen() == null && InputConstants.isKeyDown(minecraft.getWindow(),
                    getKeybind() == UNBOUND ? (int) Math.round(holdKey.getValue()) : getKeybind());
            if (pressed && !held) {
                held = true; yaw = minecraft.player.getYRot(); pitch = minecraft.player.getXRot(); originalView = minecraft.options.getCameraType();
                if (perspective.getValue()) { minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
            } else if (!pressed && held) { release(); }
        });
        listen(MouseRotationEvent.class, 100, false, event -> {
            if (held && !event.isCancelled()) { yaw += (float) (event.yawDelta() * sensitivity.getValue());
                pitch = Math.clamp(pitch + (float) (event.pitchDelta() * sensitivity.getValue()), -90, 90); event.cancel(); }
        });
        listen(CameraEvent.class, 100, false, event -> {
            if (held && !dev.riserecode.Client.INSTANCE.getModuleManager().getByClass(FreeCamModule.class).map(Module::isEnabled).orElse(false)) {
                boolean mirrored = minecraft.options.getCameraType().isMirrored();
                float cameraYaw = mirrored ? yaw + 180 : yaw;
                float cameraPitch = mirrored ? -pitch : pitch;
                Vec3 position = event.position();
                if (!minecraft.options.getCameraType().isFirstPerson() && minecraft.level != null) {
                    float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    Vec3 eye = minecraft.player.getEyePosition(partial);
                    double yr = Math.toRadians(cameraYaw), pr = Math.toRadians(cameraPitch);
                    Vec3 backward = new Vec3(Math.sin(yr) * Math.cos(pr), Math.sin(pr), -Math.cos(yr) * Math.cos(pr)).scale(4);
                    Vec3 requested = eye.add(backward);
                    var hit = minecraft.level.clip(new ClipContext(eye, requested, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
                    double distance = Math.min(4, Math.max(0, eye.distanceTo(hit.getLocation()) - 0.1));
                    position = eye.add(backward.normalize().scale(distance));
                }
                event.set(position, cameraYaw, cameraPitch);
            }
        });
    }
    public boolean isHeld() { return held; }
    private void release() { if (originalView != null) { minecraft.options.setCameraType(originalView); } held = false; originalView = null; }
    @Override protected void onDisable() { release(); }
}
```

### src/main/java/dev/riserecode/module/modules/FullBrightModule.java

Updated file.

```java
package dev.riserecode.module.modules;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.mixin.GammaOptionAccessor;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

/** Substitutes a white client lightmap while vanilla keeps updating its own untouched texture. */
public final class FullBrightModule extends Module {
    public enum Mode { LIGHTMAP, GAMMA }
    private final Minecraft minecraft;
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Lighting implementation", Mode.LIGHTMAP));
    private final DoubleSetting brightness = register(new DoubleSetting("Brightness", "Gamma multiplier up to 1000", 1, 0, 1));
    private Double previousGamma;
    private GpuTexture texture;
    private GpuTextureView view;
    public FullBrightModule(EventBus bus, Minecraft minecraft) {
        super("FullBright", "Illuminates unlit areas using a client lightmap override.", Category.RENDER, bus);
        this.minecraft = minecraft; brightness.setVisibleWhen(() -> mode.getValue() == Mode.GAMMA);
    }
    public DoubleSetting getBrightness() { return brightness; }
    public EnumSetting<Mode> getMode() { return mode; }
    @Override protected void onEnable() { applyGamma(); listen(UpdateEvent.class, ignored -> applyGamma()); }
    private void applyGamma() {
        if (minecraft.options == null) { return; }
        if (previousGamma == null) { previousGamma = minecraft.options.gamma().get(); }
        rawGamma(mode.getValue() == Mode.GAMMA ? brightness.getValue() * 1000 : previousGamma);
    }
    private void rawGamma(double value) { ((GammaOptionAccessor) (Object) minecraft.options.gamma()).riserecode$setRawValue(value); }
    /** Called from the native lightmap texture binding, never during client bootstrap. */
    public GpuTextureView lightmap(GpuTextureView original) {
        if (!isEnabled() || mode.getValue() != Mode.LIGHTMAP || minecraft.level == null) { return original; }
        if (view == null) {
            texture = RenderSystem.getDevice().createTexture(() -> "risecode fullbright lightmap",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, 16, 16, 1, 1);
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(1, 1, 1, 1));
            view = RenderSystem.getDevice().createTextureView(texture);
        }
        return view;
    }
    @Override protected void onDisable() {
        if (previousGamma != null && minecraft.options != null) { rawGamma(previousGamma); previousGamma = null; }
        if (view != null) { view.close(); view = null; }
        if (texture != null) { texture.close(); texture = null; }
    }
}
```

### src/main/java/dev/riserecode/module/modules/ItemPhysicsModule.java

New file.

```java
package dev.riserecode.module.modules;

import com.mojang.math.Axis;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.util.RandomSource;

/** Pure render transforms: item positions, pickup behavior and server physics remain vanilla. */
public final class ItemPhysicsModule extends Module {
    private final RandomSource random = RandomSource.create();
    public ItemPhysicsModule(EventBus bus) { super("ItemPhysics", "Dropped items rest on the ground with stable individual rotations.", Category.WORLD, bus); }
    @Override protected void onEnable() {
        listen(ItemRenderEvent.class, event -> {
            var state = event.state(); if (state.item.isEmpty()) { return; }
            var physics = (ItemPhysicsState) state; var bounds = state.item.getModelBoundingBox(); var poses = event.poses();
            boolean flat = bounds.getZsize() <= 0.0625;
            poses.pushPose();
            try {
                double height = flat ? bounds.getZsize() / 2 + 0.025 : -bounds.minY + 0.02;
                poses.translate(0, height, 0); poses.mulPose(Axis.YP.rotation(physics.riserecode$rotation()));
                if (flat) { poses.mulPose(Axis.XP.rotationDegrees(90)); }
                if (!physics.riserecode$onGround()) { poses.mulPose(Axis.ZP.rotation((state.ageInTicks * 0.06f) % ((float) Math.PI * 2))); }
                ItemEntityRenderer.submitMultipleFromCount(poses, event.collector(), state.lightCoords, state, random);
                event.cancel();
            } finally { poses.popPose(); }
        });
    }
}
```

### src/main/java/dev/riserecode/module/modules/WeatherChangerModule.java

New file.

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.EnumSetting;

/** Read-only local weather override; the server's weather and world fields are never changed. */
public final class WeatherChangerModule extends Module {
    public enum Weather { CLEAR, RAIN, THUNDER }
    private final EnumSetting<Weather> weather = register(new EnumSetting<>("Weather", "Local weather", Weather.CLEAR));
    public WeatherChangerModule(EventBus bus) { super("WeatherChanger", "Client-side weather and sky override.", Category.WORLD, bus); }
    public float rain() { return weather.getValue() == Weather.CLEAR ? 0 : 1; }
    public float thunder() { return weather.getValue() == Weather.THUNDER ? 1 : 0; }
}
```

### src/main/java/dev/riserecode/module/modules/hud/CoordinatesModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class CoordinatesModule extends HudModule {
    private final ColorSetting x = register(new ColorSetting("XColor", "X axis", 0xFFFF879C));
    private final ColorSetting y = register(new ColorSetting("YColor", "Y axis", 0xFF8CE8B5));
    private final ColorSetting z = register(new ColorSetting("ZColor", "Z axis", 0xFF83BEFF));
    private final DoubleSetting precision = register(new DoubleSetting("Precision", "Decimal places", 1, 0, 3));
    private final BooleanSetting direction = register(new BooleanSetting("Direction", "Show direction letter", true));
    public CoordinatesModule(EventBus bus, Minecraft mc) { super("Coordinates", "Live position and cardinal direction.", bus, mc); }
    @Override public List<HudContent.Line> content() {
        var p = minecraft.player; String format = "% ." + (int) Math.round(precision.getValue()) + "f";
        String dx = String.format(Locale.ROOT, format, p == null ? 0 : p.getX());
        String dy = String.format(Locale.ROOT, format, p == null ? 0 : p.getY());
        String dz = String.format(Locale.ROOT, format, p == null ? 0 : p.getZ());
        String facing = direction.getValue() && p != null ? "  " + p.getDirection().getName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        return List.of(new HudContent.Line(List.of(new HudContent.Span("X" + dx + "  ", x.getValue(), false),
                new HudContent.Span("Y" + dy + "  ", y.getValue(), false), new HudContent.Span("Z" + dz + facing, z.getValue(), false)), null, -1, 0));
    }
}
```

### src/main/java/dev/riserecode/module/modules/hud/FpsModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class FpsModule extends HudModule {
    public enum ColorMode { STATIC, THRESHOLD, GRADIENT }
    private final EnumSetting<ColorMode> colorMode = register(new EnumSetting<>("ColorMode", "Text color policy", ColorMode.GRADIENT));
    private final ColorSetting color = register(new ColorSetting("Color", "Static text color", 0xFFF0F2F3));
    private final BooleanSetting suffix = register(new BooleanSetting("Suffix", "Show FPS suffix", true));
    public FpsModule(EventBus bus, Minecraft mc) { super("FPS", "Current rendered frames per second.", bus, mc); }
    @Override public List<HudContent.Line> content() {
        int fps = minecraft.getFps();
        int tint = colorMode.getValue() == ColorMode.THRESHOLD ? fps >= 120 ? 0xFF59D5A2 : fps >= 60 ? 0xFFFFC857 : 0xFFFF647C : color.getValue();
        return List.of(HudContent.Line.text(fps + (suffix.getValue() ? " FPS" : ""), tint, colorMode.getValue() == ColorMode.GRADIENT));
    }
}
```

### src/main/java/dev/riserecode/module/modules/hud/HudModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {
    protected final Minecraft minecraft;
    protected HudModule(String name, String description, EventBus bus, Minecraft minecraft) {
        super(name, description, Category.HUD, bus); this.minecraft = minecraft;
    }
    public abstract List<HudContent.Line> content();
}
```

### src/main/java/dev/riserecode/module/modules/hud/MemoryUsageModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class MemoryUsageModule extends HudModule {
    public enum BarColorMode { STATIC, THRESHOLD, GRADIENT }
    private final BooleanSetting showBar = register(new BooleanSetting("ShowBar", "Display memory usage bar", true));
    private final EnumSetting<BarColorMode> colorMode = register(new EnumSetting<>("BarColorMode", "Bar color policy", BarColorMode.GRADIENT));
    private final ColorSetting color = register(new ColorSetting("BarColor", "Static bar color", 0xFF59D5CB));
    public MemoryUsageModule(EventBus bus, Minecraft mc) { super("MemoryUsage", "Used and maximum Java heap memory.", bus, mc); }
    @Override public List<HudContent.Line> content() {
        Runtime runtime = Runtime.getRuntime(); long used = runtime.totalMemory() - runtime.freeMemory(), max = runtime.maxMemory();
        double fraction = used / (double) max;
        int tint = switch (colorMode.getValue()) { case STATIC -> color.getValue(); case GRADIENT -> Theme.accent();
            case THRESHOLD -> fraction < 0.6 ? 0xFF59D5A2 : fraction < 0.85 ? 0xFFFFC857 : 0xFFFF647C; };
        return List.of(new HudContent.Line(List.of(new HudContent.Span(used / 1048576 + " / " + max / 1048576 + " MB", 0xFFF0F2F3, false)), null,
                showBar.getValue() ? fraction : -1, tint));
    }
}
```

### src/main/java/dev/riserecode/module/modules/hud/PingModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class PingModule extends HudModule {
    private final DoubleSetting good = register(new DoubleSetting("GoodThreshold", "Good latency upper bound", 70, 0, 1000));
    private final DoubleSetting medium = register(new DoubleSetting("MediumThreshold", "Medium latency upper bound", 150, 0, 2000));
    private final ColorSetting goodColor = register(new ColorSetting("GoodColor", "Good latency", 0xFF59D5A2));
    private final ColorSetting mediumColor = register(new ColorSetting("MediumColor", "Medium latency", 0xFFFFC857));
    private final ColorSetting badColor = register(new ColorSetting("BadColor", "High latency", 0xFFFF647C));
    public PingModule(EventBus bus, Minecraft mc) { super("Ping", "Server latency in milliseconds.", bus, mc); }
    @Override public List<HudContent.Line> content() {
        var connection = minecraft.getConnection();
        var info = connection == null || minecraft.player == null ? null : connection.getPlayerInfo(minecraft.player.getUUID());
        int ping = info == null ? 0 : info.getLatency();
        return List.of(HudContent.Line.text(info == null ? "— ms" : ping + " ms", ping <= good.getValue() ? goodColor.getValue()
                : ping <= Math.max(good.getValue(), medium.getValue()) ? mediumColor.getValue() : badColor.getValue(), false));
    }
}
```

### src/main/java/dev/riserecode/module/modules/hud/PotionEffectsModule.java

New file.

```java
package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;

public final class PotionEffectsModule extends HudModule {
    public enum Mode { ICONS, TEXT }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Effect presentation", Mode.TEXT));
    private final BooleanSetting hideVanilla = register(new BooleanSetting("HideVanilla", "Hide vanilla effect HUD", true));
    private final BooleanSetting duration = register(new BooleanSetting("Duration", "Display remaining time", true));
    public PotionEffectsModule(EventBus bus, Minecraft mc) { super("PotionEffects", "Custom active status effects display.", bus, mc); }
    public boolean hidesVanilla() { return isEnabled() && hideVanilla.getValue(); }
    @Override public List<HudContent.Line> content() {
        List<HudContent.Line> result = new ArrayList<>();
        if (minecraft.player != null) {
            for (var effect : minecraft.player.getActiveEffects().stream().sorted(java.util.Comparator.comparing(e -> e.getEffect().value().getDisplayName().getString())).toList()) {
                int ticks = effect.getDuration(), seconds = Math.max(0, ticks / 20);
                String time = effect.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                String name = mode.getValue() == Mode.TEXT ? effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1) : "";
                if (duration.getValue()) { name += (name.isEmpty() ? "" : " · ") + time; }
                result.add(new HudContent.Line(List.of(new HudContent.Span(name, 0xFFF0F2F3, false)),
                        mode.getValue() == Mode.ICONS ? Hud.getMobEffectSprite(effect.getEffect()) : null, -1, 0));
            }
        }
        return result.isEmpty() ? List.of(HudContent.Line.text("No active effects", 0xFF7F868E, false)) : result;
    }
}
```

### src/main/java/dev/riserecode/render/EnchantmentRenderer.java

New file.

```java
package dev.riserecode.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import dev.riserecode.event.EnchantmentRenderEvent.Style;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Native item meshes: four translucent inverted hulls produce a soft silhouette glow. */
public final class EnchantmentRenderer {
    private record Key(Identifier atlas, OutputTarget output) { }
    private static final Map<Key, RenderType> OUTLINES = new HashMap<>();
    private static final Map<OutputTarget, RenderType> FOILS = new HashMap<>();
    private static Style glintStyle;
    private static final class Pipelines {
        private static final RenderPipeline OUTLINE = pipeline("enchant_outline", true);
        private static final RenderPipeline GLINT = pipeline("enchant_glint", false);
    }
    private EnchantmentRenderer() { }
    private static RenderPipeline pipeline(String name, boolean hull) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(UiMesh.id("pipeline/" + name)).withVertexShader(UiMesh.id("core/enchant"))
                .withFragmentShader(UiMesh.id("core/" + name)).withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0).withCull(hull)
                .withColorTargetState(new ColorTargetState(hull ? BlendFunction.TRANSLUCENT : BlendFunction.GLINT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(hull ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.EQUAL, false)).build());
    }
    public static RenderType glintType(Style style, RenderType original) {
        glintStyle = style;
        boolean transparent = net.minecraft.client.Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                && original.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
        OutputTarget output = (transparent ? RenderTypes.glintTranslucent() : RenderTypes.glint()).outputTarget();
        return FOILS.computeIfAbsent(output, target -> RenderType.create("risecode_glint", RenderSetup.builder(Pipelines.GLINT)
                .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM).setOutputTarget(target)
                .setTextureTransform(new TextureTransform("risecode glint", EnchantmentRenderer::glintMatrix)).createRenderSetup()));
    }
    private static Matrix4f glintMatrix() {
        Style style = glintStyle; double t = Theme.gradientTime() * (style == null ? 1 : style.speed());
        return new Matrix4f().translation((float) (-t * 0.025 % 1), (float) (t * 0.04 % 1), 0)
                .rotateZ(0.19f).scale((float) (8 * (style == null ? 1 : style.scale())));
    }
    public static void outline(ItemFeatureRenderer.Submit submit, Style style, Function<RenderType, VertexConsumer> buffers) {
        if (!style.outline() || style.intensity() <= 0 || submit.quads().isEmpty()) { return; }
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (BakedQuad quad : submit.quads()) { for (int v = 0; v < 4; v++) { min.min(quad.position(v)); max.max(quad.position(v)); } }
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        for (int layer = 3; layer >= 0; layer--) {
            float expand = 1.015f + layer * 0.018f; int color = Theme.opacity(style.outlineColor(), style.intensity() * (0.28 - layer * 0.055));
            for (BakedQuad quad : submit.quads()) {
                Key key = new Key(quad.materialInfo().sprite().atlasLocation(), quad.materialInfo().itemRenderType().outputTarget());
                RenderType type = OUTLINES.computeIfAbsent(key, k -> RenderType.create("risecode_item_glow", RenderSetup.builder(Pipelines.OUTLINE)
                        .withTexture("Sampler0", k.atlas()).setOutputTarget(k.output()).sortOnUpload().createRenderSetup()));
                VertexConsumer vertices = buffers.apply(type);
                for (int index = 3; index >= 0; index--) {
                    Vector3f position = new Vector3f(quad.position(index)).sub(center).mul(expand).add(center);
                    long uv = quad.packedUV(index);
                    vertices.addVertex(submit.pose(), position).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv)).setColor(color);
                }
            }
        }
    }
    public static VertexConsumer tint(VertexConsumer target, int color) {
        return new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { target.addVertex(x,y,z); target.setColor(color); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(color); return this; }
            @Override public VertexConsumer setColor(int value) { target.setColor(color); return this; }
            @Override public VertexConsumer setUv(float u, float v) { target.setUv(u,v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }
        };
    }
}
```

### src/main/java/dev/riserecode/render/ItemPhysicsState.java

New file.

```java
package dev.riserecode.render;

/** Primitive extraction metadata; the delayed render never reads the live item entity. */
public interface ItemPhysicsState {
    boolean riserecode$onGround();
    float riserecode$rotation();
    void riserecode$physics(boolean onGround, float rotation);
}
```

### src/main/java/dev/riserecode/render/WorldRenderer.java

Updated file.

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
    private volatile List<Render3DEvent.Quad> quads = List.of();
    private volatile List<Render3DEvent.Line> lines = List.of();

    public WorldRenderer(EventBus bus) {
        subscription = bus.subscribe(Render3DEvent.class, Integer.MIN_VALUE, false, event -> { lines = event.getLines(); quads = event.getQuads(); });
    }
    public List<Render3DEvent.Line> getLines() { return lines; }
    public List<Render3DEvent.Quad> getQuads() { return quads; }

    public void submit(LevelRenderContext context) {
        List<Render3DEvent.Line> snapshot = lines;
        List<Render3DEvent.Quad> faces = quads;
        if (!faces.isEmpty()) {
            Vec3 eye = context.levelState().cameraRenderState.pos;
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugQuads(), (pose, vertices) -> {
                for (Render3DEvent.Quad face : faces) {
                    for (Vec3 point : List.of(face.a(), face.b(), face.c(), face.d())) {
                        Vec3 relative = point.subtract(eye); vertices.addVertex(pose, (float) relative.x, (float) relative.y, (float) relative.z).setColor(face.color());
                    }
                }
            });
        }
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
    public void close() { subscription.close(); lines = List.of(); quads = List.of(); }
}
```

### src/main/java/dev/riserecode/ui/animation/Animation.java

Updated file.

```java
package dev.riserecode.ui.animation;

import dev.riserecode.util.MathUtil;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A render-thread animation with explicit delta time, measured in seconds.
 * Retargeting starts at the current rendered value, preserving position.
 * UI instances are scheduled centrally; ordinary instances accept explicit updates.
 */
public final class Animation {
    public enum State {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private static DoubleSupplier uiSpeed = () -> 1;
    private static Supplier<MathUtil.Easing> uiCurve = () -> null;
    private boolean themed;
    private MathUtil.Easing activeCurve;
    private double segmentDuration;
    public static void configureUi(DoubleSupplier speed, Supplier<MathUtil.Easing> curve) {
        uiSpeed = Objects.requireNonNull(speed); uiCurve = Objects.requireNonNull(curve);
    }
    /** Ordinary animations stay deterministic; only UI instances adopt theme preferences. */
    public static Animation ui(double initialValue, double duration, MathUtil.Easing easing) {
        Animation result = new Animation(initialValue, duration, easing); result.themed = true; return result;
    }
    private final double durationSeconds;
    private final MathUtil.Easing easing;
    private double value;
    private double startValue;
    private double targetValue;
    private double elapsedSeconds;
    private State state = State.IDLE;

    public Animation(double initialValue, double durationSeconds, MathUtil.Easing easing) {
        requireFinite(initialValue, "Initial value");
        requireFinite(durationSeconds, "Duration");
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        this.durationSeconds = durationSeconds;
        this.easing = Objects.requireNonNull(easing, "easing");
        activeCurve = easing; segmentDuration = durationSeconds;
        value = startValue = targetValue = initialValue;
    }

    /** Repeated requests for the same target do not restart the animation. */
    public Animation animateTo(double target) {
        requireFinite(target, "Target");
        if (!Double.isFinite(target - value)) {
            throw new IllegalArgumentException("Animation range must be finite");
        }
        if (target == targetValue) {
            return this;
        }
        startValue = value;
        segmentDuration = durationSeconds; activeCurve = selectedCurve();
        targetValue = target;
        elapsedSeconds = 0.0;
        state = State.RUNNING;
        if (themed) { AnimationManager.ui().register(this); }
        if (durationSeconds == 0.0 || startValue == targetValue) {
            finish();
        }
        return this;
    }

    public double update(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0) { throw new IllegalArgumentException("Delta time cannot be negative"); }
        if (themed && AnimationManager.ui().isDriving()) { return value; }
        return advance(deltaSeconds);
    }

    double advance(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Delta time cannot be negative");
        }
        if (state != State.RUNNING) {
            return value;
        }
        MathUtil.Easing selected = selectedCurve();
        if (activeCurve != selected) {
            segmentDuration = Math.max(0, segmentDuration - elapsedSeconds);
            startValue = value; elapsedSeconds = 0; activeCurve = selected;
        }
        double speed = themed ? Math.max(0.5, Math.min(3, uiSpeed.getAsDouble())) : 1;
        elapsedSeconds = Math.min(segmentDuration, elapsedSeconds + deltaSeconds * speed);
        // Avoid an extra frame caused only by accumulated floating-point error.
        double tolerance = Math.min(4.0 * Math.ulp(segmentDuration), segmentDuration * 1.0e-12);
        if (segmentDuration - elapsedSeconds <= tolerance) {
            finish();
        } else {
            value = MathUtil.lerp(startValue, targetValue, activeCurve.apply(getProgress()));
        }
        return value;
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            if (themed) { AnimationManager.ui().unregister(this); }
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            if (themed) { AnimationManager.ui().register(this); }
        }
    }

    public void finish() {
        value = targetValue;
        elapsedSeconds = segmentDuration;
        state = State.FINISHED;
        if (themed) { AnimationManager.ui().unregister(this); }
    }

    /** Immediately establishes a new idle value and discards the old target. */
    public void reset(double initialValue) {
        requireFinite(initialValue, "Initial value");
        value = startValue = targetValue = initialValue;
        elapsedSeconds = 0.0;
        state = State.IDLE;
        if (themed) { AnimationManager.ui().unregister(this); }
    }

    public double getValue() {
        return value;
    }

    public double getTarget() {
        return targetValue;
    }

    public double getProgress() {
        if (segmentDuration == 0.0) {
            return state == State.FINISHED ? 1.0 : 0.0;
        }
        return elapsedSeconds / segmentDuration;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    private MathUtil.Easing selectedCurve() {
        MathUtil.Easing selected = themed ? uiCurve.get() : null;
        return selected == null ? easing : selected;
    }
    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
```

### src/main/java/dev/riserecode/ui/animation/AnimationManager.java

New file.

```java
package dev.riserecode.ui.animation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Independent state machines advanced once per frame. No worker or graphics threads. */
public final class AnimationManager {
    private static final AnimationManager UI = new AnimationManager();
    private final Set<Animation> active = ConcurrentHashMap.newKeySet();
    private long previousFrame;
    private boolean driving;
    public static AnimationManager ui() { return UI; }
    public void register(Animation animation) { active.add(animation); }
    public void unregister(Animation animation) { active.remove(animation); }
    public int activeCount() { return active.size(); }
    public boolean isDriving() { return driving; }
    public void frame(long now) {
        double delta = previousFrame == 0 ? 0 : Math.max(0, Math.min(0.25, (now - previousFrame) * 1e-9));
        previousFrame = now; driving = true; update(delta);
    }
    /** Explicit-delta entrypoint also supports deterministic concurrency checks. */
    public void update(double delta) {
        if (!Double.isFinite(delta) || delta < 0) { throw new IllegalArgumentException("Invalid frame delta"); }
        for (Animation animation : active) {
            animation.advance(delta);
            if (!animation.isRunning()) { active.remove(animation); }
        }
    }
    public void clear() { active.clear(); previousFrame = 0; driving = false; }
}
```

### src/main/java/dev/riserecode/ui/clickgui/ClickGui.java

Updated file.

```java
package dev.riserecode.ui.clickgui;

import com.google.gson.JsonObject;
import dev.riserecode.Client;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.Icon;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.Sidebar;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.ThemeEditor;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native 26.2 screen hosting an original, fully extracted sidebar/card interface. */
public final class ClickGui extends Screen {
    public enum Page { SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, HUD, PROFILES, FRIENDS, THEMES, LANGUAGE }
    private static final double WINDOW_WIDTH = 428;
    private static final double WINDOW_HEIGHT = 320;
    private final List<Panel> panels = new ArrayList<>();
    private final Panel searchPanel;
    private final Sidebar sidebar;
    private final TextField field = new TextField(this::placeholder);
    private final ToggleSwitch hudToggle;
    private final ActionButton editHud = new ActionButton("Edit HUD", () -> Client.INSTANCE.openHudEditor());
    private final Runnable saveChanges;
    private final List<ActionButton> actions = new ArrayList<>();
    private final Animation opening = Animation.ui(0, 0.26, MathUtil.Easing.OUT_QUART);
    private final Animation canvasZoom = Animation.ui(1, 0.24, MathUtil.Easing.OUT_QUART);
    private final Animation pageFade = Animation.ui(1, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation positionX = Animation.ui(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation positionY = Animation.ui(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation utilityScroll = Animation.ui(0, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation feedback = Animation.ui(0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
    private Screen parent;
    private Page page = Page.SEARCH;
    private UIComponent captured;
    private final ThemeManager themes;
    private final ThemeEditor themeEditor;
    private boolean closing;
    private boolean dragging;
    private double dragX;
    private double dragY;
    private double offsetX;
    private double offsetY;
    private double windowX;
    private double windowY;
    private double canvasScale = 1;
    private double preferredScale = 1;
    private double canvasWidth;
    private double canvasHeight;
    private double maxUtilityScroll;
    private long previousFrame;
    private double messageTime;
    private String message = "";

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal(Client.NAME));
        this.saveChanges = saveChanges;
        themes = Client.INSTANCE.getThemeManager();
        themeEditor = new ThemeEditor(themes, this::notice);
        searchPanel = new Panel(null, modules, saveChanges);
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
        sidebar = new Sidebar(List.of(
                new Sidebar.Item("Search", Icon.SEARCH), new Sidebar.Item("Combat", Icon.COMBAT),
                new Sidebar.Item("Movement", Icon.MOVEMENT), new Sidebar.Item("Player", Icon.PLAYER),
                new Sidebar.Item("Render", Icon.RENDER), new Sidebar.Item("World", Icon.WORLD),
                new Sidebar.Item("Utility", Icon.UTILITY), new Sidebar.Item("HUD", Icon.HUD), new Sidebar.Item("Profiles", Icon.PROFILES),
                new Sidebar.Item("Friends", Icon.FRIENDS), new Sidebar.Item("Themes", Icon.THEMES),
                new Sidebar.Item("Language", Icon.LANGUAGE)), index -> selectPage(Page.values()[index]));
        hudToggle = new ToggleSwitch("HUD", hud::isEnabled, next -> { hud.setEnabled(next); saveChanges.run(); });
        loadPreferences();
    }
    public List<Panel> getPanels() { return List.copyOf(panels); }
    public Panel getActivePanel() {
        if (page == Page.SEARCH) { return searchPanel; }
        if (page.ordinal() > Page.HUD.ordinal()) { return null; }
        Category category = Category.valueOf(page.name());
        return panels.stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow();
    }
    public ModuleButton getModuleButton(String name) {
        Panel panel = getActivePanel();
        if (panel == null) { throw new IllegalStateException("The selected page has no modules"); }
        return panel.getModuleButtons().stream().filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    public ThemeEditor getThemeEditor() { return themeEditor; }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public List<ActionButton> getActionButtons() { return List.copyOf(actions); }
    public ActionButton getEditHudButton() { return editHud; }
    public Sidebar getSidebar() { return sidebar; }
    public TextField getSearchField() { return field; }
    public Page getPage() { return page; }
    public double getCanvasScale() { return canvasScale; }
    public boolean isOpen() { return !closing && !opening.isRunning() && opening.getValue() == 1; }
    public UIComponent.Bounds getWindowBounds() { return new UIComponent.Bounds(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT); }

    private void loadPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        try {
            if (ui.has("german")) { Theme.setGerman(ui.get("german").getAsBoolean()); }
            preferredScale = themes.number("Interface Scale");
            if (ui.has("offsetX")) { offsetX = finite(ui.get("offsetX").getAsDouble(), -2000, 2000); }
            if (ui.has("offsetY")) { offsetY = finite(ui.get("offsetY").getAsDouble(), -2000, 2000); }
        } catch (RuntimeException invalid) { Client.INSTANCE.getLogger().warn("Ignoring invalid UI preference: {}", invalid.getMessage()); }
    }
    private static double finite(double value, double min, double max) {
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("Non-finite UI value"); }
        return Math.max(min, Math.min(max, value));
    }
    private void persistPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        ui.remove("accent"); ui.remove("scale"); ui.addProperty("german", Theme.isGerman());
        ui.addProperty("offsetX", offsetX); ui.addProperty("offsetY", offsetY);
        Client.INSTANCE.getConfigManager().setUiPreferences(ui);
    }
    public void refresh() {
        releaseCapture(); searchPanel.refresh(); panels.forEach(Panel::refresh);
        if (page == Page.PROFILES || page == Page.FRIENDS) { rebuildActions(); }
    }
    public void selectCategory(Category category) { selectPage(Page.valueOf(category.name())); }
    public void selectPage(Page next) {
        if (closing || next == page) { return; }
        releaseCapture(); page = next; sidebar.setSelected(next.ordinal());
        themeEditor.unfocus();
        field.setValue(""); field.setFocused(next == Page.SEARCH || next == Page.PROFILES || next == Page.FRIENDS);
        searchPanel.setQuery(""); utilityScroll.reset(0); pageFade.reset(0); pageFade.animateTo(1);
        Panel panel = getActivePanel(); if (panel != null) { panel.open(); }
        rebuildActions();
    }
    public void open(Screen parent) {
        this.parent = parent; closing = false; previousFrame = System.nanoTime();
        opening.reset(0); opening.animateTo(1); searchPanel.open(); panels.forEach(Panel::open);
        field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        minecraft.gui.setScreen(this);
    }
    @Override
    protected void init() {
        canvasScale = fittedScale(); canvasZoom.reset(canvasScale);
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        clampOffsets();
        windowX = (canvasWidth - WINDOW_WIDTH) / 2 + offsetX; windowY = (canvasHeight - WINDOW_HEIGHT) / 2 + offsetY;
        positionX.reset(windowX); positionY.reset(windowY); layout(); rebuildActions();
    }
    private double fittedScale() {
        return Math.max(0.1, Math.min(preferredScale * height / 576.0, Math.min((width - 20.0) / WINDOW_WIDTH, (height - 20.0) / WINDOW_HEIGHT)));
    }
    private void clampOffsets() {
        offsetX = Math.max(-(canvasWidth - WINDOW_WIDTH) / 2 + 8, Math.min((canvasWidth - WINDOW_WIDTH) / 2 - 8, offsetX));
        offsetY = Math.max(-(canvasHeight - WINDOW_HEIGHT) / 2 + 8, Math.min((canvasHeight - WINDOW_HEIGHT) / 2 - 8, offsetY));
    }
    private void layout() {
        sidebar.setBounds(windowX, windowY, 107, 320);
        double pad = themes.number("Content Padding");
        double left = windowX + 107 + pad, contentWidth = WINDOW_WIDTH - 107 - pad * 2;
        field.setBounds(left, windowY + pad - 1, contentWidth, 27);
        searchPanel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        for (Panel panel : panels) { panel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2); }
        themeEditor.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        hudToggle.setBounds(windowX + 129, windowY + 234, 265, 17);
    }
    private String placeholder() {
        return switch (page) {
            case SEARCH -> Theme.label("Start typing to search...");
            case PROFILES -> Theme.isGerman() ? "Profilname eingeben..." : "Enter a profile name...";
            case FRIENDS -> Theme.isGerman() ? "Spielername eingeben..." : "Enter a player name...";
            case HUD -> "HUD";
            default -> Theme.label(page.name().substring(0, 1) + page.name().substring(1).toLowerCase(java.util.Locale.ROOT));
        };
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) { extractPanorama(graphics, delta); }
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime(); double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9); previousFrame = now;
        preferredScale = themes.number("Interface Scale"); canvasZoom.animateTo(fittedScale());
        canvasScale = Math.max(0.1, canvasZoom.update(seconds));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        double alpha = opening.update(seconds);
        if (closing && !opening.isRunning()) { minecraft.gui.setScreen(parent); return; }
        clampOffsets();
        positionX.animateTo((canvasWidth - WINDOW_WIDTH) / 2 + offsetX);
        positionY.animateTo((canvasHeight - WINDOW_HEIGHT) / 2 + offsetY);
        windowX = positionX.update(seconds); windowY = positionY.update(seconds); layout();
        double pointerX = pointerX(mouseX); double pointerY = pointerY(mouseY);
        double scale = windowScale();
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        graphics.pose().translate((float) (windowX + WINDOW_WIDTH / 2), (float) (windowY + WINDOW_HEIGHT / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-windowX - WINDOW_WIDTH / 2), (float) (-windowY - WINDOW_HEIGHT / 2));
        try {
            RenderUtil.drawDropShadow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10,
                    Theme.opacity(Theme.SHADOW, alpha * 0.7));
            BlurUtil.drawGui(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10, alpha);
            RenderUtil.drawWindow(graphics, windowX, windowY, alpha);
            sidebar.render(graphics, font, pointerX, pointerY, seconds, alpha);
            double contentAlpha = alpha * pageFade.update(seconds);
            if (page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) {
                field.render(graphics, font, pointerX, pointerY, seconds, contentAlpha);
            } else {
                String title = placeholder();
                RenderUtil.text(graphics, font, title, windowX + 267.5 - RenderUtil.textWidth(font, title, false) / 2,
                        windowY + 18, 1, Theme.opacity(Theme.TEXT, contentAlpha));
            }
            if (page == Page.HUD) { editHud.setBounds(windowX + 352, windowY + 9, 64, 21); editHud.render(graphics, font, pointerX, pointerY, seconds, contentAlpha); }
            Panel panel = getActivePanel();
            if (panel != null) {
                if (page == Page.SEARCH) { panel.setQuery(field.getValue()); }
                panel.render(graphics, font, closing ? -1e6 : pointerX, closing ? -1e6 : pointerY, seconds, contentAlpha);
            } else { renderUtility(graphics, pointerX, pointerY, seconds, contentAlpha); }
            messageTime = Math.max(0, messageTime - seconds); feedback.animateTo(messageTime > 0 ? 1 : 0);
            double notice = feedback.update(seconds);
            if (notice > 0) {
                double noticeWidth = Math.min(292, RenderUtil.textWidth(font, message, false) * 0.8 + 18);
                RenderUtil.drawDropShadow(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SHADOW, alpha * notice));
                RenderUtil.drawRoundedRect(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SURFACE, alpha * notice));
                RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, message, (noticeWidth - 18) / 0.8), windowX + 128,
                        windowY + 296, 0.8, Theme.opacity(Theme.accent(), alpha * notice));
            }
        } finally { graphics.pose().popMatrix(); }
    }
    private void notice(String text) { message = text; messageTime = 3; }
    private void rebuildActions() {
        actions.clear();
        if (page == Page.PROFILES) {
            actions.add(new ActionButton("Save", () -> {
                try { notice(Client.INSTANCE.getProfileManager().saveProfile(field.getValue()) ? "Profile saved" : "Could not save profile"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (String profile : Client.INSTANCE.getProfileManager().getProfiles()) {
                actions.add(new ActionButton(profile, () -> notice(Client.INSTANCE.getProfileManager().loadProfile(profile)
                        ? "Loaded " + profile : "Could not load " + profile)));
            }
        } else if (page == Page.FRIENDS) {
            actions.add(new ActionButton("Add", () -> {
                try { Client.INSTANCE.getFriendManager().add(field.getValue()); field.setValue(""); notice("Friend added"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (var friend : Client.INSTANCE.getFriendManager().getFriends()) {
                actions.add(new ActionButton(friend.name() + "  ×", () -> { Client.INSTANCE.getFriendManager().remove(friend.name()); notice("Friend removed"); rebuildActions(); }));
            }
        } else if (page == Page.LANGUAGE) {
            actions.add(new ActionButton("English", () -> { Theme.setGerman(false); persistPreferences(); }));
            actions.add(new ActionButton("Deutsch", () -> { Theme.setGerman(true); persistPreferences(); }));
        }
    }
    private void renderUtility(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double seconds, double alpha) {
        double left = windowX + 115; double top = windowY + 38;
        if (page == Page.THEMES) {
            themeEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else if (page == Page.LANGUAGE) {
            card(graphics, left, top, 305, 84, "Interface language", "Module identifiers and commands keep their original names.", alpha);
            for (int i = 0; i < actions.size(); i++) {
                actions.get(i).setBounds(left + 8 + i * 145, top + 45, 137, 25);
                actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                if (Theme.isGerman() == (i == 1)) { RenderUtil.drawRoundedBorder(graphics, left + 8 + i * 145, top + 45, 137, 25, 5, Theme.opacity(Theme.accent(), alpha * 0.7)); }
            }
        } else {
            maxUtilityScroll = Math.max(0, actions.size() * 35.0 + 25 - 274);
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget())));
            double scroll = utilityScroll.update(seconds);
            RenderUtil.scissorPush(graphics, left, top, 305, 274);
            try {
                for (int i = 0; i < actions.size(); i++) {
                    actions.get(i).setBounds(left, top + i * 35 - scroll, 305, 27);
                    actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                }
                String hint = page == Page.PROFILES ? "Select a profile to load it." : "Select a friend to remove them.";
                RenderUtil.text(graphics, font, hint, left + 7, top + actions.size() * 35 - scroll + 5, 0.8, Theme.opacity(Theme.SECONDARY, alpha));
            } finally { RenderUtil.scissorPop(graphics); }
        }
    }
    private void card(GuiGraphicsExtractor graphics, double x, double y, double w, double h, String title, String description, double alpha) {
        RenderUtil.drawRoundedRect(graphics, x, y, w, h, 5, Theme.opacity(Theme.SURFACE, alpha));
        RenderUtil.text(graphics, font, title, x + 7, y + 7, 1, Theme.opacity(Theme.TEXT, alpha));
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, description, (w - 14) / 0.8), x + 7, y + 25, 0.8, Theme.opacity(Theme.SECONDARY, alpha * 0.8));
    }
    private double windowScale() { return MathUtil.lerp(0.94, 1, opening.getValue()); }
    private double pointerX(double mouseX) { double center = windowX + WINDOW_WIDTH / 2; return center + (mouseX / canvasScale - center) / windowScale(); }
    private double pointerY(double mouseY) { double center = windowY + WINDOW_HEIGHT / 2; return center + (mouseY / canvasScale - center) / windowScale(); }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || opening.getValue() < 0.5) { return true; }
        double x = pointerX(event.x()); double y = pointerY(event.y());
        if (page == Page.HUD && editHud.mouseClicked(x, y, event.button())) { return true; }
        if (sidebar.mouseClicked(x, y, event.button())) { return true; }
        if ((page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) && field.mouseClicked(x, y, event.button())) { return true; }
        if (x >= windowX && x < windowX + 107 && y >= windowY && y < windowY + 39 && event.button() == 0) {
            dragging = true; dragX = event.x() / canvasScale - windowX; dragY = event.y() / canvasScale - windowY; return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.mouseClicked(x, y, event.button())) { captured = panel; return true; }
        if (page == Page.THEMES && themeEditor.mouseClicked(x, y, event.button())) { captured = themeEditor; return true; }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            for (ActionButton action : List.copyOf(actions)) { if (action.mouseClicked(x, y, event.button())) { return true; } }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (closing) { return true; }
        if (dragging && event.button() == 0) {
            offsetX = event.x() / canvasScale - dragX - (canvasWidth - WINDOW_WIDTH) / 2;
            offsetY = event.y() / canvasScale - dragY - (canvasHeight - WINDOW_HEIGHT) / 2; clampOffsets(); return true;
        }
        return captured != null && captured.mouseDragged(pointerX(event.x()), pointerY(event.y()), event.button());
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean result = dragging || captured != null;
        if (captured != null) { captured.mouseReleased(pointerX(event.x()), pointerY(event.y()), event.button()); }
        if (dragging) { persistPreferences(); }
        dragging = false; captured = null; return result;
    }
    private void releaseCapture() { dragging = false; if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; } }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) { return true; }
        double x = pointerX(mouseX); double y = pointerY(mouseY);
        Panel panel = getActivePanel(); if (panel != null) { return panel.mouseScrolled(x, y, vertical); }
        if (page == Page.THEMES) { return themeEditor.mouseScrolled(x, y, vertical); }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget() - vertical * 24))); return true;
        }
        return false;
    }
    @Override
    public boolean charTyped(CharacterEvent event) { return closing || (page == Page.THEMES ? themeEditor.charTyped(event) : field.charTyped(event)); }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) { return true; }
        if (event.isEscape()) { onClose(); return true; }
        if (event.hasControlDown() && event.key() == 70) { selectPage(Page.SEARCH); field.setFocused(true); return true; }
        if (event.key() == 258) {
            int next = Math.floorMod(sidebar.getSelected() + (event.hasShiftDown() ? -1 : 1), Page.values().length);
            selectPage(Page.values()[next]); return true;
        }
        if (event.isConfirmation() && (page == Page.PROFILES || page == Page.FRIENDS) && !actions.isEmpty()) {
            var b = actions.getFirst().getBounds(); actions.getFirst().mouseClicked(b.centerX(), b.centerY(), 0); return true;
        }
        return page == Page.THEMES ? themeEditor.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)
                : field.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard);
    }
    /** Right Shift may reverse an exit before the screen is removed, preserving the rendered pose. */
    public void toggleVisibility() {
        if (closing) {
            closing = false; opening.animateTo(1);
            field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        } else { onClose(); }
    }
    @Override
    public void onClose() {
        if (closing) { return; }
        releaseCapture(); themeEditor.unfocus(); themes.save(); closing = true; field.setFocused(false); opening.animateTo(0); persistPreferences(); saveChanges.run();
    }
    @Override
    public void removed() { themeEditor.unfocus(); themes.save(); releaseCapture(); persistPreferences(); saveChanges.run(); }
    @Override
    public boolean isPauseScreen() { return false; }
}
```

### src/main/java/dev/riserecode/ui/component/Icon.java

Updated file.

```java
package dev.riserecode.ui.component;

import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original outline icons, drawn in a normalized 10x10 coordinate system. */
public enum Icon {
    SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, HUD, PROFILES, FRIENDS, THEMES, LANGUAGE;

    public void draw(GuiGraphicsExtractor graphics, double x, double y, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        switch (this) {
            case SEARCH -> { ring(graphics, 2, 0.5, 7, 7, color); segment(graphics, 3, 7, 0, 10, color); }
            case COMBAT -> {
                segment(graphics, 1, 0, 8, 8, color); segment(graphics, 9, 0, 2, 8, color);
                segment(graphics, 1, 7, 4, 10, color); segment(graphics, 6, 10, 9, 7, color);
                segment(graphics, 1, 0, 1, 3, color); segment(graphics, 9, 0, 9, 3, color);
            }
            case MOVEMENT -> {
                dot(graphics, 6.5, 0, color); segment(graphics, 5, 3, 6, 6, color);
                segment(graphics, 5, 3, 2, 5, color); segment(graphics, 5, 3, 8, 4, color);
                segment(graphics, 6, 6, 3, 9, color); segment(graphics, 6, 6, 8, 9, color);
            }
            case PLAYER, FRIENDS -> {
                ring(graphics, 0.5, 0.5, 9, 9, color); dot(graphics, 4, 2.5, color);
                segment(graphics, 3, 8, 3, 6, color); segment(graphics, 3, 6, 7, 6, color);
                segment(graphics, 7, 6, 7, 8, color);
            }
            case RENDER -> {
                segment(graphics, 0, 5, 3, 2.5, color); segment(graphics, 3, 2.5, 7, 2.5, color);
                segment(graphics, 7, 2.5, 10, 5, color); segment(graphics, 10, 5, 7, 7.5, color);
                segment(graphics, 7, 7.5, 3, 7.5, color); segment(graphics, 3, 7.5, 0, 5, color);
                dot(graphics, 4, 4, color);
            }
            case WORLD, LANGUAGE -> {
                ring(graphics, 0.5, 0.5, 9, 9, color); segment(graphics, 0.5, 5, 9.5, 5, color);
                segment(graphics, 4, 1, 3, 5, color); segment(graphics, 3, 5, 4, 9, color);
                segment(graphics, 6, 1, 7, 5, color); segment(graphics, 7, 5, 6, 9, color);
            }
            case UTILITY -> { ring(graphics, 0.5, 0.5, 9, 9, color); for (int i = 2; i <= 6; i += 2) { dot(graphics, i, 4, color); } }
            case HUD, PROFILES -> {
                segment(graphics, 0.5, 2, 4, 2, color); segment(graphics, 4, 2, 5, 1, color);
                segment(graphics, 5, 1, 9.5, 1, color); segment(graphics, 9.5, 1, 9.5, 9, color);
                segment(graphics, 9.5, 9, 0.5, 9, color); segment(graphics, 0.5, 9, 0.5, 2, color);
            }
            case THEMES -> {
                ring(graphics, 0.5, 0.5, 9, 9, color); dot(graphics, 3, 2, color);
                dot(graphics, 6, 3, color); dot(graphics, 2, 5, color); dot(graphics, 5, 6, color);
            }
        }
        graphics.pose().popMatrix();
    }
    private static void segment(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int c) {
        RenderUtil.line(g, x1, y1, x2, y2, 0.7, c);
    }
    private static void ring(GuiGraphicsExtractor g, double x, double y, double w, double h, int c) {
        RenderUtil.drawRoundedBorder(g, x, y, w, h, w / 2, c);
    }
    private static void dot(GuiGraphicsExtractor g, double x, double y, int c) {
        RenderUtil.drawRoundedRect(g, x, y, 1.5, 1.5, 0.75, c);
    }
}
```

### src/main/java/dev/riserecode/ui/component/Sidebar.java

Updated file.

```java
package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed quarter-width navigation with a compact animated selection pill. */
public final class Sidebar extends UIComponent {
    public record Item(String label, Icon icon) { }
    private static final double ROW_HEIGHT = 20.75;
    private final List<Item> items;
    private final IntConsumer selectedAction;
    private final List<Animation> hovers;
    private final Animation selectedY = Animation.ui(0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation selectedWidth = Animation.ui(52, 0.2, MathUtil.Easing.OUT_QUART);
    private int selected;
    public Sidebar(List<Item> items, IntConsumer selectedAction) {
        this.items = List.copyOf(items); this.selectedAction = selectedAction;
        hovers = items.stream().map(item -> Animation.ui(0, 0.16, MathUtil.Easing.IN_OUT_CUBIC)).toList();
    }
    public void setSelected(int index) { selected = index; selectedY.animateTo(index * ROW_HEIGHT); }
    public int getSelected() { return selected; }
    public Bounds getItemBounds(int index) { return new Bounds(x + 10, y + 43 + index * ROW_HEIGHT, width - 20, 17); }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double logoScale = Math.min(1.65, (width - 37) / Math.max(1, RenderUtil.textWidth(font, Client.NAME, false)));
        RenderUtil.text(graphics, font, Client.NAME, x + 14, y + 14, logoScale, Theme.opacity(Theme.TEXT, opacity));
        double versionX = x + 14 + RenderUtil.textWidth(font, Client.NAME, false) * logoScale + 1;
        RenderUtil.text(graphics, font, Client.INSTANCE.getVersion(), versionX, y + 12, 0.55,
                Theme.opacity(Theme.accent(), opacity * 0.85));
        String selectedLabel = Theme.label(items.get(selected).label());
        selectedWidth.animateTo(Math.min(width - 20, RenderUtil.textWidth(font, selectedLabel, false) * 0.88 + 28));
        double pillWidth = selectedWidth.update(seconds);
        double top = y + 43 + selectedY.update(seconds);
        RenderUtil.drawGlow(graphics, x + 10, top, pillWidth, 16, 4,
                Theme.opacity(Theme.accent(), opacity));
        if (Theme.manager() != null && Theme.manager().choice("Gradient Mode", dev.riserecode.util.ColorUtil.GradientMode.class) != dev.riserecode.util.ColorUtil.GradientMode.STATIC) {
            RenderUtil.drawAccentRoundedRect(graphics, x + 10, top, pillWidth, 16, 4, opacity);
        } else {
            RenderUtil.drawRoundedRect(graphics, x + 10, top, pillWidth, 16, 4,
                    Theme.opacity(Theme.lerp(Theme.accent(), 0xFF1AA99E, 0.2), opacity));
        }
        for (int i = 0; i < items.size(); i++) {
            Bounds row = getItemBounds(i);
            boolean inside = mouseX >= row.x() && mouseX < row.x() + row.width() && mouseY >= row.y() && mouseY < row.y() + row.height();
            hovers.get(i).animateTo(inside ? 1 : 0);
            double amount = hovers.get(i).update(seconds);
            if (i != selected) {
                RenderUtil.drawRoundedRect(graphics, row.x(), row.y(), Math.min(width - 20,
                        RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false) * 0.88 + 28), 16, 4,
                        Theme.opacity(0xFF314049, opacity * amount * 0.4));
            }
            int color = Theme.opacity(Theme.lerp(0xFFC9CFD1, Theme.TEXT, amount), opacity);
            items.get(i).icon().draw(graphics, x + 14 + amount * 0.65, row.y() + 3, color);
            double labelScale = Math.min(0.88, (width - 40) / Math.max(1, RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false)));
            RenderUtil.text(graphics, font, Theme.label(items.get(i).label()), x + 29 + amount * 0.65,
                    row.y() + 4, labelScale, color);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) { return false; }
        for (int i = 0; i < items.size(); i++) {
            Bounds b = getItemBounds(i);
            if (mouseX >= b.x() && mouseX < b.x() + b.width() && mouseY >= b.y() && mouseY < b.y() + b.height()) {
                selectedAction.accept(i); return true;
            }
        }
        return false;
    }
}
```

### src/main/java/dev/riserecode/ui/font/BitmapPackFont.java

Updated file.

```java
package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.system.MemoryUtil;

/** Reads the user's Qraftys resource pack without bundling its copyrighted artwork in the mod.
 * Original bitmap advances and small-cap lowercase glyphs are preserved. A linear-time Euclidean
 * distance transform creates a separate GPU atlas for live shadows/glow; text uses nearest sampling.
 */
final class BitmapPackFont implements AutoCloseable {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PAD = 48;
    private static final String[] ROWS = {
            "", "", " !\"#$%&'()*+,-./", "0123456789:;<=>?", "@ABCDEFGHIJKLMNO", "PQRSTUVWXYZ[\\]^_",
            "`abcdefghijklmno", "pqrstuvwxyz{|}~\0", "", "\0\0\0\0\0\0\0\0\0\0\0\0£\0\0ƒ",
            "\0\0\0\0\0\0ªº\0\0¬\0\0\0«»", "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐", "└┴┬├─┼╞╟╚╔╩╦╠═╬╧",
            "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀", "\0\0\0\0\0\0\0\0\0\0\0\0\0∅∈\0", "≡±≥≤⌠⌡÷≈°∙\0√ⁿ²■\0" };
    private record Glyph(int cell, int advance) { }
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final GpuTexture texture;
    private final GpuTextureView view;

    static Path find(Path configDirectory) {
        Path override = configDirectory.resolve("fonts/qraftys.zip");
        if (Files.isRegularFile(override)) { return override; }
        Path packs = configDirectory.toAbsolutePath().getParent().getParent().resolve("resourcepacks");
        Path canonical = packs.resolve("qraftys-capitalized-font.zip");
        if (Files.isRegularFile(canonical)) { return canonical; }
        if (!Files.isDirectory(packs)) { return null; }
        try (var paths = Files.list(packs)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.contains("qraft") && name.endsWith(".zip");
            }).sorted().findFirst().orElse(null);
        } catch (IOException ignored) { return null; }
    }

    BitmapPackFont(Path pack) throws IOException {
        ByteBuffer atlas = MemoryUtil.memCalloc(SIDE * SIDE);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry("assets/minecraft/textures/font/ascii.png");
            if (entry == null || entry.getSize() > 1024 * 1024) { throw new IOException("Missing/oversized Qraftys ascii.png"); }
            try (var stream = zip.getInputStream(entry); NativeImage image = NativeImage.read(stream)) {
                if (image.getWidth() != 128 || image.getHeight() != 128) { throw new IOException("Expected the Qraftys 16×16 bitmap grid"); }
                for (int row = 0; row < ROWS.length; row++) {
                    int[] codes = ROWS[row].codePoints().toArray();
                    for (int column = 0; column < codes.length; column++) {
                        int code = codes[column]; if (code == 0) { continue; }
                        int cell = row * 16 + column, extent = 0;
                        boolean[] mask = new boolean[CELL * CELL];
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                if ((image.getPixel(column * 8 + x, row * 8 + y) >>> 24) == 0) { continue; }
                                extent = Math.max(extent, x + 1);
                                for (int dy = 0; dy < DENSITY; dy++) {
                                    for (int dx = 0; dx < DENSITY; dx++) { mask[(PAD + y * DENSITY + dy) * CELL + PAD + x * DENSITY + dx] = true; }
                                }
                            }
                        }
                        glyphs.put(code, new Glyph(cell, code == ' ' ? 4 : extent + 1));
                        if (extent == 0) { continue; }
                        double[] inside = distances(mask, true), outside = distances(mask, false);
                        for (int y = 0; y < CELL; y++) {
                            for (int x = 0; x < CELL; x++) {
                                int index = y * CELL + x;
                                double distance = mask[index] ? Math.sqrt(outside[index]) - 0.5 : 0.5 - Math.sqrt(inside[index]);
                                int value = (int) Math.round(Math.max(0, Math.min(255, 128 + 2.5 * distance)));
                                atlas.put((cell / 16 * CELL + y) * SIDE + cell % 16 * CELL + x, (byte) value);
                            }
                        }
                    }
                }
            }
            texture = RenderSystem.getDevice().createTexture(() -> "risecode Qraftys bitmap SDF", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
            view = RenderSystem.getDevice().createTextureView(texture);
            try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> "risecode bitmap font upload", GpuBuffer.USAGE_COPY_SRC, atlas)) {
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, texture, 0, 0, SIDE, SIDE, 0, 0);
            }
        } finally { MemoryUtil.memFree(atlas); }
    }

    /** Squared distance to the nearest selected pixel: separable lower envelopes of parabolas. */
    private static double[] distances(boolean[] mask, boolean selected) {
        double[] result = new double[mask.length], input = new double[CELL], output = new double[CELL];
        int[] vertices = new int[CELL]; double[] boundaries = new double[CELL + 1];
        for (int x = 0; x < CELL; x++) {
            for (int y = 0; y < CELL; y++) { input[y] = mask[y * CELL + x] == selected ? 0 : 1e9; }
            distanceLine(input, output, vertices, boundaries);
            for (int y = 0; y < CELL; y++) { result[y * CELL + x] = output[y]; }
        }
        for (int y = 0; y < CELL; y++) {
            System.arraycopy(result, y * CELL, input, 0, CELL); distanceLine(input, output, vertices, boundaries);
            System.arraycopy(output, 0, result, y * CELL, CELL);
        }
        return result;
    }
    private static void distanceLine(double[] input, double[] output, int[] vertices, double[] boundaries) {
        int k = 0; vertices[0] = 0; boundaries[0] = Double.NEGATIVE_INFINITY; boundaries[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < CELL; q++) {
            double intersection;
            do {
                int p = vertices[k]; intersection = ((input[q] + q * q) - (input[p] + p * p)) / (2.0 * (q - p));
                if (intersection > boundaries[k]) { break; }
                k--;
            } while (k >= 0);
            k++; vertices[k] = q; boundaries[k] = intersection; boundaries[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < CELL; q++) {
            while (boundaries[k + 1] < q) { k++; }
            double delta = q - vertices[k]; output[q] = delta * delta + input[vertices[k]];
        }
    }
    private double advance(Font fallback, int code) {
        Glyph glyph = glyphs.get(code);
        return glyph == null ? fallback.width(Theme.text(new String(Character.toChars(code)), false)) : glyph.advance;
    }
    double width(Font fallback, String value) { return value.codePoints().mapToDouble(code -> advance(fallback, code)).sum(); }
    void draw(GuiGraphicsExtractor graphics, Font fallback, String value, double x, double y, int color, double blur, int mode, boolean gradient) {
        double cursor = 0, total = width(fallback, value);
        TextureSetup setup = TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(mode == 2 ? FilterMode.NEAREST : FilterMode.LINEAR));
        for (int code : value.codePoints().toArray()) {
            int tint = gradient ? Theme.opacity(Theme.gradientAt(graphics.pose().m20() + (x + cursor) * graphics.pose().m00(),
                    graphics.pose().m21() + y * graphics.pose().m11(), cursor / Math.max(1, total)), (color >>> 24) / 255.0) : color;
            Glyph glyph = glyphs.get(code);
            if (glyph == null) {
                CustomTextRenderer.drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + cursor, y, tint, blur);
            } else if (code != ' ') {
                float u = glyph.cell % 16 / 16f, v = glyph.cell / 16 / 16f;
                UiMesh.quad(graphics, CustomTextRenderer.fontPipeline(), setup, x + cursor - PAD / (double) DENSITY, y - PAD / (double) DENSITY,
                        CELL / (double) DENSITY, CELL / (double) DENSITY, u, v, u + 1 / 16f, v + 1 / 16f,
                        tint, tint, (int) Math.round(blur * 256), mode, 0, 0);
            }
            cursor += advance(fallback, code);
        }
    }
    @Override public void close() { view.close(); texture.close(); }
}
```

### src/main/java/dev/riserecode/ui/font/CustomTextRenderer.java

Updated file.

```java
package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Original STB TrueType renderer with filled, oversampled glyphs and fractional kerning.
 * Normal text uses an 8x coverage atlas; a separate distance field supplies soft shadows/glow.
 * Both single-channel atlases upload once per face, never during live theme edits.
 * Optional fonts are loaded from the resource directory or the user's config/fonts directory.
 */
public final class CustomTextRenderer {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PADDING = 48;
    private static final float DISTANCE_SCALE = 2.5f;
    private static final int COVERAGE_DENSITY = 8, COVERAGE_PADDING = 3;
    private static final String SYMBOLS = "…−›•×✓";
    private static final Map<String, Face> FACES = new HashMap<>();
    private static final Map<ThemeManager.Typeface, Boolean> AVAILABLE = new HashMap<>();
    private static BitmapPackFont bitmap;
    private static ThemeManager manager;
    private static final Map<String, String> FAILURES = new HashMap<>();
    private static final class Pipelines {
        private static final RenderPipeline FONT = UiMesh.pipeline("font", "font");
    }
    static RenderPipeline fontPipeline() { return Pipelines.FONT; }
    private record Glyph(int cell, int width, int height, int xOffset, int yOffset, double advance) { }
    private static final class Face implements AutoCloseable {
        private final ByteBuffer data;
        private final STBTTFontinfo info = STBTTFontinfo.malloc();
        private final float scale;
        private final double baseline;
        private final boolean pixel;
        private final Map<Integer, Glyph> glyphs = new HashMap<>();
        private final Map<Integer, Glyph> coverageGlyphs = new HashMap<>();
        private final Map<Long, Double> kerning = new HashMap<>();
        private GpuTexture texture;
        private GpuTextureView view;
        private GpuTexture coverageTexture;
        private GpuTextureView coverageView;
        private Face(byte[] bytes, boolean pixel) {
            validate(bytes);
            this.pixel = pixel;
            data = MemoryUtil.memAlloc(bytes.length); data.put(bytes).flip();
            if (!STBTruetype.stbtt_InitFont(info, data)) { info.free(); MemoryUtil.memFree(data); throw new IllegalArgumentException("Invalid TrueType font"); }
            scale = pixel ? STBTruetype.stbtt_ScaleForMappingEmToPixels(info, 12 * DENSITY)
                    : STBTruetype.stbtt_ScaleForPixelHeight(info, 11 * DENSITY);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var x0 = stack.mallocInt(1); var y0 = stack.mallocInt(1); var x1 = stack.mallocInt(1); var y1 = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointBitmapBox(info, 'H', scale, scale, x0, y0, x1, y1);
                baseline = -y0.get(0) / (double) DENSITY;
            }
        }
        private static void validate(byte[] bytes) {
            if (bytes.length < 12) { throw new IllegalArgumentException("Truncated font header"); }
            ByteBuffer header = ByteBuffer.wrap(bytes);
            int signature = header.getInt(0), tables = Short.toUnsignedInt(header.getShort(4));
            if (signature != 0x00010000 && signature != 0x4F54544F || tables == 0 || 12L + 16L * tables > bytes.length) {
                throw new IllegalArgumentException("Expected a TrueType/OpenType SFNT file");
            }
            for (int index = 0; index < tables; index++) {
                int entry = 12 + index * 16;
                long offset = Integer.toUnsignedLong(header.getInt(entry + 8)), length = Integer.toUnsignedLong(header.getInt(entry + 12));
                if (offset + length > bytes.length) { throw new IllegalArgumentException("Truncated font table"); }
            }
        }
        private double advance(int code) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var width = stack.mallocInt(1); var bearing = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointHMetrics(info, code, width, bearing);
                return width.get(0) * scale / DENSITY;
            }
        }
        private double kern(int previous, int next) {
            if (previous < 0) { return 0; }
            long key = ((long) previous << 32) | next;
            return kerning.computeIfAbsent(key, ignored -> STBTruetype.stbtt_GetCodepointKernAdvance(info, previous, next) * (double) scale / DENSITY);
        }
        private void upload() {
            if (coverageView != null) { return; }
            ByteBuffer distancePixels = MemoryUtil.memCalloc(SIDE * SIDE);
            ByteBuffer coveragePixels = MemoryUtil.memCalloc(SIDE * SIDE);
            try {
                int cell = 0;
                for (int code = 32; code < 256; code++) {
                    bake(distancePixels, code, cell); bakeCoverage(coveragePixels, code, cell++);
                }
                for (int code : SYMBOLS.codePoints().toArray()) {
                    bake(distancePixels, code, cell); bakeCoverage(coveragePixels, code, cell++);
                }
                texture = uploadAtlas(distancePixels, "risecode font effects");
                view = RenderSystem.getDevice().createTextureView(texture);
                coverageTexture = uploadAtlas(coveragePixels, "risecode filled font");
                coverageView = RenderSystem.getDevice().createTextureView(coverageTexture);
            } finally { MemoryUtil.memFree(distancePixels); MemoryUtil.memFree(coveragePixels); }
        }
        private static GpuTexture uploadAtlas(ByteBuffer pixels, String name) {
            GpuTexture atlas = RenderSystem.getDevice().createTexture(() -> name,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
            try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> name + " upload", GpuBuffer.USAGE_COPY_SRC, pixels)) {
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, atlas, 0, 0, SIDE, SIDE, 0, 0);
                return atlas;
            } catch (RuntimeException failure) { atlas.close(); throw failure; }
        }
        /** Filled coverage retains fine stems/counters which a coarse SDF can erase.
         * The transparent gutter prevents adjacent atlas cells bleeding into the glyph.
         */
        private void bakeCoverage(ByteBuffer pixels, int code, int cell) {
            if (STBTruetype.stbtt_FindGlyphIndex(info, code) == 0) { return; }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1); var h = stack.mallocInt(1); var xo = stack.mallocInt(1); var yo = stack.mallocInt(1);
                float rasterScale = scale * COVERAGE_DENSITY / DENSITY;
                ByteBuffer coverage = STBTruetype.stbtt_GetCodepointBitmap(info, rasterScale, rasterScale, code, w, h, xo, yo);
                if (coverage == null) { coverageGlyphs.put(code, new Glyph(cell, 0, 0, 0, 0, advance(code))); return; }
                try {
                    int width = w.get(0) + 2 * COVERAGE_PADDING, height = h.get(0) + 2 * COVERAGE_PADDING;
                    if (width > CELL || height > CELL) { throw new IllegalArgumentException("Font glyph exceeds coverage atlas cell"); }
                    int startX = cell % 16 * CELL + COVERAGE_PADDING, startY = cell / 16 * CELL + COVERAGE_PADDING;
                    for (int y = 0; y < h.get(0); y++) {
                        for (int x = 0; x < w.get(0); x++) { pixels.put((startY + y) * SIDE + startX + x, coverage.get(y * w.get(0) + x)); }
                    }
                    coverageGlyphs.put(code, new Glyph(cell, width, height, xo.get(0) - COVERAGE_PADDING,
                            yo.get(0) - COVERAGE_PADDING, advance(code)));
                } finally { STBTruetype.stbtt_FreeBitmap(coverage); }
            }
        }
        private void bake(ByteBuffer pixels, int code, int cell) {
            if (STBTruetype.stbtt_FindGlyphIndex(info, code) == 0) { return; }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1); var h = stack.mallocInt(1); var xo = stack.mallocInt(1); var yo = stack.mallocInt(1);
                ByteBuffer sdf = STBTruetype.stbtt_GetCodepointSDF(info, scale, code, PADDING, (byte) 128, DISTANCE_SCALE, w, h, xo, yo);
                if (sdf == null) { glyphs.put(code, new Glyph(cell, 0, 0, 0, 0, advance(code))); return; }
                try {
                    if (w.get(0) > CELL || h.get(0) > CELL) { throw new IllegalArgumentException("Font glyph exceeds atlas cell"); }
                    int startX = cell % 16 * CELL, startY = cell / 16 * CELL;
                    for (int y = 0; y < h.get(0); y++) {
                        for (int x = 0; x < w.get(0); x++) { pixels.put((startY + y) * SIDE + startX + x, sdf.get(y * w.get(0) + x)); }
                    }
                    glyphs.put(code, new Glyph(cell, w.get(0), h.get(0), xo.get(0), yo.get(0), advance(code)));
                } finally { STBTruetype.stbtt_FreeSDF(sdf); }
            }
        }
        @Override public void close() {
            if (view != null) { view.close(); }
            if (texture != null) { texture.close(); }
            if (coverageView != null) { coverageView.close(); }
            if (coverageTexture != null) { coverageTexture.close(); }
            info.free(); MemoryUtil.memFree(data);
        }
    }
    private CustomTextRenderer() { }
    public static void configure(ThemeManager value) { manager = value; }
    private static String base(ThemeManager.Typeface font) { return font.name().toLowerCase(Locale.ROOT); }
    public static boolean available(ThemeManager.Typeface font) {
        return AVAILABLE.computeIfAbsent(font, CustomTextRenderer::findAvailable);
    }
    private static boolean findAvailable(ThemeManager.Typeface font) {
        if (font == ThemeManager.Typeface.QRAFTYS) { return manager != null && BitmapPackFont.find(manager.getDirectory()) != null; }
        String file = base(font) + "_regular.ttf";
        return CustomTextRenderer.class.getResource("/assets/riserecode/font/" + file) != null
                || manager != null && (Files.isRegularFile(manager.getFontDirectory().resolve(file))
                || Files.isRegularFile(manager.getFontDirectory().resolve(base(font) + ".ttf")));
    }
    public static String status() {
        ThemeManager.Typeface chosen = selected();
        if (chosen == ThemeManager.Typeface.QRAFTYS) {
            if (FAILURES.containsKey("qraftys_pack")) { return "Invalid Qraftys pack · using Inter"; }
            return available(chosen) ? "Qraftys resource-pack glyphs · nearest sampling"
                    : "Add Qraftys ZIP to resourcepacks · using Inter";
        }
        if (FAILURES.containsKey(base(chosen) + "_regular")) { return "Invalid " + base(chosen) + " font · using Inter"; }
        return available(chosen) ? "STB 8× filled glyphs · live preview"
                : "Install " + base(chosen) + ".ttf in config/riserecode/fonts · using Inter";
    }
    private static ThemeManager.Typeface selected() {
        return manager == null ? ThemeManager.Typeface.INTER : manager.choice("Font Selection", ThemeManager.Typeface.class);
    }
    private static BitmapPackFont bitmap() {
        if (selected() == ThemeManager.Typeface.QRAFTYS && bitmap != null) { return bitmap; }
        if (selected() != ThemeManager.Typeface.QRAFTYS || !available(selected()) || FAILURES.containsKey("qraftys_pack")) { return null; }
        if (bitmap == null) {
            try { bitmap = new BitmapPackFont(BitmapPackFont.find(manager.getDirectory())); }
            catch (IOException | RuntimeException failure) {
                FAILURES.put("qraftys_pack", failure.getMessage());
                org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load Qraftys pack; using Inter", failure);
            }
        }
        return bitmap;
    }
    private static Face face(boolean title) {
        ThemeManager.Typeface typeface = selected();
        if (typeface == ThemeManager.Typeface.QRAFTYS || !available(typeface)) { typeface = ThemeManager.Typeface.INTER; }
        String name = base(typeface) + (title ? "_medium" : "_regular");
        if (FAILURES.containsKey(name)) { return fallbackFace(title); }
        Face cached = FACES.get(name); if (cached != null) { return cached; }
        try {
            Face loaded = new Face(read(name + ".ttf", name.replace("_medium", "_regular") + ".ttf"), typeface == ThemeManager.Typeface.MINECRAFTIA);
            FACES.put(name, loaded); return loaded;
        } catch (IOException | RuntimeException failure) {
            FAILURES.put(name, failure.getMessage());
            org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load {}; using Inter", name, failure);
            return fallbackFace(title);
        }
    }
    private static Face fallbackFace(boolean title) {
        String key = title ? "inter_medium" : "inter_regular";
        return FACES.computeIfAbsent(key, ignored -> {
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + key + ".ttf")) {
                if (stream == null) { throw new IOException("Bundled Inter font missing"); }
                return new Face(stream.readAllBytes(), false);
            } catch (IOException failure) { throw new IllegalStateException("Missing bundled fallback font", failure); }
        });
    }
    private static byte[] read(String preferred, String fallback) throws IOException {
        for (String file : new String[] {preferred, fallback, fallback.replace("_regular", "")}) {
            if (manager != null) {
                Path local = manager.getFontDirectory().resolve(file);
                if (Files.isRegularFile(local)) {
                    if (Files.size(local) > 16 * 1024 * 1024) { throw new IOException("Font file exceeds 16 MiB"); }
                    return Files.readAllBytes(local);
                }
            }
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + file)) {
                if (stream != null) { return stream.readAllBytes(); }
            }
        }
        throw new IOException("Missing " + preferred);
    }
    private static boolean supports(Face face, int code) {
        return (code >= 32 && code < 256 || SYMBOLS.indexOf(code) >= 0)
                && (code == ' ' || STBTruetype.stbtt_FindGlyphIndex(face.info, code) != 0);
    }
    private static double advance(Face face, Font fallback, int code, boolean title) {
        return supports(face, code) ? face.advance(code) : fallback.width(Theme.text(new String(Character.toChars(code)), title));
    }
    private static double kern(Face face, int previous, int next) { return supports(face, previous) && supports(face, next) ? face.kern(previous, next) : 0; }
    public static int width(Font fallback, String original, boolean title) {
        BitmapPackFont pack = bitmap();
        if (pack != null) { return (int) Math.ceil(pack.width(fallback, original) * Theme.number("Font Scale", 1)); }
        String value = original; Face face = face(title);
        double amount = 0; int previous = -1;
        for (int code : value.codePoints().toArray()) { amount += kern(face, previous, code) + advance(face, fallback, code, title); previous = code; }
        return (int) Math.ceil(amount * Theme.number("Font Scale", 1));
    }
    /** Describes the loaded font, including fallbacks, rather than the requested menu choice. */
    public static boolean isPixelFont() { return bitmap() != null || face(false).pixel; }
    @FunctionalInterface private interface Layer { void draw(double x, double y, int color, double blur, int mode, boolean gradient); }
    public static void draw(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent) {
        draw(graphics, fallback, original, color, title, shadow, accent, accent);
    }
    public static void draw(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent, boolean glow) {
        if ((color >>> 24) == 0 || original.isEmpty()) { return; }
        BitmapPackFont pack = bitmap();
        Layer layer;
        int normalMode;
        if (pack != null) {
            normalMode = 2;
            layer = (x, y, tint, blur, mode, gradient) -> pack.draw(graphics, fallback, original, x, y, tint, blur, mode, gradient);
        }
        else {
            Face face = face(title); face.upload();
            normalMode = face.pixel ? 4 : 3;
            layer = (x, y, tint, blur, mode, gradient) -> drawFace(graphics, fallback, face, original, x, y, tint, blur, mode, gradient, title);
        }
        graphics.pose().pushMatrix();
        float fontScale = (float) Theme.number("Font Scale", 1); graphics.pose().scale(fontScale, fontScale);
        try {
            if (shadow && Theme.bool("Text Shadow", true)) {
                layer.draw(Theme.number("Shadow Offset X", 0.5) / fontScale,
                        Theme.number("Shadow Offset Y", 0.7) / fontScale,
                        Theme.opacity(color & 0xFF000000, Theme.number("Shadow Opacity", 0.6)),
                        Theme.number("Shadow Blur", 0.65) / fontScale, 1, false);
            }
            if (glow && Theme.bool("Global Glow", true) && Theme.number("Glow Radius", 5) > 0) {
                layer.draw(0, 0, Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                        Theme.number("Glow Radius", 5) / 3 / fontScale, 1, false);
            }
            layer.draw(0, 0, color, 0, normalMode, accent);
        } finally { graphics.pose().popMatrix(); }
    }
    private static void drawFace(GuiGraphicsExtractor graphics, Font fallback, Face face, String value, double x, double y,
            int color, double blur, int mode, boolean gradient, boolean title) {
        int[] codes = value.codePoints().toArray(); double total = 0; int previous = -1;
        for (int code : codes) { total += kern(face, previous, code) + advance(face, fallback, code, title); previous = code; }
        previous = -1; double advance = 0;
        boolean filled = mode == 3 || mode == 4, pixel = mode == 4;
        int density = filled ? COVERAGE_DENSITY : DENSITY;
        Map<Integer, Glyph> atlasGlyphs = filled ? face.coverageGlyphs : face.glyphs;
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(pixel ? FilterMode.NEAREST : FilterMode.LINEAR);
        TextureSetup setup = TextureSetup.singleTexture(filled ? face.coverageView : face.view, sampler);
        for (int code : codes) {
            advance += kern(face, previous, code); Glyph glyph = atlasGlyphs.get(code);
            if (!supports(face, code)) {
                drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + advance, y, color, blur);
            }
            if (glyph != null && glyph.width > 0 && code != ' ') {
                double gx = x + advance + glyph.xOffset / (double) density;
                double gy = y + face.baseline + glyph.yOffset / (double) density;
                if (pixel) { gx = Math.rint(gx); gy = Math.rint(gy); }
                int tint = gradient ? Theme.opacity(Theme.gradientAt(graphics.pose().m20() + advance * graphics.pose().m00(), graphics.pose().m21(), advance / Math.max(1, total)), (color >>> 24) / 255.0) : color;
                float u = glyph.cell % 16 * CELL / (float) SIDE, v = glyph.cell / 16 * CELL / (float) SIDE;
                UiMesh.quad(graphics, Pipelines.FONT, setup, gx, gy, glyph.width / (double) density,
                        glyph.height / (double) density, u, v, u + glyph.width / (float) SIDE, v + glyph.height / (float) SIDE,
                        tint, tint, (int) Math.round(blur * 256), mode, 0, 0);
            }
            advance += advance(face, fallback, code, title); previous = code;
        }
    }
    /** Unicode outside the cached Latin atlas retains the selected font for its neighbouring glyphs.
     * Native fallback shadows use the same offsets and a normalized Gaussian stencil.
     */
    static void drawFallbackGlyph(GuiGraphicsExtractor graphics, Font font, String glyph, double x, double y, int color, double blur) {
        int radius = blur > 0.05 ? 3 : 0; double sum = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) { sum += Math.exp(-(dx * dx + dy * dy) / 2.0); }
        }
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tint = Theme.opacity(color, Math.exp(-(dx * dx + dy * dy) / 2.0) / sum);
                if ((tint >>> 24) == 0) { continue; }
                graphics.pose().pushMatrix(); graphics.pose().translate((float) (x + dx * blur), (float) (y + dy * blur));
                graphics.text(font, Theme.text(glyph, false), 0, 0, tint, false); graphics.pose().popMatrix();
            }
        }
    }
    public static void close() {
        FACES.values().forEach(Face::close); FACES.clear(); FAILURES.clear(); AVAILABLE.clear();
        if (bitmap != null) { bitmap.close(); bitmap = null; }
    }
}
```

### src/main/java/dev/riserecode/ui/hud/HudContent.java

New file.

```java
package dev.riserecode.ui.hud;

import java.util.List;
import net.minecraft.resources.Identifier;

/** Presentation data, without Minecraft widgets or drawing calls in feature modules. */
public final class HudContent {
    public record Span(String text, int color, boolean gradient) { }
    public record Line(List<Span> spans, Identifier icon, double bar, int barColor) {
        public Line { spans = List.copyOf(spans); }
        public static Line text(String text, int color, boolean gradient) { return new Line(List.of(new Span(text, color, gradient)), null, -1, color); }
    }
    private HudContent() { }
}
```

### src/main/java/dev/riserecode/ui/hud/HudEditorScreen.java

New file.

```java
package dev.riserecode.ui.hud;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Original drag/snap editor using the same live element registry as gameplay. */
public final class HudEditorScreen extends Screen {
    private record Guide(double value, boolean vertical) { }
    private final HudRenderer hud;
    private final List<Guide> guides = new ArrayList<>();
    private final List<UIComponent> controls = new ArrayList<>();
    private final Animation contextOpening = Animation.ui(0, 0.18, MathUtil.Easing.OUT_QUART);
    private HudRenderer.Box dragging;
    private HudElement selected;
    private UIComponent captured;
    private double dragX, dragY, panelX, panelY;
    private long previousFrame;
    public HudEditorScreen(HudRenderer hud) { super(Component.literal("risecode HUD editor")); this.hud = hud; }
    public HudElement getSelected() { return selected; }
    public boolean isDraggingElement() { return dragging != null; }
    public List<UIComponent> getControls() { return List.copyOf(controls); }
    private void changed() { Client.INSTANCE.getConfigManager().requestSave(); }
    private void select(HudElement element) {
        selected = element; controls.clear(); contextOpening.reset(0); contextOpening.animateTo(1);
        controls.add(new Slider(element.layout().scale, this::changed, 0.01));
        controls.add(new EnumSelector<>(element.layout().alignment, this::changed));
        controls.add(new EnumSelector<>(element.layout().background, this::changed));
        if (element.module() != null) { controls.add(new ToggleSwitch("Enabled", element.module()::isEnabled,
                value -> { Client.INSTANCE.getModuleManager().setEnabled(element.module(), value); changed(); })); }
        controls.add(new ToggleSwitch(hud.getSnap(), this::changed)); controls.add(new Slider(hud.getGridSize(), this::changed, 1));
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) { }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime(); double seconds = previousFrame == 0 ? 0 : (now - previousFrame) * 1e-9; previousFrame = now;
        hud.extractHud(g, true);
        if (dragging != null && hud.getSnap().getValue()) {
            double step = hud.getGridSize().getValue() * g.guiHeight() / 576.0;
            for (double x = 0; x < width; x += step) { RenderUtil.line(g, x, 0, x, height, 0.4, 0x10FFFFFF); }
            for (double y = 0; y < height; y += step) { RenderUtil.line(g, 0, y, width, y, 0.4, 0x10FFFFFF); }
        }
        for (HudRenderer.Box box : hud.getBoxes()) {
            int color = box.element() == selected ? Theme.accent() : 0x6096A5B6;
            RenderUtil.drawRoundedBorder(g, box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, 2, color, 0.8);
            double labelY = box.y() >= 12 ? box.y() - 10 : box.y() + box.height() + 3;
            RenderUtil.text(g, font, box.element().label(), box.x(), Math.min(height - 9, labelY), 0.8, color);
        }
        for (Guide guide : guides) {
            if (guide.vertical()) { RenderUtil.line(g, guide.value(), 0, guide.value(), height, 0.8, Theme.opacity(Theme.accent(), 0.7)); }
            else { RenderUtil.line(g, 0, guide.value(), width, guide.value(), 0.8, Theme.opacity(Theme.accent(), 0.7)); }
        }
        String hint = "HUD Editor  ·  Drag to move  ·  Right click to customize  ·  G: grid  ·  Esc: save";
        double hintWidth = RenderUtil.textWidth(font, hint, false) * 0.9 + 22;
        RenderUtil.drawRoundedRect(g, (width - hintWidth) / 2, height - 26, hintWidth, 21, 6, 0xDF111820);
        RenderUtil.text(g, font, hint, (width - hintWidth) / 2 + 11, height - 19, 0.9, Theme.TEXT);
        if (selected != null && !controls.isEmpty()) {
            double panelWidth = Math.min(190, width - 12), panelHeight = 33 + controls.size() * 28;
            panelX = Math.max(6, width - panelWidth - 12); panelY = Math.max(8, Math.min(height - panelHeight - 34, 40));
            double opening = MathUtil.clamp01(contextOpening.getValue());
            RenderUtil.drawDropShadow(g, panelX, panelY, panelWidth, panelHeight, 8, Theme.opacity(0x70000000, opening));
            RenderUtil.drawRoundedRect(g, panelX, panelY, panelWidth, panelHeight, 8, Theme.opacity(0xED181F29, opening));
            RenderUtil.accentText(g, font, selected.label(), panelX + 10, panelY + 9 + (1 - opening) * 4, 1, opening);
            for (int i = 0; i < controls.size(); i++) {
                controls.get(i).setBounds(panelX + 10, panelY + 30 + i * 28, panelWidth - 20, 25);
                controls.get(i).render(g, font, mouseX, mouseY, seconds, opening);
            }
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (selected != null && event.x() >= panelX && event.x() <= panelX + 190 && event.y() >= panelY && event.y() <= panelY + 33 + controls.size() * 28) {
            for (UIComponent control : controls) { if (control.mouseClicked(event.x(), event.y(), event.button())) { captured = control; return true; } }
            return true;
        }
        List<HudRenderer.Box> boxes = hud.getBoxes();
        for (int i = boxes.size() - 1; i >= 0; i--) {
            HudRenderer.Box box = boxes.get(i);
            if (box.contains(event.x(), event.y())) {
                select(box.element());
                if (event.button() == 0) { dragging = box; dragX = event.x() - box.x(); dragY = event.y() - box.y(); }
                return true;
            }
        }
        selected = null; controls.clear(); return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (captured != null) { return captured.mouseDragged(event.x(), event.y(), event.button()); }
        if (dragging == null) { return false; }
        double x = event.x() - dragX, y = event.y() - dragY;
        if (hud.getSnap().getValue()) { double grid = hud.getGridSize().getValue() * height / 576.0; x = Math.rint(x / grid) * grid; y = Math.rint(y / grid) * grid; }
        guides.clear();
        x = align(x, dragging.width(), width, true); y = align(y, dragging.height(), height, false);
        x = Math.clamp(x, 0, Math.max(0, width - dragging.width())); y = Math.clamp(y, 0, Math.max(0, height - dragging.height()));
        selected.layout().position(x / width, y / height); changed(); return true;
    }
    private double align(double start, double size, double screen, boolean vertical) {
        List<Double> anchors = new ArrayList<>(List.of(0.0, screen / 2, screen));
        for (HudRenderer.Box box : hud.getBoxes()) {
            if (box.element() == selected) { continue; }
            double p = vertical ? box.x() : box.y(), s = vertical ? box.width() : box.height(); anchors.add(p); anchors.add(p + s / 2); anchors.add(p + s);
        }
        double best = start, distance = 4.5;
        for (double anchor : anchors) {
            for (double offset : new double[] {0, size / 2, size}) {
                double diff = Math.abs(start + offset - anchor);
                if (diff < distance) { distance = diff; best = anchor - offset; }
            }
        }
        double startAligned = best;
        if (best != start) { for (double offset : new double[] {0, size / 2, size}) { if (anchors.stream().anyMatch(v -> Math.abs(v - (startAligned + offset)) < 0.01)) { guides.add(new Guide(best + offset, vertical)); } } }
        return best;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (captured != null) { captured.mouseReleased(event.x(), event.y(), event.button()); captured = null; }
        dragging = null; guides.clear(); changed(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() || event.key() == 344) { onClose(); return true; }
        if (event.key() == 71) { hud.getSnap().setValue(!hud.getSnap().getValue()); changed(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
    @Override public void removed() { Client.INSTANCE.getConfigManager().save(); Client.INSTANCE.getProfileManager().saveActiveHudLayout(); }
    @Override public boolean isPauseScreen() { return true; }
}
```

### src/main/java/dev/riserecode/ui/hud/HudElement.java

New file.

```java
package dev.riserecode.ui.hud;

import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;

public final class HudElement {
    private final String id, label;
    private final HudModule module;
    private final HudLayout layout;
    private final Animation opacity = Animation.ui(0, 0.22, MathUtil.Easing.OUT_QUART);
    private final Animation x = Animation.ui(0, 0.07, MathUtil.Easing.OUT_QUART);
    private final Animation y = Animation.ui(0, 0.07, MathUtil.Easing.OUT_QUART);
    private boolean positioned;
    public HudElement(String id, String label, HudModule module, double x, double y) {
        this.id = id; this.label = label; this.module = module; layout = new HudLayout(x, y);
    }
    public String id() { return id; }
    public String label() { return label; }
    public HudModule module() { return module; }
    public HudLayout layout() { return layout; }
    public boolean enabled() { return module == null || module.isEnabled(); }
    public double opacity() { opacity.animateTo(enabled() ? 1 : 0); return MathUtil.clamp01(opacity.getValue()); }
    public double[] position(double targetX, double targetY) {
        if (!positioned) { x.reset(targetX); y.reset(targetY); positioned = true; }
        x.animateTo(targetX); y.animateTo(targetY); return new double[] {x.getValue(), y.getValue()};
    }
}
```

### src/main/java/dev/riserecode/ui/hud/HudLayout.java

New file.

```java
package dev.riserecode.ui.hud;

import com.google.gson.JsonObject;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;

/** Normalized top-left anchors survive resolution changes; scale/alignment/backdrop are per element. */
public final class HudLayout {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Background { INHERIT, NONE, SOLID, BLUR }
    public final DoubleSetting scale = new DoubleSetting("Scale", "Element scale", 1, 0.5, 3);
    public final EnumSetting<Alignment> alignment = new EnumSetting<>("Alignment", "Text alignment", Alignment.LEFT);
    public final EnumSetting<Background> background = new EnumSetting<>("Background", "Element backdrop", Background.INHERIT);
    private double x, y;
    public HudLayout(double x, double y) { position(x, y); }
    public double x() { return x; }
    public double y() { return y; }
    public void position(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) { throw new IllegalArgumentException("Non-finite HUD position"); }
        this.x = Math.clamp(x, 0, 1); this.y = Math.clamp(y, 0, 1);
    }
    public JsonObject snapshot() {
        JsonObject object = new JsonObject(); object.addProperty("x", x); object.addProperty("y", y);
        object.addProperty("scale", scale.getValue()); object.addProperty("alignment", alignment.getValue().name());
        object.addProperty("background", background.getValue().name()); return object;
    }
    public static void validate(JsonObject object) {
        for (String key : new String[] {"x", "y", "scale"}) {
            if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) { throw new IllegalArgumentException("Invalid HUD " + key); }
            double value = object.get(key).getAsDouble();
            if (!Double.isFinite(value) || value < (key.equals("scale") ? 0.5 : 0) || value > (key.equals("scale") ? 3 : 1)) { throw new IllegalArgumentException("Out-of-range HUD " + key); }
        }
        for (String key : new String[] {"alignment", "background"}) {
            if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()) { throw new IllegalArgumentException("Invalid HUD " + key); }
        }
        Alignment.valueOf(object.get("alignment").getAsString()); Background.valueOf(object.get("background").getAsString());
    }
    public void apply(JsonObject object) {
        validate(object); position(object.get("x").getAsDouble(), object.get("y").getAsDouble());
        scale.setValue(object.get("scale").getAsDouble()); alignment.setValue(Alignment.valueOf(object.get("alignment").getAsString()));
        background.setValue(Background.valueOf(object.get("background").getAsString()));
    }
}
```

### src/main/java/dev/riserecode/ui/hud/HudRenderer.java

Updated file.

```java
package dev.riserecode.ui.hud;

import com.google.gson.JsonObject;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Shared live HUD registry and editor rendering; modules supply data, not drawing code. */
public final class HudRenderer implements AutoCloseable {
    public record EntryState(Module module, double visibility, double y) { }
    public record Box(HudElement element, double x, double y, double width, double height) {
        public boolean contains(double mx, double my) { return mx >= x && mx <= x + width && my >= y && my <= y + height; }
    }
    public static final double ROW_HEIGHT = 11.5;
    private static final class Entry {
        private final Module module;
        private final Animation y = Animation.ui(0, 0.24, MathUtil.Easing.OUT_QUART);
        private final Animation slide = Animation.ui(0, 0.32, MathUtil.Easing.OUT_QUART);
        private boolean active;
        private int width;
        private Entry(Module module) { this.module = module; }
        private void update() { active = module.isEnabled(); slide.animateTo(active ? 1 : 0); }
    }
    private final ThemeManager themes;
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final String watermark;
    private final EventBus.Subscription subscription;
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private final Map<String, HudElement> elements = new LinkedHashMap<>();
    private final Animation visibility = Animation.ui(1, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    private final BooleanSetting snap = new BooleanSetting("SnapToGrid", "Snap while dragging", true);
    private final DoubleSetting gridSize = new DoubleSetting("GridSize", "Grid pixels in reference canvas", 6, 2, 32);
    private List<Entry> sorted = List.of();
    private List<Box> boxes = List.of();
    private boolean enabled = true;
    public HudRenderer(EventBus bus, Minecraft mc, ModuleManager modules, String watermark) { this(bus, mc, modules, watermark, null); }
    public HudRenderer(EventBus bus, Minecraft mc, ModuleManager modules, String watermark, ThemeManager themes) {
        this.themes = themes; minecraft = mc; this.modules = modules; this.watermark = watermark;
        register(new HudElement("watermark", "Watermark", null, 0.004, 0.006));
        HudElement array = new HudElement("arraylist", "ArrayList", null, 1, 0); array.layout().alignment.setValue(HudLayout.Alignment.RIGHT); register(array);
        double top = 0.055;
        for (Module module : modules.getModules()) {
            if (module instanceof HudModule hud) { register(new HudElement(module.getName(), Theme.displayName(module.getName()), hud, 0.008, top)); top += 0.048; }
        }
        subscription = bus.subscribe(Render2DEvent.class, this::render);
    }
    public void register(HudElement element) {
        if (elements.putIfAbsent(element.id(), element) != null) { throw new IllegalArgumentException("Duplicate HUD element " + element.id()); }
    }
    public List<HudElement> getElements() { return List.copyOf(elements.values()); }
    public List<Box> getBoxes() { return boxes; }
    public BooleanSetting getSnap() { return snap; }
    public DoubleSetting getGridSize() { return gridSize; }
    public boolean isEnabled() { return themes == null ? enabled : themes.bool("HUD Visible"); }
    public void setEnabled(boolean value) { enabled = value; if (themes != null) { ((BooleanSetting) themes.getSetting("HUD Visible")).setValue(value); themes.changed(); } }
    public List<EntryState> getEntries() { return entries.values().stream().map(e -> new EntryState(e.module, e.slide.getValue(), e.y.getValue())).toList(); }
    public JsonObject snapshotLayouts() {
        JsonObject result = new JsonObject(), values = new JsonObject(); result.add("elements", values);
        elements.forEach((id, element) -> values.add(id, element.layout().snapshot()));
        result.addProperty("snap", snap.getValue()); result.addProperty("gridSize", gridSize.getValue()); return result;
    }
    public void validateLayouts(JsonObject data) {
        if (!data.has("elements") || !data.get("elements").isJsonObject()) { throw new IllegalArgumentException("Missing HUD elements"); }
        for (var entry : data.getAsJsonObject("elements").entrySet()) { if (elements.containsKey(entry.getKey())) { HudLayout.validate(entry.getValue().getAsJsonObject()); } }
        if (data.has("snap") && (!data.get("snap").isJsonPrimitive() || !data.getAsJsonPrimitive("snap").isBoolean())) { throw new IllegalArgumentException("Invalid HUD snap"); }
        if (data.has("gridSize")) {
            if (!data.get("gridSize").isJsonPrimitive() || !data.getAsJsonPrimitive("gridSize").isNumber()) { throw new IllegalArgumentException("Invalid HUD grid"); }
            double size = data.get("gridSize").getAsDouble(); if (!Double.isFinite(size) || size < 2 || size > 32) { throw new IllegalArgumentException("Invalid HUD grid"); }
        }
    }
    public void applyLayouts(JsonObject data) {
        validateLayouts(data); data.getAsJsonObject("elements").entrySet().forEach(entry -> { if (elements.containsKey(entry.getKey())) { elements.get(entry.getKey()).layout().apply(entry.getValue().getAsJsonObject()); } });
        if (data.has("snap")) { snap.setValue(data.get("snap").getAsBoolean()); } if (data.has("gridSize")) { gridSize.setValue(data.get("gridSize").getAsDouble()); }
    }
    private void update() {
        for (Module module : modules.getModules()) { if (module.isEnabled() && !(module instanceof HudModule)) { entries.computeIfAbsent(module, Entry::new); } }
        entries.values().forEach(Entry::update);
        entries.values().removeIf(e -> !e.active && !e.slide.isRunning() && e.slide.getValue() == 0);
        ArrayList<Entry> next = new ArrayList<>(entries.values());
        for (Entry entry : next) { entry.width = RenderUtil.textWidth(minecraft.font, Theme.displayName(entry.module.getName()), false); }
        next.sort(Comparator.comparingInt((Entry e) -> e.width).reversed().thenComparing(e -> e.module.getName())); sorted = next;
        double row = rowHeight();
        for (int i = 0; i < sorted.size(); i++) { sorted.get(i).y.animateTo(i * (row + Theme.number("ArrayList Spacing", 0))); }
        visibility.animateTo(isEnabled() ? 1 : 0);
    }
    private double rowHeight() { return 7.5 * Theme.number("Font Scale", 1) + Theme.number("ArrayList Padding", 2) * 2; }
    private void render(Render2DEvent event) {
        if (minecraft.font == null) { return; } update();
        if (minecraft.gui.screen() instanceof HudEditorScreen || minecraft.player == null || minecraft.gui.hud.isHidden()) { return; }
        extractHud(event.getGraphics(), false);
    }
    public void extractHud(GuiGraphicsExtractor graphics, boolean edit) {
        update(); List<Box> currentBoxes = new ArrayList<>();
        double reference = graphics.guiHeight() / 576.0, canvasWidth = graphics.guiWidth() / reference;
        double global = edit ? 1 : MathUtil.clamp01(visibility.getValue());
        for (HudElement element : elements.values()) {
            double opacity = edit ? 1 : element.opacity() * global;
            List<HudContent.Line> content = element.module() == null ? List.of() : element.module().content();
            double padding = Theme.number("ArrayList Padding", 2), w, h;
            if (element.id().equals("watermark")) { w = RenderUtil.textWidth(minecraft.font, watermark, false) + padding * 2 + 1; h = rowHeight() + 1; }
            else if (element.id().equals("arraylist")) { w = sorted.stream().mapToInt(e -> e.width).max().orElse(70) + padding * 2 + 1; h = Math.max(rowHeight(), sorted.size() * (rowHeight() + Theme.number("ArrayList Spacing", 0))); }
            else { w = content.stream().mapToDouble(this::lineWidth).max().orElse(50) + 8; h = content.stream().mapToDouble(this::lineHeight).sum() + 6; }
            double scale = Math.min(element.layout().scale.getValue(), Math.min(canvasWidth / w, 576 / h)), physicalScale = reference * scale;
            double tx = Math.clamp(element.layout().x() * canvasWidth, 0, Math.max(0, canvasWidth - w * scale));
            double ty = Math.clamp(element.layout().y() * 576, 0, Math.max(0, 576 - h * scale));
            double[] xy = element.position(tx, ty); double x = Math.clamp(xy[0], 0, Math.max(0, canvasWidth - w * scale)), y = Math.clamp(xy[1], 0, Math.max(0, 576 - h * scale));
            if (opacity <= 0 && !edit) { continue; }
            currentBoxes.add(new Box(element, x * reference, y * reference, w * physicalScale, h * physicalScale));
            graphics.pose().pushMatrix(); graphics.pose().translate((float) (x * reference), (float) (y * reference)); graphics.pose().scale((float) physicalScale, (float) physicalScale);
            try {
                if (element.id().equals("arraylist")) { drawArray(graphics, element, w, padding, opacity, edit); }
                else {
                    background(graphics, element, 0, 0, w, h, opacity);
                    if (element.id().equals("watermark")) {
                        RenderUtil.accentText(graphics, minecraft.font, watermark, padding, padding, 1, opacity);
                        RenderUtil.drawAccentRoundedRect(graphics, padding, h - 1, w - padding * 2, 0.7, 0.35, opacity * 0.7);
                    } else {
                        double top = 3;
                        for (HudContent.Line line : content) { drawLine(graphics, element, line, w, top, opacity); top += lineHeight(line); }
                    }
                }
            } finally { graphics.pose().popMatrix(); }
        }
        boxes = List.copyOf(currentBoxes);
    }
    private double lineWidth(HudContent.Line line) { return (line.icon() == null ? 0 : 22) + line.spans().stream().mapToDouble(span -> RenderUtil.textWidth(minecraft.font, span.text(), false)).sum(); }
    private double lineHeight(HudContent.Line line) { return Math.max(line.icon() == null ? 0 : 20, 10 * Theme.number("Font Scale", 1)) + (line.bar() >= 0 ? 6 : 0); }
    private void drawLine(GuiGraphicsExtractor graphics, HudElement element, HudContent.Line line, double width, double y, double opacity) {
        double contentWidth = lineWidth(line), x = aligned(element, width, contentWidth, 4);
        if (line.icon() != null) { graphics.blitSprite(RenderPipelines.GUI_TEXTURED, line.icon(), (int) x, (int) y, 18, 18, Theme.opacity(0xFFFFFFFF, opacity)); x += 22; }
        for (HudContent.Span span : line.spans()) {
            if (span.gradient()) { RenderUtil.accentText(graphics, minecraft.font, span.text(), x, y + (line.icon() == null ? 0 : 5), 1, opacity); }
            else { RenderUtil.hudText(graphics, minecraft.font, span.text(), x, y + (line.icon() == null ? 0 : 5), Theme.opacity(span.color(), opacity)); }
            x += RenderUtil.textWidth(minecraft.font, span.text(), false);
        }
        if (line.bar() >= 0) { double top = y + lineHeight(line) - 5;
            RenderUtil.drawRoundedRect(graphics, 4, top, width - 8, 2, 1, Theme.opacity(0xFF34434F, opacity));
            RenderUtil.drawRoundedRect(graphics, 4, top, (width - 8) * MathUtil.clamp01(line.bar()), 2, 1, Theme.opacity(line.barColor(), opacity)); }
    }
    private static double aligned(HudElement element, double width, double contentWidth, double padding) {
        return switch (element.layout().alignment.getValue()) { case LEFT -> padding; case CENTER -> (width - contentWidth) / 2; case RIGHT -> width - contentWidth - padding; };
    }
    private void drawArray(GuiGraphicsExtractor graphics, HudElement element, double width, double padding, double opacity, boolean edit) {
        if (sorted.isEmpty() && edit) { background(graphics, element, 0, 0, width, rowHeight(), 1); RenderUtil.accentText(graphics, minecraft.font, "ArrayList", padding, padding, 1, 1); }
        for (Entry entry : sorted) {
            double alpha = opacity * MathUtil.clamp01(entry.slide.getValue()), rowWidth = entry.width + padding * 2 + 1;
            double x = aligned(element, width, rowWidth, 0) + (1 - entry.slide.getValue()) * (width + 8), y = entry.y.getValue();
            background(graphics, element, x, y, rowWidth, rowHeight(), alpha);
            RenderUtil.accentText(graphics, minecraft.font, Theme.displayName(entry.module.getName()), x + padding, y + padding, 1, alpha);
        }
    }
    private void background(GuiGraphicsExtractor g, HudElement element, double x, double y, double w, double h, double opacity) {
        HudLayout.Background kind = element.layout().background.getValue();
        if (kind == HudLayout.Background.INHERIT) { kind = switch (themes == null ? ThemeManager.HudBackground.SOLID_BOX : themes.choice("HUD Background", ThemeManager.HudBackground.class)) {
            case NONE -> HudLayout.Background.NONE; case SOLID_BOX -> HudLayout.Background.SOLID; case BLUR_BOX -> HudLayout.Background.BLUR; }; }
        if (kind == HudLayout.Background.NONE) { return; }
        if (kind == HudLayout.Background.BLUR) { BlurUtil.drawBox(g, x, y, w, h, 3, opacity * 0.8); }
        RenderUtil.drawRoundedRect(g, x, y, w, h, 3, Theme.opacity(Theme.SURFACE, opacity * (kind == HudLayout.Background.BLUR ? 0.3 : 0.8)));
    }
    @Override public void close() { subscription.close(); entries.clear(); elements.clear(); }
}
```

### src/main/java/dev/riserecode/ui/render/UiMesh.java

Updated file.

```java
package dev.riserecode.ui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Four-vertex extracted quads. Effects travel in vertex attributes, avoiding mutable shared UBOs. */
public final class UiMesh {
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UV1", GpuFormat.RG16_SINT)
            .addAttribute("UV2", GpuFormat.RG16_SINT).build();
    private UiMesh() { }
    public static RenderPipeline pipeline(String name, String fragment) {
        return RenderPipelines.register(RenderPipeline.builder(fragment.equals("shape") || fragment.equals("window") || fragment.equals("hsv") ? RenderPipelines.GUI_SNIPPET : RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name)).withVertexShader(id("core/ui"))
                .withFragmentShader(id("core/" + fragment)).withVertexBinding(0, FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("riserecode", path); }
    public static void quad(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double width, double height, float u0, float v0, float u1, float v1,
            int topColor, int bottomColor, int a, int b, int c, int d) {
        quad(graphics, pipeline, texture, x, y, width, height, u0, v0, u1, v1, topColor, bottomColor, a, b, c, d, false);
    }
    public static void quad(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double width, double height, float u0, float v0, float u1, float v1,
            int firstColor, int secondColor, int a, int b, int c, int d, boolean horizontal) {
        if (width <= 0 || height <= 0) { return; }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(width + 1), (int) Math.ceil(height + 1)).transformMaxBounds(pose);
        if (scissor != null) { bounds = bounds.intersection(scissor); }
        if (bounds == null) { return; }
        graphics.guiRenderState.addGuiElement(new Quad(pipeline, texture, pose, (float) x, (float) y,
                (float) width, (float) height, u0, v0, u1, v1, firstColor, secondColor, a, b, c, d, horizontal, scissor, bounds));
    }
    private record Quad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x, float y,
            float width, float height, float u0, float v0, float u1, float v1, int top, int bottom,
            int a, int b, int c, int d, boolean horizontal, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, x, y, u0, v0, top); vertex(vertices, x, y + height, u0, v1, horizontal ? top : bottom);
            vertex(vertices, x + width, y + height, u1, v1, bottom); vertex(vertices, x + width, y, u1, v0, horizontal ? bottom : top);
        }
        private void vertex(VertexConsumer vertices, float px, float py, float u, float v, int color) {
            vertices.addVertexWith2DPose(pose, px, py).setUv(u, v).setColor(color).setUv1(a, b).setUv2(c, d);
        }
    }
}
```

### src/main/java/dev/riserecode/ui/theme/Theme.java

Updated file.

```java
package dev.riserecode.ui.theme;

import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.animation.Animation;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Reference-derived palette and presentation preferences, independent of feature settings. */
public final class Theme {
    public static final int BACKGROUND = 0xFF191F27;
    public static final int PANEL = 0xFF151D20;
    public static final int SURFACE = 0xFF111820;
    public static final int HOVER = 0xFF18242C;
    public static final int TEXT = 0xFFF0F2F3;
    public static final int SECONDARY = 0xFF7F868E;
    public static final int MUTED = 0xFF525C65;
    public static final int TEAL = 0xFF59D5CB;
    public static final int BLUE = 0xFF3B82F6;
    public static final int FUCHSIA = 0xFFD946EF;
    public static final int BORDER = 0x08FFFFFF;
    public static final int SHADOW = 0x60000000;
    public static final int PANEL_RADIUS = 10;
    public static final int BUTTON_RADIUS = 5;
    public static final int PADDING = 8;
    private static int accent = TEAL;
    private static int previousAccent = TEAL;
    private static final Animation accentBlend = Animation.ui(1, 0.24, MathUtil.Easing.IN_OUT_CUBIC);
    private static long previousFrame;
    private static double gradientSeconds;
    private static boolean german;
    private static ThemeManager manager;
    public static void bind(ThemeManager value) { manager = value; }
    public static ThemeManager manager() { return manager; }
    public static double number(String name, double fallback) { return manager == null ? fallback : manager.number(name); }
    public static boolean bool(String name, boolean fallback) { return manager == null ? fallback : manager.bool(name); }
    public static double radius(double original) { return original * number("Corner Radius", 10) / 10; }
    public static int secondaryAccent() { return manager == null ? BLUE : manager.color("Secondary Accent"); }
    public static int accentAt(double amount) {
        if (manager != null && manager.choice("Gradient Mode", dev.riserecode.util.ColorUtil.GradientMode.class) != dev.riserecode.util.ColorUtil.GradientMode.STATIC) {
            return manager.getGradientColor(amount * manager.number("Gradient Spacing"), gradientSeconds);
        }
        return bool("Accent Gradient", true) ? lerp(accent(), secondaryAccent(), amount) : accent();
    }
    public static int gradientAt(double x, double y, double localAmount) {
        if (manager == null || manager.choice("Gradient Mode", dev.riserecode.util.ColorUtil.GradientMode.class) == dev.riserecode.util.ColorUtil.GradientMode.STATIC) { return accentAt(localAmount); }
        double offset = manager.choice("Gradient Direction", dev.riserecode.util.ColorUtil.GradientDirection.class) == dev.riserecode.util.ColorUtil.GradientDirection.HORIZONTAL ? x : y;
        return manager.getGradientColor(offset, gradientSeconds);
    }
    public static double gradientTime() { return gradientSeconds; }
    private static final Map<String, String> GERMAN = Map.ofEntries(
            Map.entry("Search", "Suche"), Map.entry("Combat", "Kampf"), Map.entry("Movement", "Bewegung"),
            Map.entry("Player", "Spieler"), Map.entry("Render", "Anzeige"), Map.entry("World", "Welt"),
            Map.entry("Utility", "Extras"), Map.entry("Profiles", "Profile"), Map.entry("Friends", "Freunde"),
            Map.entry("Themes", "Design"), Map.entry("Language", "Sprache"), Map.entry("Save", "Speichern"),
            Map.entry("Load", "Laden"), Map.entry("Add", "Hinzufügen"), Map.entry("Remove", "Entfernen"),
            Map.entry("Start typing to search...", "Zum Suchen tippen..."), Map.entry("No matching modules", "Keine passenden Module"),
            Map.entry("No modules in this category", "Keine Module in dieser Kategorie"));
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_regular")));
    private static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "roboto_medium")));

    private Theme() { }
    public static int accent() {
        return manager != null && manager.choice("Gradient Mode", dev.riserecode.util.ColorUtil.GradientMode.class) != dev.riserecode.util.ColorUtil.GradientMode.STATIC
                ? manager.getGradientColor(0, gradientSeconds) : lerp(previousAccent, accent, accentBlend.getValue());
    }
    public static int accentTarget() { return accent; }
    public static void setAccent(int color) {
        int next = color | 0xFF000000;
        if (manager != null && manager.color("Primary Accent") != next) {
            ((dev.riserecode.setting.ColorSetting) manager.getSetting("Primary Accent")).setValue(next); manager.changed();
        }
        if (next == accent) { return; }
        previousAccent = accent(); accent = next; accentBlend.reset(0); accentBlend.animateTo(1);
    }
    /** HUD and screen share this clock, so two rendering callbacks never advance a transition twice. */
    public static void advance(long now) {
        double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9);
        previousFrame = now;
        gradientSeconds += Math.min(0.25, seconds) * number("Animation Speed", 1);
        if (manager != null && manager.color("Primary Accent") != accent) { setAccent(manager.color("Primary Accent")); }
        accentBlend.update(seconds);
    }
    public static boolean isGerman() { return german; }
    public static void setGerman(boolean value) { german = value; }
    public static String label(String value) { return german ? GERMAN.getOrDefault(value, value) : value; }

    /** Adds word spacing to identifiers without changing their persisted setting/module keys. */
    public static String displayName(String value) {
        if (value.equals("FPS") || value.equals("HUD")) { return value; }
        String spaced = value.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        if (value.equals(value.toUpperCase(Locale.ROOT))) {
            StringBuilder title = new StringBuilder();
            for (String word : spaced.toLowerCase(Locale.ROOT).split(" ")) {
                if (!word.isEmpty()) {
                    if (!title.isEmpty()) { title.append(' '); }
                    title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
            }
            return title.toString();
        }
        return spaced;
    }
    public static Component text(String text, boolean title) {
        return Component.literal(text).withStyle(title ? TITLE : BODY);
    }
    public static int lerp(int from, int to, double progress) {
        double t = MathUtil.clamp01(progress);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            result |= (int) Math.round(MathUtil.lerp((from >>> shift) & 255, (to >>> shift) & 255, t)) << shift;
        }
        return result;
    }
    public static int opacity(int color, double opacity) {
        return (color & 0x00FFFFFF) | ((int) Math.round((color >>> 24) * MathUtil.clamp01(opacity)) << 24);
    }
}
```

### src/main/java/dev/riserecode/ui/theme/ThemeManager.java

Updated file.

```java
package dev.riserecode.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.ColorUtil;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;

/** Visual settings are independent of module configs and module profiles. Client-thread only. */
public final class ThemeManager implements AutoCloseable {
    public enum Typeface { GOOGLE_SANS, QRAFTYS, MINECRAFTIA, MONTSERRAT, INTER }
    public enum HudBackground { NONE, SOLID_BOX, BLUR_BOX }
    public record Control(String group, Setting<?> setting, double step) { }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    private final Logger logger;
    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
    private final List<Control> controls = new ArrayList<>();
    private EventBus.Subscription subscription;
    private long dirtySince;
    private long retryAfter;
    private long revision;
    private String activeProfile = "default";
    private boolean loaded;

    public ThemeManager(Path directory, Logger logger) {
        this.directory = directory; this.logger = logger;
        add("Typography", new EnumSetting<>("Font Selection", "Typeface shared by menu and HUD", Typeface.GOOGLE_SANS));
        number("Typography", "Font Scale", 1, 0.75, 2, 0.01);
        bool("Typography", "Text Shadow", true);
        number("Typography", "Shadow Offset X", 0.5, -4, 4, 0.05);
        number("Typography", "Shadow Offset Y", 0.7, -4, 4, 0.05);
        number("Typography", "Shadow Blur", 0.65, 0, 8, 0.05);
        number("Typography", "Shadow Opacity", 0.6, 0, 1, 0.01);
        bool("Colors & Glow", "Global Glow", true);
        number("Colors & Glow", "Glow Radius", 5, 0, 20, 0.1);
        number("Colors & Glow", "Glow Intensity", 0.2, 0, 1, 0.01);
        bool("Colors & Glow", "Accent Gradient", true);
        add("Colors & Glow", new EnumSetting<>("Gradient Mode", "Animated accent style", ColorUtil.GradientMode.STATIC));
        number("Colors & Glow", "Gradient Speed", 0.18, 0, 3, 0.01);
        add("Colors & Glow", new EnumSetting<>("Gradient Direction", "Spatial gradient axis", ColorUtil.GradientDirection.HORIZONTAL));
        number("Colors & Glow", "Gradient Spacing", 100, 10, 400, 1);
        add("Colors & Glow", new ColorSetting("Primary Accent", "Main active color", Theme.TEAL));
        add("Colors & Glow", new ColorSetting("Secondary Accent", "Gradient endpoint", 0xFF7C9FF5));
        number("Geometry & Layout", "Corner Radius", 10, 0, 30, 1);
        number("Geometry & Layout", "Panel Opacity", 0.87, 0, 1, 0.01);
        number("Geometry & Layout", "Border Thickness", 0.5, 0, 3, 0.05);
        number("Geometry & Layout", "Border Opacity", 0.08, 0, 1, 0.01);
        number("Geometry & Layout", "Content Padding", 8, 4, 20, 1);
        number("Geometry & Layout", "Interface Scale", 1, 0.75, 1.3, 0.01);
        number("Animations", "Animation Speed", 1, 0.5, 3, 0.05);
        add("Animations", new EnumSetting<>("Easing Curve", "Curve applied to every UI transition", MathUtil.Easing.OUT_QUART));
        number("Background Effects", "GUI Blur Radius", 12, 0, 30, 1);
        number("Background Effects", "GUI Blur Opacity", 0.8, 0, 1, 0.01);
        bool("Background Effects", "World Blur", false);
        number("Background Effects", "World Blur Radius", 8, 0, 30, 1);
        bool("HUD", "HUD Visible", true);
        add("HUD", new EnumSetting<>("HUD Background", "Backdrop behind watermark and enabled modules", HudBackground.SOLID_BOX));
        number("HUD", "ArrayList Spacing", 0, 0, 12, 0.25);
        number("HUD", "ArrayList Padding", 2, 0, 12, 0.25);
    }
    private void add(String group, Setting<?> setting) { add(group, setting, 0); }
    private void add(String group, Setting<?> setting, double step) {
        settings.put(setting.getName(), setting); controls.add(new Control(group, setting, step));
    }
    private void number(String group, String name, double value, double min, double max, double step) {
        add(group, new DoubleSetting(name, name, value, min, max), step);
    }
    private void bool(String group, String name, boolean value) { add(group, new BooleanSetting(name, name, value)); }
    public Path getDirectory() { return directory; }
    public Path getFontDirectory() { return directory.resolve("fonts"); }
    public List<Control> getControls() { return List.copyOf(controls); }
    public Setting<?> getSetting(String name) {
        Setting<?> result = settings.get(name);
        if (result == null) { throw new IllegalArgumentException("Unknown visual setting " + name); }
        return result;
    }
    public double number(String name) { return ((DoubleSetting) getSetting(name)).getValue(); }
    public boolean bool(String name) { return ((BooleanSetting) getSetting(name)).getValue(); }
    public int color(String name) { return ((ColorSetting) getSetting(name)).getValue(); }
    public <E extends Enum<E>> E choice(String name, Class<E> type) { return type.cast(getSetting(name).getValue()); }
    public long getRevision() { return revision; }
    public String getActiveProfile() { return activeProfile; }
    public boolean wasLoaded() { return loaded; }
    public void changed() { revision++; dirtySince = System.nanoTime(); retryAfter = 0; }
    public void bind(EventBus bus) {
        if (subscription != null) { return; }
        Theme.bind(this);
        Animation.configureUi(() -> number("Animation Speed"), () -> choice("Easing Curve", MathUtil.Easing.class));
        subscription = bus.subscribe(UpdateEvent.class, ignored -> {
            long now = System.nanoTime();
            if (dirtySince != 0 && now >= retryAfter && now - dirtySince >= 400_000_000L) { save(); }
        });
    }
    public int getGradientColor(double offset, double currentTime) {
        return ColorUtil.gradient(choice("Gradient Mode", ColorUtil.GradientMode.class), color("Primary Accent"),
                color("Secondary Accent"), offset, currentTime, number("Gradient Speed"), number("Gradient Spacing"));
    }
    private static final List<String> POLICY = List.of("Gradient Mode", "Gradient Speed", "Gradient Direction", "Gradient Spacing", "Animation Speed", "Easing Curve");
    public JsonObject policySnapshot() {
        JsonObject result = new JsonObject(); POLICY.forEach(name -> result.add(name, GSON.toJsonTree(getSetting(name).getValue()))); return result;
    }
    public void validatePolicy(JsonObject values) { POLICY.forEach(name -> { if (values.has(name)) { decode(getSetting(name), values.get(name)); } }); }
    public void applyPolicy(JsonObject values) {
        validatePolicy(values); POLICY.forEach(name -> { if (values.has(name)) { assign(getSetting(name), decode(getSetting(name), values.get(name))); } }); changed();
    }
    public ThemeProfile snapshot() {
        JsonObject values = new JsonObject();
        settings.forEach((name, setting) -> values.add(name, GSON.toJsonTree(setting.getValue())));
        return new ThemeProfile(ThemeProfile.VERSION, values);
    }
    /** Validate the complete document before applying it, so a bad file cannot half-load a theme. */
    public void apply(ThemeProfile profile) {
        JsonObject values = profile.settings();
        Map<Setting<?>, Object> parsed = new LinkedHashMap<>();
        for (Setting<?> setting : settings.values()) {
            JsonElement value = values.get(setting.getName());
            parsed.put(setting, value == null ? setting.getDefaultValue() : decode(setting, value));
        }
        parsed.forEach(ThemeManager::assign);
        changed();
    }
    private static Object decode(Setting<?> setting, JsonElement element) {
        if (!element.isJsonPrimitive()) { throw new IllegalArgumentException("Invalid " + setting.getName()); }
        var value = element.getAsJsonPrimitive();
        if (setting instanceof BooleanSetting) {
            if (!value.isBoolean()) { throw new IllegalArgumentException("Expected boolean " + setting.getName()); }
            return value.getAsBoolean();
        }
        if (setting instanceof DoubleSetting number) {
            if (!value.isNumber()) { throw new IllegalArgumentException("Expected number " + setting.getName()); }
            double result = value.getAsDouble();
            if (!Double.isFinite(result) || result < number.getMin() || result > number.getMax()) {
                throw new IllegalArgumentException("Out-of-range " + setting.getName());
            }
            return result;
        }
        if (setting instanceof ColorSetting) {
            if (!value.isNumber()) { throw new IllegalArgumentException("Expected ARGB color"); }
            return value.getAsBigDecimal().intValueExact();
        }
        if (setting instanceof EnumSetting<?> options) {
            if (!value.isString()) { throw new IllegalArgumentException("Expected option " + setting.getName()); }
            return options.getOptions().stream().filter(option -> option.name().equals(value.getAsString())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown option " + value.getAsString()));
        }
        throw new IllegalArgumentException("Unsupported setting type");
    }
    private static <T> void assign(Setting<T> setting, Object value) {
        @SuppressWarnings("unchecked") T typed = (T) value;
        setting.setValue(typed);
    }
    public void load() {
        Path file = directory.resolve("theme.json");
        try {
            Files.createDirectories(directory.resolve("theme-profiles")); Files.createDirectories(getFontDirectory());
            if (Files.isRegularFile(file)) { apply(read(file)); loaded = true; dirtySince = 0; }
            else { save(); }
        } catch (IOException | RuntimeException failure) {
            logger.warn("Could not load theme; keeping defaults", failure);
            if (Files.isRegularFile(file)) {
                try {
                    Path backup = directory.resolve("theme-invalid-" + System.currentTimeMillis() + ".json");
                    Files.move(file, backup); loaded = true;
                    logger.warn("Preserved unreadable theme as {}", backup); save();
                } catch (IOException backupFailure) { logger.warn("Could not preserve unreadable theme", backupFailure); }
            }
        }
    }
    public boolean save() {
        try { write(directory.resolve("theme.json"), snapshot()); dirtySince = 0; retryAfter = 0; return true; }
        catch (IOException failure) { retryAfter = System.nanoTime() + 5_000_000_000L; logger.warn("Could not save theme", failure); return false; }
    }
    private Path profilePath(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,40}")) {
            throw new IllegalArgumentException("Use 1–40 letters, digits, _ or - for theme names");
        }
        return directory.resolve("theme-profiles").resolve(name.toLowerCase(Locale.ROOT) + ".json");
    }
    public boolean saveProfile(String name) {
        Path file = profilePath(name);
        try { write(file, snapshot()); activeProfile = name.toLowerCase(Locale.ROOT); return true; }
        catch (IOException failure) { logger.warn("Could not save theme profile {}", name, failure); return false; }
    }
    public boolean loadProfile(String name) {
        Path file = profilePath(name);
        try { apply(read(file)); activeProfile = name.toLowerCase(Locale.ROOT); return true; }
        catch (IOException | RuntimeException failure) { logger.warn("Could not load theme profile {}", name, failure); return false; }
    }
    public List<String> getProfiles() {
        Path folder = directory.resolve("theme-profiles");
        if (!Files.isDirectory(folder)) { return List.of(); }
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).map(file -> file.getFileName().toString())
                    .filter(name -> name.matches("[a-z0-9_-]{1,40}\\.json"))
                    .map(name -> name.substring(0, name.length() - 5)).sorted().toList();
        } catch (IOException failure) { logger.warn("Could not list theme profiles", failure); return List.of(); }
    }
    public void reset() { settings.values().forEach(Setting::reset); activeProfile = "default"; changed(); }
    private static ThemeProfile read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("schemaVersion") || !root.has("settings")) { throw new IllegalArgumentException("Invalid theme profile"); }
            return new ThemeProfile(root.get("schemaVersion").getAsBigDecimal().intValueExact(), root.getAsJsonObject("settings"));
        }
    }
    private static void write(Path file, ThemeProfile profile) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "theme-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(profile, writer); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    @Override public void close() { save(); if (subscription != null) { subscription.close(); subscription = null; } }
}
```

### src/main/java/dev/riserecode/util/ColorUtil.java

New file.

```java
package dev.riserecode.util;

import dev.riserecode.ui.theme.Theme;

/** Original time-continuous gradients; offsets are logical UI pixels. */
public final class ColorUtil {
    public enum GradientMode { STATIC, RAINBOW, ASTOLFO, SINE }
    public enum GradientDirection { HORIZONTAL, VERTICAL }
    private ColorUtil() { }
    public static int gradient(GradientMode mode, int primary, int secondary, double offset, double seconds, double speed, double spacing) {
        double phase = offset / Math.max(1, spacing) + seconds * speed;
        phase -= Math.floor(phase);
        return switch (mode) {
            case STATIC -> primary;
            case RAINBOW -> hsv(phase - Math.floor(phase), 0.65, 1);
            case ASTOLFO -> hsv(0.5 + 0.35 * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2)), 0.45, 1);
            case SINE -> Theme.lerp(primary, secondary, 0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
        };
    }
    public static int hsv(double hue, double saturation, double value) {
        double h = (hue - Math.floor(hue)) * 6, c = value * saturation, x = c * (1 - Math.abs(h % 2 - 1)), m = value - c;
        double r = 0, g = 0, b = 0;
        switch ((int) h) {
            case 0 -> { r = c; g = x; } case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; } case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; } default -> { r = c; b = x; }
        }
        return 0xFF000000 | (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }
}
```

### src/main/java/dev/riserecode/util/RenderUtil.java

Updated file.

```java
package dev.riserecode.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Antialiased signed-distance geometry extracted into the game's modern GUI batching pipeline. */
public final class RenderUtil {
    private static final RenderPipeline SHAPE = UiMesh.pipeline("shape", "shape");
    private static final RenderPipeline WINDOW = UiMesh.pipeline("window", "window");
    private RenderUtil() { }
    public static void drawWindow(GuiGraphicsExtractor graphics, double x, double y, double opacity) {
        shape(graphics, WINDOW, x, y, 428, 320, Theme.number("Corner Radius", 10),
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Panel Opacity", 1)),
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Panel Opacity", 1)), 0);
        drawRoundedBorder(graphics, x, y, 428, 320, 10,
                Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Border Opacity", 0.08)));
    }
    public static void drawRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        int rgb = color & 0xFFFFFF;
        int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
        boolean surface = (r >= 17 && r <= 25 && g >= 24 && g <= 36 && b >= 30 && b <= 44) || rgb == (Theme.SURFACE & 0xFFFFFF) || rgb == (Theme.PANEL & 0xFFFFFF)
                || rgb == (Theme.BACKGROUND & 0xFFFFFF) || rgb == (Theme.HOVER & 0xFFFFFF);
        if (surface) { color = Theme.opacity(color, Theme.number("Panel Opacity", 1)); }
        shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, 0);
        if (surface) {
            drawRoundedBorder(graphics, x, y, width, height, radius,
                    Theme.opacity(0xFFFFFFFF, (color >>> 24) / 255.0 * Theme.number("Border Opacity", 0.08)));
        }
    }
    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawRoundedBorder(graphics, x, y, width, height, radius, color, Theme.number("Border Thickness", 1));
    }
    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color, double thickness) {
        if (thickness > 0) { shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, thickness); }
    }
    public static void drawDropShadow(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        shape(graphics, SHAPE, x, y + 3, width, height, Theme.radius(radius), color, color, -12);
    }
    public static void drawGlow(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, int color) {
        double size = Theme.number("Glow Radius", 5);
        if (Theme.bool("Global Glow", true) && size > 0) {
            shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)), -size);
        }
    }
    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int top, int bottom) {
        shape(graphics, SHAPE, x, y, width, height, 0, top, bottom, 0);
    }
    public static void drawAccentRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, double opacity) {
        boolean horizontal = Theme.manager() == null || Theme.manager().choice("Gradient Direction", ColorUtil.GradientDirection.class) == ColorUtil.GradientDirection.HORIZONTAL;
        var pose = graphics.pose();
        double sx = pose.m20() + x * pose.m00() + y * pose.m10(), sy = pose.m21() + x * pose.m01() + y * pose.m11();
        double ex = sx + (horizontal ? width * pose.m00() : height * pose.m10());
        double ey = sy + (horizontal ? width * pose.m01() : height * pose.m11());
        UiMesh.quad(graphics, SHAPE, TextureSetup.noTexture(), x - 1, y - 1, width + 2, height + 2,
                (float) (-width / 2 - 1), (float) (-height / 2 - 1), (float) (width / 2 + 1), (float) (height / 2 + 1),
                Theme.opacity(Theme.gradientAt(sx, sy, 0), opacity), Theme.opacity(Theme.gradientAt(ex, ey, 1), opacity),
                (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(Theme.radius(radius) * 256), 0, horizontal);
    }
    private static void shape(GuiGraphicsExtractor graphics, RenderPipeline pipeline, double x, double y,
            double width, double height, double radius, int top, int bottom, double effect) {
        if (width <= 0 || height <= 0 || (top >>> 24) == 0) { return; }
        double pad = effect < 0 ? -effect + 1 : 1;
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int code = effect < 0 ? -(int) Math.round(-effect * 256) - 1 : (int) Math.round(effect * 256);
        UiMesh.quad(graphics, pipeline, TextureSetup.noTexture(), x - pad, y - pad, width + pad * 2,
                height + pad * 2, (float) (-width / 2 - pad), (float) (-height / 2 - pad),
                (float) (width / 2 + pad), (float) (height / 2 + pad), top, bottom,
                (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(radius * 256), code);
    }
    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color) { drawText(graphics, font, text, x, y, color, false, true); }
    public static void drawText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            int color, boolean title, boolean shadow) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y);
        CustomTextRenderer.draw(graphics, font, text, color, title, shadow, false);
        graphics.pose().popMatrix();
    }
    public static void accentText(GuiGraphicsExtractor graphics, Font font, String text, double x,
            double y, double scale, double opacity) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, Theme.opacity(Theme.accent(), opacity), false, true, true);
        graphics.pose().popMatrix();
    }
    public static void hudText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y, int color) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y);
        CustomTextRenderer.draw(graphics, font, text, color, false, true, false, true);
        graphics.pose().popMatrix();
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            double scale, int color) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, color, false, true, false); graphics.pose().popMatrix();
    }
    public static int textWidth(Font font, String text, boolean title) { return CustomTextRenderer.width(font, text, title); }
    public static void line(GuiGraphicsExtractor graphics, double x1, double y1, double x2, double y2, double thickness, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) { return; }
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x1, (float) y1);
        graphics.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
        drawRoundedRect(graphics, 0, -thickness / 2, length, thickness, thickness / 2, color); graphics.pose().popMatrix();
    }
    public static String ellipsize(Font font, String text, double width) {
        if (textWidth(font, text, false) <= width) { return text; }
        String shortened = text;
        while (!shortened.isEmpty() && textWidth(font, shortened + "…", false) > width) {
            shortened = shortened.substring(0, shortened.offsetByCodePoints(shortened.length(), -1));
        }
        return shortened + "…";
    }
    public static void scissorPush(GuiGraphicsExtractor graphics, double x, double y, double width, double height) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }
    public static void scissorPop(GuiGraphicsExtractor graphics) { graphics.disableScissor(); }
}
```

### Resource and build integration

#### src/main/resources/assets/riserecode/shaders/core/enchant.vsh

```glsl
#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 texCoord;
out vec4 tint;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    tint = Color;
}
```

#### src/main/resources/assets/riserecode/shaders/core/enchant_glint.fsh

```glsl
#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
void main() {
    vec3 pattern = texture(Sampler0, texCoord).rgb;
    float light = max(pattern.r, max(pattern.g, pattern.b));
    fragColor = vec4(tint.rgb * light, tint.a * light);
}
```

#### src/main/resources/assets/riserecode/shaders/core/enchant_outline.fsh

```glsl
#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
void main() {
    float mask = texture(Sampler0, texCoord).a;
    if (mask < 0.1) discard;
    fragColor = vec4(tint.rgb, tint.a * mask);
}
```

#### src/main/resources/riserecode.mixins.json

```json
{
  "required": true,
  "package": "dev.riserecode.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": [
    "KeyboardHandlerMixin",
    "ChatInputMixin",
    "ConnectionMixin",
    "GuiRendererMixin",
    "AnimationFrameMixin",
    "GammaOptionAccessor",
    "LightmapMixin",
    "CameraMixin",
    "MouseHandlerMixin",
    "KeyboardInputMixin",
    "WeatherMixin",
    "PotionHudMixin",
    "ItemPhysicsStateMixin",
    "ItemEntityRendererMixin",
    "EnchantmentRendererMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

#### build.gradle

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

tasks.register('combatCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.CombatChecks'
}

tasks.register('themeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.ThemeChecks'
}

tasks.register('phase8Check', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.Phase8Checks'
}

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck', 'uiCheck', 'engineCheck', 'combatCheck', 'themeCheck', 'phase8Check' }

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
    if (providers.gradleProperty('phase8Only').isPresent()) { vmArg '-Driserecode.phase8Only=true' }
}

// The test instance is disposable. Copy only optional, user-installed font assets after cleanup;
// they remain local to development and never enter either distributable JAR.
tasks.named('runClientGameTest') {
    doFirst {
        copy {
            from 'run/resourcepacks/qraftys-capitalized-font.zip'
            into 'build/run/clientGameTest/resourcepacks'
        }
        copy {
            from 'run/config/riserecode/fonts'
            include '*.ttf', 'qraftys.zip'
            into 'build/run/clientGameTest/config/riserecode/fonts'
        }
    }
}
```

#### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.2
mod_version=0.7.0
maven_group=dev.riserecode
archives_base_name=riserecode
```

### Verification source files

These sources run in isolated harnesses and are excluded from the installed mod.

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
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.hud.HudRenderer;
import net.minecraft.client.input.CharacterEvent;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 240;
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
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> minecraft.options.guiScale().set(2));
        if (Boolean.getBoolean("riserecode.phase8Only")) {
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksRender(); Phase8IntegrationTest.verify(context, world);
            }
            return;
        }
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getThemeManager().reset();
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            fullBright.getMode().setValue(FullBrightModule.Mode.GAMMA);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitTick();
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> minecraft.gui.screen() == gui()), "Right Shift reverses an unfinished exit");
        context.runOnClient(minecraft -> { gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("FullBright"); });
        context.waitTicks(5);
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1000.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.THEMES));
        context.waitTicks(5);
        context.runOnClient(minecraft -> gui().getThemeEditor().reveal("HUD Visible"));
        context.waitTicks(8);
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
        // Search through the real character-input adapter; a space matches formatted module names.
        context.runOnClient(minecraft -> {
            gui().getSearchField().setFocused(true);
            gui().getSearchField().setValue("");
            "full bright".codePoints().forEach(cp -> gui().charTyped(new CharacterEvent(cp)));
        });
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().size() == 1);
        check(context.computeOnClient(minecraft -> gui().getActivePanel().getVisibleButtons().getFirst().getModule() == fullBright()), "search filters formatted names");
        context.runOnClient(minecraft -> gui().getSearchField().setValue("no matching module"));
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().isEmpty());
        context.runOnClient(minecraft -> gui().getSearchField().setValue("FullBright"));
        context.waitTicks(5);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 63, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().stream().filter(control -> control.setting() == fullBright().getBrightness()).findFirst().orElseThrow().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue() * 1000) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.runOnClient(minecraft -> gui().selectCategory(Category.UTILITY));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().size() == 2);
        context.waitTicks(5);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 40, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.color.getValue() == 0x80D946EF), "color palette preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-4);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 50, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Drag the single window through the actual native mouse handler.
        double startX = context.computeOnClient(minecraft -> gui().getWindowBounds().x());
        move(context, () -> gui().getWindowBounds(), 0.08, 0.04);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> gui().getWindowBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().y() >= 8), "window drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().x() >= 7.99
                && (gui().getWindowBounds().x() + gui().getWindowBounds().width()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledWidth()
                && (gui().getWindowBounds().y() + gui().getWindowBounds().height()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledHeight()), "small window fits and stays clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
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
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % HudRenderer.ROW_HEIGHT == 0), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, Theme.displayName(entry.module().getName()), false))).orElseThrow();
                return widest.y() == 0;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 0 || entry.y() == HudRenderer.ROW_HEIGHT), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.THEMES));
            context.waitTicks(5);
            context.runOnClient(minecraft -> {
                Theme.setAccent(Theme.BLUE);
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.15);
                Client.INSTANCE.getThemeManager().changed();
            });
            context.waitFor(minecraft -> Theme.accent() == Theme.BLUE && Math.abs(gui().getCanvasScale() - 1.15) < 0.001, ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> Theme.accentTarget() == Theme.BLUE), "theme selection animates across GUI and HUD");
            context.runOnClient(minecraft -> {
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.0);
                Theme.setAccent(Theme.TEAL); gui().getThemeEditor().reveal("Font Selection");
            });
            context.waitFor(minecraft -> gui().getCanvasScale() == 1 && Theme.accent() == Theme.TEAL, ANIMATION_TIMEOUT);
            context.waitTicks(8);
            context.takeScreenshot("riserecode-themes");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.FRIENDS));
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiFriend"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI adds through the existing manager");
            context.takeScreenshot("riserecode-friends");
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> !Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI removes through the existing manager");
            context.runOnClient(minecraft -> { gui().selectPage(ClickGui.Page.PROFILES); fullBright().getBrightness().setValue(0.6); });
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiProfile"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(Files.exists(Client.INSTANCE.getProfileManager().getFile("GuiProfile")), "profile UI saves a real snapshot");
            context.runOnClient(minecraft -> fullBright().getBrightness().setValue(0.2));
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() == 0.6), "profile UI restores settings and refreshes");
            context.takeScreenshot("riserecode-profiles");
            context.runOnClient(minecraft -> {
                fullBright().getBrightness().setValue(1.0); gui().selectPage(ClickGui.Page.SEARCH);
                gui().getSearchField().setValue("tracer");
            });
            context.waitTicks(6);
            context.takeScreenshot("riserecode-search");
            context.getInput().resizeWindow(2560, 1440);
            context.waitTicks(5);
            double referenceWidth = context.computeOnClient(minecraft -> gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth());
            context.runOnClient(minecraft -> minecraft.options.guiScale().set(3));
            context.waitTicks(5);
            check(context.computeOnClient(minecraft -> Math.abs(gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth() - referenceWidth) < 1), "reference window size is independent of Minecraft GUI scale");
            context.runOnClient(minecraft -> {
                gui().getSearchField().setValue("render");
                var module = Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow();
                module.setEnabled(true);
            });
            context.waitTicks(6);
            context.runOnClient(minecraft -> {
                var button = gui().getModuleButton("Tracers");
                if (!button.isExpanded()) { button.mouseClicked(button.getBounds().x() + 10, button.getBounds().y() + 10, 1); }
            });
            context.waitTicks(8);
            context.takeScreenshot("risecode-reference-layout");
            context.runOnClient(minecraft -> {
                Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow().setEnabled(false);
                minecraft.options.guiScale().set(2);
            });
            context.getInput().resizeWindow(2048, 1152);
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue(""));
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
            EngineIntegrationTest.verify(context, world);
            CombatIntegrationTest.verify(context, world);
            ThemeIntegrationTest.verify(context);
            FontIntegrationTest.verify(context);
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        // Each phase owns its world fixture; prior combat/lighting state cannot leak into the cave proof.
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); Phase8IntegrationTest.verify(context, world);
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
        return gui().getModuleButton(name);
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().isOpen(), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.centerY() - control.centerY()) / 24;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 24));
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

#### src/gametest/java/dev/riserecode/Phase8IntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.*;
import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.render.ItemPhysicsState;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.ColorUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Exercises native camera/input, untouched server state, shader draws and persistent HUD editing. */
public final class Phase8IntegrationTest {
    private static int assertions;
    private Phase8IntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        context.getInput().resizeWindow(2048, 1152);
        var client = Client.INSTANCE;
        var modules = client.getModuleManager();
        context.runOnClient(mc -> {
            modules.disableAll(); client.getThemeManager().reset(); mc.options.guiScale().set(2);
            mc.options.bobView().set(false); mc.options.setCameraType(CameraType.FIRST_PERSON);
            client.getHudRenderer().setEnabled(true);
        });
        // An enclosed room in the disposable integrated test world has zero block and sky light.
        world.getServer().runCommand("fill 96 70 96 104 76 104 minecraft:stone hollow");
        world.getServer().waitFor(server -> server.overworld().getBrightness(LightLayer.SKY, new BlockPos(100, 71, 100)) == 0, 400);
        world.getServer().runCommand("tp @a 100.5 71 100.5 0 0");
        world.getServer().runCommand("time set midnight");
        world.getConnection().waitForChunksRender(); context.waitTicks(25);
        context.waitFor(mc -> mc.player.getX() > 99 && mc.player.getX() < 102);
        context.runOnClient(mc -> mc.options.gamma().set(0.0)); context.waitTicks(8);
        context.takeScreenshot("phase8-cave-lighting-check");
        String caveStatus = context.computeOnClient(mc -> mc.player.blockPosition() + " block=" + mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) + " sky=" + mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()));
        client.getLogger().info("Cave setup: {}", caveStatus);
        context.waitFor(mc -> mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) == 0 && mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()) == 0, 400);
        check(context.computeOnClient(mc -> mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) == 0
                && mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()) == 0), "test cave has no light sources");
        var fullbright = module(context, FullBrightModule.class);
        Object original = context.computeOnClient(mc -> mc.gameRenderer.levelLightmap());
        context.takeScreenshot("phase8-cave-dark"); double dark = average("phase8-cave-dark");
        context.runOnClient(mc -> { fullbright.getMode().setValue(FullBrightModule.Mode.LIGHTMAP); fullbright.setEnabled(true); });
        context.waitTicks(10);
        check(context.computeOnClient(mc -> mc.gameRenderer.levelLightmap() != original && mc.options.gamma().get() == 0), "lightmap mode substitutes texture without changing gamma");
        context.takeScreenshot("phase8-cave-fullbright"); double bright = average("phase8-cave-fullbright");
        check(bright > dark * 2 && bright > 25, "zero-light cave becomes visibly illuminated");
        context.runOnClient(mc -> fullbright.setEnabled(false)); context.waitTicks(8);
        check(context.computeOnClient(mc -> mc.gameRenderer.levelLightmap() == original && mc.options.gamma().get() == 0), "native lightmap identity and gamma restore exactly");
        context.takeScreenshot("phase8-cave-restored"); double restored = average("phase8-cave-restored");
        check(Math.abs(restored - dark) < 3, "disabled cave returns to original darkness");
        context.runOnClient(mc -> {
            fullbright.getMode().setValue(FullBrightModule.Mode.GAMMA); fullbright.getBrightness().setValue(0.7); fullbright.setEnabled(true);
        });
        context.waitTick(); check(context.computeOnClient(mc -> mc.options.gamma().get() == 700), "gamma mode bypasses slider clamp");
        context.runOnClient(mc -> { fullbright.setEnabled(false); fullbright.getMode().setValue(FullBrightModule.Mode.LIGHTMAP); fullbright.setEnabled(true); });

        var overlay = module(context, BlockOverlayModule.class); context.runOnClient(mc -> overlay.setEnabled(true));
        context.waitFor(mc -> mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK && client.getWorldRenderer().getQuads().size() == 6);
        check(context.computeOnClient(mc -> client.getWorldRenderer().getLines().size() == 12), "targeted block has twelve independent edges");
        context.waitTicks(8); context.takeScreenshot("phase8-block-overlay");
        context.runOnClient(mc -> flag(overlay, "OverlayEnabled", false)); context.waitFor(mc -> client.getWorldRenderer().getQuads().isEmpty());
        check(context.computeOnClient(mc -> client.getWorldRenderer().getLines().size() == 12), "fill disabled preserves outline");
        context.runOnClient(mc -> { flag(overlay, "OverlayEnabled", true); flag(overlay, "OutlineEnabled", false); });
        context.waitFor(mc -> client.getWorldRenderer().getLines().isEmpty() && client.getWorldRenderer().getQuads().size() == 6);
        context.runOnClient(mc -> flag(overlay, "OutlineEnabled", true));

        var weather = module(context, WeatherChangerModule.class);
        float serverRain = world.getServer().computeOnServer(server -> server.overworld().getRainLevel(1));
        context.runOnClient(mc -> { choice(weather, "Weather", WeatherChangerModule.Weather.THUNDER); weather.setEnabled(true); }); context.waitTicks(4);
        check(context.computeOnClient(mc -> mc.level.getRainLevel(1) == 1 && mc.level.getThunderLevel(1) == 1), "local thunder and rain override");
        check(world.getServer().computeOnServer(server -> server.overworld().getRainLevel(1)) == serverRain, "server weather remains unchanged");
        context.runOnClient(mc -> { choice(weather, "Weather", WeatherChangerModule.Weather.CLEAR); }); context.waitTick();
        check(context.computeOnClient(mc -> mc.level.getRainLevel(1) == 0 && mc.level.getThunderLevel(1) == 0), "weather setting updates immediately");
        context.runOnClient(mc -> weather.setEnabled(false));

        // Camera input is consumed locally; body position and yaw stay vanilla.
        var freelook = module(context, FreeLookModule.class);
        var freecam = module(context, FreeCamModule.class);
        context.runOnClient(mc -> freelook.setEnabled(true));
        float yaw = context.computeOnClient(mc -> mc.player.getYRot());
        context.getInput().holdKey(InputConstants.KEY_LALT); context.waitTicks(3);
        check(context.computeOnClient(mc -> freelook.isHeld() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK), "hold key enters freelook perspective");
        Vec3 eye = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position());
        context.getInput().moveCursor(180, 0); context.waitTicks(2);
        context.getInput().moveCursor(120, 0); context.waitTicks(3);
        context.runOnClient(mc -> client.getLogger().info("Freelook native input: held={} bodyYaw={} cameraYaw={} grabbed={}", freelook.isHeld(), mc.player.getYRot(), mc.gameRenderer.mainCamera().yRot(), mc.mouseHandler.isMouseGrabbed()));
        check(context.computeOnClient(mc -> mc.player.getYRot() == yaw && Math.abs(mc.gameRenderer.mainCamera().yRot() - yaw) > 1), "freelook rotates camera without rotating body");
        check(context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position().distanceTo(eye) > 0.1), "freelook third-person camera orbits the player");
        context.getInput().releaseKey(InputConstants.KEY_LALT); context.waitTicks(3);
        check(context.computeOnClient(mc -> !freelook.isHeld() && mc.options.getCameraType() == CameraType.FIRST_PERSON), "freelook release restores original view");
        context.runOnClient(mc -> { freelook.setEnabled(false); freecam.setEnabled(true); });
        Vec3 playerPosition = context.computeOnClient(mc -> mc.player.position());
        Vec3 cameraPosition = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position());
        context.getInput().holdKey(InputConstants.KEY_W); context.getInput().holdKey(InputConstants.KEY_SPACE); context.waitTicks(8);
        context.getInput().releaseKey(InputConstants.KEY_W); context.getInput().releaseKey(InputConstants.KEY_SPACE);
        check(context.computeOnClient(mc -> mc.player.position().distanceTo(playerPosition) < 0.05 && mc.gameRenderer.mainCamera().position().distanceTo(cameraPosition) > 1), "freecam moves independently while player remains in place");
        context.runOnClient(mc -> freecam.setEnabled(false)); context.waitTicks(4);
        check(context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition()) < 0.02), "freecam disable restores native player camera");

        // Server-spawned drops exercise the native renderer rather than fabricated render state.
        world.getServer().runCommand("summon minecraft:item 100.5 71 102 {Item:{id:\"minecraft:diamond\",count:1},PickupDelay:32767,Tags:[\"phase8_drop\"]}");
        world.getServer().runCommand("give @a minecraft:diamond_sword[minecraft:enchantment_glint_override=true]");
        context.waitTicks(20);
        var physics = module(context, ItemPhysicsModule.class); var enchant = module(context, EnchantEffectModule.class);
        AtomicInteger flatRenders = new AtomicInteger(), styledItems = new AtomicInteger();
        var itemAudit = client.getEventBus().subscribe(ItemRenderEvent.class, -100, true, event -> {
            if (event.isCancelled() && ((ItemPhysicsState) event.state()).riserecode$onGround()) { flatRenders.incrementAndGet(); }
        });
        var enchantAudit = client.getEventBus().subscribe(EnchantmentRenderEvent.class, -100, false, event -> {
            if (event.style() != null && event.style().outline() && event.style().overlay()) { styledItems.incrementAndGet(); }
        });
        try {
            context.runOnClient(mc -> {
                physics.setEnabled(true); enchant.setEnabled(true);
                for (int slot = 0; slot < 9; slot++) {
                    if (mc.player.getInventory().getItem(slot).is(net.minecraft.world.item.Items.DIAMOND_SWORD)) { mc.player.getInventory().setSelectedSlot(slot); break; }
                }
                mc.player.setXRot(25); mc.player.setYRot(0); mc.player.setOldPosAndRot();
            });
            context.waitFor(mc -> flatRenders.get() > 0 && styledItems.get() > 0);
            context.waitTicks(8); context.takeScreenshot("phase8-item-physics-enchant");
            check(flatRenders.get() > 0 && styledItems.get() > 0, "item mesh transforms and enchant shaders receive real render submissions");
            context.runOnClient(mc -> { number(enchant, "GlintSpeed", 2); number(enchant, "GlintScale", 0.7); });
            context.waitTicks(8); context.takeScreenshot("phase8-enchant-live-settings");
            check(world.getServer().computeOnServer(server -> java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                    .anyMatch(entity -> entity.entityTags().contains("phase8_drop") && entity.onGround())), "dropped item keeps ordinary server physics");
        } finally { itemAudit.close(); enchantAudit.close(); }

        context.runOnClient(mc -> {
            for (Module module : modules.getModules()) { if (module instanceof HudModule) { module.setEnabled(true); } }
            choice(client.getThemeManager(), "Gradient Mode", ColorUtil.GradientMode.RAINBOW);
            choice(client.getThemeManager(), "Gradient Direction", ColorUtil.GradientDirection.VERTICAL);
            ((DoubleSetting) client.getThemeManager().getSetting("Gradient Speed")).setValue(0.4);
            client.getThemeManager().changed();
        });
        world.getServer().runCommand("effect give @a minecraft:speed 90 0 true"); context.waitTicks(10);
        context.runOnClient(mc -> mc.player.setXRot(0));
        context.waitTicks(15); context.takeScreenshot("phase8-gradient-arraylist");
        context.getInput().pressKey(InputConstants.KEY_F1);
        check(context.computeOnClient(mc -> mc.gui.hud.isHidden()), "native HUD can be hidden");
        double hiddenTime = context.computeOnClient(mc -> dev.riserecode.ui.theme.Theme.gradientTime()); context.waitTicks(8);
        check(context.computeOnClient(mc -> dev.riserecode.ui.theme.Theme.gradientTime() > hiddenTime), "frame gradient clock keeps running while HUD extraction is hidden");
        context.getInput().pressKey(InputConstants.KEY_F1);
        int color = context.computeOnClient(mc -> client.getThemeManager().getGradientColor(20, dev.riserecode.ui.theme.Theme.gradientTime()));
        context.waitTicks(15);
        check(context.computeOnClient(mc -> client.getThemeManager().getGradientColor(20, dev.riserecode.ui.theme.Theme.gradientTime()) != color), "gradient changes smoothly with live frame time");
        context.takeScreenshot("phase8-gradient-arraylist-later");
        var sprint = module(context, SprintModule.class);
        context.runOnClient(mc -> { sprint.setEnabled(true); weather.setEnabled(true); });
        context.waitFor(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather).count() == 2
                && client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather).allMatch(e -> e.visibility() > 0 && e.visibility() < 1), 100);
        check(context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather)
                .allMatch(e -> e.visibility() > 0 && e.visibility() < 1)), "two new array entries animate concurrently");
        double entering = context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint).findFirst().orElseThrow().visibility());
        context.runOnClient(mc -> sprint.setEnabled(false));
        double retargeted = context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint).findFirst().orElseThrow().visibility());
        check(retargeted <= entering + 0.25 && retargeted > 0, "mid-flight disable retains visible entry");

        command(context, ".hud"); context.waitForScreen(HudEditorScreen.class); context.waitTicks(10);
        check(context.computeOnClient(mc -> editor().isPauseScreen() && client.getHudRenderer().getBoxes().size() == 7), "HUD editor pauses and previews every element");
        var fps = context.computeOnClient(mc -> client.getHudRenderer().getElements().stream().filter(e -> e.id().equals("FPS")).findFirst().orElseThrow());
        double oldX = fps.layout().x();
        moveGui(context, () -> box("FPS"), 0.5, 0.5); context.getInput().holdMouse(0);
        setGuiCursor(context, 512, 288); context.waitTicks(5);
        check(context.computeOnClient(mc -> editor().isDraggingElement()), "real mouse captures HUD drag");
        context.takeScreenshot("phase8-hud-editor-dragged");
        context.getInput().releaseMouse(0); context.waitTicks(5);
        check(fps.layout().x() > oldX + 0.1, "drag changes normalized HUD anchor");
        double savedX = fps.layout().x(), savedY = fps.layout().y();
        moveGui(context, () -> editor().getControls().getFirst().getBounds(), 0.35, 0.75);
        context.getInput().holdMouse(0);
        moveGui(context, () -> editor().getControls().getFirst().getBounds(), 0.45, 0.75);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(mc -> editor().getControls().getFirst() instanceof Slider && fps.layout().scale.getValue() > 1.5), "editor context scale slider updates live through mouse input");
        moveGui(context, () -> editor().getControls().get(1).getBounds(), 0.5, 0.5); context.getInput().pressMouse(0);
        check(fps.layout().alignment.getValue() == HudLayout.Alignment.CENTER, "editor context alignment control");
        context.runOnClient(mc -> { fps.layout().scale.setValue(1.35); fps.layout().alignment.setValue(HudLayout.Alignment.CENTER); fps.layout().background.setValue(HudLayout.Background.BLUR); });
        context.waitTicks(8); context.takeScreenshot("phase8-hud-editor-customized");
        command(context, ".profile save Phase8");
        context.runOnClient(mc -> { fps.layout().position(0.1, 0.1); ((DoubleSetting) client.getThemeManager().getSetting("Animation Speed")).setValue(2.0); choice(client.getThemeManager(), "Gradient Mode", ColorUtil.GradientMode.STATIC); });
        command(context, ".profile load Phase8");
        check(fps.layout().x() == savedX && fps.layout().y() == savedY && fps.layout().scale.getValue() == 1.35, "profile restores HUD position and element styling");
        check(context.computeOnClient(mc -> client.getThemeManager().choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.RAINBOW
                && client.getThemeManager().number("Animation Speed") == 1), "profile restores gradient and animation speed");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(mc -> mc.gui.screen() == null);
        check(Files.exists(client.getConfigManager().getFile()), "editor exit saves config");
        command(context, ".hudedit"); context.waitForScreen(HudEditorScreen.class);
        double editedX = savedX + 0.05;
        context.runOnClient(mc -> fps.layout().position(editedX, savedY));
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> mc.gui.screen() == null);
        context.runOnClient(mc -> fps.layout().position(0, 0)); command(context, ".profile load Phase8");
        check(fps.layout().x() == editedX && fps.layout().y() == savedY, "editor exit persists the selected named profile without another save command");
        context.runOnClient(mc -> { fps.layout().position(0, 0); client.getConfigManager().load(); });
        check(fps.layout().x() == editedX && fps.layout().y() == savedY, "layout survives main config reload");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitForScreen(ClickGui.class); context.waitFor(mc -> client.getClickGui().isOpen(), 240);
        context.runOnClient(mc -> client.getClickGui().selectPage(ClickGui.Page.HUD)); context.waitTicks(8);
        context.takeScreenshot("phase8-hud-clickgui");
        double canvas = context.computeOnClient(mc -> client.getClickGui().getCanvasScale());
        UIComponent.Bounds edit = context.computeOnClient(mc -> client.getClickGui().getEditHudButton().getBounds());
        setGuiCursor(context, edit.centerX() * canvas, edit.centerY() * canvas);
        context.getInput().pressMouse(0); context.waitForScreen(HudEditorScreen.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(mc -> mc.gui.screen() == null);
        context.runOnClient(mc -> { modules.disableAll(); client.getThemeManager().save(); client.getConfigManager().save(); });
        context.runOnClient(mc -> {
            mc.options.gamma().set(0.23); fullbright.getMode().setValue(FullBrightModule.Mode.GAMMA); fullbright.getBrightness().setValue(1.0); fullbright.setEnabled(true);
            check(mc.options.gamma().get() == 1000, "shutdown fixture has an active gamma override");
            client.shutdown();
            check(mc.options.gamma().get() == 0.23 && mc.gameRenderer.levelLightmap() == original, "client shutdown restores gamma and native lightmap");
        });
        client.getLogger().info("Phase 8 native OpenGL checks passed ({} assertions); cave brightness dark={} fullbright={} restored={}", assertions, dark, bright, restored);
    }
    private static UIComponent.Bounds box(String id) { var b = Client.INSTANCE.getHudRenderer().getBoxes().stream().filter(v -> v.element().id().equals(id)).findFirst().orElseThrow(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), b.height()); }
    private static HudEditorScreen editor() { return (HudEditorScreen) net.minecraft.client.Minecraft.getInstance().gui.screen(); }
    private static <T extends Module> T module(ClientGameTestContext context, Class<T> type) { return context.computeOnClient(mc -> Client.INSTANCE.getModuleManager().getByClass(type).orElseThrow()); }
    private static void number(Module module, String name, double value) { ((DoubleSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static void flag(Module module, String name, boolean value) { ((BooleanSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static <E extends Enum<E>> void choice(Module module, String name, E value) { @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(); setting.setValue(value); }
    private static <E extends Enum<E>> void choice(ThemeManager themes, String name, E value) { @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) themes.getSetting(name); setting.setValue(value); }
    private static void command(ClientGameTestContext context, String command) { context.runOnClient(mc -> mc.getConnection().sendChat(command)); context.waitTick(); }
    private static void moveGui(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) { double[] p = context.computeOnClient(mc -> { var b = bounds.get(); return new double[] {b.x() + b.width()*x, b.y() + b.height()*y}; }); setGuiCursor(context, p[0], p[1]); }
    private static void setGuiCursor(ClientGameTestContext context, double x, double y) { double[] p = context.computeOnClient(mc -> new double[] {x * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(), y * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()}); context.getInput().setCursorPos(p[0], p[1]); context.waitTick(); }
    private static double average(String name) {
        Path directory = Path.of("screenshots");
        try (var paths = Files.list(directory)) {
            Path path = paths.filter(p -> p.getFileName().toString().contains(name)).findFirst().orElseThrow();
            try (var stream = Files.newInputStream(path); NativeImage image = NativeImage.read(stream)) {
                double sum = 0; int count = 0;
                for (int y = image.getHeight()/3; y < image.getHeight()*2/3; y += 3) { for (int x = image.getWidth()/3; x < image.getWidth()*2/3; x += 3) {
                    int rgb = image.getPixel(x,y); sum += ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255))/3.0; count++;
                } }
                return sum/count;
            }
        } catch (IOException failure) { throw new AssertionError("Read native screenshot", failure); }
    }
    private static void check(boolean passed, String message) { assertions++; if (!passed) { throw new AssertionError(message); } }
}
```

#### src/test/java/dev/riserecode/Phase8Checks.java

```java
package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Behavioral checks for overlapping animations and atomic visual/layout profile restoration. */
public final class Phase8Checks {
    private static int assertions;
    private Phase8Checks() { }
    public static void main(String[] args) throws Exception {
        AnimationManager manager = AnimationManager.ui(); manager.clear();
        Animation.configureUi(() -> 1, () -> MathUtil.Easing.LINEAR);
        ArrayList<Animation> animations = new ArrayList<>();
        for (int i = 0; i < 80; i++) { animations.add(Animation.ui(0, 1, MathUtil.Easing.OUT_QUART).animateTo(1)); }
        manager.frame(1_000_000_000L); manager.frame(1_200_000_000L);
        check(animations.stream().allMatch(a -> Math.abs(a.getValue() - 0.2) < 1e-10), "80 animations advance together");
        Animation a = animations.getFirst(); a.animateTo(0);
        check(a.getValue() == 0.2 && a.getTarget() == 0, "reversal retains current rendered value");
        Animation b = Animation.ui(0, 1, MathUtil.Easing.LINEAR).animateTo(1);
        manager.frame(1_300_000_000L);
        check(Math.abs(a.getValue() - 0.18) < 1e-10 && Math.abs(b.getValue() - 0.1) < 1e-10, "new entry progresses while earlier one exits");
        double before = b.getValue(); b.update(0.2); b.update(0.2);
        check(b.getValue() == before, "legacy component reads cannot double-advance a scheduled animation");
        expectInvalid(() -> b.update(-1));
        a.pause(); manager.update(0.1); check(Math.abs(a.getValue() - 0.18) < 1e-10, "paused state is independent of siblings");
        a.resume(); manager.update(0.1); check(a.getValue() < 0.18, "resume preserves the reversed segment");
        manager.update(2);
        check(manager.activeCount() == 0 && a.getValue() == 0 && b.getValue() == 1, "finished states leave the active set and land exactly");
        Animation backend = new Animation(0, 1, MathUtil.Easing.LINEAR).animateTo(1); backend.update(0.3);
        check(backend.getValue() == 0.3, "backend timing remains explicit and unthemed"); manager.clear();

        check(ColorUtil.hsv(0, 1, 1) == 0xFFFF0000 && ColorUtil.hsv(1, 1, 1) == 0xFFFF0000, "rainbow wraps without a seam");
        for (ColorUtil.GradientMode mode : ColorUtil.GradientMode.values()) {
            int first = ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 0, 0, 1, 100);
            int repeated = ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 100, 0, 1, 100);
            check(first == repeated, "spatial gradient cycle " + mode);
            check((first >>> 24) == 255, "opaque gradient " + mode);
            if (mode != ColorUtil.GradientMode.STATIC) {
                check(first != ColorUtil.gradient(mode, 0xFF0000FF, 0xFFFF00FF, 0, 0.15, 1, 100), "time changes " + mode);
            }
        }
        HudLayout layout = new HudLayout(0.2, 0.3); layout.scale.setValue(1.4); layout.background.setValue(HudLayout.Background.BLUR);
        JsonObject saved = layout.snapshot(); layout.position(0.8, 0.9); layout.apply(saved);
        check(layout.x() == 0.2 && layout.y() == 0.3 && layout.scale.getValue() == 1.4 && layout.background.getValue() == HudLayout.Background.BLUR, "layout round trip");
        saved.addProperty("x", 0.5); check(layout.x() == 0.2, "layout snapshot is detached");
        JsonObject bad = layout.snapshot(); bad.addProperty("x", Double.NaN); expectInvalid(() -> layout.apply(bad));
        check(layout.x() == 0.2, "invalid layout makes no partial changes");
        layout.position(-1, 9); check(layout.x() == 0 && layout.y() == 1, "normalized anchors clamp to the screen");
        expectInvalid(() -> new HudLayout(Double.POSITIVE_INFINITY, 0));
        expectInvalid(() -> new Render3DEvent.Quad(Vec3.ZERO, Vec3.ZERO, new Vec3(Double.NaN, 0, 0), Vec3.ZERO, 0));

        Path directory = Files.createTempDirectory("risecode-phase8-check-");
        try {
            var logger = LoggerFactory.getLogger(Phase8Checks.class);
            ThemeManager themes = new ThemeManager(directory, logger); themes.load();
            EventBus bus = new EventBus(); ModuleManager modules = new ModuleManager(bus, logger);
            Module test = modules.register(new Module("Profile fixture", "", Category.HUD, bus) { });
            Module movement = modules.register(new Module("Movement fixture", "", Category.MOVEMENT, bus) { });
            ConfigManager config = new ConfigManager(directory, modules, () -> true, ignored -> { }, logger);
            config.useExternalVisualPreferences();
            config.bindHudStore(layout::snapshot, layout::apply, HudLayout::validate);
            config.bindVisualPolicyStore(themes::policySnapshot, themes::applyPolicy, themes::validatePolicy);
            ProfileManager profiles = new ProfileManager(config, () -> { }, logger);
            layout.position(0.25, 0.45); modules.setEnabled(test, true);
            setChoice(themes, "Gradient Mode", ColorUtil.GradientMode.ASTOLFO);
            ((DoubleSetting) themes.getSetting("Animation Speed")).setValue(2.5);
            ((DoubleSetting) themes.getSetting("Font Scale")).setValue(1.3);
            check(profiles.saveProfile("Visual"), "combined profile saved");
            layout.position(0.8, 0.8); modules.setEnabled(test, false);
            setChoice(themes, "Gradient Mode", ColorUtil.GradientMode.STATIC);
            ((DoubleSetting) themes.getSetting("Animation Speed")).setValue(1.0);
            ((DoubleSetting) themes.getSetting("Font Scale")).setValue(1.1);
            check(profiles.loadProfile("Visual"), "combined profile loaded");
            check(layout.x() == 0.25 && layout.y() == 0.45 && test.isEnabled(), "profile restores HUD anchors and modules");
            check(themes.choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.ASTOLFO && themes.number("Animation Speed") == 2.5, "profile restores animation and gradient policy");
            check(themes.number("Font Scale") == 1.1, "profile preserves independent typography preferences");
            JsonObject invalid = config.profileSnapshot(); invalid.getAsJsonObject("visualPolicy").addProperty("Animation Speed", 99);
            invalid.getAsJsonObject("hudLayout").addProperty("x", 0.9);
            expectInvalid(() -> config.applyProfile(invalid));
            check(layout.x() == 0.25 && test.isEnabled() && themes.number("Animation Speed") == 2.5, "invalid visual policy cannot partially move HUD or restart modules");
            check(config.save(), "layout saves to main config"); layout.position(0, 0); config.load();
            check(layout.x() == 0.25 && layout.y() == 0.45, "main config reload restores layout");
            layout.position(0.35, 0.55); modules.setEnabled(test, false); modules.setEnabled(movement, true);
            check(profiles.saveActiveHudLayout(), "editor persists its selected profile");
            layout.position(0, 0); modules.setEnabled(test, true); check(profiles.loadProfile("Visual"), "edited profile reloads");
            check(layout.x() == 0.35 && layout.y() == 0.55 && !test.isEnabled(), "profile retains edited HUD layout and toggle");
            check(!movement.isEnabled(), "editor save preserves the profile's movement/combat snapshot");
            config.save();
            check(config.getActiveProfile().equals("Visual"), "selected profile persists in main config");
            modules.disableAll();
        } finally {
            try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); } }
            manager.clear(); Animation.configureUi(() -> 1, () -> null);
        }
        System.out.println("Phase 8 checks passed (" + assertions + " assertions).");
    }
    private static <E extends Enum<E>> void setChoice(ThemeManager manager, String name, E value) {
        @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) manager.getSetting(name); setting.setValue(value);
    }
    private static void expectInvalid(Runnable action) { try { action.run(); throw new AssertionError("Expected invalid input"); } catch (IllegalArgumentException expected) { assertions++; } }
    private static void check(boolean result, String message) { assertions++; if (!result) { throw new AssertionError(message); } }
}
```

## 3. Configuration and integration notes

### Persistence

- `config/riserecode/config.json` is schema 3. `hudLayout` stores each registered
  element's normalized x/y position, scale (0.5–3), alignment and background, plus
  snap/grid preferences. Resolution changes preserve anchors and rendered boxes
  are clamped to the screen. Older schema 1/2 configurations remain readable.
- `config/riserecode/profiles/<name>.json` contains module enabled states, keybinds,
  typed settings, `hudLayout` and `visualPolicy`. Visual policy contains gradient
  mode/speed/direction/spacing, global animation speed and easing curve.
- `.profile save <name>` and `.profile load <name>` select the active module
  profile. The main config remembers `activeProfile`. Closing the HUD editor
  atomically saves the main layout and updates only the selected profile's HUD
  module state/settings, layout and visual policy; the profile's saved combat,
  movement, player and world module snapshot is preserved.
- Fonts, primary/secondary colors, blur preferences, overall HUD visibility and
  other visual settings continue to live in `theme.json` / `theme-profiles/`.
  Gradient and animation controls are also included in those visual snapshots;
  loading a module profile applies its explicit visual policy immediately.
- Friends and command prefix remain global. Profile validation finishes before
  lifecycle changes; failed application attempts to restore the previous state.
  Missing/new elements use defaults and existing font resources are retained.

### Animation and rendering wiring

`GameRenderer.extract` drives `AnimationManager.UI.frame` once per frame and advances
Theme's gradient clock even while F1 hides the HUD. Each active animation advances
from its own current value and target using the same bounded frame delta. Retargeting
captures the current value, so reversals and separate module toggles never wait for
another animation. Existing per-component `update` calls cannot double-step a
managed UI animation. There are no rendering or animation worker threads.

Modules keep owned EventBus subscriptions. `HudModule` supplies immutable content;
`HudRenderer` handles fonts, shadow, glow, background and positioning. Camera and
mouse/keyboard mixins feed original CameraEvent, MouseRotationEvent and
CameraInputEvent types into the same bus. FreeCam owns the camera without moving
the player; FreeLook consumes mouse input only while its hold key is down.
Weather overrides read queries and leaves server weather unchanged.

FullBright LIGHTMAP supplies a cached white 16×16 RGBA GPU texture at the native
lightmap binding while Minecraft continues updating its untouched lightmap.
Disabling restores the native binding and closes the owned texture/view. GAMMA
sets only the gamma option's raw value (up to 1000), restoring the captured value
when changing to LIGHTMAP, disabling, or shutting down. Shutdown saves enabled
states first, then disables modules and restores gamma before vanilla options save.

World extraction collects immutable block faces and edges. The submit stage uses
camera-relative vertices through native RenderTypes and SubmitNodeCollector.
BlockOverlay uses all boxes in the selected block shape, with independent fill and
outline settings. ItemPhysics modifies render state/pose only. EnchantEffect uses
three bundled shaders through Blaze3D RenderPipelines: expanded alpha-masked mesh
silhouettes and a depth-matched, tinted scrolling glint. WorldItems extends outlines
to dropped items and frames. The native translucent glint output target is preserved.

HUD Blur backgrounds reuse the existing scene capture and downscaled blur targets;
this phase does not introduce a new per-element full-resolution framebuffer.
Gradients sample transformed screen coordinates for horizontal/vertical bands,
including Qraftys bitmap text and the sidebar selection pill.

### Controls

1. Right Shift opens ClickGUI. HUD contains five new HUD modules and **Edit HUD**.
   `.hud` / `.hudedit` also opens the editor and closes the ClickGUI cleanly.
2. Drag an element. Grid snapping and center/edge/neighbor alignment guides are
   available; **G** toggles snapping. Right click opens its scale, text alignment,
   background and module controls. Escape or Right Shift saves and exits.
   Editor preview includes real data for all seven elements, even disabled modules.
   A singleplayer world pauses while editing.
3. Themes → Colors & Glow exposes Static, Rainbow, Astolfo and Sine gradients,
   speed, direction and spacing. Themes → Animations controls global speed/easing.
4. FullBright defaults to LIGHTMAP. Choose GAMMA to expose its Brightness setting.
5. Enable FreeLook and hold Left Alt; `.bind FreeLook F6` changes the hold key.
   Enable FreeCam to fly the view with WASD / Jump / Sneak. Releasing FreeLook or
   disabling the camera modules restores the normal view.
6. Render contains BlockOverlay and EnchantEffect; World contains ItemPhysics and
   WeatherChanger. Newly registered modules start disabled on an existing install
   unless enabled in the user's config/profile.

Development: `./gradlew runClient`. Build and CPU checks: `./gradlew build`.
Complete real-client regression: `./gradlew runClientGameTest`.
Focused Phase 8 run: `./gradlew runClientGameTest -Pphase8Only`.

## 4. Verification and regenerated OpenGL screenshots

The final source was built and tested with Java 25 and `-Xlint:all -Werror`:

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew build runClientGameTest --offline --no-daemon
```

- All seven CPU harnesses passed: **315 assertions** (90 foundation, 22 runtime,
  25 UI, 55 engine, 50 combat, 31 theme, 42 Phase 8).
- The full real-client OpenGL regression passed, including earlier UI, engine,
  combat, theme, blur and font checks plus **36 Phase 8 assertions**.
- The sealed cave fixture had block light 0 and sky light 0. Average screenshot
  brightness was **4.053568** disabled, **98.807543** with LIGHTMAP, and
  **4.053568** after disabling. Gamma mode and shutdown restoration also passed.
- Real inputs checked FreeLook hold/release, FreeCam movement without player
  movement, concurrent ArrayList entries/reversal, HUD drag, scale/alignment,
  command/button access and main/named-profile reload. Rendering exercised block
  faces/edges, flat dropped items and enchantment shaders. Server weather remained
  unchanged during client overrides. Gradient time continued with F1 active.
- These screenshots are actual Minecraft OpenGL output at 2048×1152. They were
  inspected after the final run; no mockups or postprocessed images are used.

| Evidence | Screenshot |
| --- | --- |
| HUD editor, FPS dragged to the center, guides visible | [HUD editor dragged](build/verification/phase8/opengl/0039_phase8-hud-editor-dragged.png) |
| FPS scale/alignment/blur live controls | [HUD editor customized](build/verification/phase8/opengl/0040_phase8-hud-editor-customized.png) |
| Independent block fill/outline on the target | [BlockOverlay](build/verification/phase8/opengl/0034_phase8-block-overlay.png) |
| Rainbow ArrayList and watermark | [Gradient frame](build/verification/phase8/opengl/0037_phase8-gradient-arraylist.png) |
| Gradient changes a few frames later | [Later gradient frame](build/verification/phase8/opengl/0038_phase8-gradient-arraylist-later.png) |
| Flat dropped diamond and tinted enchanted held sword | [ItemPhysics and EnchantEffect](build/verification/phase8/opengl/0035_phase8-item-physics-enchant.png) |
| New HUD category and editor button | [HUD ClickGUI](build/verification/phase8/opengl/0041_phase8-hud-clickgui.png) |
| Sealed cave with native lighting | [Dark cave](build/verification/phase8/opengl/0031_phase8-cave-dark.png) |
| Same cave with LIGHTMAP | [FullBright cave](build/verification/phase8/opengl/0032_phase8-cave-fullbright.png) |
| Native cave lighting restored | [Restored cave](build/verification/phase8/opengl/0033_phase8-cave-restored.png) |

All **42** screenshots from the complete run are in
[build/verification/phase8/opengl](build/verification/phase8/opengl).
[Build/native log](build/verification/phase8/build-and-opengl.log),
[Minecraft log](build/verification/phase8/minecraft.log), and
[machine-readable verification](build/verification/phase8/verification.json)
retain the test evidence. Graphics backend: OpenGL, Mesa 26.2.2-arch1.1, core 4.6.
Vulkan was not exercised in Phase 8. The native tests use an isolated test instance;
the user's Sodium/Iris modpack has been installed to, but was not launched by the tests.

### Installed release

Installed **riserecode-0.7.0.jar** into `/home/toast/.local/share/ModrinthApp/profiles/Fabric 26.2`.
SHA-256: `87d2a7735ef4495697439c5fcb49763b1853d106a5242c8b4d0a1f77f9a45fcd`. The release contains Java 25 class files (major 69),
client-only Fabric metadata, bundled original shaders and licensed fonts, and no
native-test fixtures, Meteor imports or copied external client sources.
Exactly one riserecode mod is installed. **11** other
mod files, **5** riserecode config/font files and
**1** resource pack file were verified unchanged.
Previous release and config backup: `/home/toast/.local/share/ModrinthApp/profiles/Fabric 26.2/riserecode-backups/phase8-20261006-231713`.
[Installation manifest](build/verification/phase8/installation.json).

Restart Minecraft in the Fabric 26.2 instance to load the update. The running game
was not stopped. The selected-profile UI and new modules become available after restart.
