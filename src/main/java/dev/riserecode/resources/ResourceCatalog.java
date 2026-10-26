package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The public picker excludes entries marked experimental, including its hidden WIP categories. */
public record ResourceCatalog(String revision, List<ResourceEntry> entries, Map<String, ResourceEntry> byId) {
    public ResourceCatalog { entries = List.copyOf(entries); byId = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(byId)); }
    public static ResourceCatalog read(byte[] bytes) {
        if (bytes.length > 4 * 1024 * 1024) { throw new IllegalArgumentException("Catalog too large"); }
        JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        var entries = new ArrayList<ResourceEntry>();
        walk(root.getAsJsonArray("categories"), "", "", "", entries);
        var ids = new LinkedHashMap<String, ResourceEntry>();
        for (var entry : entries) { if (ids.putIfAbsent(entry.id(), entry) != null) { throw new IllegalArgumentException("Duplicate resource: " + entry.id()); } }
        if (entries.isEmpty() || entries.size() > 3000) { throw new IllegalArgumentException("Invalid catalog size"); }
        return new ResourceCatalog(ResourceIo.hash(bytes), entries, ids);
    }
    private static void walk(JsonArray categories, String parent, String group, String inherited, List<ResourceEntry> result) {
        if (categories == null) { return; }
        for (var value : categories) {
            var category = value.getAsJsonObject(); String label = category.get("category").getAsString();
            String path = parent.isEmpty() ? label : parent + " / " + label;
            String slug = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 -]", "").strip().replaceAll("\\s+", "-");
            String requestGroup = group.isEmpty() ? slug : group + "." + slug;
            String warning = inherited;
            if (category.has("warning")) { warning += " " + plain(category.getAsJsonObject("warning").get("text").getAsString()); }
            if (category.has("packs")) { for (var item : category.getAsJsonArray("packs")) {
                JsonObject pack = item.getAsJsonObject();
                if (pack.has("experiment") && pack.get("experiment").getAsBoolean()) { continue; }
                var conflicts = new ArrayList<String>();
                if (pack.has("incompatible")) { for (var conflict : pack.getAsJsonArray("incompatible")) { conflicts.add(conflict.getAsString()); } }
                result.add(new ResourceEntry(pack.get("name").getAsString(), plain(pack.get("display").getAsString()), path, requestGroup,
                        (plain(pack.get("description").getAsString()) + " " + warning).strip(), conflicts,
                        pack.has("priority") ? pack.get("priority").getAsInt() : 0,
                        pack.has("previewExtension") ? pack.get("previewExtension").getAsString() : "png"));
            } }
            walk(category.has("categories") ? category.getAsJsonArray("categories") : null, path, requestGroup, warning, result);
        }
    }
    public static String plain(String text) {
        return text.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replaceAll("\\s+", " ").strip();
    }
    public List<String> defaults(List<String> ids) {
        // The website places priority tweaks ahead of ordinary rows; preserve catalog order within each tier.
        return entries.stream().filter(e -> ids.contains(e.id())).sorted(java.util.Comparator.comparingInt(ResourceEntry::priority).reversed())
                .map(ResourceEntry::id).toList();
    }
}
