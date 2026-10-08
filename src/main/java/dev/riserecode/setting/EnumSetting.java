package dev.riserecode.setting;

import java.util.List;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> enumType;
    private final List<E> options;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        enumType = defaultValue.getDeclaringClass();
        options = List.of(enumType.getEnumConstants());
    }

    @Override
    protected E validate(E value) {
        if (!enumType.isInstance(value)) {
            throw new IllegalArgumentException("Value must belong to " + enumType.getName());
        }
        return value;
    }

    public List<E> getOptions() {
        return options;
    }
}
