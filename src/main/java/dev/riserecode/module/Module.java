package dev.riserecode.module;

import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base for self-contained features. Lifecycle and settings belong to the client
 * thread. Use listen in onEnable to automatically release event subscriptions
 * on disable, including a failed enable. Network callbacks must marshal game
 * mutations to the client thread; the bus itself does not schedule callbacks.
 */
public abstract class Module {
    public static final int UNBOUND = -1;
    public enum BindMode { TOGGLE, HOLD }

    private final String name;
    private final String description;
    private final Category category;
    private final EventBus eventBus;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(settings);
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();
    private int keybind = UNBOUND;
    private int defaultKeybind = UNBOUND;
    private BindMode bindMode = BindMode.TOGGLE;
    private BindMode defaultBindMode = BindMode.TOGGLE;
    private volatile boolean enabled;
    private volatile boolean transitioning;

    protected Module(String name, String description, Category category, EventBus eventBus) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final Category getCategory() {
        return category;
    }

    public final int getKeybind() {
        return keybind;
    }
    public final int getDefaultKeybind() { return defaultKeybind; }
    public final BindMode getBindMode() { return bindMode; }
    public final BindMode getDefaultBindMode() { return defaultBindMode; }
    public final void setBindMode(BindMode mode) { bindMode = Objects.requireNonNull(mode); }
    protected final void defaultBinding(int key, BindMode mode) {
        if (enabled || transitioning) { throw new IllegalStateException("Set binding defaults before activation"); }
        setKeybind(key); setBindMode(mode); defaultKeybind = key; defaultBindMode = mode;
    }

    /** Stores a raw GLFW key code; input processing belongs to ModuleManager. */
    public final void setKeybind(int keybind) {
        if (keybind < UNBOUND) {
            throw new IllegalArgumentException("Keybind must be unbound (-1) or non-negative");
        }
        this.keybind = keybind;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final List<Setting<?>> getSettings() {
        return settingsView;
    }

    protected final <S extends Setting<?>> S register(S setting) {
        Objects.requireNonNull(setting, "setting");
        if (enabled || transitioning) {
            throw new IllegalStateException("Register settings while the module is disabled");
        }
        for (Setting<?> existing : settings) {
            if (existing.getName().equals(setting.getName())) {
                throw new IllegalArgumentException("Duplicate setting: " + setting.getName());
            }
        }
        settings.add(setting);
        return setting;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (transitioning) {
            throw new IllegalStateException("Module lifecycle cannot be re-entered");
        }
        if (this.enabled == enabled) {
            return;
        }
        transitioning = true;
        this.enabled = enabled;
        try {
            if (enabled) {
                try {
                    onEnable();
                } catch (RuntimeException | Error failure) {
                    this.enabled = false;
                    releaseSubscriptions();
                    try {
                        onDisable();
                    } catch (RuntimeException | Error cleanupFailure) {
                        if (cleanupFailure != failure) {
                            failure.addSuppressed(cleanupFailure);
                        }
                    }
                    throw failure;
                }
            } else {
                releaseSubscriptions();
                onDisable();
            }
        } finally {
            transitioning = false;
        }
    }

    protected void onEnable() {
    }

    /** Also called to roll back a partially completed onEnable. */
    protected void onDisable() {
    }

    protected final <E extends Event> EventBus.Subscription listen(Class<E> eventType,
            Consumer<? super E> listener) {
        return listen(eventType, 0, false, listener);
    }

    protected final <E extends Event> EventBus.Subscription listen(Class<E> eventType, int priority,
            boolean receiveCancelled, Consumer<? super E> listener) {
        if (!enabled) {
            throw new IllegalStateException("Subscribe during onEnable or while enabled");
        }
        Objects.requireNonNull(listener, "listener");
        EventBus.Subscription subscription = eventBus.subscribe(eventType, priority, receiveCancelled,
                event -> {
                    if (enabled && !transitioning) {
                        listener.accept(event);
                    }
                });
        subscriptions.add(subscription);
        return subscription;
    }

    private void releaseSubscriptions() {
        for (EventBus.Subscription subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
    }
}
