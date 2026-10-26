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
