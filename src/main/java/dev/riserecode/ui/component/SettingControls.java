package dev.riserecode.ui.component;

import dev.riserecode.setting.*;
import dev.riserecode.ui.component.setting.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Extensible setting adapters; stable setting IDs and persistence stay in the model. */
public final class SettingControls {
    public record Control(UIComponent component, double height) { }
    private final Map<Class<?>, BiFunction<Setting<?>, Runnable, Control>> factories = new LinkedHashMap<>();
    public <S extends Setting<?>> void register(Class<S> type, BiFunction<S, Runnable, Control> factory) {
        factories.put(type, (setting, changed) -> factory.apply(type.cast(setting), changed));
    }
    public Control create(Setting<?> setting, Runnable changed) {
        for (var entry : factories.entrySet()) {
            if (entry.getKey().isInstance(setting)) { return entry.getValue().apply(setting, changed); }
        }
        throw new IllegalArgumentException("No UI adapter for " + setting.getClass().getName());
    }
    public static SettingControls defaults() {
        var controls = new SettingControls();
        controls.register(BooleanSetting.class, (s, c) -> new Control(new ToggleSwitch(s, c), 15));
        controls.register(StringSetting.class, (s, c) -> new Control(new StringControl(s, c), 32));
        controls.register(DoubleSetting.class, (s, c) -> new Control(new Slider(s, c), 24));
        controls.register(EnumSetting.class, SettingControls::enumControl);
        controls.register(BlockListSetting.class, (s, c) -> new Control(new BlockListControl(s, c), 20));
        controls.register(ColorSetting.class, (s, c) -> new Control(new ColorPicker(s, c, true), 22));
        return controls;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Control enumControl(EnumSetting setting, Runnable changed) { return new Control(new EnumSelector(setting, changed), 15); }
}
