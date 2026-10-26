package dev.riserecode.resources;

import com.google.gson.JsonObject;

/** Reversible document migration; callers back up the source file before persisting its result. */
public final class ResourcesMigration {
    private ResourcesMigration() { }
    public static JsonObject upgrade(JsonObject original) {
        var result = original.deepCopy();
        if (result.has("schemaVersion") && result.get("schemaVersion").getAsInt() > 5) { throw new IllegalArgumentException("Unsupported profile schema"); }
        JsonObject modules = result.has("modules") && result.get("modules").isJsonObject() ? result.getAsJsonObject("modules") : new JsonObject();
        result.add("modules", modules);
        if (!modules.has("Ambience") && result.has("styles") && result.get("styles").isJsonObject()) {
            var styles = result.getAsJsonObject("styles");
            if (styles.has("atmosphere") && styles.get("atmosphere").isJsonObject()) {
                var atmosphere = styles.getAsJsonObject("atmosphere").deepCopy(); var ambience = new JsonObject();
                ambience.addProperty("enabled", atmosphere.has("Sky & Water") && atmosphere.get("Sky & Water").getAsBoolean());
                if (atmosphere.has("Atmosphere Key")) { ambience.add("keybind", atmosphere.get("Atmosphere Key")); }
                if (atmosphere.has("Atmosphere Binding")) { ambience.add("bindMode", atmosphere.get("Atmosphere Binding")); }
                atmosphere.remove("Sky & Water"); atmosphere.remove("Atmosphere Key"); atmosphere.remove("Atmosphere Binding");
                ambience.add("settings", atmosphere); modules.add("Ambience", ambience);
            }
        }
        if (!result.has("resources")) { result.add("resources", ResourcesState.empty().json()); }
        else { ResourcesState.read(result.getAsJsonObject("resources")); }
        result.remove("styles"); result.addProperty("schemaVersion", 5);
        if (result.has("ui") && result.get("ui").isJsonObject()) {
            var ui = result.getAsJsonObject("ui");
            if (ui.has("selectedTab") && ui.get("selectedTab").getAsString().equals("STYLES")) { ui.addProperty("selectedTab", "RESOURCES"); }
        }
        return result;
    }
}
