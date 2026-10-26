package dev.riserecode.config;

import java.util.Locale;

/** Stable machine keys independent from display formatting and profile labels. */
public final class CapabilityIds {
    private CapabilityIds() { }
    public static String segment(String name) {
        String id = name.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (id.isEmpty()) { throw new IllegalArgumentException("Capability ID must contain letters or digits"); }
        return id;
    }
}
