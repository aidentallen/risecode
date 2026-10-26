package dev.riserecode.resources;

import java.util.List;

/** Official names are persisted; display labels and categories are never resource identifiers. */
public record ResourceEntry(String id, String name, String category, String group, String description,
        List<String> incompatible, int priority, String previewExtension) {
    public ResourceEntry {
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,159}") || name.isBlank() || category.isBlank() || group.isBlank()) {
            throw new IllegalArgumentException("Invalid resource catalog entry");
        }
        incompatible = List.copyOf(incompatible);
        if (!List.of("png", "gif", "jpg", "webp").contains(previewExtension)) { previewExtension = "png"; }
    }
    public String searchText() { return (name + " " + category + " " + description).toLowerCase(java.util.Locale.ROOT); }
}
