package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline composition. JSON merging is restricted to resources whose loaders combine named entries. */
public final class ResourceComposer {
    public record Patch(List<String> members, Map<String, byte[]> files, List<String> removed, Map<String, List<String>> removedKeys) {
        public Patch { members = List.copyOf(members); files = Map.copyOf(files); removed = List.copyOf(removed); removedKeys = Map.copyOf(removedKeys); }
    }
    private ResourceComposer() { }
    public static void merge(Map<String, byte[]> result, Map<String, byte[]> higher) throws IOException {
        for (var entry : higher.entrySet()) {
            String path = entry.getKey(); byte[] previous = result.get(path), next = entry.getValue();
            if (previous != null && mergesKeys(path)) {
                try {
                    JsonObject merged = ResourceArchive.json(previous), value = ResourceArchive.json(next);
                    value.entrySet().forEach(e -> merged.add(e.getKey(), e.getValue().deepCopy()));
                    next = json(merged);
                } catch (IllegalArgumentException | IllegalStateException invalid) { throw new IOException("Invalid merged resource: " + path, invalid); }
            }
            result.put(path, next);
        }
    }
    public static Patch difference(List<String> members, Map<String, byte[]> baseline, Map<String, byte[]> official) {
        var changed = new LinkedHashMap<String, byte[]>();
        var removedKeys = new LinkedHashMap<String, List<String>>();
        official.forEach((path, bytes) -> {
            if (equivalent(path, bytes, baseline.get(path))) { return; }
            if (baseline.containsKey(path) && mergesKeys(path)) {
                var before = ResourceArchive.json(baseline.get(path)); var after = ResourceArchive.json(bytes); var delta = new JsonObject();
                after.entrySet().forEach(entry -> { if (!entry.getValue().equals(before.get(entry.getKey()))) { delta.add(entry.getKey(), entry.getValue().deepCopy()); } });
                var removed = before.keySet().stream().filter(key -> !after.has(key)).toList();
                if (!removed.isEmpty()) { removedKeys.put(path, removed); }
                changed.put(path, json(delta));
            } else { changed.put(path, bytes); }
        });
        var removed = baseline.keySet().stream().filter(path -> !official.containsKey(path)).sorted().toList();
        return new Patch(members, changed, removed, removedKeys);
    }
    public static void patch(Map<String, byte[]> result, Patch patch) throws IOException {
        patch.removed().forEach(result::remove);
        for (var removal : patch.removedKeys().entrySet()) {
            if (result.containsKey(removal.getKey())) { var json = ResourceArchive.json(result.get(removal.getKey())); removal.getValue().forEach(json::remove); result.put(removal.getKey(), json(json)); }
        }
        merge(result, patch.files());
    }
    public static boolean mergesKeys(String path) { return path.matches("assets/[^/]+/lang/[^/]+\\.json") || path.matches("assets/[^/]+/sounds\\.json"); }
    public static boolean equivalent(String path, byte[] left, byte[] right) {
        if (left == null || right == null) { return left == right; }
        if (Arrays.equals(left, right)) { return true; }
        if (path.endsWith(".json") || path.endsWith(".mcmeta") || path.endsWith(".jem") || path.endsWith(".jpm")) {
            try { return com.google.gson.JsonParser.parseString(new String(left, StandardCharsets.UTF_8))
                    .equals(com.google.gson.JsonParser.parseString(new String(right, StandardCharsets.UTF_8))); }
            catch (RuntimeException ignored) { return false; }
        }
        return false;
    }
    public static JsonObject patchJson(Patch patch, Map<String, String> hashes) {
        var root = new JsonObject(); var members = new JsonArray(); patch.members().forEach(members::add); root.add("members", members);
        var files = new JsonObject(); hashes.forEach(files::addProperty); root.add("files", files);
        var removed = new JsonArray(); patch.removed().forEach(removed::add); root.add("removed", removed);
        var keys = new JsonObject(); patch.removedKeys().forEach((path, names) -> { var array = new JsonArray(); names.forEach(array::add); keys.add(path, array); }); root.add("removedKeys", keys); return root;
    }
    public static byte[] json(JsonElement value) { return value.toString().getBytes(StandardCharsets.UTF_8); }
}
