package dev.riserecode.setting;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A typed, client-thread value with independent UI visibility. */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private String capabilityId;
    private long revision;
    private final java.util.List<Runnable> changeListeners = new java.util.ArrayList<>();
    private final T defaultValue;
    private T value;
    private BooleanSupplier visibleWhen = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Setting name cannot be blank");
        }
        this.description = Objects.requireNonNull(description, "description");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        value = defaultValue;
        capabilityId = dev.riserecode.config.CapabilityIds.segment(name);
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final T getValue() {
        return value;
    }

    public final T getDefaultValue() {
        return defaultValue;
    }

    /** Validation happens before assignment, leaving invalid changes unapplied. */
    public final void setValue(T value) {
        T next = Objects.requireNonNull(validate(Objects.requireNonNull(value, "value")), "validated value");
        if (!this.value.equals(next)) { this.value = next; changed(); }
    }

    public final String capabilityId() { return capabilityId; }
    /** Override before registering in a module; profile labels remain compatible. */
    protected final void capabilityId(String id) { capabilityId = dev.riserecode.config.CapabilityIds.segment(id); }
    public final long revision() { return revision; }
    public final void onChange(Runnable listener) { changeListeners.add(Objects.requireNonNull(listener)); }
    protected final void changed() { revision++; changeListeners.forEach(Runnable::run); }

    protected T validate(T value) {
        return value;
    }

    public final void reset() {
        setValue(defaultValue);
        onReset();
    }

    protected void onReset() { }

    public final void setVisibleWhen(BooleanSupplier predicate) {
        visibleWhen = Objects.requireNonNull(predicate, "predicate");
    }

    public final boolean isVisible() {
        return visibleWhen.getAsBoolean();
    }
}
