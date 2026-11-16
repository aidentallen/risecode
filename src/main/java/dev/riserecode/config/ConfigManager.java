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
    private record ProfileState(boolean hud, List<ModuleState> modules, JsonObject layouts, JsonObject visuals, JsonObject resources, JsonObject cosmetics) { }
    private Supplier<JsonObject> cosmeticsSnapshot = () -> dev.riserecode.cosmetics.CosmeticLoadout.empty().json();
    private Consumer<JsonObject> applyCosmetics = value -> { }, validateCosmetics = value -> { };
    public void bindCosmeticsStore(Supplier<JsonObject> snapshot,Consumer<JsonObject> apply,Consumer<JsonObject> validate){cosmeticsSnapshot=snapshot;applyCosmetics=apply;validateCosmetics=validate;}
    private dev.riserecode.resources.ResourcesManager resourcesManager;
    public void bindResourcesStore(dev.riserecode.resources.ResourcesManager manager) { resourcesManager = manager; }
    private static JsonObject resourcesOf(JsonObject data) {
        return dev.riserecode.resources.ResourcesMigration.upgrade(data).getAsJsonObject("resources");
    }
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
    private volatile boolean dirty;
    private final java.util.concurrent.ExecutorService writer;
    private final java.util.concurrent.Executor clientExecutor;
    private int pendingWrites;
    private volatile long saveRevision;
    private volatile boolean writing;
    private final Object writeLock = new Object();
    private volatile long saveAfterNanos;

    public ConfigManager(Path directory, ModuleManager modules, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this(directory, modules, new FriendManager(), hudEnabled, setHudEnabled, logger);
    }

    public ConfigManager(Path directory, ModuleManager modules, FriendManager friends, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger) {
        this(directory, modules, friends, hudEnabled, setHudEnabled, logger,
                task -> net.minecraft.client.Minecraft.getInstance().execute(task),
                java.util.concurrent.Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "risecode-config-io"); t.setDaemon(true); return t; }));
    }

    ConfigManager(Path directory, ModuleManager modules, FriendManager friends, BooleanSupplier hudEnabled,
            Consumer<Boolean> setHudEnabled, Logger logger, java.util.concurrent.Executor clientExecutor,
            java.util.concurrent.ExecutorService writer) {
        this.clientExecutor = java.util.Objects.requireNonNull(clientExecutor); this.writer = writer;
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
        profile = dev.riserecode.resources.ResourcesMigration.upgrade(profile);
        parseProfile(profile);
        JsonObject result = profile.deepCopy(); result.addProperty("schemaVersion", 5);
        result.add("resources", resourcesOf(profile));
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
            if (dirty && !writing && System.nanoTime() - saveAfterNanos >= 0) {
                saveAsync().thenAccept(ok -> { if (!ok) { saveAfterNanos = System.nanoTime() + 5_000_000_000L; } });
            }
        });
    }
    public void requestSave() { saveRevision++; dirty = true; saveAfterNanos = System.nanoTime() + 500_000_000L; }

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
        try {
            if (!document.has("resources") || document.has("styles")) {
                Path backup = directory.resolve("config.pre-resources.json"); if (!Files.exists(backup)) { Files.copy(file, backup); }
            }
            document = dev.riserecode.resources.ResourcesMigration.upgrade(document);
            if (resourcesManager != null) { resourcesManager.restoreStartup(resourcesOf(document)); }
        } catch (IOException | RuntimeException invalid) { logger.warn("Could not migrate Resources; original file is preserved", invalid); }
        if (document.has("commandPrefix")) { apply("commandPrefix", () -> setCommandPrefix(stringValue(document.get("commandPrefix")))); }
        loadFriends(document.get("friends"));
        if(document.has("cosmetics")){apply("cosmetics",()->{var value=requiredObject(document.get("cosmetics"));validateCosmetics.accept(value);applyCosmetics.accept(value);});}
        if (document.has("hudLayout")) { apply("HUD layout", () -> { JsonObject layouts = requiredObject(document.get("hudLayout")); validateHud.accept(layouts); applyHud.accept(layouts); }); }
        if (!externalVisualPreferences && document.has("hudEnabled")) { apply("hudEnabled", () -> setHudEnabled.accept(booleanValue(document.get("hudEnabled")))); }
        JsonObject entries = migrateModuleNames(objectOrEmpty(document.get("modules")));
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
        result.addProperty("schemaVersion", 5);
        result.add("cosmetics", cosmeticsSnapshot.get());
        if (resourcesManager != null) { result.add("resources", resourcesManager.snapshot()); }
        result.add("hudLayout", hudSnapshot.get()); result.add("visualPolicy", visualSnapshot.get());
        if (!externalVisualPreferences) { result.addProperty("hudEnabled", hudEnabled.getAsBoolean()); }
        JsonObject entries = new JsonObject(); result.add("modules", entries);
        for (Module module : modules.getModules()) { entries.add(module.getName(), moduleSnapshot(module, new JsonObject())); }
        return result.deepCopy();
    }

    /** Resolve omitted fields through the exact load semantics before a profile preview. */
    public JsonObject resolvedProfile(JsonObject data){
        ProfileState state=parseProfile(data);JsonObject resolved=profileSnapshot();JsonObject entries=new JsonObject();
        for(ModuleState m:state.modules()){
            JsonObject entry=new JsonObject();entry.addProperty("enabled",m.enabled());entry.addProperty("keybind",m.keybind());entry.addProperty("bindMode",m.bindMode().name());JsonObject settings=new JsonObject();m.settings().forEach((setting,value)->settings.add(setting.getName(),value.deepCopy()));entry.add("settings",settings);entries.add(m.module().getName(),entry);
        }
        resolved.add("cosmetics",state.cosmetics());resolved.add("modules",entries);if(!externalVisualPreferences){resolved.addProperty("hudEnabled",state.hud());}if(state.layouts()!=null){resolved.add("hudLayout",state.layouts());}if(state.visuals()!=null){resolved.add("visualPolicy",state.visuals());}if(resourcesManager!=null){resolved.add("resources",state.resources());}return resolved;
    }

    private static JsonObject migrateModuleNames(JsonObject entries) {
        if (entries.has("NoFall")) {
            if (!entries.has("Clutch")) { entries.add("Clutch", entries.get("NoFall").deepCopy()); }
            entries.remove("NoFall");
        }
        return entries;
    }

    private JsonObject moduleSnapshot(Module module, JsonObject entry) {
        entry.addProperty("enabled", enabledSnapshot.test(module)); entry.addProperty("keybind", module.getKeybind());
        entry.addProperty("bindMode", module.getBindMode().name());
        JsonObject values = objectOrEmpty(entry.get("settings")); entry.add("settings", values);
        for (Setting<?> setting : module.getSettings()) { values.add(setting.getName(), encodeSetting(setting, setting.getValue())); }
        return entry;
    }

    public static JsonElement encodeSetting(Setting<?> setting, Object value) { return SettingCodecs.encode(setting, value); }

    /** Capture mutable managers only on their owning client thread. */
    private JsonObject saveSnapshot() {
            JsonObject next = document.deepCopy();
            next.addProperty("schemaVersion", 5);
            next.add("cosmetics", cosmeticsSnapshot.get());
            if (resourcesManager != null) { next.add("resources", resourcesManager.snapshot()); }
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
            JsonObject entries = migrateModuleNames(objectOrEmpty(next.get("modules"))); next.add("modules", entries);
            for (Module module : modules.getModules()) {
                entries.add(module.getName(), moduleSnapshot(module, objectOrEmpty(entries.get(module.getName()))));
            }
            next.remove("styles");
            return next.deepCopy();
    }
    /** Snapshot capture, bookkeeping and every completion (including failure) run on the client executor. */
    public java.util.concurrent.CompletionStage<Boolean> saveAsync() {
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        clientExecutor.execute(() -> {
            JsonObject next;
            try { next = saveSnapshot(); }
            catch (RuntimeException invalid) { logger.error("Could not capture config snapshot", invalid); dirty=true;result.complete(false);return; }
            // The worker owns next exclusively; future edits mutate a separate tree.
            document = next.deepCopy();
            long revision = saveRevision; pendingWrites++; writing = true;
            try {
                writer.execute(() -> {
                    boolean success;
                    try { synchronized (writeLock) { JsonStorage.write(file, next); } success = true; }
                    catch (IOException | RuntimeException failure) { logger.error("Could not save {}", file, failure); success = false; }
                    boolean ok = success;
                    clientExecutor.execute(() -> {
                        if (saveRevision == revision) { dirty = !ok; }
                        writing = --pendingWrites != 0;
                        result.complete(ok);
                    });
                });
            } catch (java.util.concurrent.RejectedExecutionException failure) {
                dirty = true; writing = --pendingWrites != 0; result.complete(false);
            }
        });
        return result;
    }
    public boolean save() {
        try {
            // Explicit legacy saves and shutdown remain ordered after queued snapshots.
            writer.submit(() -> { }).get(5, java.util.concurrent.TimeUnit.SECONDS);
            JsonObject next = saveSnapshot(); synchronized (writeLock) { JsonStorage.write(file, next); }
            document = next; dirty = false; return true;
        } catch (InterruptedException failure) { Thread.currentThread().interrupt(); dirty = true; return false; }
        catch (Exception failure) { logger.error("Could not save {}", file, failure); dirty = true; return false; }
    }

    /** Validate first, then restart lifecycles; rollback is attempted on activation failure. */
    public void applyProfile(JsonObject data) {
        ProfileState next = parseProfile(data);
        ProfileState previous = parseProfile(profileSnapshot());
        if (resourcesManager != null) {
            var resources = dev.riserecode.resources.ResourcesState.read(next.resources());
            if (!resources.equals(resourcesManager.state())) { throw new IllegalArgumentException("Texture profile changes require asynchronous loading"); }
            resourcesManager.restoreStartup(next.resources());
        }
        try { commit(next); }
        catch (RuntimeException failure) {
            try { commit(previous); if (resourcesManager != null) { resourcesManager.restoreStartup(previous.resources()); } }
            catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            requestSave(); throw failure;
        }
        requestSave();
    }

    public java.util.concurrent.CompletableFuture<Boolean> applyProfileAsync(JsonObject data) {return applyProfileAsync(data,()->true);}
    /** Recheck an asynchronous caller's session/state grant immediately before lifecycles can restart. */
    public java.util.concurrent.CompletableFuture<Boolean> applyProfileAsync(JsonObject data,BooleanSupplier allowCommit) {
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        JsonObject owned = data.deepCopy();
        clientExecutor.execute(() -> {
            try {
                if (!allowCommit.getAsBoolean()) { result.complete(false); return; }
                if (resourcesManager == null) { applyProfile(owned); result.complete(true); return; }
                ProfileState next = parseProfile(owned), previous = parseProfile(profileSnapshot());
                resourcesManager.apply(dev.riserecode.resources.ResourcesState.read(next.resources()), () -> {
                    if (!allowCommit.getAsBoolean()) { throw new java.util.concurrent.CancellationException("Profile grant invalidated before commit"); }
                    try { commit(next); } catch (RuntimeException failure) { commit(previous); throw failure; }
                }).whenCompleteAsync((applied, failure) -> {
                    requestSave();
                    if (failure != null) { logger.warn("Profile Resources failed", failure); result.complete(false); }
                    else { if (!applied.success()) { logger.warn("Profile Resources failed: {}", applied.message()); } result.complete(applied.success()); }
                }, clientExecutor);
            } catch (RuntimeException failure) { logger.warn("Could not apply profile", failure); result.complete(false); }
        });
        return result;
    }

    java.util.concurrent.Executor clientExecutor() { return clientExecutor; }

    private ProfileState parseProfile(JsonObject data) {
        data = dev.riserecode.resources.ResourcesMigration.upgrade(data);
        if (data.has("schemaVersion")) {
            int version = integerValue(data.get("schemaVersion"));
            if (version < 1 || version > 5) { throw new IllegalArgumentException("Unsupported profile schema " + version); }
        }
        JsonObject entries = migrateModuleNames(requiredObject(data.get("modules")).deepCopy());
        boolean hud = !externalVisualPreferences && data.has("hudEnabled") ? booleanValue(data.get("hudEnabled")) : true;
        List<ModuleState> states = new ArrayList<>();
        for (Module module : modules.getModules()) {
            JsonObject entry = entries.has(module.getName()) ? requiredObject(entries.get(module.getName())) : new JsonObject();
            boolean enabled = entry.has("enabled") ? booleanValue(entry.get("enabled")) : module instanceof dev.riserecode.module.modules.hud.WatermarkModule || module instanceof dev.riserecode.module.modules.hud.ArrayListModule || module instanceof dev.riserecode.module.modules.WorldMapModule;
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
        JsonObject resources = resourcesOf(data);
        var cosmetics=data.has("cosmetics")?requiredObject(data.get("cosmetics")).deepCopy():cosmeticsSnapshot.get(); validateCosmetics.accept(cosmetics);
        return new ProfileState(hud, states, layouts, visuals, resources, cosmetics);
    }

    private void commit(ProfileState state) {
        applyCosmetics.accept(state.cosmetics());
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

    public static void validateSetting(Setting<?> setting, JsonElement value) { SettingCodecs.validate(setting, value); }
    public static void loadSetting(Setting<?> setting, JsonElement value) { SettingCodecs.apply(setting, value); }
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
    public void close() { if (subscription != null) { subscription.close(); } writer.shutdown(); }
}
