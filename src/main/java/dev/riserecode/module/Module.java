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
    private String capabilityId;
    private long stateRevision, enabledRevision;
    private final List<Runnable> changeListeners = new ArrayList<>();
    public final String capabilityId() { return capabilityId; }
    protected final void capabilityId(String id) { capabilityId = "client.module." + dev.riserecode.config.CapabilityIds.segment(id); }
    public final long stateRevision() { return stateRevision; }
    public final long enabledRevision() { return enabledRevision; }
    public final void onChange(Runnable listener) { changeListeners.add(Objects.requireNonNull(listener)); }
    private void changed() { stateRevision++; changeListeners.forEach(Runnable::run); }
    private final Category category;
    private final EventBus eventBus;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(settings);
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();
    public final dev.riserecode.setting.BooleanSetting popIn=register(new dev.riserecode.setting.BooleanSetting("Pop In","Animate this module's panels and overlay entries",true));
    public final dev.riserecode.setting.BooleanSetting popOverride=register(new dev.riserecode.setting.BooleanSetting("Animation Override","Use independent module animation settings",false));
    public final dev.riserecode.setting.DoubleSetting popTiming=register(dev.riserecode.setting.DoubleSetting.extended("Pop Duration","Independent entrance duration",.2,0,10));
    public final dev.riserecode.setting.DoubleSetting popStrength=register(dev.riserecode.setting.DoubleSetting.extended("Pop Strength","Independent entrance scale and movement",1,0,10));
    public double popDuration(){if((popOverride.getValue()||!popIn.getValue())&&dev.riserecode.ui.theme.Theme.manager()!=null){dev.riserecode.ui.theme.Theme.manager().module(this).migrate(this);}return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId,()->dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)?0:dev.riserecode.ui.theme.Theme.number("Opening Duration",.2));}
    public double popScale(double progress){return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId,()->dev.riserecode.ui.animation.PopIn.scale(dev.riserecode.ui.animation.Animation.scale(dev.riserecode.ui.theme.ThemeManager.Channel.OPENING),progress));}
    private int keybind = UNBOUND;
    private int defaultKeybind = UNBOUND;
    private BindMode bindMode = BindMode.TOGGLE;
    private BindMode defaultBindMode = BindMode.TOGGLE;
    private volatile boolean enabled;
    private volatile boolean transitioning;

    protected Module(String name, String description, Category category, EventBus eventBus) {
        this(name, description, category, eventBus, name);
    }

    protected Module(String name, String description, Category category, EventBus eventBus, String stableId) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        capabilityId(stableId);
        popTiming.setVisibleWhen(popOverride::getValue);popStrength.setVisibleWhen(popOverride::getValue);
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public ModuleClassification classification() {
        return switch (getName()) {
            case "KillAura" -> ModuleClassification.BLATANT;
            case "FastBreak" -> this instanceof dev.riserecode.module.modules.FastBreakModule f && f.multiplier.getValue() > 1 ? ModuleClassification.BLATANT : ModuleClassification.GHOST;
            case "Sprint" -> this instanceof dev.riserecode.module.modules.SprintModule s && s.getOmniSprint().getValue() ? ModuleClassification.BLATANT : ModuleClassification.GHOST;
            case "AutoTool", "Baritone", "Clutch" -> ModuleClassification.GHOST;
            default -> ModuleClassification.CLIENT;
        };
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
    public final void setBindMode(BindMode mode) { if (bindMode != Objects.requireNonNull(mode)) { bindMode = mode; changed(); } }
    protected final void defaultBinding(int key, BindMode mode) {
        if (enabled || transitioning) { throw new IllegalStateException("Set binding defaults before activation"); }
        setKeybind(key); setBindMode(mode); defaultKeybind = key; defaultBindMode = mode;
    }

    /** Stores a raw GLFW key code; input processing belongs to ModuleManager. */
    public final void setKeybind(int keybind) {
        if (keybind < UNBOUND) {
            throw new IllegalArgumentException("Keybind must be unbound (-1) or non-negative");
        }
        if (this.keybind != keybind) { this.keybind = keybind; changed(); }
    }

    public final boolean isEnabled() {
        return enabled;
    }

    /** Armed modules may reserve their binding for an effect instead of toggling enablement. */
    public boolean bindingActivatesEffect() { return false; }
    public void activateBinding(boolean active) { }

    public final List<Setting<?>> getSettings() {
        return settingsView;
    }

    protected final <S extends Setting<?>> S register(S setting) {
        Objects.requireNonNull(setting, "setting");
        if (enabled || transitioning) {
            throw new IllegalStateException("Register settings while the module is disabled");
        }
        for (Setting<?> existing : settings) {
            if (existing.getName().equals(setting.getName()) || existing.capabilityId().equals(setting.capabilityId())) {
                throw new IllegalArgumentException("Duplicate setting: " + setting.getName());
            }
        }
        settings.add(setting); setting.onChange(this::changed);
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
            transitioning = false; enabledRevision++; changed();
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
                        if(event instanceof dev.riserecode.event.Render3DEvent || event instanceof dev.riserecode.event.EnchantmentRenderEvent) {
                            dev.riserecode.ui.theme.ModuleAppearance.with(capabilityId,()->listener.accept(event));
                        } else { listener.accept(event); }
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
