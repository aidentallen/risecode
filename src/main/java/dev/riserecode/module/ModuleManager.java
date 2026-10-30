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
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Client-thread registry. Module owns the subscriptions for its lifecycle. */
public final class ModuleManager {
    private final EventBus eventBus;
    private final Logger logger;
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> view = Collections.unmodifiableList(modules);
    private final Map<String, Module> byName = new LinkedHashMap<>();
    private long registryRevision;
    private final Map<Class<?>,Optional<Module>> byClass=new java.util.HashMap<>();
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private long stateRevision;
    public long stateRevision() { return stateRevision; }
    public Optional<Module> getById(String id) { return Optional.ofNullable(byId.get(id)); }
    public long registryRevision() { return registryRevision; }
    private Runnable changeListener = () -> { };
    private Consumer<Module> bindingChangeListener = ignored -> { };

    public ModuleManager(EventBus eventBus, Logger logger) {
        this.eventBus = Objects.requireNonNull(eventBus);
        this.logger = Objects.requireNonNull(logger);
    }

    public void initDefaults(Minecraft minecraft, FriendManager friends, RotationManager rotations, AttackManager attacks, dev.riserecode.util.CameraEffectsManager camera, dev.riserecode.render.LoadedBlockIndex blocks) {
        var trajectories = register(new dev.riserecode.module.modules.TrajectoriesModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.BetterChatModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.BetterTabModule(eventBus));
        register(new dev.riserecode.module.modules.NametagsModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.WaypointsModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.AutoToolModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.FastBreakModule(eventBus));
        register(new dev.riserecode.module.modules.hud.MinimapModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.ZoomModule(eventBus, camera));
        register(new dev.riserecode.module.modules.CrosshairModule(eventBus));
        register(new dev.riserecode.module.modules.ViewModelModule(eventBus));
        register(new dev.riserecode.module.modules.StorageEspModule(eventBus, minecraft, blocks));
        register(new dev.riserecode.module.modules.PlayerEspModule(eventBus, minecraft, friends));
        register(new dev.riserecode.module.modules.BlockEspModule(eventBus, minecraft, blocks));
        register(new dev.riserecode.module.modules.AmbienceModule(eventBus));
        register(new dev.riserecode.module.modules.NoRenderModule(eventBus));
        register(new dev.riserecode.module.modules.ChunkAnimationsModule(eventBus));
        register(new dev.riserecode.module.modules.CapePhysicsModule(eventBus));
        register(new dev.riserecode.module.modules.BaritoneModule(eventBus));
        register(new dev.riserecode.module.modules.SeedCrackerModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.hud.WatermarkModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.hud.ArrayListModule(eventBus, minecraft));
        register(new FullBrightModule(eventBus, minecraft));
        register(new SprintModule(eventBus, minecraft));
        register(new KillAuraModule(eventBus, minecraft, friends, rotations, attacks));
        register(new dev.riserecode.module.modules.AutoBowModule(eventBus, minecraft, friends, trajectories));
        register(new TracersModule(eventBus, minecraft, friends));
        register(new NoFallModule(eventBus, minecraft));
        register(new FpsModule(eventBus, minecraft)); register(new PingModule(eventBus, minecraft));
        register(new CoordinatesModule(eventBus, minecraft)); register(new PotionEffectsModule(eventBus, minecraft));
        register(new MemoryUsageModule(eventBus, minecraft));
        register(new dev.riserecode.module.modules.hud.ArmorHudModule(eventBus, minecraft));
        register(new FreeLookModule(eventBus, minecraft)); register(new FreeCamModule(eventBus, minecraft));
        register(new EnchantEffectModule(eventBus)); register(new dev.riserecode.module.modules.GlassModule(eventBus)); register(new ItemPhysicsModule(eventBus));
        register(new WeatherChangerModule(eventBus));
        var blockOverlay=register(new BlockOverlayModule(eventBus,minecraft));
        register(new dev.riserecode.module.modules.HitboxesModule(eventBus,minecraft,blockOverlay));
        register(new dev.riserecode.module.modules.BlockAnimationsModule(eventBus,minecraft));
    }

    public <M extends Module> M register(M module) {
        Objects.requireNonNull(module);
        String key = normalize(module.getName());
        if (byName.containsKey(key) || byId.containsKey(module.capabilityId())) {
            throw new IllegalArgumentException("Duplicate module: " + module.getName());
        }
        byName.put(key, module); byId.put(module.capabilityId(), module);
        module.onChange(() -> { stateRevision++; changeListener.run(); });
        modules.add(module); registryRevision++; byClass.clear();
        return module;
    }

    public List<Module> getModules() { return view; }
    public Optional<Module> getByName(String name) { return Optional.ofNullable(byName.get(normalize(name.equalsIgnoreCase("NoFall") ? "Clutch" : name))); }
    public <M extends Module> Optional<M> getByClass(Class<M> type) {
        var match=byClass.computeIfAbsent(type,key->{for(var module:modules){if(key.isInstance(module)){return Optional.of(module);}}return Optional.empty();});
        @SuppressWarnings("unchecked") Optional<M> result=(Optional<M>)(Optional<?>)match;return result;
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
        if (module.getKeybind() == key) { return; }
        if (key < Module.UNBOUND) { throw new IllegalArgumentException("Invalid keybind"); }
        bindingChangeListener.accept(module);
        module.setKeybind(key);
        changeListener.run();
    }
    public void setBindMode(Module module, Module.BindMode mode) {
        requireRegistered(module); Objects.requireNonNull(mode);
        if (module.getBindMode() == mode) { return; }
        bindingChangeListener.accept(module); module.setBindMode(mode); changeListener.run();
    }
    public void setBindingChangeListener(Consumer<Module> listener) { bindingChangeListener = Objects.requireNonNull(listener); }
    public void releaseKeybindActivations() { modules.forEach(bindingChangeListener); }

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
