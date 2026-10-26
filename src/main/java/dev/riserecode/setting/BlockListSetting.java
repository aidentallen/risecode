package dev.riserecode.setting;

import com.google.gson.JsonElement;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Immutable registry identifiers; unavailable mod blocks remain in profiles. */
public final class BlockListSetting extends Setting<List<String>> {
    public BlockListSetting(String name, String description, List<String> defaults) {
        super(name, description, normalize(defaults));
    }
    private static List<String> normalize(List<String> values) {
        if (values.size() > 4096) { throw new IllegalArgumentException("Too many block identifiers"); }
        return values.stream().map(value -> Identifier.parse(value).toString()).distinct().sorted().toList();
    }
    @Override protected List<String> validate(List<String> values) { return normalize(values); }
    public List<String> decode(JsonElement json) {
        if (!json.isJsonArray()) { throw new IllegalArgumentException("Blocks must be an array"); }
        var values = new java.util.ArrayList<String>();
        for (JsonElement value : json.getAsJsonArray()) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) { throw new IllegalArgumentException("Block id must be a string"); }
            values.add(value.getAsString());
        }
        return normalize(values);
    }
    public void toggle(String id) {
        var values = new java.util.ArrayList<>(getValue());
        if (!values.remove(id)) { values.add(id); }
        setValue(values);
    }
}
