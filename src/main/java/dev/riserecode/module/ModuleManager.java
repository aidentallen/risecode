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
