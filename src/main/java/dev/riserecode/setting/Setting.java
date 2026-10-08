package dev.riserecode.setting;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A typed, client-thread value with independent UI visibility. */
public abstract class Setting<T> {
    private final String name;
    private final String description;
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
        this.value = Objects.requireNonNull(validate(Objects.requireNonNull(value, "value")),
                "validated value");
    }

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
