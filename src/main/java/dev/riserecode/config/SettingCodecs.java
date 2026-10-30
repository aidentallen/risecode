package dev.riserecode.config;

import com.google.gson.*;
import dev.riserecode.setting.*;

/** Shared complete setting serialization and prevalidation for profiles and UI. */
public final class SettingCodecs {
    private static final Gson GSON = new Gson();
    private SettingCodecs() { }
    public static boolean supported(Setting<?> setting) {
        return setting instanceof BooleanSetting || setting instanceof DoubleSetting || setting instanceof EnumSetting<?>
                || setting instanceof StringSetting || setting instanceof ColorSetting || setting instanceof BlockListSetting || setting instanceof KeySetting;
    }
    public static JsonElement encode(Setting<?> setting, Object value) {
        if (setting instanceof ColorSetting color) { return color.toJson(); }
        if (value instanceof Enum<?> option) { return new JsonPrimitive(option.name()); }
        return GSON.toJsonTree(value);
    }
    public static JsonElement snapshot(Setting<?> setting) { return encode(setting, setting.getValue()); }
    public static void validate(Setting<?> setting, JsonElement value) {
        if (setting instanceof dev.riserecode.setting.StringSetting text) { String decoded = stringValue(value); if (decoded.length() > text.maximum() || decoded.codePoints().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Invalid text"); } }
        else if (setting instanceof KeySetting) { int key = numberValue(value).getAsInt(); if (key < -1 || key > 400) { throw new IllegalArgumentException("Invalid key code"); } }
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
    public static void apply(Setting<?> setting, JsonElement value) {
        validate(setting, value);
        if (setting instanceof dev.riserecode.setting.StringSetting text) { text.setValue(stringValue(value)); }
        else if (setting instanceof KeySetting key) { key.setValue(numberValue(value).getAsInt()); }
        else if (setting instanceof BooleanSetting flag) { flag.setValue(booleanValue(value)); }
        else if (setting instanceof DoubleSetting number) { number.setValue(numberValue(value).getAsDouble()); }
        else if (setting instanceof ColorSetting color) { color.apply(color.decode(value)); }
        else if (setting instanceof dev.riserecode.setting.BlockListSetting blocks) { blocks.setValue(blocks.decode(value)); }
        else if (setting instanceof EnumSetting<?> choice) { loadEnum(choice, value); }
    }
    private static <E extends Enum<E>> E enumValue(EnumSetting<E> setting, JsonElement value) {
        String name = stringValue(value);
        String migrated=name.equals("VANILLA")&&setting.getDefaultValue().getDeclaringClass().getName().equals("dev.riserecode.module.modules.NoFallModule$Mode")?"AUTO":name;
        return setting.getOptions().stream().filter(option -> option.name().equals(migrated)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown option " + name));
    }
    private static <E extends Enum<E>> void loadEnum(EnumSetting<E> setting, JsonElement value) { setting.setValue(enumValue(setting, value)); }
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
}
