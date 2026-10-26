package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Top-first priority and a pinned cache revision are part of each existing client profile. */
public record ResourcesState(String revision, List<String> selected) {
    public static final String MINECRAFT = "26.2";
    public ResourcesState {
        selected = List.copyOf(selected);
        if (!revision.isEmpty() && !revision.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException("Invalid resource revision"); }
        if (selected.size() > 3000 || new HashSet<>(selected).size() != selected.size()
                || selected.stream().anyMatch(id -> !id.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,159}"))
                || !selected.isEmpty() && revision.isEmpty()) { throw new IllegalArgumentException("Invalid resource selection"); }
    }
    public static ResourcesState empty() { return new ResourcesState("", List.of()); }
    public JsonObject json() {
        var root = new JsonObject(); root.addProperty("version", 1); root.addProperty("minecraft", MINECRAFT);
        root.addProperty("revision", revision); var ids = new JsonArray(); selected.forEach(ids::add); root.add("selected", ids); return root;
    }
    public static ResourcesState read(JsonObject root) {
        if (!root.has("version") || root.get("version").getAsInt() != 1 || !MINECRAFT.equals(root.get("minecraft").getAsString())) {
            throw new IllegalArgumentException("Unsupported Resources state");
        }
        var ids = new ArrayList<String>(); for (var value : root.getAsJsonArray("selected")) { ids.add(value.getAsString()); }
        return new ResourcesState(root.get("revision").getAsString(), ids);
    }
}
