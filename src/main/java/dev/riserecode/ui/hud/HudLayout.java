package dev.riserecode.ui.hud;

import com.google.gson.JsonObject;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;

/** Normalized top-left anchors survive resolution changes; scale/alignment/backdrop are per element. */
public final class HudLayout {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Background { INHERIT, NONE, SOLID, BLUR }
    public enum Border { INHERIT, ON, OFF }
    public final EnumSetting<Border> border = new EnumSetting<>("Border", "Use the interface border style", Border.INHERIT);
    public final DoubleSetting scale = DoubleSetting.extended("Scale", "Element scale", 1, 0.5, 3);
    public final EnumSetting<Alignment> alignment = new EnumSetting<>("Alignment", "Text alignment", Alignment.LEFT);
    public final EnumSetting<Background> background = new EnumSetting<>("Background", "Element backdrop", Background.INHERIT);
    public final ColorSetting backgroundColor = new ColorSetting("BackgroundColor", "Element background or inherited HUD theme", 0xFF111820, ColorSetting.Mode.THEME);
    private double x, y;
    public HudLayout(double x, double y) { position(x, y); }
    public double x() { return x; }
    public double y() { return y; }
    public void position(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) { throw new IllegalArgumentException("Non-finite HUD position"); }
        this.x = Math.clamp(x, 0, 1); this.y = Math.clamp(y, 0, 1);
    }
    public JsonObject snapshot() {
        JsonObject object = new JsonObject(); object.addProperty("x", x); object.addProperty("y", y);
        object.addProperty("scale", scale.getValue()); object.addProperty("alignment", alignment.getValue().name());
        object.addProperty("border", border.getValue().name());
        object.addProperty("background", background.getValue().name()); object.add("backgroundColor", backgroundColor.toJson()); return object;
    }
    public static void validate(JsonObject object) {
        for (String key : new String[] {"x", "y", "scale"}) {
            if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) { throw new IllegalArgumentException("Invalid HUD " + key); }
            double value = object.get(key).getAsDouble();
            if (!Double.isFinite(value) || value < (key.equals("scale") ? 0.5 : 0) || value > (key.equals("scale") ? 12 : 1)) { throw new IllegalArgumentException("Out-of-range HUD " + key); }
        }
        for (String key : new String[] {"alignment", "background"}) {
            if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()) { throw new IllegalArgumentException("Invalid HUD " + key); }
        }
        Alignment.valueOf(object.get("alignment").getAsString()); Background.valueOf(object.get("background").getAsString());
        if (object.has("border")) { Border.valueOf(object.get("border").getAsString()); }
        if (object.has("backgroundColor")) { new ColorSetting("BackgroundColor", "", 0xFF111820, ColorSetting.Mode.THEME).decode(object.get("backgroundColor")); }
    }
    public void apply(JsonObject object) {
        validate(object); position(object.get("x").getAsDouble(), object.get("y").getAsDouble());
        scale.setValue(object.get("scale").getAsDouble()); alignment.setValue(Alignment.valueOf(object.get("alignment").getAsString()));
        background.setValue(Background.valueOf(object.get("background").getAsString()));
        if (object.has("backgroundColor")) { backgroundColor.apply(backgroundColor.decode(object.get("backgroundColor"))); } else { backgroundColor.reset(); }
        border.setValue(object.has("border") ? Border.valueOf(object.get("border").getAsString()) : Border.INHERIT);
    }
}
