package dev.riserecode.ui.theme;

import com.google.gson.JsonObject;
import java.util.Objects;

/** Versioned visual snapshot. Neither callers nor Gson can mutate a live profile's settings. */
public record ThemeProfile(int schemaVersion, JsonObject settings) {
    public static final int VERSION = 1;
    public ThemeProfile {
        if (schemaVersion != VERSION) { throw new IllegalArgumentException("Unsupported theme schema " + schemaVersion); }
        settings = Objects.requireNonNull(settings, "settings").deepCopy();
    }
    @Override public JsonObject settings() { return settings.deepCopy(); }
}
