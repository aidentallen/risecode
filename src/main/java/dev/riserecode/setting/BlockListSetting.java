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
    public enum Kind { BLOCK, ENTITY }
    private Kind kind = Kind.BLOCK;
    /** Lets the same picker choose entity types instead of blocks. */
    public BlockListSetting kind(Kind value) { kind = value; return this; }
    public Kind kind() { return kind; }
    private java.util.function.Supplier<List<String>> candidates;
    /** Restricts what the picker offers (e.g. only storage blocks); already selected ids always stay visible. */
    public BlockListSetting candidates(java.util.function.Supplier<List<String>> supplier) { candidates = supplier; return this; }
    public List<String> candidates() { return candidates == null ? null : candidates.get(); }
    private StringSetting colors;
    /** Per-block colors live in a companion text setting ("id=AARRGGBB,..."), so profiles keep them. */
    public BlockListSetting colors(StringSetting setting) { colors = setting; return this; }
    public boolean hasColors() { return colors != null; }
    public java.util.Map<String, Integer> colorMap() {
        var result = new java.util.HashMap<String, Integer>();
        if (colors == null) { return result; }
        for (String entry : colors.getValue().split(",")) {
            int eq = entry.indexOf('=');
            if (eq < 1) { continue; }
            try { result.put(entry.substring(0, eq).strip(), ColorSetting.parseHex(entry.substring(eq + 1))); } catch (RuntimeException ignored) { }
        }
        return result;
    }
    public void setColor(String id, Integer argb) {
        if (colors == null) { return; }
        var map = new java.util.TreeMap<>(colorMap());
        if (argb == null) { map.remove(id); } else { map.put(id, argb); }
        var text = new StringBuilder();
        map.forEach((key, value) -> { if (text.length() > 0) { text.append(','); } text.append(key).append('=').append(ColorSetting.hex(value)); });
        if (text.length() <= colors.maximum()) { colors.setValue(text.toString()); }
    }
    public void toggle(String id) {
        var values = new java.util.ArrayList<>(getValue());
        if (!values.remove(id)) { values.add(id); }
        setValue(values);
    }
}
