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
import java.util.function.Predicate;
import org.slf4j.Logger;

/** Global preferences plus reusable, validated module snapshots for profiles. */
public final class ConfigManager implements AutoCloseable {
    private record ModuleState(Module module, boolean enabled, int keybind, Module.BindMode bindMode, Map<Setting<?>, JsonElement> settings) { }
    private record Binding(int key, Module.BindMode mode) { }
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
    private Predicate<Module> enabledSnapshot = Module::isEnabled;
    public void bindEnabledSnapshot(Predicate<Module> snapshot) { enabledSnapshot = java.util.Objects.requireNonNull(snapshot); }
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
        modules.releaseKeybindActivations();
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
            apply(module.getName() + ".binding", () -> {
                Binding binding = binding(module, entry); modules.setKeybind(module, binding.key()); modules.setBindMode(module, binding.mode());
            });
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
        entry.addProperty("enabled", enabledSnapshot.test(module)); entry.addProperty("keybind", module.getKeybind());
        entry.addProperty("bindMode", module.getBindMode().name());
        JsonObject values = objectOrEmpty(entry.get("settings")); entry.add("settings", values);
        for (Setting<?> setting : module.getSettings()) { values.add(setting.getName(), encodeSetting(setting, setting.getValue())); }
        return entry;
    }

    private JsonElement encodeSetting(Setting<?> setting, Object value) {
        if (setting instanceof ColorSetting color) { return color.toJson(); }
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
            boolean enabled = entry.has("enabled") ? booleanValue(entry.get("enabled")) : module instanceof dev.riserecode.module.modules.hud.WatermarkModule || module instanceof dev.riserecode.module.modules.hud.ArrayListModule;
            Binding binding = binding(module, entry);
            JsonObject values = entry.has("settings") ? requiredObject(entry.get("settings")) : new JsonObject();
            Map<Setting<?>, JsonElement> settings = new LinkedHashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                JsonElement value = values.has(setting.getName()) ? values.get(setting.getName()).deepCopy()
                        : setting instanceof ColorSetting color ? ColorSetting.toJson(color.defaultPaint()) : encodeSetting(setting, setting.getDefaultValue());
                validateSetting(setting, value); settings.put(setting, value);
            }
            states.add(new ModuleState(module, enabled, binding.key(), binding.mode(), settings));
        }
        JsonObject layouts = data.has("hudLayout") ? requiredObject(data.get("hudLayout")).deepCopy() : null;
        JsonObject visuals = data.has("visualPolicy") ? requiredObject(data.get("visualPolicy")).deepCopy() : null;
        if (layouts != null) { validateHud.accept(layouts); } if (visuals != null) { validateVisual.accept(visuals); }
        return new ProfileState(hud, states, layouts, visuals);
    }

    private void commit(ProfileState state) {
        modules.releaseKeybindActivations();
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), false)) { throw new IllegalStateException("Could not disable " + entry.module().getName()); }
        }
        for (ModuleState entry : state.modules()) {
            entry.settings().forEach(ConfigManager::loadSetting); modules.setKeybind(entry.module(), entry.keybind()); modules.setBindMode(entry.module(), entry.bindMode());
        }
        if (state.layouts() != null) { applyHud.accept(state.layouts()); }
        if (state.visuals() != null) { applyVisual.accept(state.visuals()); }
        if (!externalVisualPreferences) { setHudEnabled.accept(state.hud()); }
        for (ModuleState entry : state.modules()) {
            if (!modules.setEnabled(entry.module(), entry.enabled())) { throw new IllegalStateException("Could not enable " + entry.module().getName()); }
        }
    }

    private static void validateSetting(Setting<?> setting, JsonElement value) {
        if (setting instanceof dev.riserecode.setting.StringSetting text) { String decoded = stringValue(value); if (decoded.length() > text.maximum() || decoded.codePoints().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Invalid text"); } }
        else if (setting instanceof BooleanSetting) { booleanValue(value); }
        else if (setting instanceof DoubleSetting number) {
            double decoded = numberValue(value).getAsDouble();
            if (!Double.isFinite(decoded) || decoded < number.getMin() || decoded > number.getMax()) {
                throw new IllegalArgumentException("Out-of-range value for " + setting.getName());
            }
        } else if (setting instanceof ColorSetting color) { color.decode(value); }
        else if (setting instanceof dev.riserecode.setting.BlockListSetting blocks) { blocks.decode(value); }
        else if (setting instanceof EnumSetting<?> choice) { enumValue(choice, value); }
        else { throw new IllegalArgumentException("Unsupported setting type " + setting.getClass().getName()); }
    }
    private static void loadSetting(Setting<?> setting, JsonElement value) {
        validateSetting(setting, value);
        if (setting instanceof dev.riserecode.setting.StringSetting text) { text.setValue(stringValue(value)); }
        else if (setting instanceof BooleanSetting flag) { flag.setValue(booleanValue(value)); }
        else if (setting instanceof DoubleSetting number) { number.setValue(numberValue(value).getAsDouble()); }
        else if (setting instanceof ColorSetting color) { color.apply(color.decode(value)); }
        else if (setting instanceof dev.riserecode.setting.BlockListSetting blocks) { blocks.setValue(blocks.decode(value)); }
        else if (setting instanceof EnumSetting<?> choice) { loadEnum(choice, value); }
    }
    private static <E extends Enum<E>> E enumValue(EnumSetting<E> setting, JsonElement value) {
        String name = stringValue(value);
        return setting.getOptions().stream().filter(option -> option.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown option " + name));
    }
    private static <E extends Enum<E>> void loadEnum(EnumSetting<E> setting, JsonElement value) { setting.setValue(enumValue(setting, value)); }
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
        if (key != Module.UNBOUND && (key < 32 || key > 348)) { throw new IllegalArgumentException("Key must be NONE or a valid keyboard key"); }
        return key;
    }
    private static Binding binding(Module module, JsonObject entry) {
        int key = entry.has("keybind") ? keyValue(entry.get("keybind")) : module.getDefaultKeybind();
        Module.BindMode mode = entry.has("bindMode") ? Module.BindMode.valueOf(stringValue(entry.get("bindMode"))) : module.getDefaultBindMode();
        // Old FreeLook used a hidden fallback key when its generic binding was NONE.
        // Explicit NONE in the new format remains cleared and can never reactivate that fallback.
        if (module.getName().equals("FreeLook") && !entry.has("bindMode") && key == Module.UNBOUND) {
            JsonObject values = objectOrEmpty(entry.get("settings"));
            key = values.has("HoldKey") ? keyValue(values.get("HoldKey")) : module.getDefaultKeybind();
            if (key < 32 || key > 348) { throw new IllegalArgumentException("Invalid legacy FreeLook key"); }
        }
        return new Binding(key, mode);
    }
    private static JsonObject requiredObject(JsonElement value) {
        if (value == null || !value.isJsonObject()) { throw new IllegalArgumentException("Expected an object"); }
        return value.getAsJsonObject();
    }
    private static JsonObject objectOrEmpty(JsonElement value) { return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject(); }
    @Override
    public void close() { if (subscription != null) { subscription.close(); } }
}
