package dev.riserecode.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete modern packs, without the old Styles category extraction or legacy texture rewriting. */
public record ResourceArchive(Map<String, byte[]> files, JsonObject metadata) {
    public ResourceArchive { files = Collections.unmodifiableMap(new LinkedHashMap<>(files)); metadata = metadata.deepCopy(); }
    public static ResourceArchive read(byte[] zip) throws IOException {
        if (zip.length > 128 * 1024 * 1024) { throw new IOException("Pack too large"); }
        var files = readCentralDirectory(zip);
        if (!files.containsKey("pack.mcmeta")) { throw new IOException("Pack has no metadata"); }
        try {
            var metadata = json(files.get("pack.mcmeta"));
            if (!metadata.has("pack") || !metadata.get("pack").isJsonObject()) { throw new IOException("Invalid pack metadata"); }
            return new ResourceArchive(files, metadata);
        } catch (IllegalArgumentException | IllegalStateException invalid) { throw new IOException("Malformed pack metadata", invalid); }
    }
    private static Map<String, byte[]> readCentralDirectory(byte[] zip) throws IOException {
        // The official PHP generator emits STORED directory entries with data descriptors.
        // ZipInputStream rejects those; the authoritative central directory supports them safely.
        var buffer = java.nio.ByteBuffer.wrap(zip).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int end = -1;
        for (int i = zip.length - 22; i >= Math.max(0, zip.length - 65557); i--) {
            if (buffer.getInt(i) == 0x06054b50 && i + 22 + Short.toUnsignedInt(buffer.getShort(i + 20)) == zip.length) { end = i; break; }
        }
        if (end < 0) { throw new IOException("Missing ZIP central directory"); }
        int count = Short.toUnsignedInt(buffer.getShort(end + 10));
        long offset = Integer.toUnsignedLong(buffer.getInt(end + 16)), size = Integer.toUnsignedLong(buffer.getInt(end + 12));
        if (buffer.getShort(end + 4) != 0 || buffer.getShort(end + 6) != 0 || count != Short.toUnsignedInt(buffer.getShort(end + 8))
                || count > 50000 || offset + size > end) { throw new IOException("Unsupported or invalid ZIP directory"); }
        var result = new LinkedHashMap<String, byte[]>(); long total = 0; int position = (int) offset;
        for (int i = 0; i < count; i++) {
            if (position < 0 || position + 46 > end || buffer.getInt(position) != 0x02014b50) { throw new IOException("Invalid ZIP directory entry"); }
            int flags = Short.toUnsignedInt(buffer.getShort(position + 8)), method = Short.toUnsignedInt(buffer.getShort(position + 10));
            long crc = Integer.toUnsignedLong(buffer.getInt(position + 16)), compressed = Integer.toUnsignedLong(buffer.getInt(position + 20)), length = Integer.toUnsignedLong(buffer.getInt(position + 24));
            int nameLength = Short.toUnsignedInt(buffer.getShort(position + 28)), extra = Short.toUnsignedInt(buffer.getShort(position + 30)), comment = Short.toUnsignedInt(buffer.getShort(position + 32));
            long local = Integer.toUnsignedLong(buffer.getInt(position + 42)); int next = position + 46 + nameLength + extra + comment;
            if (next > end || (flags & 1) != 0 || method != 0 && method != 8 || local + 30 > offset) { throw new IOException("Unsupported ZIP entry"); }
            String name = new String(zip, position + 46, nameLength, (flags & 0x800) != 0 ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName("IBM437"));
            ResourceIo.child(Path.of("pack"), name); position = next;
            if (name.endsWith("/")) { continue; }
            total += length;
            if (length > 64 * 1024 * 1024 || total > 512L * 1024 * 1024 || buffer.getInt((int) local) != 0x04034b50) { throw new IOException("Invalid or oversized ZIP entry"); }
            long payload = local + 30 + Short.toUnsignedInt(buffer.getShort((int) local + 26)) + Short.toUnsignedInt(buffer.getShort((int) local + 28));
            if (payload + compressed > offset || compressed > zip.length) { throw new IOException("Truncated ZIP data"); }
            byte[] bytes;
            if (method == 0) {
                if (compressed != length) { throw new IOException("Invalid STORED ZIP entry"); }
                bytes = java.util.Arrays.copyOfRange(zip, (int) payload, (int) (payload + compressed));
            } else {
                var inflater = new java.util.zip.Inflater(true);
                try (var input = new java.util.zip.InflaterInputStream(new ByteArrayInputStream(zip, (int) payload, (int) compressed), inflater)) {
                    bytes = input.readNBytes((int) length + 1);
                    if (!inflater.finished()) { throw new IOException("Truncated compressed ZIP entry"); }
                } finally { inflater.end(); }
            }
            var checksum = new java.util.zip.CRC32(); checksum.update(bytes);
            if (bytes.length != length || checksum.getValue() != crc) { throw new IOException("ZIP size or CRC mismatch"); }
            if (result.putIfAbsent(name, bytes) != null) { throw new IOException("Duplicate archive path: " + name); }
        }
        return result;
    }
    public Map<String, byte[]> effective(int major, int minor) throws IOException {
        var pack = metadata.getAsJsonObject("pack");
        if (pack.has("min_format") && compare(major, minor, pack.get("min_format"), false) < 0
                || pack.has("max_format") && compare(major, minor, pack.get("max_format"), true) > 0) {
            throw new IOException("Resource pack does not support this Minecraft format");
        }
        var result = new LinkedHashMap<String, byte[]>();
        files.forEach((path, bytes) -> { if (path.startsWith("assets/")) { result.put(path, bytes); } });
        if (metadata.has("overlays")) {
            for (var value : metadata.getAsJsonObject("overlays").getAsJsonArray("entries")) {
                var overlay = value.getAsJsonObject(); String directory = overlay.get("directory").getAsString();
                ResourceIo.child(Path.of("pack"), directory);
                if (!matches(overlay, major, minor)) { continue; }
                String prefix = directory + "/";
                files.forEach((path, bytes) -> { if (path.startsWith(prefix + "assets/")) { result.put(path.substring(prefix.length()), bytes); } });
            }
        }
        return result;
    }
    private static boolean matches(JsonObject overlay, int major, int minor) {
        if (overlay.has("min_format") && compare(major, minor, overlay.get("min_format"), false) < 0
                || overlay.has("max_format") && compare(major, minor, overlay.get("max_format"), true) > 0) { return false; }
        if (!overlay.has("formats")) { return true; }
        var format = overlay.get("formats");
        if (format.isJsonPrimitive()) { return format.getAsInt() == major; }
        if (format.isJsonArray()) { return major >= format.getAsJsonArray().get(0).getAsInt() && major <= format.getAsJsonArray().get(1).getAsInt(); }
        var range = format.getAsJsonObject(); return major >= range.get("min_inclusive").getAsInt() && major <= range.get("max_inclusive").getAsInt();
    }
    private static int compare(int major, int minor, JsonElement format, boolean upper) {
        int expectedMajor = format.isJsonArray() ? format.getAsJsonArray().get(0).getAsInt() : format.getAsInt();
        int expectedMinor = format.isJsonArray() ? format.getAsJsonArray().get(1).getAsInt() : upper ? Integer.MAX_VALUE : 0;
        return major != expectedMajor ? Integer.compare(major, expectedMajor) : Integer.compare(minor, expectedMinor);
    }
    public List<List<String>> combinations() throws IOException {
        var bytes = files.get("Selected Packs.txt"); if (bytes == null) { throw new IOException("Official pack receipt missing"); }
        boolean combined = false; var recipes = new ArrayList<List<String>>();
        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
            String text = line.strip(); if (text.equals("Combined packs:")) { combined = true; continue; }
            if (combined && !text.isEmpty()) {
                var ids = List.of(text.split("\\+"));
                if (ids.size() < 2 || ids.size() > 32 || ids.stream().anyMatch(id -> !id.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,159}"))) { throw new IOException("Invalid official combination receipt"); }
                recipes.add(ids.stream().sorted().toList());
            }
        }
        return List.copyOf(recipes);
    }
    public void verifyReceipt(List<String> requested) throws IOException {
        byte[] receipt = files.get("Selected Packs.txt"); if (receipt == null) { throw new IOException("Missing selection receipt"); }
        var lines = new String(receipt, StandardCharsets.UTF_8).lines().map(String::strip).toList();
        if (!lines.contains("Version: 26.2") || !lines.containsAll(requested)) { throw new IOException("Official pack receipt does not match requested resources: " + requested.stream().filter(id -> !lines.contains(id)).limit(10).toList()); }
    }
    public static JsonObject json(byte[] bytes) { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
}
