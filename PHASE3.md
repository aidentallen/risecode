# Phase 3: Fabric runtime integration

## 1. Updated project file tree

Generated build, logs, run directories, and dependency caches are omitted.

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
│   │   │           └── ClientIntegrationTest.java
│   │   └── resources/
│   │       └── fabric.mod.json
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── config/
│   │   │           │   └── ConfigManager.java
│   │   │           ├── event/
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   └── KeyboardHandlerMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   └── FullBrightModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
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
│   │   │           │   └── hud/
│   │   │           │       └── HudRenderer.java
│   │   │           ├── util/
│   │   │           │   └── MathUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       └── lang/
│   │       │           └── en_us.json
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── FoundationChecks.java
│                   └── RuntimeChecks.java
├── .gitignore
├── PHASE3.md
├── README.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

## 2. Code files

The original five foundation classes, categories, typed settings, and FoundationChecks are unchanged. Every new Java file follows in full.


### src/main/java/dev/riserecode/Client.java

```java
package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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
    private HudRenderer hudRenderer;
    private ClickGui clickGui;
    private boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME + " " + version);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.load();
        moduleManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
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
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> client.shutdown());
    }
}
```


### src/main/java/dev/riserecode/event/UpdateEvent.java

```java
package dev.riserecode.event;

import net.minecraft.client.Minecraft;

/** Posted at the end of a client tick, including ticks outside a world. */
public final class UpdateEvent extends Event {
    private final Minecraft minecraft;
    private final long tick;

    public UpdateEvent(Minecraft minecraft, long tick) {
        this.minecraft = minecraft;
        this.tick = tick;
    }

    public Minecraft getMinecraft() { return minecraft; }
    public long getTick() { return tick; }
}
```


### src/main/java/dev/riserecode/event/Render2DEvent.java

```java
package dev.riserecode.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** HUD extraction phase; never retain the extractor after dispatch. */
public final class Render2DEvent extends Event {
    private final GuiGraphicsExtractor graphics;
    private final DeltaTracker deltaTracker;
    private final double deltaSeconds;

    public Render2DEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, double deltaSeconds) {
        this.graphics = graphics;
        this.deltaTracker = deltaTracker;
        this.deltaSeconds = deltaSeconds;
    }

    public GuiGraphicsExtractor getGraphics() { return graphics; }
    public DeltaTracker getDeltaTracker() { return deltaTracker; }
    public double getDeltaSeconds() { return deltaSeconds; }
}
```


### src/main/java/dev/riserecode/event/Render3DEvent.java

```java
package dev.riserecode.event;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;

/**
 * Posted after world extraction. Collect render state here; this is not a
 * legacy immediate-mode drawing hook. Do not retain the mutable context.
 */
public final class Render3DEvent extends Event {
    private final LevelExtractionContext context;

    public Render3DEvent(LevelExtractionContext context) { this.context = context; }
    public LevelExtractionContext getContext() { return context; }
}
```


### src/main/java/dev/riserecode/event/KeyPressEvent.java

```java
package dev.riserecode.event;

import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Raw keyboard input. Cancellation consumes the original Minecraft action. */
public final class KeyPressEvent extends Event {
    private final KeyEvent input;
    private final int action;

    public KeyPressEvent(KeyEvent input, int action) {
        super(true);
        this.input = input;
        this.action = action;
    }

    public KeyEvent getInput() { return input; }
    public int getKey() { return input.key(); }
    public int getAction() { return action; }
    public boolean isPress() { return action == GLFW.GLFW_PRESS; }
    public boolean isRelease() { return action == GLFW.GLFW_RELEASE; }
    public boolean isRepeat() { return action == GLFW.GLFW_REPEAT; }
}
```


### src/main/java/dev/riserecode/module/ModuleManager.java

```java
package dev.riserecode.module;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.modules.FullBrightModule;
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

    public void initDefaults(Minecraft minecraft) {
        register(new FullBrightModule(eventBus, minecraft));
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


### src/main/java/dev/riserecode/config/ConfigManager.java

```java
package dev.riserecode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.slf4j.Logger;

/** Versioned JSON with per-value validation and atomic replacement. */
public final class ConfigManager implements AutoCloseable {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    private final Path file;
    private final ModuleManager modules;
    private final BooleanSupplier hudEnabled;
    private final Consumer<Boolean> setHudEnabled;
    private final Logger logger;
    private JsonObject document = new JsonObject();
    private EventBus.Subscription subscription;
    private boolean dirty;
    private long saveAfterNanos;

    public ConfigManager(Path directory, ModuleManager modules, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this.directory = directory;
        file = directory.resolve("config.json");
        this.modules = modules;
        this.hudEnabled = hudEnabled;
        this.setHudEnabled = setHudEnabled;
        this.logger = logger;
    }

    public Path getFile() { return file; }

    public void init(EventBus eventBus) {
        if (subscription != null) { return; }
        subscription = eventBus.subscribe(UpdateEvent.class, -100, false, event -> {
            if (dirty && System.nanoTime() - saveAfterNanos >= 0) {
                if (!save()) { saveAfterNanos = System.nanoTime() + 5_000_000_000L; }
            }
        });
    }

    public void requestSave() {
        dirty = true;
        saveAfterNanos = System.nanoTime() + 500_000_000L;
    }

    public void load() {
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) { save(); return; }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) { throw new IllegalArgumentException("Config root must be an object"); }
                document = root.getAsJsonObject();
            }
        } catch (IOException | RuntimeException failure) {
            logger.warn("Could not read {}; preserving the bad file and using defaults", file, failure);
            try {
                if (Files.exists(file)) {
                    Files.move(file, directory.resolve("config.invalid-" + System.currentTimeMillis() + ".json"));
                }
                document = new JsonObject();
                save();
            } catch (IOException backupFailure) {
                logger.error("Could not preserve invalid config", backupFailure);
            }
            return;
        }
        apply("hudEnabled", () -> {
            if (document.has("hudEnabled")) { setHudEnabled.accept(booleanValue(document.get("hudEnabled"))); }
        });
        JsonObject entries = objectOrEmpty(document.get("modules"));
        // Restore every setting/keybind before any module starts running.
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            JsonObject values = objectOrEmpty(entry.get("settings"));
            for (Setting<?> setting : module.getSettings()) {
                if (values.has(setting.getName())) {
                    apply(module.getName() + "." + setting.getName(),
                            () -> loadSetting(setting, values.get(setting.getName())));
                }
            }
            if (entry.has("keybind")) {
                apply(module.getName() + ".keybind", () -> modules.setKeybind(module, integerValue(entry.get("keybind"))));
            }
        }
        for (Module module : modules.getModules()) {
            JsonObject entry = objectOrEmpty(entries.get(module.getName()));
            if (entry.has("enabled")) {
                apply(module.getName() + ".enabled", () -> modules.setEnabled(module, booleanValue(entry.get("enabled"))));
            }
        }
    }

    public boolean save() {
        Path temporary = null;
        try {
            JsonObject next = document.deepCopy();
            next.addProperty("schemaVersion", 1);
            next.addProperty("hudEnabled", hudEnabled.getAsBoolean());
            JsonObject entries = objectOrEmpty(next.get("modules"));
            next.add("modules", entries);
            for (Module module : modules.getModules()) {
                JsonObject entry = objectOrEmpty(entries.get(module.getName()));
                entries.add(module.getName(), entry);
                entry.addProperty("enabled", module.isEnabled());
                entry.addProperty("keybind", module.getKeybind());
                JsonObject values = objectOrEmpty(entry.get("settings"));
                entry.add("settings", values);
                for (Setting<?> setting : module.getSettings()) {
                    Object value = setting.getValue();
                    if (setting instanceof ColorSetting color) {
                        values.addProperty(setting.getName(), String.format("%08X", color.getValue()));
                    } else if (value instanceof Enum<?> option) {
                        values.addProperty(setting.getName(), option.name());
                    } else {
                        values.add(setting.getName(), gson.toJsonTree(value));
                    }
                }
            }
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "config-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { gson.toJson(next, writer); }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            document = next;
            dirty = false;
            return true;
        } catch (IOException | RuntimeException failure) {
            logger.error("Could not save {}", file, failure);
            dirty = true;
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException failure) { logger.warn("Could not delete temporary config {}", temporary, failure); }
            }
        }
    }

    private static void loadSetting(Setting<?> setting, JsonElement value) {
        if (setting instanceof BooleanSetting flag) { flag.setValue(booleanValue(value)); }
        else if (setting instanceof DoubleSetting number) {
            number.setValue(numberValue(value).getAsDouble());
        } else if (setting instanceof ColorSetting color) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || !value.getAsString().matches("[0-9a-fA-F]{8}")) {
                throw new IllegalArgumentException("Color must be eight ARGB hexadecimal digits");
            }
            color.setValue((int) Long.parseLong(value.getAsString(), 16));
        } else if (setting instanceof EnumSetting<?> choice) { loadEnum(choice, value); }
        else { throw new IllegalArgumentException("Unsupported setting type: " + setting.getClass().getName()); }
    }

    private static <E extends Enum<E>> void loadEnum(EnumSetting<E> setting, JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Enum value must be a name");
        }
        for (E option : setting.getOptions()) {
            if (option.name().equals(value.getAsString())) { setting.setValue(option); return; }
        }
        throw new IllegalArgumentException("Unknown enum option: " + value.getAsString());
    }

    private void apply(String label, Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException failure) { logger.warn("Ignoring invalid config value {}: {}", label, failure.getMessage()); }
    }

    private static boolean booleanValue(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Expected a boolean");
        }
        return value.getAsBoolean();
    }

    private static JsonPrimitive numberValue(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected a number");
        }
        return value.getAsJsonPrimitive();
    }

    private static int integerValue(JsonElement value) { return numberValue(value).getAsBigDecimal().intValueExact(); }
    private static JsonObject objectOrEmpty(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    @Override
    public void close() {
        if (subscription != null) { subscription.close(); }
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


### src/main/java/dev/riserecode/module/modules/FullBrightModule.java

```java
package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;

/** Uses the supported vanilla gamma range, without modifying world lighting. */
public final class FullBrightModule extends Module {
    private final Minecraft minecraft;
    private final DoubleSetting brightness = register(new DoubleSetting(
            "Brightness", "Vanilla brightness from 0% to 100%", 1.0, 0.0, 1.0));
    private Double previousGamma;

    public FullBrightModule(EventBus eventBus, Minecraft minecraft) {
        super("FullBright", "Raises client brightness and restores it on disable.", Category.RENDER, eventBus);
        this.minecraft = minecraft;
    }

    public DoubleSetting getBrightness() { return brightness; }

    @Override
    protected void onEnable() {
        previousGamma = minecraft.options.gamma().get();
        applyBrightness();
        listen(UpdateEvent.class, event -> applyBrightness());
    }

    private void applyBrightness() { minecraft.options.gamma().set(brightness.getValue()); }

    @Override
    protected void onDisable() {
        if (previousGamma != null) {
            minecraft.options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }
}
```


### src/main/java/dev/riserecode/ui/hud/HudRenderer.java

```java
package dev.riserecode.ui.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer implements AutoCloseable {
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final String watermark;
    private final EventBus.Subscription subscription;
    private final Animation visibility = new Animation(1.0, 0.15, MathUtil.Easing.IN_OUT_CUBIC);
    private boolean enabled = true;

    public HudRenderer(EventBus eventBus, Minecraft minecraft, ModuleManager modules, String watermark) {
        this.minecraft = minecraft;
        this.modules = modules;
        this.watermark = watermark;
        subscription = eventBus.subscribe(Render2DEvent.class, this::render);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    private void render(Render2DEvent event) {
        visibility.animateTo(enabled ? 1.0 : 0.0);
        double opacity = MathUtil.clamp01(visibility.update(event.getDeltaSeconds()));
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || opacity <= 0.0) { return; }
        GuiGraphicsExtractor graphics = event.getGraphics();
        int textColor = ((int) Math.round(opacity * 255.0) << 24) | 0xFFFFFF;
        int background = ((int) Math.round(opacity * 160.0) << 24) | 0x0F1115;
        graphics.fill(4, 4, minecraft.font.width(watermark) + 12, 20, background);
        graphics.text(minecraft.font, watermark, 8, 8, textColor, true);
        int y = 8;
        var sorted = modules.getEnabledModules().stream()
                .sorted(Comparator.comparingInt((Module module) -> minecraft.font.width(module.getName()))
                        .reversed().thenComparing(Module::getName)).toList();
        for (Module module : sorted) {
            int x = graphics.guiWidth() - minecraft.font.width(module.getName()) - 8;
            graphics.fill(x - 4, y - 3, graphics.guiWidth() - 4, y + 11, background);
            graphics.text(minecraft.font, module.getName(), x, y, textColor, true);
            y += 16;
        }
    }

    @Override
    public void close() { subscription.close(); }
}
```


### src/main/java/dev/riserecode/ui/clickgui/ClickGui.java

```java
package dev.riserecode.ui.clickgui;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.hud.HudRenderer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A native Screen with categories and paged module buttons, ready to extend. */
public final class ClickGui extends Screen {
    private final ModuleManager modules;
    private final HudRenderer hud;
    private final Runnable saveChanges;
    private Category selected = Category.RENDER;
    private Screen parent;
    private int page;

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal("RiseRecode"));
        this.modules = modules;
        this.hud = hud;
        this.saveChanges = saveChanges;
    }

    public void open(Screen parent) {
        this.parent = parent;
        minecraft.gui.setScreen(this);
    }

    @Override
    protected void init() {
        int left = 12;
        int contentX = left + 104;
        int contentWidth = Math.max(80, width - contentX - 12);
        int y = 42;
        for (Category category : Category.values()) {
            Button button = Button.builder(Component.literal(category.getDisplayName()), pressed -> {
                selected = category;
                page = 0;
                rebuildWidgets();
            }).bounds(left, y, 96, 20).build();
            button.active = category != selected;
            addRenderableWidget(button);
            y += 22;
        }
        List<Module> visible = modules.getByCategory(selected);
        int pageSize = Math.max(1, (height - 110) / 24);
        int maxPage = Math.max(0, (visible.size() - 1) / pageSize);
        page = Math.min(page, maxPage);
        for (int i = page * pageSize; i < Math.min(visible.size(), (page + 1) * pageSize); i++) {
            Module module = visible.get(i);
            Button button = Button.builder(moduleLabel(module), pressed -> {
                modules.toggle(module);
                pressed.setMessage(moduleLabel(module));
            }).bounds(contentX, 42 + (i % pageSize) * 24, contentWidth, 20)
                    .tooltip(Tooltip.create(Component.literal(module.getDescription()))).build();
            addRenderableWidget(button);
        }
        if (maxPage > 0) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("Previous"), button -> {
                page--;
                rebuildWidgets();
            }).bounds(contentX, height - 60, contentWidth / 2 - 2, 20).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
                page++;
                rebuildWidgets();
            }).bounds(contentX + contentWidth / 2 + 2, height - 60, contentWidth / 2 - 2, 20).build());
            next.active = page < maxPage;
        }
        addRenderableWidget(Button.builder(hudLabel(), button -> {
            hud.setEnabled(!hud.isEnabled());
            button.setMessage(hudLabel());
            saveChanges.run();
        }).bounds(12, height - 30, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width - 92, height - 30, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE80F1115);
        graphics.fill(8, 36, width - 8, height - 36, 0xFF181A20);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, "RiseRecode | " + selected.getDisplayName(), 12, 14, 0xFFFFFFFF, true);
        if (modules.getByCategory(selected).isEmpty()) {
            graphics.text(font, "No modules in this category", 120, 46, 0xFFA0A4B0, false);
        }
    }

    @Override
    public void onClose() {
        saveChanges.run();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static Component moduleLabel(Module module) {
        return Component.literal(module.getName() + (module.isEnabled() ? " [ON]" : " [OFF]"));
    }

    private Component hudLabel() { return Component.literal("HUD: " + (hud.isEnabled() ? "ON" : "OFF")); }
}
```


### src/main/java/dev/riserecode/mixin/KeyboardHandlerMixin.java

```java
package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.KeyPressEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void riserecode$keyPress(long window, int action, KeyEvent input, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized() || window != Minecraft.getInstance().getWindow().handle()) { return; }
        KeyPressEvent event = Client.INSTANCE.getEventBus().post(new KeyPressEvent(input, action));
        if (event.isCancelled()) { callback.cancel(); }
    }
}
```


### src/test/java/dev/riserecode/RuntimeChecks.java

```java
package dev.riserecode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Checks persistence and registry behavior without launching Minecraft. */
public final class RuntimeChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeChecks.class);
    private static int assertions;

    private RuntimeChecks() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("riserecode-config-check-");
        try {
            EventBus bus = new EventBus();
            ModuleManager manager = new ModuleManager(bus, LOGGER);
            CheckModule module = manager.register(new CheckModule(bus));
            AtomicBoolean hud = new AtomicBoolean(true);
            ConfigManager config = new ConfigManager(directory, manager, hud::get, hud::set, LOGGER);
            config.load();
            check(Files.exists(config.getFile()), "missing config creates defaults");
            check(manager.getByName("  CONFIGCHECK ").orElseThrow() == module, "name lookup");
            check(manager.getByClass(CheckModule.class).orElseThrow() == module, "class lookup");
            check(manager.getByCategory(Category.UTILITY).equals(manager.getModules()), "category lookup");
            module.flag.setValue(false);
            module.scale.setValue(1.75);
            module.curve.setValue(MathUtil.Easing.OUT_ELASTIC);
            module.color.setValue(0x803B82F6);
            manager.setKeybind(module, 65);
            check(manager.setEnabled(module, true), "manager enables module");
            bus.post(new CheckEvent());
            check(module.calls == 1 && bus.getListenerCount() == 1, "enable owns subscription");
            hud.set(false);
            check(config.save(), "atomic save succeeds");

            EventBus restoredBus = new EventBus();
            ModuleManager restoredManager = new ModuleManager(restoredBus, LOGGER);
            CheckModule restored = restoredManager.register(new CheckModule(restoredBus));
            AtomicBoolean restoredHud = new AtomicBoolean(true);
            ConfigManager loaded = new ConfigManager(directory, restoredManager, restoredHud::get, restoredHud::set, LOGGER);
            loaded.load();
            check(restored.isEnabled() && restored.getKeybind() == 65, "toggle and keybind round trip");
            check(!restored.flag.getValue() && restored.scale.getValue() == 1.75, "boolean and number round trip");
            check(restored.curve.getValue() == MathUtil.Easing.OUT_ELASTIC, "enum round trip");
            check(restored.color.getValue() == 0x803B82F6, "transparent ARGB round trip");
            check(restored.scaleAtEnable == 1.75, "settings restored before module enable");
            check(!restoredHud.get(), "HUD preference round trip");
            restoredManager.setEnabled(restored, false);
            check(restoredBus.getListenerCount() == 0, "manager disable unsubscribes");

            JsonObject root = JsonParser.parseString(Files.readString(config.getFile())).getAsJsonObject();
            root.addProperty("futureField", "preserve me");
            JsonObject entry = root.getAsJsonObject("modules").getAsJsonObject("ConfigCheck");
            entry.addProperty("keybind", 1.5);
            entry.getAsJsonObject("settings").addProperty("scale", 99.0);
            Files.writeString(config.getFile(), root.toString());
            loaded.load();
            check(restored.scale.getValue() == 1.75 && restored.getKeybind() == 65, "invalid values preserve current state");
            loaded.save();
            check(JsonParser.parseString(Files.readString(config.getFile())).getAsJsonObject()
                    .get("futureField").getAsString().equals("preserve me"), "unknown fields survive saves");

            Files.writeString(config.getFile(), "{broken json");
            ConfigManager recovery = new ConfigManager(directory, new ModuleManager(new EventBus(), LOGGER),
                    hud::get, hud::set, LOGGER);
            recovery.load();
            check(JsonParser.parseString(Files.readString(config.getFile())).isJsonObject(), "malformed JSON recovers");
            try (var files = Files.list(directory)) {
                check(files.anyMatch(path -> path.getFileName().toString().startsWith("config.invalid-")),
                        "malformed input is backed up");
            }
            manager.disableAll();
            restoredManager.disableAll();
            check(bus.getListenerCount() == 0 && restoredBus.getListenerCount() == 0, "shutdown releases listeners");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
        System.out.println("Runtime checks passed (" + assertions + " assertions).");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) { throw new AssertionError(message); }
    }

    private static final class CheckEvent extends Event { }

    private static final class CheckModule extends Module {
        private final BooleanSetting flag = register(new BooleanSetting("flag", "Test boolean", true));
        private final DoubleSetting scale = register(new DoubleSetting("scale", "Test range", 1.0, 0.5, 2.0));
        private final EnumSetting<MathUtil.Easing> curve = register(
                new EnumSetting<>("curve", "Test enum", MathUtil.Easing.OUT_QUART));
        private final ColorSetting color = register(new ColorSetting("color", "Test ARGB", 0xFFFFFFFF));
        private int calls;
        private double scaleAtEnable;

        private CheckModule(EventBus bus) { super("ConfigCheck", "Config test", Category.UTILITY, bus); }

        @Override
        protected void onEnable() {
            scaleAtEnable = scale.getValue();
            listen(CheckEvent.class, event -> calls++);
        }
    }
}
```


### src/gametest/java/dev/riserecode/ClientIntegrationTest.java

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.ui.clickgui.ClickGui;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Runs in a separate test mod and is never packaged in the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(960, 540);
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            var fullBright = Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.clickScreenButton("FullBright [OFF]");
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1.0), "FullBright applies gamma");
        context.clickScreenButton("FullBright [ON]");
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        context.clickScreenButton("HUD: ON");
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD toggle");
        context.clickScreenButton("HUD: OFF");
        context.takeScreenshot("riserecode-clickgui");
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
            context.runOnClient(minecraft -> {
                var fullBright = Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
                Client.INSTANCE.getModuleManager().setKeybind(fullBright, InputConstants.KEY_B);
            });
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager()
                    .getByClass(FullBrightModule.class).orElseThrow().isEnabled()), "module key toggles once");
            context.waitTicks(5);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.takeScreenshot("riserecode-hud");
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
        } finally {
            hudSubscription.close();
            worldSubscription.close();
        }
        context.runOnClient(minecraft -> {
            var fullBright = Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
            Client.INSTANCE.getModuleManager().setKeybind(fullBright, -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
```


## 3. Required resource/config changes


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

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck' }

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
    programArgs '--graphicsBackend', 'OPENGL'
}
```


### settings.gradle

```groovy
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = 'riserecode'
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
mod_version=0.1.0
maven_group=dev.riserecode
archives_base_name=riserecode
```


### gradle/wrapper/gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.0-bin.zip
distributionSha256Sum=bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01
networkTimeout=60000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
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


### src/main/resources/riserecode.mixins.json

```json
{
  "required": true,
  "package": "dev.riserecode.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": ["KeyboardHandlerMixin"],
  "injectors": { "defaultRequire": 1 }
}
```


### src/main/resources/assets/riserecode/lang/en_us.json

```json
{
  "key.category.riserecode.controls": "RiseRecode",
  "key.riserecode.clickgui": "Open / close ClickGUI"
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
  "entrypoints": {
    "fabric-client-gametest": ["dev.riserecode.ClientIntegrationTest"]
  },
  "depends": {
    "riserecode": "*",
    "fabric-client-gametest-api-v1": "*"
  }
}
```


### .gitignore

```text
/build/
/.gradle/
/.tooling/
/run/
/logs/
/.idea/
*.iml
```


### scripts/verify.sh

```bash
#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd -- "$project_dir"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_dir/.tooling/gradle-home}"
./gradlew build "$@"
```


### README.md

````markdown
# RiseRecode

Original Java 25 foundations for a client-side Fabric utility mod targeting
Minecraft 26.2. The backend follows the requested modular, event-driven design;
the animation curves support the requested Rise-inspired UI. No upstream client
source, branding assets, or fonts are included.

Phase 3 adds a working Fabric bootstrap, custom event wiring, module/config/keybind
managers, FullBright, a watermark/module HUD, and a native placeholder ClickGUI.
The five original foundation classes and typed settings remain unchanged.
Premium GUI styling, settings widgets, and additional modules are future work.

## Files

All packages are under `src/main/java/dev/riserecode`.

| File | Responsibility |
| --- | --- |
| `util/MathUtil.java` | OutQuart, InOutCubic, OutElastic, interpolation |
| `ui/animation/Animation.java` | Explicit delta time, retargeting, pause/resume, completion |
| `event/Event.java` | Opt-in cancellation for future packet hooks |
| `event/EventBus.java` | Typed synchronous listeners, priority, cached dispatch, subscription cleanup |
| `module/Module.java` | Metadata, keybind, toggle lifecycle, settings, owned listeners |
| `module/Category.java` | Feature categories |
| `setting/*.java` | Boolean, bounded double, enum, ARGB color, `visibleWhen` predicates |
| `Client.java` | Singleton and ordered system initialization/cleanup |
| `RiseRecodeClient.java` | Fabric client entrypoint and event adapters |
| `event/*Event.java` | Tick, HUD extraction, world extraction, keyboard events |
| `module/ModuleManager.java` | Registry, lookup, toggles, default modules |
| `module/modules/FullBrightModule.java` | Supported vanilla gamma override and restoration |
| `config/ConfigManager.java` | Gson JSON persistence and validation |
| `keybind/KeybindManager.java` | Registered GUI key and module key dispatch |
| `ui/hud/HudRenderer.java` | Watermark and enabled modules sorted by text width |
| `ui/clickgui/ClickGui.java` | Native categories, module toggles, HUD switch |
| `mixin/KeyboardHandlerMixin.java` | Raw keyboard input adapter |

## Build and run

Pinned versions: Minecraft 26.2, Fabric Loader 0.19.5, Fabric API
0.161.0+26.2, Loom 1.17.21, Gradle 9.6.0, Java 25. No preview features.
Minecraft is unobfuscated; this project uses `net.fabricmc.fabric-loom` without
Yarn or a mappings dependency. All code is client-only by mod metadata.

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

The installable mod is `build/libs/riserecode-0.1.0.jar`. Install it together with
Fabric API in a Minecraft 26.2 profile using Fabric Loader and Java 25.

Right Shift opens the GUI from gameplay or the title screen; Right Shift again,
Escape, or Done closes it. Change the GUI binding in Minecraft's Controls menu.
Module binds only fire in gameplay, once per press, and ignore keyboard repeats.
They start unbound; edit the module's `keybind` in the config while the game is
closed, or assign it through `ModuleManager.setKeybind`. The placeholder does
not yet offer a binding/settings editor. The HUD button toggles the overlay; F1
also hides it through Minecraft's native HUD visibility.

FullBright defaults to disabled. Its `Brightness` setting accepts 0.0 through
1.0, with 1.0 corresponding to vanilla's maximum brightness. This is a reversible
gamma utility, not a replacement for the game's lightmap or full cave lighting.
It captures the current gamma on enable, reapplies the setting on client ticks,
and restores the captured value on disable and shutdown.

## Verification

Requires JDK 25. From this directory:

```bash
bash scripts/verify.sh
```

Builds the mod with all compiler warnings treated as errors and runs the
dependency-free `FoundationChecks` and `RuntimeChecks` harnesses through Gradle
JavaExec tasks. The standard JUnit task is disabled because those harnesses do
not use JUnit. Checks cover easing, animation, subscriptions, cancellation,
concurrent registry changes, lifecycle rollback, all four setting types, atomic
config saves, invalid input recovery, and registry lookups.

```bash
./gradlew runClientGameTest
```

The separate `src/gametest` mod boots the real client, checks GUI opening/closing
through the keyboard mixin, native module buttons, gamma restoration, HUD
visibility, module keys, and HUD/world event delivery in an isolated test world.
It runs with the OpenGL backend, requires a display/graphics driver, and exits
automatically. Its run directory and screenshots are in
`build/run/clientGameTest`; test classes are excluded from the installable JAR.

## Integration contract

- `RiseRecodeClient` invokes `Client.INSTANCE.init()`. It constructs the registry,
  default modules, HUD, config, and key manager, loading setting values and keys
  before restoring enabled states. The GUI is constructed at `CLIENT_STARTED`,
  after Minecraft's font is available; getters also support lazy construction.
- `END_CLIENT_TICK` posts `UpdateEvent`. `HudElementRegistry` posts
  `Render2DEvent` before chat using `GuiGraphicsExtractor`. World
  `LevelExtractionEvents.END_EXTRACTION` posts `Render3DEvent` for collecting
  render state. The keyboard mixin posts press/release/repeat `KeyPressEvent`
  objects; cancelling one stops that original Minecraft input action.
- Config lives at `config/riserecode/config.json`, saves changes after a short
  debounce, and saves again before shutdown cleanup. Settings are loaded before
  enabling modules. Missing config creates defaults. Malformed JSON is backed up;
  invalid individual values are ignored. Saves preserve unknown fields, write a
  temporary file, and atomically replace the destination where supported.
- No packet hooks are implemented in this phase. A future cancellable packet
  event must be inspected by its adapter to stop the original operation.
- Event callbacks run synchronously on the posting thread. Packet callbacks may
  run on a network thread; schedule Minecraft mutations onto the client thread.
  Module lifecycle and setting changes also belong to the client thread.
- Define settings with `register(...)` in module constructors or field
  initializers. Names must be unique within a module. Hidden settings retain
  their values; visibility only affects the UI. Numeric values outside their
  declared bounds are rejected without changing the previous value.
- Register feature handlers through `listen(...)` in `onEnable()`. They become
  active after initialization succeeds and are removed before `onDisable()`.
  `onDisable()` also rolls back failed initialization, so it should tolerate
  partial setup. Recursive enable/disable calls are rejected.
- Higher numeric event priorities run first; equal priorities retain insertion
  order. Cancelled events reach only listeners registered with
  `receiveCancelled = true`. Base event listeners also receive subclasses.
  Runtime listener failures are logged and remaining handlers continue; JVM
  errors propagate. A custom error reporter must return normally to continue.
- Animations take seconds, including duration and frame delta. Call `update`
  once per rendered frame, even at variable FPS; read `getValue()` for drawing.
  Requesting the current target again does not restart the clock. Retargeting
  preserves position, but does not preserve velocity. Elastic output may
  overshoot; clamp it explicitly for opacity/color channels.

```java
Animation panel = new Animation(0.0, 0.25, MathUtil.Easing.OUT_QUART);
panel.animateTo(1.0);
double openAmount = panel.update(deltaSeconds);

BooleanSetting advanced = register(new BooleanSetting("advanced", "Show extra controls", false));
DoubleSetting scale = register(new DoubleSetting("scale", "HUD scale", 1.0, 0.5, 2.0));
scale.setVisibleWhen(advanced::getValue);
```

## UI blueprint for the next layer

Use backgrounds `#0F1115` / `#181A20`, accents `#3B82F6` / `#D946EF`,
12 px panel corners, 8 px button corners, 16 px padding, and 1 px inner borders
at 8% white opacity. Target 20 px Gaussian blur with a dark tint and soft shadows.
Use Inter or Montserrat at 500 for titles and 400 for module labels; fonts still
need to be selected and bundled with their licenses.

Panel openings use OutQuart, toggles use InOutCubic, and ArrayList entry slides
use OutElastic. Apply a 2 px hover text shift and lighten hovered backgrounds.
The future ArrayList renderer will sort entries by measured text width and animate
their vertical positions. These visual effects are specified here; this step
provides their animation primitives rather than a rendered screen.

## Fabric target notes

[Fabric's 26.2 migration guidance](https://fabricmc.net/2026/06/15/262.html)
describes the OpenGL/Vulkan rendering transition and GUI/HUD API reorganization.
The HUD and GUI use Minecraft's rendering abstractions rather than raw OpenGL.
The keyboard adapter is the only production mixin in this phase.

[Fabric's 26.1 tooling guidance](https://fabricmc.net/2026/03/14/261.html)
established Java 25 and the unobfuscated `net.fabricmc.fabric-loom` plugin, with
standard dependency configurations and no Yarn mappings. Preserve that workflow
in this 26.2 build.
````


The standard Gradle launchers `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are present. The binary wrapper is not represented as source text.


## 4. Setup notes

- The client entrypoint calls `Client.INSTANCE.init()`. ClickGUI construction waits for `CLIENT_STARTED`, because Screen captures the Minecraft font.
- Fabric end-tick, HUD extraction, and world extraction events feed the custom bus. The keyboard mixin bridges all keyboard actions to `KeyPressEvent`; the key manager acts only on initial presses.
- Right Shift opens/closes the GUI from gameplay or the title screen. Escape and Done close it. The native Controls menu can remap the global GUI key. Module keys start unbound and can be assigned in JSON or through ModuleManager.
- FullBright sets gamma within the supported 0–1 range, defaulting to 100% vanilla brightness, and restores the captured gamma on disable. It does not replace the lightmap. The HUD button persists overlay visibility.
- Config is created at `config/riserecode/config.json`. Settings and binds load before modules enable; saves use a temporary file and atomic replacement where supported. Invalid JSON is backed up.

```bash
cd /home/toast/Work/riserecode
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
./gradlew runClientGameTest
```

Verified with Java 25: the build passed, all 82 foundation and 19 config/registry assertions passed, and the real OpenGL client integration test passed. The release JAR uses Java 25 bytecode and excludes test classes. Vulkan was not tested.

- [Installable mod](/home/toast/Work/riserecode/build/libs/riserecode-0.1.0.jar)
- [ClickGUI screenshot](/home/toast/Work/riserecode/build/run/clientGameTest/screenshots/0000_riserecode-clickgui.png)
- [HUD screenshot](/home/toast/Work/riserecode/build/run/clientGameTest/screenshots/0001_riserecode-hud.png)

Minecraft 26.2 uses the unobfuscated Loom workflow rather than Yarn mappings. See [Fabric's tooling guidance](https://fabricmc.net/2026/03/14/261.html) and [26.2 rendering/GUI migration notes](https://fabricmc.net/2026/06/15/262.html).
